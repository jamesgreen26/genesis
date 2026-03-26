package shipwrights.genesis.space.renderer;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.Registry;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.*;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.PlanetDimensionEffects;
import shipwrights.genesis.client.ShaderRegistry;
import shipwrights.genesis.mixin.LevelRendererAccessor;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.VantagePoint;
import shipwrights.genesis.space.properties.StarProperties;

import java.lang.Math;

import static shipwrights.genesis.client.ShaderRegistry.getSunRenderType;

public class StarRenderer implements CelestialRenderer {

    @Override
    public void invoke(@NotNull RenderLevelStageEvent event, @NotNull Celestial toRender, @NotNull VantagePoint vantagePoint) {
        ClientLevel level = ((LevelRendererAccessor)event.getLevelRenderer()).getLevel();
        long ticks = GenesisMod.getTicks(level);
        float partialTick = GenesisMod.getPartialTick(level, event);
        Registry<Celestial> registry = GenesisMod.getCelestialRegistry(level);
        Vector3dc position = toRender.getPosition(ticks, partialTick, registry);
        Quaterniondc rotation = toRender.getRotation(ticks, partialTick, registry);

        Vector3dc vantagePos = vantagePoint.getPosition();
        Quaterniondc vantageRot = vantagePoint.getRotation();

        float opacity = 1f;
        double distanceSquared = vantagePos.distanceSquared(position);

        if (vantagePoint instanceof VantagePoint.OnCelestial) {
            opacity = (float) Math.min(1.0, PlanetDimensionEffects.cachedStarBrightness + 20_000 * 20_000 / Math.max(distanceSquared, 0.00000001));
        }

        if (opacity < 0.01) {
            return;
        }

        // Calculate relative position (subtract vantage point position)
        Vector3d relativePos = new Vector3d(
            position.x() - vantagePos.x(),
            position.y() - vantagePos.y(),
            position.z() - vantagePos.z()
        );

        Quaterniond starRotation = new Quaterniond();

        // Apply inverse rotation of vantage point
        Quaterniond inverseVantageRot = starRotation.premul(vantageRot).conjugate();
        inverseVantageRot.transform(relativePos);
        position = relativePos;

        // Apply inverse rotation to the celestial's own rotation
        rotation = new Quaterniond(inverseVantageRot).mul(new Quaterniond(rotation));

        StarProperties starProps = toRender.properties() instanceof StarProperties sp ? sp : null;

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer sunBuffer = bufferSource.getBuffer(getSunRenderType());
        renderSun(event.getCamera().getPosition(), event.getPoseStack(), sunBuffer, toRender.getActualSize(), position, rotation, starProps, opacity);
        bufferSource.endBatch(getSunRenderType());
    }

    private void renderSun(Vec3 cameraPos, PoseStack poseStack, VertexConsumer buffer, double size, Vector3dc center, Quaterniondc localRotation, StarProperties starProps, float opacity) {

        Vector3f cameraPos0 = new Vector3f(
            (float) cameraPos.x,
            (float) cameraPos.y,
            (float) cameraPos.z
        ).sub((float) center.x(), (float) center.y(), (float) center.z());

        // Transform camera position into sun's local rotated space
        Vector3f rotatedCameraPos = new Vector3f(cameraPos0);
        new Quaternionf(localRotation).conjugate().transform(rotatedCameraPos);

        float halfSize = (float) size / 2;

        ShaderInstance shader = ShaderRegistry.SUN_SHADER.getInstance().get();
        if (shader != null) {
             Uniform uniform = shader.getUniform("CameraPosition");
             if (uniform != null) {
                 uniform.set(rotatedCameraPos.x, rotatedCameraPos.y, rotatedCameraPos.z);
             }

            Uniform uniform1 = shader.getUniform("HalfSize");
             if (uniform1 != null) {
                 uniform1.set(halfSize);
             }

            Uniform color0 = shader.getUniform("Color0");
            if (color0 != null) {
                if (starProps != null) {
                    color0.set(starProps.r0() / 255f, starProps.g0() / 255f, starProps.b0() / 255f);
                } else {
                    color0.set(1.0f, 0.0f, 0.0f);
                }
            }

            Uniform color1 = shader.getUniform("Color1");
            if (color1 != null) {
                if (starProps != null) {
                    color1.set(starProps.r1() / 255f, starProps.g1() / 255f, starProps.b1() / 255f);
                } else {
                    color1.set(1.0f, 0.8f, 0.15f);
                }
            }

            Uniform uniform2 = shader.getUniform("Opacity");
            if (uniform2 != null) {
                uniform2.set(opacity);
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


        addCubeFaceSun(matrix, buffer, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize);
        addCubeFaceSun(matrix, buffer, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize);
        addCubeFaceSun(matrix, buffer, -halfSize, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize);
        addCubeFaceSun(matrix, buffer, halfSize, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize);
        addCubeFaceSun(matrix, buffer, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize);
        addCubeFaceSun(matrix, buffer, -halfSize, halfSize, -halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize);
    }

    private static void addCubeFaceSun(Matrix4f matrix, VertexConsumer buffer, float x4, float y4, float z4, float x3, float y3, float z3,
                                       float x2, float y2, float z2, float x1, float y1, float z1) {
        
        float halfSize = Math.abs(x1);
        float size = 2 * halfSize;
        buffer.vertex(matrix, x1, y1, z1).color((int)(255 * (x1 + halfSize) / size), (int)(255 * (y1 + halfSize) / size), (int)(255 * (z1 + halfSize) / size), 255).endVertex();
        buffer.vertex(matrix, x2, y2, z2).color((int)(255 * (x2 + halfSize) / size), (int)(255 * (y2 + halfSize) / size), (int)(255 * (z2 + halfSize) / size), 255).endVertex();
        buffer.vertex(matrix, x3, y3, z3).color((int)(255 * (x3 + halfSize) / size), (int)(255 * (y3 + halfSize) / size), (int)(255 * (z3 + halfSize) / size), 255).endVertex();
        buffer.vertex(matrix, x4, y4, z4).color((int)(255 * (x4 + halfSize) / size), (int)(255 * (y4 + halfSize) / size), (int)(255 * (z4 + halfSize) / size), 255).endVertex();
    }
}
