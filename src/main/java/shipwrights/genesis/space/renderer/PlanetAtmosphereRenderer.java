package shipwrights.genesis.space.renderer;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.*;
import net.minecraft.core.Registry;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.PlanetDimensionEffects;
import shipwrights.genesis.client.ShaderRegistry;
import shipwrights.genesis.config.GenesisCommonConfig;
import shipwrights.genesis.mixin.LevelRendererAccessor;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.VantagePoint;
import shipwrights.genesis.space.properties.PlanetProperties;

import java.lang.Math;

import static shipwrights.genesis.client.ShaderRegistry.getPlanetAtmosphereRenderType;

public class PlanetAtmosphereRenderer implements CelestialRenderer {

    @Override
    public void invoke(@NotNull RenderLevelStageEvent event, @NotNull Celestial toRender, @NotNull VantagePoint vantagePoint) {
        ClientLevel level = ((LevelRendererAccessor)event.getLevelRenderer()).getLevel();
        Registry<Celestial> registry = GenesisMod.getCelestialRegistry(level);
        long ticks = GenesisMod.getTicks(level);
        float partialTick = GenesisMod.getPartialTick(level, event);
        Vector3dc position = toRender.getPosition(ticks, partialTick, registry);
        Quaterniondc rotation = toRender.getRotation(ticks, partialTick, registry);
        double size = toRender.getActualSize();

        // Special case: if rendering the vantage point itself, lock it at a fixed position in world space
        if (vantagePoint instanceof VantagePoint.OnCelestial oc && oc.celestial().equals(toRender)) {
            var camera = event.getCamera();
            double halfExtent = Minecraft.getInstance().gameRenderer.getRenderDistance();
            position = new Vector3d(
                0,
                - (camera.getPosition().y / 16) - halfExtent - 32,
                0
            );
            rotation = oc.cameraRotationFromNorthPole();
            size = halfExtent * 2;
        }
        // In space: planets are at absolute positions, so subtract camera position
        else if (vantagePoint instanceof VantagePoint.InSpace) {
            var cameraPos = event.getCamera().getPosition();
            position = new Vector3d(position).sub(cameraPos.x, cameraPos.y, cameraPos.z);
        }
        // Transform by inverse of vantage point (OnCelestial)
        else {
            Vector3dc vantagePos = vantagePoint.getPosition();
            Quaterniondc vantageRot = vantagePoint.getRotation();

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
        }

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer atmosphereBuffer = bufferSource.getBuffer(getPlanetAtmosphereRenderType());
        renderAtmosphere(event.getPoseStack(), atmosphereBuffer, size, position, rotation, event, toRender, vantagePoint);
        bufferSource.endBatch(getPlanetAtmosphereRenderType());
    }

    private void renderAtmosphere(PoseStack poseStack, VertexConsumer buffer, double size, Vector3dc center, Quaterniondc localRotation, @NotNull RenderLevelStageEvent event, @NotNull Celestial toRender, VantagePoint vantagePoint) {

        // Camera is at render origin; planet center is at `center` in render space.
        // Camera relative to planet = -center; transform into planet local space for shader.
        Vector3f cameraPos0 = new Vector3f((float) -center.x(), (float) -center.y(), (float) -center.z());
        Vector3f rotatedCameraPos = new Vector3f(cameraPos0);
        new Quaternionf(localRotation).conjugate().transform(rotatedCameraPos);

        float halfSize = (float) size / 2;

        if (!(toRender.properties() instanceof PlanetProperties props)) return;

        // densityFade only applies when leaving a planet's atmosphere (y-based fade in planet dimension).
        // When viewing from space, use full density.
        double densityFade = (vantagePoint instanceof VantagePoint.InSpace)
                ? 1.0
                : Mth.clamp((event.getCamera().getPosition().y - 320.0) / (GenesisCommonConfig.getAtmosphereEntryHeight() - 320.0), 0.0, 1.0);

        double starBrightness = PlanetDimensionEffects.cachedStarBrightness;

        if (vantagePoint instanceof VantagePoint.OnCelestial oc && !oc.celestial().equals(toRender)) {
            densityFade = starBrightness;
        }

        if (props.atmosphere().density() * densityFade < 0.01) return;

        float relativeAtmosphereSize = 1.0f + 0.3f * (float) props.atmosphere().thickness();

        ClientLevel level = ((LevelRendererAccessor)event.getLevelRenderer()).getLevel();
        Registry<Celestial> reg = GenesisMod.getCelestialRegistry(level);
        long ticks = GenesisMod.getTicks(level);
        float partialTick = GenesisMod.getPartialTick(level, event);

        Vector3dc starPosition = toRender.getNearestStar(ticks, partialTick, reg).getPosition(ticks, partialTick, reg);
        Vector3dc planetPosition = toRender.getPosition(ticks, partialTick, reg);

        Quaterniondc lightRot = (vantagePoint instanceof VantagePoint.OnCelestial oc && oc.celestial().equals(toRender))
                ? vantagePoint.getRotation().mul(oc.cameraRotationFromNorthPole(), new Quaterniond())
                : toRender.getRotation(ticks, partialTick, reg);
        Vector3d lightDir = new Vector3d(planetPosition).sub(starPosition).rotate(new Quaterniond(lightRot).conjugate());
        ShaderInstance shader = ShaderRegistry.PLANET_ATMOSPHERE_SHADER.getInstance().get();
        shader.safeGetUniform("LightDirection").set((float) lightDir.x, (float) lightDir.y, (float) lightDir.z);
        Uniform uniform = shader.getUniform("CameraPosition");
        if (uniform != null) {
            uniform.set(rotatedCameraPos.x, rotatedCameraPos.y, rotatedCameraPos.z);
        }

        Uniform uniform1 = shader.getUniform("HalfSize");
        if (uniform1 != null) {
            uniform1.set(halfSize);
        }

        Uniform uniformThickness = shader.getUniform("AtmosphereThickness");
        if (uniformThickness != null) {
            uniformThickness.set(relativeAtmosphereSize);
        }

        Uniform uniformDensity = shader.getUniform("Density");
        if (uniformDensity != null) {
            uniformDensity.set((float) (props.atmosphere().density() * densityFade));
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

        float outOverexposureCancel = 1.0f;

        // When the player is standing on this planet, they are always inside the atmosphere
        // cube by definition. Using the testPos boundary check here causes flickering as the
        // camera oscillates around the threshold (e.g. at higher altitudes or lower render
        // distances), so we skip it and force the inside-atmosphere rendering mode instead.
        boolean cameraInsideAtmosphere = (vantagePoint instanceof VantagePoint.OnCelestial oc && oc.celestial().equals(toRender))
                || (Math.abs(testPos.x) < halfSize * relativeAtmosphereSize
                    && Math.abs(testPos.y) < halfSize * relativeAtmosphereSize
                    && Math.abs(testPos.z) < halfSize * relativeAtmosphereSize);

        if (cameraInsideAtmosphere) {
            correctionDist = 1.001f;
        } else {
            outOverexposureCancel = 0.000000001f;
        }

        addCubeFaceAtmosphere(matrix, buffer, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize,  halfSize, correctionDist, relativeAtmosphereSize,outOverexposureCancel);
        addCubeFaceAtmosphere(matrix, buffer, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize,  halfSize, correctionDist, relativeAtmosphereSize,outOverexposureCancel);
        addCubeFaceAtmosphere(matrix, buffer, -halfSize, -halfSize, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, -halfSize,  halfSize, correctionDist, relativeAtmosphereSize,outOverexposureCancel);
        addCubeFaceAtmosphere(matrix, buffer, halfSize, -halfSize, -halfSize, halfSize, halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize, halfSize,  halfSize, correctionDist, relativeAtmosphereSize,outOverexposureCancel);
        addCubeFaceAtmosphere(matrix, buffer, -halfSize, -halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize, -halfSize, halfSize, -halfSize, -halfSize, halfSize,  halfSize, correctionDist, relativeAtmosphereSize,outOverexposureCancel);
        addCubeFaceAtmosphere(matrix, buffer, -halfSize, halfSize, -halfSize, -halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, halfSize, -halfSize,  halfSize, correctionDist, relativeAtmosphereSize,outOverexposureCancel);
    }

    private static void addCubeFaceAtmosphere(Matrix4f matrix, VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2,
                                              float x3, float y3, float z3, float x4, float y4, float z4,float halfSize,float zFightingCorrection,float atmosphereThickness,float outFact) {

        float size = 2 * halfSize;
        float sclFct;

        sclFct = zFightingCorrection;
        buffer.vertex(matrix, x1 * sclFct, y1 * sclFct, z1 * sclFct).color((int)(255 * (x1 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (y1 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (z1 * sclFct / atmosphereThickness + halfSize) / size), 255).endVertex();
        buffer.vertex(matrix, x2 * sclFct, y2 * sclFct, z2 * sclFct).color((int)(255 * (x2 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (y2 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (z2 * sclFct / atmosphereThickness + halfSize) / size), 255).endVertex();
        buffer.vertex(matrix, x3 * sclFct, y3 * sclFct, z3 * sclFct).color((int)(255 * (x3 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (y3 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (z3 * sclFct / atmosphereThickness + halfSize) / size), 255).endVertex();
        buffer.vertex(matrix, x4 * sclFct, y4 * sclFct, z4 * sclFct).color((int)(255 * (x4 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (y4 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (z4 * sclFct / atmosphereThickness + halfSize) / size), 255).endVertex();

        sclFct = -atmosphereThickness * outFact;
        buffer.vertex(matrix, x1 * sclFct, y1 * sclFct, z1 * sclFct).color((int)(255 * (x1 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (y1 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (z1 * sclFct / atmosphereThickness + halfSize) / size), 255).endVertex();
        buffer.vertex(matrix, x2 * sclFct, y2 * sclFct, z2 * sclFct).color((int)(255 * (x2 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (y2 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (z2 * sclFct / atmosphereThickness + halfSize) / size), 255).endVertex();
        buffer.vertex(matrix, x3 * sclFct, y3 * sclFct, z3 * sclFct).color((int)(255 * (x3 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (y3 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (z3 * sclFct / atmosphereThickness + halfSize) / size), 255).endVertex();
        buffer.vertex(matrix, x4 * sclFct, y4 * sclFct, z4 * sclFct).color((int)(255 * (x4 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (y4 * sclFct / atmosphereThickness + halfSize) / size), (int)(255 * (z4 * sclFct / atmosphereThickness + halfSize) / size), 255).endVertex();
    }
}
