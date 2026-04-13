package shipwrights.genesis.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.joml.Vector4fc;


import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.ShaderInstance;
import com.mojang.blaze3d.vertex.BufferUploader;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Registry;
import kotlin.Pair;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.SpaceLevel;
import shipwrights.genesis.space.properties.StarProperties;
import shipwrights.genesis.space.type.BuiltinCelestialTypes;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class SpaceDimensionEffects extends DimensionSpecialEffects {
    public SpaceDimensionEffects() {
        super(Float.NaN, false, SkyType.NONE, false, false);
        createStars();
    }

    private final int starBufferCount = 3;

    private final List<VertexBuffer> starBuffers = new ArrayList<>(starBufferCount);

    private final List<Vector4fc> starColors = List.of(
            new Vector4f(1f, 1f, 1f, 0.8f),
            new Vector4f(0.8f, 0.8f, 1f, 0.8f),
            new Vector4f(1f, 1f, 0.8f, 0.8f)
    );

    public Vec3 getBrightnessDependentFogColor(Vec3 arg, float f) {
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
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, double camX, double camY, double camZ, Matrix4f projectionMatrix) {
        return true;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        FogRenderer.setupNoFog();
        RenderSystem.depthMask(false);

        renderNearestStarGlow(level, partialTick, poseStack, camera);

        for (int i = 0; i < starBufferCount; i++) {
            Vector4fc color = starColors.get(i);
            RenderSystem.setShaderColor(color.x(), color.y(), color.z(), color.w());
            VertexBuffer starBuffer = starBuffers.get(i);
            starBuffer.bind();
            starBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, GameRenderer.getPositionShader());
            VertexBuffer.unbind();
        }

        RenderSystem.depthMask(true);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        return true;
    }

    private void renderNearestStarGlow(ClientLevel level, float partialTick, PoseStack poseStack, Camera camera) {
        Registry<Celestial> registry = GenesisMod.getCelestialRegistry(level);
        Vec3 camPos = camera.getPosition();
        Vector3d camPosJoml = new Vector3d(camPos.x, camPos.y, camPos.z);
        long gameTicks = GenesisMod.getTicks(level);

        Pair<Celestial, Double> nearestStar = SpaceLevel.nearestCelestialWhere(
                registry,
                camPosJoml,
                gameTicks,
                partialTick,
                Predicate.isEqual(BuiltinCelestialTypes.STAR)
        );

        if (nearestStar == null) {
            return;
        }

        Celestial star = nearestStar.getFirst();
        Vector3dc starPos = star.getPosition(gameTicks, partialTick, registry);
        double distance = Math.sqrt(nearestStar.getSecond());
        float fade = (float) Mth.clamp(distance / 3000.0, 0.0, 1.0);
        if (fade <= 0.001f) {
            return;
        }

        Vector3d toStar = new Vector3d(starPos.x(), starPos.y(), starPos.z())
                .sub(camPosJoml)
                .normalize();

        Vector3d up = new Vector3d(0.0, 1.0, 0.0);
        if (Math.abs(toStar.dot(up)) > 0.98) {
            up.set(1.0, 0.0, 0.0);
        }

        Vector3d right = new Vector3d();
        toStar.cross(up, right).normalize();
        Vector3d upPerp = new Vector3d();
        right.cross(toStar, upPerp).normalize();

        float glowDistance = 100.0f;
        float outerRadius = (float) (36.0 * (10000.0 / Math.max(distance, 1.0)));
        float innerAlpha = 0.3f * fade;

        float r = 1.0f;
        float g = 1.0f;
        float b = 1.0f;
        if (star.properties() instanceof StarProperties starProps) {
            r = starProps.r1() / 255f;
            g = starProps.g1() / 255f;
            b = starProps.b1() / 255f;
        }

        // brighten toward white
        float brighten = 0.25f;
        r += (1f - r) * brighten;
        g += (1f - g) * brighten;
        b += (1f - b) * brighten;

        // desaturate slightly
        float lum = 0.299f * r + 0.587f * g + 0.114f * b;
        float desat = 0.3f;

        r += (lum - r) * desat;
        g += (lum - g) * desat;
        b += (lum - b) * desat;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ShaderInstance shader = ShaderRegistry.STAR_GLOW_SHADER.getInstance().get();
        if (shader == null) {
            RenderSystem.disableBlend();
            return;
        }
        RenderSystem.setShader(() -> shader);
        Uniform baseAlpha = shader.getUniform("BaseAlpha");
        if (baseAlpha != null) {
            baseAlpha.set(innerAlpha);
        }

        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        Matrix4f pose = poseStack.last().pose();
        Vector3d center = new Vector3d(toStar).mul(glowDistance);
        bufferbuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        bufferbuilder.vertex(pose, (float) center.x, (float) center.y, (float) center.z).color(r, g, b, 0.0f).endVertex();

        int steps = 32;
        for (int i = 0; i <= steps; i++) {
            double angle = i * (Math.PI * 2.0) / steps;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            Vector3d offset = new Vector3d(right).mul(cos * outerRadius).add(new Vector3d(upPerp).mul(sin * outerRadius));
            Vector3d pos = new Vector3d(center).add(offset);
            bufferbuilder.vertex(pose, (float) pos.x, (float) pos.y, (float) pos.z).color(r, g, b, 1.0f).endVertex();
        }

        BufferUploader.drawWithShader(bufferbuilder.end());
        RenderSystem.disableBlend();
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
            double d0 = (double)(randomsource.nextFloat() * 2.0F - 1.0F);
            double d1 = (double)(randomsource.nextFloat() * 2.0F - 1.0F);
            double d2 = (double)(randomsource.nextFloat() * 2.0F - 1.0F);
            double d3 = (double)(0.05F + randomsource.nextFloat() * 0.2F);
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
                    double d20 = 0.0;
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
