package g_mungus.genesis.dimension;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import g_mungus.genesis.GenesisMod;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import org.joml.*;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class GreatUnknownDimension {
    public static void registerEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(GenesisMod.SPACE_DIM, new Effects());
    }

    public static class Effects extends DimensionSpecialEffects {
        public Effects() {
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

        @Nullable
        public float[] getSunriseColor(float f, float g) {
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
        public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
            PoseStack posestack = new PoseStack();
            posestack.mulPose(modelViewMatrix);

            FogRenderer.setupNoFog();

            for (int i = 0; i < starBufferCount; i++) {
                Vector4fc color = starColors.get(i);
                RenderSystem.setShaderColor(color.x(), color.y(), color.z(), color.w());
                VertexBuffer starBuffer = starBuffers.get(i);
                starBuffer.bind();
                starBuffer.drawWithShader(posestack.last().pose(), projectionMatrix, GameRenderer.getPositionShader());
                VertexBuffer.unbind();
            }

            return true;
        }

        private void createStars() {

            starBuffers.forEach(VertexBuffer::close);
            starBuffers.clear();

            for(int i = 0; i < starBufferCount; i++) {
                VertexBuffer starBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
                starBuffer.bind();
                starBuffer.upload(this.drawStars(Tesselator.getInstance(), 10842L / (i + 4)));
                VertexBuffer.unbind();
                starBuffers.add(starBuffer);
            }
        }

        private MeshData drawStars(Tesselator arg, long seed) {
            RandomSource randomsource = RandomSource.create(seed);

            BufferBuilder bufferbuilder = arg.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

            for(int j = 0; j < 1600; ++j) {
                float f1 = randomsource.nextFloat() * 2.0F - 1.0F;
                float f2 = randomsource.nextFloat() * 2.0F - 1.0F;
                float f3 = randomsource.nextFloat() * 2.0F - 1.0F;
                float f4 = 0.05F + randomsource.nextFloat() * 0.2F;
                float f5 = Mth.lengthSquared(f1, f2, f3);
                if (!(f5 <= 0.010000001F) && !(f5 >= 1.0F)) {
                    Vector3f vector3f = (new Vector3f(f1, f2, f3)).normalize(100.0F);
                    float f6 = (float)(randomsource.nextDouble() * 3.1415927410125732 * 2.0);
                    Quaternionf quaternionf = (new Quaternionf()).rotateTo(new Vector3f(0.0F, 0.0F, -1.0F), vector3f).rotateZ(f6);
                    bufferbuilder.addVertex(vector3f.add((new Vector3f(f4, -f4, 0.0F)).rotate(quaternionf)));
                    bufferbuilder.addVertex(vector3f.add((new Vector3f(f4, f4, 0.0F)).rotate(quaternionf)));
                    bufferbuilder.addVertex(vector3f.add((new Vector3f(-f4, f4, 0.0F)).rotate(quaternionf)));
                    bufferbuilder.addVertex(vector3f.add((new Vector3f(-f4, -f4, 0.0F)).rotate(quaternionf)));
                }
            }

            return bufferbuilder.buildOrThrow();
        }
    }
}
