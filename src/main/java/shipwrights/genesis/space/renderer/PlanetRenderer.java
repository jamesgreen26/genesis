package shipwrights.genesis.space.renderer;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.*;
import org.lwjgl.opengl.GL11;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.PlanetTextures;
import shipwrights.genesis.client.ShaderRegistry;
import shipwrights.genesis.client.shading.FaceShadow;
import shipwrights.genesis.client.shading.ShadowProjection;
import shipwrights.genesis.client.shading.ShadowRenderer;
import shipwrights.genesis.math.AAPlane;
import shipwrights.genesis.math.OBB;
import shipwrights.genesis.mixin.FogRendererAccessor;
import shipwrights.genesis.mixin.LevelRendererAccessor;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.VantagePoint;
import shipwrights.genesis.space.type.CelestialType;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;

import static shipwrights.genesis.client.ShaderRegistry.getPlanetShadowRenderType;
import static shipwrights.genesis.client.ShaderRegistry.getTexturedPlanetRenderType;

public class PlanetRenderer implements CelestialRenderer {

    private static final boolean USE_TEST_SHADOWS = false; // Set to false to use real shadows

    @Override
    public void teardown(@NotNull RenderLevelStageEvent event, @NotNull VantagePoint vantagePoint) {
        CelestialRenderer.super.teardown(event, vantagePoint);

        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
    }

    @Override
    public void invoke(@NotNull RenderLevelStageEvent event, @NotNull Celestial toRender, @NotNull VantagePoint vantagePoint) {
        ClientLevel level = ((LevelRendererAccessor)event.getLevelRenderer()).getLevel();
        long ticks = GenesisMod.getTicks(level);
        float partialTick = GenesisMod.getPartialTick(level, event);

        Vector3dc position = toRender.getPosition(ticks, partialTick);
        Quaterniondc rotation = toRender.getRotation(ticks, partialTick);
        double halfExtent = toRender.getActualSize() / 2;
        float alpha = 1f;

        List<FaceShadow> shadows;
        if (USE_TEST_SHADOWS) {
            shadows = createTestShadows(halfExtent);
        } else {
            // Get all the data needed for shadow computation
            OBB selfOBB = toRender.getOBB(ticks, partialTick);
            List<Celestial> allCelestials = GenesisMod.SPACE_REGISTRY.getWhere(CelestialType::castsShadow).stream().filter(it -> !it.equals(toRender)).toList();
            List<OBB> otherOBBs = allCelestials.stream().map(it -> it.getOBB(ticks, partialTick)).toList();
            Vector3dc starPosition = toRender.getNearestStar(ticks, partialTick).getPosition(ticks, partialTick);
            Vector3d lightDir = new Vector3d(position).sub(starPosition);
            ShaderInstance shader = ShaderRegistry.PLANET_TEXTURED_SHADER.getInstance().get();
            shader.safeGetUniform("LightDirection").set((float) lightDir.x, (float) lightDir.y, (float) lightDir.z);

            shadows = ShadowProjection.computeShadows(selfOBB, otherOBBs, starPosition);
        }

        // Special case: if rendering the vantage point itself, lock it at a fixed position in world space
        if (vantagePoint instanceof VantagePoint.OnCelestial oc && oc.celestial().equals(toRender)) {
            var camera = event.getCamera();
            halfExtent = Minecraft.getInstance().gameRenderer.getRenderDistance();
            position = new Vector3d(
                0,
                - (camera.getPosition().y / 16) - halfExtent - 64,
                0
            );
            rotation = oc.cameraRotationFromNorthPole();
            int buildHeight = level.getMaxBuildHeight();
            float alphaInterpolateStart = (float) (halfExtent + buildHeight / 3f);
            float alphaInterpolateEnd = (float) (halfExtent + buildHeight);

            float cameraY = (float) camera.getPosition().y;
            alpha = Math.max(0f, Math.min(1f, (cameraY - alphaInterpolateStart) / (alphaInterpolateEnd - alphaInterpolateStart)));
        }
        // Transform by inverse of vantage point
        else {
            Vector3dc vantagePos = vantagePoint.getPosition();
            Quaterniondc vantageRot = vantagePoint.getRotation();

            // Calculate relative position (subtract vantage point position)
            Vector3d relativePos = new Vector3d(
                position.x() - vantagePos.x(),
                position.y() - vantagePos.y(),
                position.z() - vantagePos.z()
            );

            Quaterniond planetRotation = new Quaterniond();

            // Apply inverse rotation of vantage point
            Quaterniond inverseVantageRot = planetRotation.premul(vantageRot).conjugate();
            inverseVantageRot.transform(relativePos);
            position = relativePos;

            // Apply inverse rotation to the celestial's own rotation
            rotation = new Quaterniond(inverseVantageRot).mul(new Quaterniond(rotation));
        }

        renderPlanetAt(toRender.getID(), shadows, event.getPoseStack(), position.x(), position.y(), position.z(), halfExtent, rotation, alpha);

        new PlanetAtmosphereRenderer().invoke(event, toRender, vantagePoint);
    }

    private void renderPlanetAt(ResourceLocation planetID, List<FaceShadow> shadows, PoseStack poseStack, double x, double y, double z, double halfExtent, Quaterniondc localRotation, float alpha) {
        // Get the texture for this planet
        ResourceLocation textureLocation = PlanetTextures.getTexture(planetID);
        if (textureLocation == null) {
            return; // No texture available
        }

        // Set up buffer source
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        var renderType = getTexturedPlanetRenderType(textureLocation);
        VertexConsumer buffer = bufferSource.getBuffer(renderType);

        // Clone the matrix
        Matrix4f matrix;
        try {
            matrix = (Matrix4f) poseStack.last().pose().clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        // Apply translation
        matrix.translate((float) x, (float) y, (float) z);

        // Apply rotation
        Quaternionf rotation = new Quaternionf(localRotation);
        matrix.rotate(rotation);

        float halfSize = (float) halfExtent;

        // UV layout (3x2 grid):
        // | north (0,0)     | west (1/3,0)   | south (2/3,0)  |
        // | east (0,0.5)    | down (1/3,0.5) | up (2/3,0.5)   |
        float third = 1.0f / 3.0f;
        float twoThirds = 2.0f / 3.0f;

        addTexturedCubeFace(matrix, buffer, halfSize, new Vector3f(0.0f, 0.0f, 1.0f), rotation, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, twoThirds, 0.0f, 1.0f, 0.5f, alpha);        // South face (+Z)
        addTexturedCubeFace(matrix, buffer, halfSize, new Vector3f(0.0f, 0.0f, -1.0f), rotation, halfSize, -halfSize, -halfSize, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, 0.0f, 0.0f, third, 0.5f, alpha);        // North face (-Z)
        addTexturedCubeFace(matrix, buffer, halfSize, new Vector3f(-1.0f, 0.0f, 0.0f), rotation, -halfSize, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize, third, 0.0f, twoThirds, 0.5f, alpha);       // West face (-X)
        addTexturedCubeFace(matrix, buffer, halfSize, new Vector3f(1.0f, 0.0f, 0.0f), rotation, halfSize, -halfSize, halfSize, halfSize, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, 0.0f, 0.5f, third, 1.0f, alpha);            // East face (+X)
        addTexturedCubeFace(matrix, buffer, halfSize, new Vector3f(0.0f, -1.0f, 0.0f), rotation, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize, third, 0.5f, twoThirds, 1.0f, alpha);       // Down face (-Y)
        addTexturedCubeFace(matrix, buffer, halfSize, new Vector3f(0.0f, 1.0f, 0.0f), rotation, -halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, -halfSize, halfSize, -halfSize, twoThirds, 0.5f, 1.0f, 1.0f, alpha);        // Up face (+Y)

        // End batch to flush planet rendering
        bufferSource.endBatch(renderType);

        // Render shadows on planet faces
        renderShadows(shadows, poseStack, x, y, z, halfExtent, localRotation);
    }

    private static void addTexturedCubeFace(Matrix4f matrix, VertexConsumer buffer, float halfSize,
                                            Vector3f normal, Quaternionf rotation,
                                            float x1, float y1, float z1,
                                            float x2, float y2, float z2,
                                            float x3, float y3, float z3,
                                            float x4, float y4, float z4,
                                            float u1, float v1, float u2, float v2, float alpha) {
        addTexturedVertexWithLighting(matrix, buffer, x1, y1, z1, u1, v2, normal, rotation, alpha);
        addTexturedVertexWithLighting(matrix, buffer, x2, y2, z2, u2, v2, normal, rotation, alpha);
        addTexturedVertexWithLighting(matrix, buffer, x3, y3, z3, u2, v1, normal, rotation, alpha);
        addTexturedVertexWithLighting(matrix, buffer, x4, y4, z4, u1, v1, normal, rotation, alpha);
    }

    private static void addTexturedVertexWithLighting(Matrix4f matrix, VertexConsumer buffer,
                                                      float x, float y, float z,
                                                      float u, float v,
                                                      Vector3f normal, Quaternionf rotation, float alpha) {
        // Pass fog color in RGB and alpha in A for shader interpolation
        int fogRed = (int) (255 * FogRendererAccessor.getFogRed());
        int fogGreen = (int) (255 * FogRendererAccessor.getFogGreen());
        int fogBlue = (int) (255 * FogRendererAccessor.getFogBlue());

        Vector3f rotatedNormal = new Vector3f(x,y,z);
        rotatedNormal = rotation.transform(rotatedNormal.normalize());

        buffer.vertex(matrix, x, y, z)
            .uv(u, v)
            .color(fogRed, fogGreen, fogBlue, (int)(alpha * 255))
            .normal(rotatedNormal.x, rotatedNormal.y, rotatedNormal.z)
            .endVertex();
    }

    private void renderShadows(List<FaceShadow> shadows, PoseStack poseStack, double x, double y, double z, double halfExtent, Quaterniondc localRotation) {
        if (shadows == null || shadows.isEmpty()) {
            return;
        }

        // Render each shadow in its own batch so we can upload
        // per-shadow projection vertices to the shader.
        for (FaceShadow shadow : shadows) {
            MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            VertexConsumer shadowBuffer = bufferSource.getBuffer(getPlanetShadowRenderType());

            // Clone and transform matrix (same as planet rendering)
            Matrix4f matrix;
            try {
                matrix = (Matrix4f) poseStack.last().pose().clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }

            matrix.translate((float) x, (float) y, (float) z);
            matrix.rotate(new Quaternionf(localRotation));

            // Compute up to 8 vertices for this shadow from the projected
            // polygon, transform them with the same matrix used for
            // rendering, and upload them as uniforms. This keeps the
            // shader's ShadowVertexN positions in the same space as the
            // Position/localPos attribute.
            ShaderInstance shader = ShaderRegistry.PLANET_SHADOW_SHADER.getInstance().get();
            if (shader != null) {
                List<Vector2dc> polygon = shadow.polygon();
                AAPlane plane = shadow.plane();

                int count = Math.min(polygon.size(), 8);

                Uniform countUniform = shader.getUniform("ShadowVertexCount");
                if (countUniform != null) {
                    countUniform.set((float) count);
                }

                for (int i = 0; i < 8; i++) {
                    Uniform u = shader.getUniform("ShadowVertex[" + i + "]");
                    if (u != null) {
                        if (i < count) {
                        // Start from local-space projection and apply
                        // the same z-fighting offset and matrix that
                        // the geometry uses.
                        Vector3d v3d = ShadowRenderer.applyZFightingOffset(
                            ShadowRenderer.convertPlaneToLocal3D(polygon.get(i), plane),
                            plane,
                            halfExtent
                        );

                        Vector3f transformed = new Vector3f(
                            (float) v3d.x,
                            (float) v3d.y,
                            (float) v3d.z
                        );
                        matrix.transformPosition(transformed);

                        u.set(transformed.x, transformed.y, transformed.z);
                        } else {
                            u.set(0.0F, 0.0F, 0.0F);
                        }
                    }
                }
            }

            ShadowRenderer.renderShadow(shadow, matrix, shadowBuffer, halfExtent);

            // Flush this shadow's batch so its uniforms apply only to it
            bufferSource.endBatch(getPlanetShadowRenderType());
        }
    }

    /**
     * Creates test shadows for debugging the rendering pipeline.
     * Generates simple square shadows on each face of the cube.
     */
    private static List<FaceShadow> createTestShadows(double halfExtent) {
        List<FaceShadow> testShadows = new ArrayList<>();

        // Create a square shadow on the +Y face (top face) - TESTING IF Y AXIS WORKS
        AAPlane topPlane = new AAPlane(new Vector3i(0, 1, 0), halfExtent);
        List<Vector2dc> topSquare = List.of(
                new Vector2d(-halfExtent * 0.3, -halfExtent * 0.3),
                new Vector2d(halfExtent * 0.3, -halfExtent * 0.3),
                new Vector2d(halfExtent * 0.3, halfExtent * 0.3),
                new Vector2d(-halfExtent * 0.3, halfExtent * 0.3)
        );
        testShadows.add(new FaceShadow(topPlane, topSquare));

        // Create a triangle shadow on the +Z face (front face)
        // Note: Vertex order matters for face culling
        AAPlane frontPlane = new AAPlane(new Vector3i(0, 0, 1), halfExtent);
        List<Vector2dc> frontTriangle = List.of(
                new Vector2d(0, -halfExtent * 0.4),
                new Vector2d(halfExtent * 0.4, halfExtent * 0.4),
                new Vector2d(-halfExtent * 0.4, halfExtent * 0.4)
        );
        testShadows.add(new FaceShadow(frontPlane, frontTriangle));

        // Create a pentagon shadow on the +X face (right face)
        AAPlane rightPlane = new AAPlane(new Vector3i(1, 0, 0), halfExtent);
        List<Vector2dc> rightPentagon = List.of(
                new Vector2d(0, -halfExtent * 0.4),
                new Vector2d(halfExtent * 0.3, -halfExtent * 0.2),
                new Vector2d(halfExtent * 0.3, halfExtent * 0.2),
                new Vector2d(0, halfExtent * 0.4),
                new Vector2d(-halfExtent * 0.3, 0)
        );
        testShadows.add(new FaceShadow(rightPlane, rightPentagon));

        // Create a hexagon shadow on the -Y face (bottom face)
        AAPlane bottomPlane = new AAPlane(new Vector3i(0, -1, 0), -halfExtent);
        List<Vector2dc> bottomHexagon = List.of(
                new Vector2d(halfExtent * 0.3, 0),
                new Vector2d(halfExtent * 0.15, halfExtent * 0.3),
                new Vector2d(-halfExtent * 0.15, halfExtent * 0.3),
                new Vector2d(-halfExtent * 0.3, 0),
                new Vector2d(-halfExtent * 0.15, -halfExtent * 0.3),
                new Vector2d(halfExtent * 0.15, -halfExtent * 0.3)
        );
        testShadows.add(new FaceShadow(bottomPlane, bottomHexagon));

        // Create a diamond shadow on the -Z face (back face)
        AAPlane backPlane = new AAPlane(new Vector3i(0, 0, -1), -halfExtent);
        List<Vector2dc> backDiamond = List.of(
                new Vector2d(0, -halfExtent * 0.4),
                new Vector2d(halfExtent * 0.3, 0),
                new Vector2d(0, halfExtent * 0.4),
                new Vector2d(-halfExtent * 0.3, 0)
        );
        testShadows.add(new FaceShadow(backPlane, backDiamond));

        // Create a rectangle shadow on the -X face (left face)
        AAPlane leftPlane = new AAPlane(new Vector3i(-1, 0, 0), -halfExtent);
        List<Vector2dc> leftRectangle = List.of(
                new Vector2d(-halfExtent * 0.2, -halfExtent * 0.4),
                new Vector2d(halfExtent * 0.2, -halfExtent * 0.4),
                new Vector2d(halfExtent * 0.2, halfExtent * 0.4),
                new Vector2d(-halfExtent * 0.2, halfExtent * 0.4)
        );
        testShadows.add(new FaceShadow(leftPlane, leftRectangle));

        return testShadows;
    }
}
