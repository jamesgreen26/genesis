package shipwrights.genesis.space.renderer;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.ShaderRegistry;
import shipwrights.genesis.mixin.LevelRendererAccessor;
import shipwrights.genesis.space.Celestial;

import java.lang.Math;

import static shipwrights.genesis.client.ShaderRegistry.getPlanetAtmosphereRenderType;

public class PlanetAtmosphereRenderer implements CelestialRenderer {

    @Override
    public void invoke(@NotNull RenderLevelStageEvent event, @NotNull Celestial toRender, @Nullable Celestial vantagePoint) {
        ClientLevel level = ((LevelRendererAccessor)event.getLevelRenderer()).getLevel();
        long ticks = GenesisMod.getTicks(level);
        float partialTick = GenesisMod.getPartialTick(level, event);
        Vector3dc position = toRender.getPosition(ticks, partialTick);
        Quaterniondc rotation = toRender.getRotation(ticks, partialTick);

        // Transform by inverse of vantage point if present
        if (vantagePoint != null) {
            Vector3dc vantagePos = vantagePoint.getPosition(ticks, partialTick);
            Quaterniondc vantageRot = vantagePoint.getRotation(ticks, partialTick);

            // Calculate relative position (subtract vantage point position)
            Vector3d relativePos = new Vector3d(
                position.x() - vantagePos.x(),
                position.y() - vantagePos.y(),
                position.z() - vantagePos.z()
            );

            Quaterniond starRotation = new Quaterniond().rotateX(- Math.PI/2);

            // Apply inverse rotation of vantage point
            Quaterniond inverseVantageRot = starRotation.premul(vantageRot).conjugate();
            inverseVantageRot.transform(relativePos);
            position = relativePos;

            // Apply inverse rotation to the celestial's own rotation
            rotation = new Quaterniond(inverseVantageRot).mul(new Quaterniond(rotation));
        }

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer atmosphereBuffer = bufferSource.getBuffer(getPlanetAtmosphereRenderType());
        renderAtmosphere(event.getCamera().getPosition(), event.getPoseStack(), atmosphereBuffer, toRender.getActualSize(), position, rotation, event, toRender, vantagePoint);
        bufferSource.endBatch(getPlanetAtmosphereRenderType());
    }

    private void renderAtmosphere(Vec3 cameraPos, PoseStack poseStack, VertexConsumer buffer, double size, Vector3dc center, Quaterniondc localRotation, @NotNull RenderLevelStageEvent event, @NotNull Celestial toRender, @Nullable Celestial vantagePoint) {

        Vector3f cameraPos0 = new Vector3f(
            (float) cameraPos.x,
            (float) cameraPos.y,
            (float) cameraPos.z
        ).sub((float) center.x(), (float) center.y(), (float) center.z());

        // Transform camera position into sun's local rotated space
        Vector3f rotatedCameraPos = new Vector3f(cameraPos0);
        new Quaternionf(localRotation).conjugate().transform(rotatedCameraPos);

        float halfSize = (float) size / 2;

        float relativeAtmosphereSize = 1.3f;

        ClientLevel level = ((LevelRendererAccessor)event.getLevelRenderer()).getLevel();
        long ticks = GenesisMod.getTicks(level);
        float partialTick = GenesisMod.getPartialTick(level, event);

        Vector3dc starPosition = toRender.getNearestStar(ticks, partialTick).getPosition(ticks, partialTick);
        Vector3dc position = toRender.getPosition(ticks, partialTick);

        Quaterniondc rotation = toRender.getRotation(ticks, partialTick);

        if (vantagePoint != null && vantagePoint.equals(toRender)) {
            var camera = event.getCamera();
            halfSize = 0.000001f;
            cameraPos0 = new Vector3f(
                    0,
                    1000000000000.0f,
                    0
            );
            localRotation = new Quaterniond().rotateX(Math.PI/2);

        }
        // Transform by inverse of vantage point if present
        else if (vantagePoint != null) {
            Vector3dc vantagePos = vantagePoint.getPosition(ticks, partialTick);
            Quaterniondc vantageRot = vantagePoint.getRotation(ticks, partialTick);

            // Calculate relative position (subtract vantage point position)
            cameraPos0 = new Vector3f(
                    (float) (vantagePos.x() - position.x()),
                    (float) (vantagePos.y() - position.y()),
                    (float) (vantagePos.z() - position.z())
            );

            // Transform the view to the side of the planet
            Quaterniond planetRotation = new Quaterniond().rotateX(- Math.PI/2);

            // Apply inverse rotation of vantage point
            Quaterniond inverseVantageRot = planetRotation.premul(vantageRot).conjugate();
            inverseVantageRot.transform(cameraPos0);
            //position = Vector3dc(cameraPos0);

            // Apply inverse rotation to the celestial's own rotation
            localRotation = new Quaterniond(inverseVantageRot).mul(new Quaterniond(rotation));
        }

        Vector3d lightDir = new Vector3d(position).sub(starPosition).rotate(new Quaterniond(localRotation).conjugate());
        ShaderInstance shader = ShaderRegistry.PLANET_ATMOSPHERE_SHADER.getInstance().get();
        shader.safeGetUniform("LightDirection").set((float) lightDir.x, (float) lightDir.y, (float) lightDir.z);
        if (shader != null) {
            Uniform uniform = shader.getUniform("CameraPosition");
            if (uniform != null) {
                uniform.set(rotatedCameraPos.x, rotatedCameraPos.y, rotatedCameraPos.z);
            }

            Uniform uniform1 = shader.getUniform("HalfSize");
            if (uniform1 != null) {
                uniform1.set(halfSize);
            }
        }

        Matrix4f matrix;
        try {
            matrix = (Matrix4f) poseStack.last().pose().clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }

        matrix.translate(cameraPos0.negate(new Vector3f()));

        matrix.rotate(new Quaternionf(localRotation));

        Vector3f testPos = new Vector3f(cameraPos0).rotate(new Quaternionf(localRotation).conjugate());

        float correctionDist = relativeAtmosphereSize;

        if(
        Math.abs(testPos.x) < halfSize * relativeAtmosphereSize &&
        Math.abs(testPos.y) < halfSize * relativeAtmosphereSize &&
        Math.abs(testPos.z) < halfSize * relativeAtmosphereSize
        ) {
            correctionDist = 1.001f;
        }

        addCubeFaceAtmosphere(matrix, buffer, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize,  halfSize, correctionDist, relativeAtmosphereSize);
        addCubeFaceAtmosphere(matrix, buffer, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize,  halfSize, correctionDist, relativeAtmosphereSize);
        addCubeFaceAtmosphere(matrix, buffer, -halfSize, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize,  halfSize, correctionDist, relativeAtmosphereSize);
        addCubeFaceAtmosphere(matrix, buffer, halfSize, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize,  halfSize, correctionDist, relativeAtmosphereSize);
        addCubeFaceAtmosphere(matrix, buffer, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize,  halfSize, correctionDist, relativeAtmosphereSize);
        addCubeFaceAtmosphere(matrix, buffer, -halfSize, halfSize, -halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize,  halfSize, correctionDist, relativeAtmosphereSize);
    }

    private static void addCubeFaceAtmosphere(Matrix4f matrix, VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2,
                                              float x3, float y3, float z3, float x4, float y4, float z4,float halfSize,float zFightingCorrection,float atmosphereThickness) {

        float size = 2 * halfSize;
        float sclFct;

        sclFct = zFightingCorrection;
        buffer.vertex(matrix, x1 * sclFct, y1 * sclFct, z1 * sclFct).color((int)(255 * (x1 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (y1 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (z1 * sclFct / atmosphereThickness + halfSize) / size), 255).endVertex();
        buffer.vertex(matrix, x2 * sclFct, y2 * sclFct, z2 * sclFct).color((int)(255 * (x2 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (y2 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (z2 * sclFct / atmosphereThickness + halfSize) / size), 255).endVertex();
        buffer.vertex(matrix, x3 * sclFct, y3 * sclFct, z3 * sclFct).color((int)(255 * (x3 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (y3 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (z3 * sclFct / atmosphereThickness + halfSize) / size), 255).endVertex();
        buffer.vertex(matrix, x4 * sclFct, y4 * sclFct, z4 * sclFct).color((int)(255 * (x4 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (y4 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (z4 * sclFct / atmosphereThickness + halfSize) / size), 255).endVertex();

        sclFct = -atmosphereThickness;
        buffer.vertex(matrix, x1 * sclFct, y1 * sclFct, z1 * sclFct).color((int)(255 * (x1 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (y1 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (z1 * sclFct / atmosphereThickness + halfSize) / size), 255).endVertex();
        buffer.vertex(matrix, x2 * sclFct, y2 * sclFct, z2 * sclFct).color((int)(255 * (x2 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (y2 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (z2 * sclFct / atmosphereThickness + halfSize) / size), 255).endVertex();
        buffer.vertex(matrix, x3 * sclFct, y3 * sclFct, z3 * sclFct).color((int)(255 * (x3 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (y3 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (z3 * sclFct / atmosphereThickness + halfSize) / size), 255).endVertex();
        buffer.vertex(matrix, x4 * sclFct, y4 * sclFct, z4 * sclFct).color((int)(255 * (x4 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (y4 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (z4 * sclFct / atmosphereThickness + halfSize) / size), 255).endVertex();
    }
}
