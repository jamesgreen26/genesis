package shipwrights.genesis.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.*;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.joml.Vector4fc;


import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

public class WormholeDimensionEffects extends DimensionSpecialEffects {
    public WormholeDimensionEffects() {
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
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        FogRenderer.setupNoFog();

        ShaderInstance shader = ShaderRegistry.WORMHOLE_SHADER.getInstance().get();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (int i = 0; i < starBufferCount; i++) {
            VertexBuffer starBuffer = starBuffers.get(i);
            starBuffer.bind();
            starBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
            VertexBuffer.unbind();
        }

        RenderSystem.disableBlend();

        setupFog.run();

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
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

        for(int i = 0; i < 300; ++i) {
            double d0 = (double)(randomsource.nextFloat() * 2.0F - 1.0F);
            double d1 = (double)(randomsource.nextFloat() * 2.0F - 1.0F);
            double d2 = (double)(randomsource.nextFloat() * 2.0F - 1.0F);
            double d3 = (double)(15.5F + randomsource.nextFloat() * 12.5F);
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

                int color = randomsource.nextInt();

                int r = (int) (FastColor.ARGB32.red(color) * 0.7);
                int g = (int) (FastColor.ARGB32.green(color) * 0.7);
                int b = (int) (FastColor.ARGB32.blue(color) * 0.7);
                int a = FastColor.ARGB32.alpha(color);


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

                    // Map UV to match the quad vertex positions
                    // d18 goes from -d3 to +d3, d19 goes from -d3 to +d3
                    float u = ((j & 2) == 0) ? 0.0f : 1.0f;
                    float v = ((j + 1 & 2) == 0) ? 1.0f : 0.0f;

                    bufferbuilder.vertex(d5 + d25, d6 + d23, d7 + d26).color(r, g, b, a).uv(u, v).endVertex();
                }
            }
        }

        return bufferbuilder.end();
    }
}
