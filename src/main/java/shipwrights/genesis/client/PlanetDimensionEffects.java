package shipwrights.genesis.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.util.CubicSampler;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.config.GenesisCommonConfig;
import shipwrights.genesis.mixin.LevelRendererAccessor;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.VantagePoint;
import shipwrights.genesis.space.properties.PlanetColorPalette;
import shipwrights.genesis.space.properties.PlanetProperties;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;

public class PlanetDimensionEffects extends DimensionSpecialEffects {

    public PlanetDimensionEffects() {
        super(192f, false, SkyType.NORMAL, false, false);
        createStars();
    }

    private static double cachedClampedDensity = 1.0;
    private static double cachedRawDensity = 1.0;
    public static Vec3 cachedSkyColor = Vec3.ZERO;
    public static double cachedStarBrightness = 1.0;

    private final int starBufferCount = 3;

    private final List<VertexBuffer> starBuffers = new ArrayList<>(starBufferCount);

    private final List<Vector4fc> starColors = List.of(
            new Vector4f(1f, 1f, 1f, 0.8f),
            new Vector4f(0.8f, 0.8f, 1f, 0.8f),
            new Vector4f(1f, 1f, 0.8f, 0.8f)
    );

    public @NotNull Vec3 getBrightnessDependentFogColor(@NotNull Vec3 color, float brightness) {
        return color.multiply(
                cachedClampedDensity * (brightness * 0.94F + 0.06F),
                cachedClampedDensity * (brightness * 0.94F + 0.06F),
                cachedClampedDensity * (brightness * 0.91F + 0.09F)
        );
    }

    public boolean isFoggyAt(int i, int j) {
        return cachedRawDensity > 1.3;
    }

    public float @Nullable [] getSunriseColor(float f, float g) {
        float[] original = super.getSunriseColor(f, g);
        if (original != null) {
            original[3] = (float) (original[3] * cachedClampedDensity);
        }
        return original;
    }

    @Nullable
    private PlanetProperties getPlanetProperties(ClientLevel level) {
        VantagePoint vp = VantagePoint.get(level, new Vector3d(), 0, 0f);
        Celestial celestial = vp instanceof VantagePoint.OnCelestial oc ? oc.celestial() : null;
        if (celestial == null) return null;
        return celestial.properties() instanceof PlanetProperties pp ? pp : null;
    }

    private boolean hasPrecipitation(ClientLevel level) {
        PlanetProperties props = getPlanetProperties(level);
        if (props == null) return true;
        return props.atmosphere().precipitation();
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack, double camX, double camY, double camZ, Matrix4f projectionMatrix) {
        PlanetProperties planetProps = getPlanetProperties(level);
        double density = planetProps != null ? planetProps.atmosphere().density() : 1.0;
        return camY > 500 || density <= 0.7;
    }

    @Override
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, LightTexture lightTexture, double camX, double camY, double camZ) {
        return !hasPrecipitation(level) || camY > 360;
    }

    @Override
    public boolean tickRain(ClientLevel level, int ticks, Camera camera) {
        return !hasPrecipitation(level);
    }

    @Override
    public boolean renderSky(ClientLevel level, int unused, float partialTick, PoseStack poseStack, Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {

        long gameTime = GenesisMod.getTicks(level);
        VantagePoint vp = VantagePoint.get(level, new Vector3d(camera.getPosition().x, camera.getPosition().y, camera.getPosition().z), gameTime, partialTick);
        if (!(vp instanceof VantagePoint.OnCelestial vpOc)) {
            return false;
        }
        final Celestial celestial = vpOc.celestial();
        
        Celestial star = celestial.getNearestStar(gameTime, partialTick, vpOc.registry());

        Vector3d toStar = new Vector3d(star.getPosition(gameTime, partialTick, vpOc.registry()))
                .sub(celestial.getPosition(gameTime, partialTick, vpOc.registry()))
                .normalize();
        
        Quaterniondc rot = new Quaterniond(vp.getRotation()).conjugate();
        toStar.rotate(rot);
        
        double starUpDot = GenesisMod.UP.dot(toStar);
        double starEastDot = GenesisMod.EAST.dot(toStar);
        PlanetProperties planetProps = getPlanetProperties(level);
        cachedRawDensity = planetProps != null ? planetProps.atmosphere().density() : 1.0;
        double density = Mth.clamp(cachedRawDensity, 0.0, 1.0);

        // fade out density with camera y level, from y=320 to y=GenesisMod.atmosphereEntryHeight
        double cameraY = camera.getPosition().y;
        double densityFade = 1.0 - Mth.clamp((cameraY - 320.0) / (GenesisCommonConfig.getAtmosphereEntryHeight() - 320.0), 0.0, 1.0);
        density *= densityFade;
        cachedRawDensity *= densityFade;

        cachedClampedDensity = density;
        PlanetColorPalette palette = planetProps != null ? planetProps.atmosphere().color() : new PlanetColorPalette.Overworld();

        float rainLevel = hasPrecipitation(level) ? level.getRainLevel(partialTick) : 0f;
        double rawStarBrightness = 2 * Math.min(Math.max(-starUpDot, 0), 0.5d) * (1f - rainLevel);
        cachedStarBrightness = Mth.lerp(density, 1.0, rawStarBrightness);
        double apparentSunAngle = getApparentSunAngle(starUpDot, starEastDot);
        // apparent world time
        long fakeTime = (long) (apparentSunAngle * 24000);

        Vec3 skyColor = getSkyColor(camera.getPosition(), partialTick, fakeTime, level, palette);
        cachedSkyColor = skyColor;
        float skyR = (float)skyColor.x;
        float skyG = (float)skyColor.y;
        float skyB = (float)skyColor.z;
        FogRenderer.levelFogColor();
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(skyR, skyG, skyB, 1.0F);
        ShaderInstance shader = RenderSystem.getShader();
        if (shader != null) {
            VertexBuffer skyBuffer = ((LevelRendererAccessor) Minecraft.getInstance().levelRenderer).getSkyBuffer();
            skyBuffer.bind();
            skyBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, shader);
            VertexBuffer.unbind();
        }
        
        RenderSystem.enableBlend();
        float dayTime = level.dimensionType().timeOfDay(fakeTime);
        float[] acolor = this.getSunriseColor(dayTime, partialTick);
        if (acolor != null) {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.pushPose();
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90.0F));
            
            Quaternionf fullRot = new Quaternionf();/*.set(rot).rotateTo(
                    1.0f,
                    0.0f,
                    0.0f,
                    (float) toStar.x,
                    (float) toStar.y,
                    (float) toStar.z
            );*/
            float r = acolor[0];
            float g = acolor[1];
            float b = acolor[2];
            float a = (float) (acolor[3] * (1 - cachedStarBrightness));
            poseStack.mulPose(fullRot);
            Matrix4f pose = poseStack.last().pose();
            
            bufferbuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            bufferbuilder.vertex(pose, 0.0F, 100.0F, 0.0F).color(r, g, b, a).endVertex();
            int i = 16;
            
            for(int j = 0; j <= i; ++j) {
                float angle = (float)j * ((float)Math.PI * 2F) / i;
                float sin = Mth.sin(angle);
                float cos = Mth.cos(angle);
                bufferbuilder.vertex(pose, sin * 120.0F, cos * 120.0F, -cos * 40.0F * a).color(acolor[0], acolor[1], acolor[2], 0.0F).endVertex();
            }
            
            BufferUploader.drawWithShader(bufferbuilder.end());
            poseStack.popPose();
        }
        
        poseStack.pushPose();

        //transform stars view to the side of the planet fixme todo change this to use the new vantage point system
        poseStack.mulPose(new Quaternionf().rotateX((float) (Math.PI/2)));
        poseStack.mulPose(new Quaternionf(celestial.getRotation(gameTime, partialTick, vpOc.registry())).invert());

        for (int i = 0; i < starBufferCount; i++) {
            Vector4fc color = starColors.get(i);
            RenderSystem.setShaderColor(color.x(), color.y(), color.z(), (float) (color.w() * cachedStarBrightness));
            VertexBuffer starBuffer = starBuffers.get(i);
            starBuffer.bind();
            assert GameRenderer.getPositionShader() != null;
            starBuffer.drawWithShader(poseStack.last().pose(), projectionMatrix, GameRenderer.getPositionShader());
            VertexBuffer.unbind();
        }
        setupFog.run();

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
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
    
    public static Vec3 getSkyColor(Vec3 position, float partialTick, long time, ClientLevel level, PlanetColorPalette palette) {
        float f = level.dimensionType().timeOfDay(time);
        float intensity = Mth.cos(f * ((float) Math.PI * 2F)) * 2.0F + 0.5F;
        intensity = Mth.clamp(intensity, 0.0F, 1.0F);

        float r, g, b;
        if (palette.isOverworld()) {
            Vec3 vec3 = position.subtract(2.0F, 2.0F, 2.0F).scale(0.25F);
            BiomeManager biomemanager = level.getBiomeManager();
            Vec3 skyColor = CubicSampler.gaussianSampleVec3(vec3, (p1, p2, p3) -> Vec3.fromRGB24(biomemanager.getNoiseBiomeAtQuart(p1, p2, p3).value().getSkyColor()));
            r = (float) skyColor.x * intensity;
            g = (float) skyColor.y * intensity;
            b = (float) skyColor.z * intensity;
        } else {
            int[] rgb = palette.getRGB();
            r = (rgb[0] / 255.0f) * intensity;
            g = (rgb[1] / 255.0f) * intensity;
            b = (rgb[2] / 255.0f) * intensity;
        }

        float rainLevel = level.getRainLevel(partialTick);
        if (rainLevel > 0.0F) {
            float f6 = (r * 0.3F + g * 0.59F + b * 0.11F) * 0.6F;
            float f7 = 1.0F - rainLevel * 0.75F;
            r = r * f7 + f6 * (1.0F - f7);
            g = g * f7 + f6 * (1.0F - f7);
            b = b * f7 + f6 * (1.0F - f7);
        }

        float thunderLevel = level.getThunderLevel(partialTick);
        if (thunderLevel > 0.0F) {
            float f10 = (r * 0.3F + g * 0.59F + b * 0.11F) * 0.2F;
            float f8 = 1.0F - thunderLevel * 0.75F;
            r = r * f8 + f10 * (1.0F - f8);
            g = g * f8 + f10 * (1.0F - f8);
            b = b * f8 + f10 * (1.0F - f8);
        }

        int skyFlashTime = level.getSkyFlashTime();
        if (skyFlashTime > 0) {
            float f11 = (float) skyFlashTime - partialTick;
            if (f11 > 1.0F) f11 = 1.0F;
            f11 *= 0.45F;
            r = r * (1.0F - f11) + 0.8F * f11;
            g = g * (1.0F - f11) + 0.8F * f11;
            b = b * (1.0F - f11) + f11;
        }

        r *= (float) cachedClampedDensity;
        g *= (float) cachedClampedDensity;
        b *= (float) cachedClampedDensity;

        return new Vec3(r, g, b);
    }

    public static double getApparentSunAngle(double starUpDot, double starEastDot) {
        return GenesisMod.getApparentSunAngle(starUpDot, starEastDot);
    }
}
