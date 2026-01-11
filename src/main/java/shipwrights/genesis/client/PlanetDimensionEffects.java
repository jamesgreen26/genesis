package shipwrights.genesis.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.OrbitingBody;
import shipwrights.genesis.space.SpaceLevel;
import shipwrights.genesis.space.Star;
import shipwrights.genesis.space.registry.SpaceRegistry;

import java.lang.Math;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class PlanetDimensionEffects extends DimensionSpecialEffects {
    final SpaceRegistry spaceRegistry;


    public PlanetDimensionEffects(SpaceRegistry spaceRegistry) {
        super(Float.NaN, false, DimensionSpecialEffects.SkyType.NONE, false, false);
        this.spaceRegistry = spaceRegistry;
        createStars();
    }

    private final int starBufferCount = 3;

    private final List<VertexBuffer> starBuffers = new ArrayList<>(starBufferCount);

    private final List<Vector4fc> starColors = List.of(
            new Vector4f(1f, 1f, 1f, 0.8f),
            new Vector4f(0.8f, 0.8f, 1f, 0.8f),
            new Vector4f(1f, 1f, 0.8f, 0.8f)
    );

    public @NotNull Vec3 getBrightnessDependentFogColor(@NotNull Vec3 arg, float f) {
        return new Vec3(0,0,0);
    }

    public boolean isFoggyAt(int i, int j) {
        return false;
    }

    public float @Nullable [] getSunriseColor(float f, float g) {
        return null;
    }

    @Override
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, LightTexture lightTexture, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public boolean tickRain(ClientLevel level, int ticks, Camera camera) {
        return true;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        FogRenderer.setupNoFog();

        final OrbitingBody body = GenesisMod.getDataForLevel(level);
        if (body == null) {
            return false;
        }

        poseStack.pushPose();

        poseStack.rotateAround(new Quaternionf(body.getRotation(ticks, partialTick)).invert(), 0, 0, 0);

        for (int i = 0; i < starBufferCount; i++) {
            Vector4fc color = starColors.get(i);
            RenderSystem.setShaderColor(color.x(), color.y(), color.z(), color.w());
            VertexBuffer starBuffer = starBuffers.get(i);
            starBuffer.bind();
            starBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, GameRenderer.getPositionShader());
            VertexBuffer.unbind();
        }

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        Vector3dc origin = body.getCurrentPos(ticks, partialTick);

        Optional<Star.WithDistanceSq> star = SpaceLevel.getNearestStar(origin, ticks);

        Vector3dc lightOrigin = origin;

        if (star.isPresent()) {
            lightOrigin = star.get().celestial.getCurrentPos(ticks, partialTick).sub(origin, new Vector3d());
        }

        double desiredDistance = Minecraft.getInstance().gameRenderer.getRenderDistance() * 2;


        for (var otherBody : spaceRegistry.getAllOrbitingBodies().stream()
                .sorted(Comparator.comparingDouble(
                        b -> -1 * b.getCurrentPos(ticks, partialTick).distanceSquared(origin)
                )).toList()) {
            if (otherBody == body) continue;

            Vector3dc actualOffset = otherBody.getCurrentPos(ticks, partialTick).sub(origin);
            Vector3dc lightOffset = lightOrigin.sub(actualOffset, new Vector3d()).normalize();
            double actualDistance = actualOffset.length();
            double requiredScaling = desiredDistance / actualDistance;
            double actualHalfSize = otherBody.getActualSize() / 2;
            Vector3dc scaledOffset = actualOffset.mul(requiredScaling, new Vector3d());

            // TODO should not write depth
            SimplePlanetRenderer.RenderPlanetAt(otherBody.getID(), poseStack, scaledOffset.x(), scaledOffset.y(), scaledOffset.z(), actualHalfSize * requiredScaling, otherBody.getRotation(ticks, partialTick), lightOffset);
        }


        poseStack.popPose();

        return true;
    }

    private void createStars() {
        starBuffers.forEach(VertexBuffer::close);
        starBuffers.clear();

        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        for(int i = 0; i < starBufferCount; i++) {
            VertexBuffer starBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            BufferBuilder.RenderedBuffer renderedBuffer = this.drawStars(bufferbuilder, 10842L / (i + 4));
            starBuffer.bind();
            starBuffer.upload(renderedBuffer);
            VertexBuffer.unbind();
            starBuffers.add(starBuffer);
        }
    }

    private BufferBuilder.RenderedBuffer drawStars(BufferBuilder bufferbuilder, long seed) {
        RandomSource randomsource = RandomSource.create(seed);
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

        for(int i = 0; i < 1600; ++i) {
            double d0 = randomsource.nextFloat() * 2.0F - 1.0F;
            double d1 = randomsource.nextFloat() * 2.0F - 1.0F;
            double d2 = randomsource.nextFloat() * 2.0F - 1.0F;
            double d3 = 0.05F + randomsource.nextFloat() * 0.2F;
            double d4 = d0 * d0 + d1 * d1 + d2 * d2;
            if (d4 < 1.0 && d4 > 0.01) {
                d4 = 1.0 / Math.sqrt(d4);
                d0 *= d4;
                d1 *= d4;
                d2 *= d4;
                double d5 = d0 * 100.0;
                double d6 = d1 * 100.0;
                double d7 = d2 * 100.0;
                double d8 = Math.atan2(d0, d2);
                double d9 = Math.sin(d8);
                double d10 = Math.cos(d8);
                double d11 = Math.atan2(Math.sqrt(d0 * d0 + d2 * d2), d1);
                double d12 = Math.sin(d11);
                double d13 = Math.cos(d11);
                double d14 = randomsource.nextDouble() * Math.PI * 2.0;
                double d15 = Math.sin(d14);
                double d16 = Math.cos(d14);

                for(int j = 0; j < 4; ++j) {
                    double d17 = 0.0;
                    double d18 = (double)((j & 2) - 1) * d3;
                    double d19 = (double)((j + 1 & 2) - 1) * d3;
                    double d21 = d18 * d16 - d19 * d15;
                    double d22 = d19 * d16 + d18 * d15;
                    double d23 = d21 * d12 + d17 * d13;
                    double d24 = d17 * d12 - d21 * d13;
                    double d25 = d24 * d9 - d22 * d10;
                    double d26 = d22 * d9 + d24 * d10;
                    bufferbuilder.vertex(d5 + d25, d6 + d23, d7 + d26).endVertex();
                }
            }
        }

        return bufferbuilder.end();
    }
}
