package shipwrights.genesis.space.renderer.planet;

import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.DepthTest;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.Effect;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.material.SimpleFogShader;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.material.SimpleMaterialShaders;
import dev.engine_room.flywheel.lib.model.SimpleModel;
import dev.engine_room.flywheel.lib.model.SimpleQuadMesh;
import dev.engine_room.flywheel.lib.task.RunnablePlan;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Vec3i;
import org.joml.*;
import shipwrights.genesis.space.renderer.PosVertexView;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.client.PlanetTextures;

import com.mojang.blaze3d.systems.RenderSystem;
import shipwrights.genesis.space.Celestial;

import java.util.List;

public class PlanetEffect implements Effect {
    private final Level level;
    private final Celestial celestial;

    public PlanetEffect(Celestial celestial, Level level) {
        this.level = level;
        this.celestial = celestial;
    }

    @Override
    public LevelAccessor level() {
        return level;
    }

    @Override
    public EffectVisual<?> visualize(VisualizationContext ctx, float partialTick) {
        return new PlanetVisual(celestial, level, ctx, partialTick);
    }

    private static final SimpleMaterial getMaterial(ResourceLocation texture) {
        return SimpleMaterial.builder()
                .shaders(new SimpleMaterialShaders(
                        GenesisMod.resource("material/planet.vert"),
                        GenesisMod.resource("material/planet.frag")))
                .fog(new SimpleFogShader(GenesisMod.resource("material/no_fog.glsl")))
                .transparency(Transparency.OPAQUE)
                .depthTest(DepthTest.LEQUAL)
                .writeMask(WriteMask.COLOR_DEPTH)
                .backfaceCulling(true)
                .cardinalLightingMode(CardinalLightingMode.ENTITY)
                .ambientOcclusion(false)
                .useOverlay(false)
                .useLight(false)
                .texture(texture)
                .build();
    }

    private static SimpleQuadMesh MESH = null;

    private static SimpleQuadMesh getMesh() {
        if (MESH == null) MESH = buildCubeMesh();
        return MESH;
    }

    public static class PlanetVisual implements EffectVisual<PlanetEffect>, DynamicVisual {
        private final Celestial celestial;
        private final Level level;
        private final PlanetInstance instance;
        private final Vec3i renderOrigin;

        public PlanetVisual(Celestial celestial, Level level, VisualizationContext ctx, float partialTick) {
            this.celestial = celestial;
            this.level = level;
            SimpleModel model = new SimpleModel(List.of(new Model.ConfiguredMesh(getMaterial(PlanetTextures.getTextureLocationForFlywheel(celestial.getID())), getMesh())));
            var instancer = ctx.instancerProvider().instancer(PlanetInstance.TYPE, model);
            instance = instancer.createInstance();
            instance.setHalfSize((float) celestial.getActualSize() / 2f);
            renderOrigin = ctx.renderOrigin();

            updateTransform(partialTick);
        }

        private void updateTransform(float partialTick) {
            long ticks = GenesisMod.getTicks(level);

            Vector3dc position = celestial.getPosition(ticks, partialTick);
            Quaternionf rotation = new Quaternionf(celestial.getRotation(ticks, partialTick));
            double size = celestial.getActualSize();
            Matrix4f transform = new Matrix4f();
            transform.translate((float) (position.x() - renderOrigin.getX()), (float) (position.y() - renderOrigin.getY()), (float) (position.z() - renderOrigin.getZ()));
            transform.scale((float) (size / 2));
            transform.rotate(rotation);

            Vector3f cameraPos = new Vector3f(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().toVector3f());

            Vector3f localCameraPos = new Vector3f(cameraPos);
            localCameraPos.sub((float) position.x(), (float) position.y(), (float) position.z());
            new Quaternionf(rotation).conjugate().transform(localCameraPos);

            // Update shader lights for cardinal lighting
            try {
                Celestial star = celestial.getNearestStar(ticks, partialTick);
                Vector3dc starPos = star.getPosition(ticks, partialTick);
                Vector3f lightDir = new Vector3f(
                        (float) (position.x() - starPos.x()),
                        (float) (position.y() - starPos.y()),
                        (float) (position.z() - starPos.z())
                ).normalize();
                Vector3f lightCol = new Vector3f(star.r(), star.g(), star.b());
                RenderSystem.setShaderLights(lightDir, lightCol);
            } catch (Exception ignored) {
            }

            instance.setTransform(transform, localCameraPos);
        }

        @Override
        public Plan<Context> planFrame() {
            return RunnablePlan.of(context -> updateTransform(context.partialTick()));
        }

        @Override
        public void update(float partialTick) {}

        @Override
        public void delete() { instance.delete(); }
    }

    private static SimpleQuadMesh buildCubeMesh() {
        PosVertexView vertexList = new PosVertexView();
        vertexList.vertexCount(24);

        int vertexIndex = 0;

        vertexIndex = addCubeFace(vertexList, vertexIndex,
                -1.0f, -1.0f, 1.0f,
                1.0f, -1.0f, 1.0f,
                1.0f, 1.0f, 1.0f,
                -1.0f, 1.0f, 1.0f);

        vertexIndex = addCubeFace(vertexList, vertexIndex,
                1.0f, -1.0f, -1.0f,
                -1.0f, -1.0f, -1.0f,
                -1.0f, 1.0f, -1.0f,
                1.0f, 1.0f, -1.0f);

        vertexIndex = addCubeFace(vertexList, vertexIndex,
                -1.0f, -1.0f, -1.0f,
                -1.0f, -1.0f, 1.0f,
                -1.0f, 1.0f, 1.0f,
                -1.0f, 1.0f, -1.0f);

        vertexIndex = addCubeFace(vertexList, vertexIndex,
                1.0f, -1.0f, -1.0f,
                1.0f, 1.0f, -1.0f,
                1.0f, 1.0f, 1.0f,
                1.0f, -1.0f, 1.0f);

        vertexIndex = addCubeFace(vertexList, vertexIndex,
                -1.0f, -1.0f, -1.0f,
                1.0f, -1.0f, -1.0f,
                1.0f, -1.0f, 1.0f,
                -1.0f, -1.0f, 1.0f);

        addCubeFace(vertexList, vertexIndex,
                -1.0f, 1.0f, -1.0f,
                -1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, -1.0f);

        return new SimpleQuadMesh(vertexList, "planet_cube");
    }

    private static int addCubeFace(PosVertexView vertexList, int startIndex,
                                   float x1, float y1, float z1,
                                   float x2, float y2, float z2,
                                   float x3, float y3, float z3,
                                   float x4, float y4, float z4) {
        vertexList.x(startIndex, x1);
        vertexList.y(startIndex, y1);
        vertexList.z(startIndex, z1);
        startIndex++;
        vertexList.x(startIndex, x2);
        vertexList.y(startIndex, y2);
        vertexList.z(startIndex, z2);
        startIndex++;
        vertexList.x(startIndex, x3);
        vertexList.y(startIndex, y3);
        vertexList.z(startIndex, z3);
        startIndex++;
        vertexList.x(startIndex, x4);
        vertexList.y(startIndex, y4);
        vertexList.z(startIndex, z4);
        startIndex++;

        return startIndex;
    }
}
