package shipwrights.genesis.client;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.*;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL11C;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.OrbitingBody;
import team.lodestar.lodestone.helpers.RenderHelper;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;

import static shipwrights.genesis.client.ShaderRegistry.*;
import static shipwrights.genesis.client.SunRenderer.renderSun;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class PlanetRenderer {


    public static Matrix3f inverseViewMatrix = new Matrix3f();

    private static void setUniforms(ShaderInstance shader, Vec3 camPos, List<OrbitingBody> allPlanets,
            long ticks, float partialTick) {
        Uniform cameraPosUniform = shader.getUniform("CameraPos");
        if (cameraPosUniform != null) {
            cameraPosUniform.set((float) camPos.x, (float) camPos.y, (float) camPos.z);
        }
        for (int planetIdx = 0; planetIdx < allPlanets.size(); planetIdx++) {
            OrbitingBody planet = allPlanets.get(planetIdx);
            Vector3d pos = planet.getCurrentPos(ticks, partialTick);
            // Vector3d pos = new Vector3d(2000f + (float)planet.getActualSize() * (planetIdx + 1), 0f, 0f);
            Uniform planetPosUniform = shader.getUniform("planetPos[" + planetIdx + "]");
            if (planetPosUniform != null) {
                // planetPosUniform.set((float) pos.x, (float) pos.y, (float) pos.z);
                // in view Pos for precision reasons (?)
                planetPosUniform.set((float) (pos.x - camPos.x), (float) (pos.y - camPos.y),
                        (float) (pos.z - camPos.z));
            }
            Uniform planetRotUniform = shader.getUniform("planetRot[" + planetIdx + "]");
            if (planetRotUniform != null) {
                //todo: make this work with quats in shader
                Vector3d planetRot = planet.getRotation(ticks, partialTick).getEulerAnglesXYZ(new Vector3d());
                planetRotUniform.set((float) planetRot.x,
                        (float) planetRot.y,
                        (float) planetRot.z);
            }
            Uniform planetRoundednessUniform = shader.getUniform("planetRoundedness[" + planetIdx + "]");
            if (planetRoundednessUniform != null) {
                planetRoundednessUniform.set(0.f);
            }
            float halfSize = (float) planet.getActualSize() / 2;
            Uniform planetHalfSizeUniform = shader.getUniform("planetHalfSize[" + planetIdx + "]");
            if (planetHalfSizeUniform != null) {
                planetHalfSizeUniform.set(halfSize, halfSize, halfSize);
            }
        }

        Uniform inverseViewMatrixUniform = shader.getUniform("InverseViewMatrix");
        if (inverseViewMatrixUniform != null) {
            inverseViewMatrixUniform.set(inverseViewMatrix);
        }

        for (int i = 0; i < 3; i++) {
            Uniform sunPosUniform = shader.getUniform("sunPos[" + i + "]");
            if (sunPosUniform != null) {
                // sunPosUniform.set(0f, 0f, 0f);
                sunPosUniform.set((float) -camPos.x, (float) -camPos.y, (float) -camPos.z);
            }
            Uniform sunColUniform = shader.getUniform("sunCol[" + i + "]");
            if (sunColUniform != null) {
                sunColUniform.set(1f, 1f, 1f);
            }
            Uniform sunRotUniform = shader.getUniform("sunRot[" + i + "]");
            if (sunRotUniform != null) {
                sunRotUniform.set(0f, 0f, 0f);
            }
            Uniform sunHalfSizeUniform = shader.getUniform("sunHalfSize[" + i + "]");
            if (sunHalfSizeUniform != null) {
                sunHalfSizeUniform.set(720.f, 720.f, 720.f);
            }
            Uniform sunRoundednessUniform = shader.getUniform("sunRoundedness[" + i + "]");
            if (sunRoundednessUniform != null) {
                sunRoundednessUniform.set(0.f);
            }
        }

    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;

        Level level = Minecraft.getInstance().level;
        if (level == null || !GenesisMod.isSpaceDimension(level)) {
            return;
        }
        renderSpaceScene(
                level,
                event.getCamera(),
                event.getPoseStack(),
                event.getPartialTick()
        );
    }


    public static void renderSpaceScene(Level level, Camera camera, PoseStack poseStack, float partialTick) {

        long ticks = GenesisMod.getTicks(level);

        // Separate planets into textured and non-textured
        List<OrbitingBody> proceduralPlanets = new ArrayList<>();
        List<OrbitingBody> texturedPlanets = new ArrayList<>();

        for (var body : GenesisMod.SPACE_REGISTRY.getAllOrbitingBodies()) {
            if (PlanetTextures.hasTexture(body.getID())) {
                texturedPlanets.add(body);
            } else {
                proceduralPlanets.add(body);
            }
        }

        List<OrbitingBody> allPlanets = new ArrayList<>();
        allPlanets.addAll(proceduralPlanets);
        allPlanets.addAll(texturedPlanets);

        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(GL11C.GL_LEQUAL);
        RenderSystem.enableCull();

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

        Vec3 camPos = camera.getPosition();

        // Render non-textured planets with procedural shader
        if (!proceduralPlanets.isEmpty()) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(true);
            var renderType = getRaymarchProceduralPlanetRenderType();
            ShaderInstance shader = RenderHelper.getShader(renderType);
            setUniforms(shader, camPos, allPlanets, ticks, partialTick);

            VertexConsumer planetBuffer = bufferSource.getBuffer(renderType);
            for (var planet : proceduralPlanets) {
                Uniform vPlanetIndexUniform = shader.getUniform("vPlanetIndex");
                if (vPlanetIndexUniform != null) {
                    vPlanetIndexUniform.set(allPlanets.indexOf(planet));
                }

                // for procedural planets
                Uniform textureScaleUniform = shader.getUniform("textureScale");
                if (textureScaleUniform != null) {
                    int textureScale = planet.getID().hashCode() % 256;
                    textureScaleUniform.set((float) textureScale);
                }
                renderProceduralPlanet(camera, poseStack, planet, planetBuffer, ticks, partialTick);
            }
            bufferSource.endBatch(renderType);
        }

        // Render each planet cube
        for (var planet : texturedPlanets) {
            ResourceLocation textureLocation = PlanetTextures.getTexture(planet.getID());
            if (textureLocation != null) {
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glDepthFunc(GL11.GL_LEQUAL);
                GL11.glDepthMask(true);
                var renderType = getRaymarchTexturedPlanetRenderType(textureLocation);
                ShaderInstance shader = RenderHelper.getShader(renderType);
                setUniforms(shader, camPos, allPlanets, ticks, partialTick);
                Uniform vPlanetIndexUniform = shader.getUniform("vPlanetIndex");
                if (vPlanetIndexUniform != null) {
                    vPlanetIndexUniform.set(allPlanets.indexOf(planet));
                }
                VertexConsumer texturedBuffer = bufferSource.getBuffer(renderType);
                renderTexturedPlanet(camera, poseStack, planet, texturedBuffer, ticks, partialTick);
                bufferSource.endBatch(renderType);
            }
        }

        // Render sun
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glDepthMask(true);

        VertexConsumer sunBuffer = bufferSource.getBuffer(getSunRenderType());
        renderSun(camera.getPosition(), poseStack, sunBuffer, 1440, new Vector3d(), new Quaterniond());
        bufferSource.endBatch(getSunRenderType());

        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }

    private static void renderProceduralPlanet(Camera camera, PoseStack poseStack, OrbitingBody data, VertexConsumer buffer,
            long ticks, float partialTick) {
        Matrix4f matrix;
        try {
            matrix = (Matrix4f) poseStack.last().pose().clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        Vector3d pos = data.getCurrentPos(ticks, partialTick);

        matrix.translate((float) (pos.x - camera.getPosition().x),
                (float) (pos.y - camera.getPosition().y),
                (float) (pos.z - camera.getPosition().z));
        Quaternionf rotation = new Quaternionf(data.getRotation(ticks, partialTick));
        matrix.rotate(rotation);

        float halfSize = (float) (data.getActualSize() / 2);

        int textureScale = data.getID().hashCode() % 256;
        float color = packColor(data.r(), data.g(), data.b());

        addCubeFacePlanet(matrix, buffer, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize,
                halfSize, halfSize, -halfSize, halfSize, halfSize, textureScale, color, pos, rotation,
                new Vector3d(0, 0, 1));
        addCubeFacePlanet(matrix, buffer, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize,
                halfSize, -halfSize, halfSize, -halfSize, -halfSize, textureScale, color, pos, rotation,
                new Vector3d(0, 0, -1));
        addCubeFacePlanet(matrix, buffer, -halfSize, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize,
                halfSize, halfSize, -halfSize, halfSize, -halfSize, textureScale, color, pos, rotation,
                new Vector3d(-1, 0, 0));
        addCubeFacePlanet(matrix, buffer, halfSize, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize,
                halfSize, halfSize, halfSize, -halfSize, halfSize, textureScale, color, pos, rotation,
                new Vector3d(1, 0, 0));
        addCubeFacePlanet(matrix, buffer, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize,
                -halfSize, halfSize, -halfSize, -halfSize, halfSize, textureScale, color, pos, rotation,
                new Vector3d(0, -1, 0));
        addCubeFacePlanet(matrix, buffer, -halfSize, halfSize, -halfSize, -halfSize, halfSize, halfSize, halfSize,
                halfSize, halfSize, halfSize, halfSize, -halfSize, textureScale, color, pos, rotation,
                new Vector3d(0, 1, 0));
    }

    private static void renderTexturedPlanet(Camera camera, PoseStack poseStack, OrbitingBody data, VertexConsumer buffer,
            long ticks, float partialTick) {
        Matrix4f matrix;
        try {
            matrix = (Matrix4f) poseStack.last().pose().clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        Vector3d pos = data.getCurrentPos(ticks, partialTick);

        matrix.translate((float) (pos.x - camera.getPosition().x),
                (float) (pos.y - camera.getPosition().y),
                (float) (pos.z - camera.getPosition().z));

        Quaternionf rotation = new Quaternionf(data.getRotation(ticks, partialTick));
        matrix.rotate(rotation);

        float halfSize = (float) (data.getActualSize() / 2);


        // UV layout (3x2 grid):
        // | north (0,0) | west (1/3,0) | south (2/3,0) |
        // | east (0,0.5) | down (1/3,0.5) | up (2/3,0.5) |
        float third = 1.0f / 3.0f;
        float twoThirds = 2.0f / 3.0f;

        // South face (+Z)
        addTexturedCubeFace(matrix, buffer, halfSize, rotation,
                new Vector3d(0, 0, 1),
                -halfSize, -halfSize, halfSize,
                halfSize, -halfSize, halfSize,
                halfSize, halfSize, halfSize,
                -halfSize, halfSize, halfSize,
                twoThirds, 0.0f, 1.0f, 0.5f);

        // North face (-Z)
        addTexturedCubeFace(matrix, buffer, halfSize, rotation,
                new Vector3d(0, 0, -1),
                halfSize, -halfSize, -halfSize,
                -halfSize, -halfSize, -halfSize,
                -halfSize, halfSize, -halfSize,
                halfSize, halfSize, -halfSize,
                0.0f, 0.0f, third, 0.5f);

        // West face (-X)
        addTexturedCubeFace(matrix, buffer, halfSize, rotation,
                new Vector3d(-1, 0, 0),
                -halfSize, -halfSize, -halfSize,
                -halfSize, -halfSize, halfSize,
                -halfSize, halfSize, halfSize,
                -halfSize, halfSize, -halfSize,
                third, 0.0f, twoThirds, 0.5f);

        // East face (+X)
        addTexturedCubeFace(matrix, buffer, halfSize, rotation,
                new Vector3d(1, 0, 0),
                halfSize, -halfSize, halfSize,
                halfSize, -halfSize, -halfSize,
                halfSize, halfSize, -halfSize,
                halfSize, halfSize, halfSize,
                0.0f, 0.5f, third, 1.0f);

        // Down face (-Y)
        addTexturedCubeFace(matrix, buffer, halfSize, rotation,
                new Vector3d(0, -1, 0),
                -halfSize, -halfSize, -halfSize,
                halfSize, -halfSize, -halfSize,
                halfSize, -halfSize, halfSize,
                -halfSize, -halfSize, halfSize,
                third, 0.5f, twoThirds, 1.0f);

        // Up face (+Y)
        addTexturedCubeFace(matrix, buffer, halfSize, rotation,
                new Vector3d(0, 1, 0),
                -halfSize, halfSize, halfSize,
                halfSize, halfSize, halfSize,
                halfSize, halfSize, -halfSize,
                -halfSize, halfSize, -halfSize,
                twoThirds, 0.5f, 1.0f, 1.0f);
    }

    private static void addTexturedCubeFace(Matrix4f matrix, VertexConsumer buffer, float halfSize, Quaternionf rotation, Vector3d normal,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            float u1, float v1, float u2, float v2) {
        addTexturedVertex(matrix, buffer, x1, y1, z1, u1, v2, rotation, normal);
        addTexturedVertex(matrix, buffer, x2, y2, z2, u2, v2, rotation, normal);
        addTexturedVertex(matrix, buffer, x3, y3, z3, u2, v1, rotation, normal);
        addTexturedVertex(matrix, buffer, x4, y4, z4, u1, v1, rotation, normal);
    }

    private static void addTexturedVertex(Matrix4f matrix, VertexConsumer buffer,
            float x, float y, float z,
            float u, float v,
            Quaternionf rotation, Vector3d normal) {
        Vector3d rotatedNormal = new Vector3d(normal);
        rotation.transform(rotatedNormal);

        buffer.vertex(matrix, x, y, z)
                .uv(u, v)
                .color(255, 255, 255, 255) // Placeholder; actual lighting to be handled in shader
                .normal((float) rotatedNormal.x, (float) rotatedNormal.y, (float) rotatedNormal.z)
                .endVertex();
    }

    private static void addCubeFacePlanet(Matrix4f matrix, VertexConsumer buffer, float x1, float y1, float z1,
            float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, int textureScale,
            float color, Vector3d planetPos, Quaternionf rotation, Vector3d normal) {
        int[] rgb = unpackColor(color);
        int r = rgb[0], g = rgb[1], b = rgb[2];

        addVertex(matrix, buffer, x1, y1, z1, r, g, b, textureScale, rotation, normal);
        addVertex(matrix, buffer, x2, y2, z2, r, g, b, textureScale, rotation, normal);
        addVertex(matrix, buffer, x3, y3, z3, r, g, b, textureScale, rotation, normal);
        addVertex(matrix, buffer, x4, y4, z4, r, g, b, textureScale, rotation, normal);
    }

    private static void addVertex(Matrix4f matrix, VertexConsumer buffer, float x, float y, float z, int r,
            int g, int b, int textureScale, Quaternionf rotation, Vector3d normal) {
        Vector3d rotatedNormal = new Vector3d(normal);
        rotation.transform(rotatedNormal);

        buffer.vertex(matrix, x, y, z)
                // .color(litR, litG, litB, textureScale)
                .color(r, g, b, 255)
                .normal((float) rotatedNormal.x, (float) rotatedNormal.y, (float) rotatedNormal.z)
                .endVertex();
    }

    private static float packColor(float r, float g, float b) {
        int ri = (int)(r * 255.0f) & 0xFF;
        int gi = (int)(g * 255.0f) & 0xFF;
        int bi = (int)(b * 255.0f) & 0xFF;
        int packed = (ri << 16) | (gi << 8) | bi;
        return Float.intBitsToFloat(packed);
    }

    private static int[] unpackColor(float packedFloat) {
        int packed = Float.floatToIntBits(packedFloat);
        int r = ((packed >> 16) & 0xFF);
        int g = ((packed >> 8) & 0xFF);
        int b = (packed & 0xFF);
        return new int[] { r, g, b };
    }
}
