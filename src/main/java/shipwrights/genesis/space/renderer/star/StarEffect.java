package shipwrights.genesis.space.renderer.star;

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
import shipwrights.genesis.space.Celestial;

import java.text.NumberFormat;
import java.util.List;

public class StarEffect implements Effect {
    private final Level level;
    private final Celestial celestial;

    public StarEffect(Celestial celestial, Level level) {
        this.level = level;
        this.celestial = celestial;
    }

    @Override
    public LevelAccessor level() {
        return level;
    }

    @Override
    public EffectVisual<?> visualize(VisualizationContext ctx, float partialTick) {
        return new StarEffectVisual(celestial, level, ctx, partialTick);
    }

    private static final SimpleMaterial MATERIAL = SimpleMaterial.builder()
            .shaders(new SimpleMaterialShaders(
                    GenesisMod.resource( "material/star.vert"),
                    GenesisMod.resource("material/star.frag")))
            .fog(new SimpleFogShader(GenesisMod.resource("material/no_fog.glsl")))
            .transparency(Transparency.TRANSLUCENT)
            .depthTest(DepthTest.ALWAYS)
            .writeMask(WriteMask.COLOR)
            .backfaceCulling(true)
            .cardinalLightingMode(CardinalLightingMode.OFF)
            .ambientOcclusion(false)
            .useOverlay(false)
            .useLight(false)
            .texture(GenesisMod.resource("textures/misc/white.png"))
            .build();

    private static SimpleQuadMesh MESH = null;

    private static SimpleQuadMesh getMesh() {
        if (MESH == null) {
            MESH = buildCubeMesh();
        }
        return MESH;
    }

    public static class StarEffectVisual implements EffectVisual<StarEffect>, DynamicVisual {
        private final Celestial celestial;
        private final Level level;
        private final StarInstance instance;
        private final Vec3i renderOrigin;

        public StarEffectVisual(Celestial celestial, Level level, VisualizationContext ctx, float partialTick) {
            this.celestial = celestial;
            this.level = level;

            SimpleModel model = new SimpleModel(List.of(new Model.ConfiguredMesh(MATERIAL, getMesh())));
            var instancer = ctx.instancerProvider().instancer(StarInstance.TYPE, model);
            instance = instancer.createInstance();
            instance.setHalfSize((float) celestial.getActualSize() / 2f);
            renderOrigin = ctx.renderOrigin();

            // Set initial transform
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

            // Transform camera position to local space (relative to star center and inverse rotation, but NOT scaled)
            // This matches what the old renderer did - camera needs to be in the same space as the [-halfSize, +halfSize] vertices
            Vector3f localCameraPos = new Vector3f(cameraPos);
            localCameraPos.sub((float) position.x(), (float) position.y(), (float) position.z());
            new Quaternionf(rotation).conjugate().transform(localCameraPos);

            // Apply vantage point transform if needed
            Celestial vantagePoint = GenesisMod.getCelestialForLevel(level);
            instance.setTransform(transform, localCameraPos);
        }

        @Override
        public Plan<Context> planFrame() {
            return RunnablePlan.of(context -> updateTransform(context.partialTick()));
        }

        @Override
        public void update(float partialTick) {}

        @Override
        public void delete() {
            instance.delete();
        }
    }


    private static SimpleQuadMesh buildCubeMesh() {
        PosVertexView vertexList = new PosVertexView();
        vertexList.vertexCount(24); // 6 faces × 4 vertices

        int vertexIndex = 0;

        // Front face (z+)
        vertexIndex = addCubeFace(vertexList, vertexIndex,
                -1.0f, -1.0f, 1.0f,
                1.0f, -1.0f, 1.0f,
                1.0f, 1.0f, 1.0f,
                -1.0f, 1.0f, 1.0f);

        // Back face (z-)
        vertexIndex = addCubeFace(vertexList, vertexIndex,
                1.0f, -1.0f, -1.0f,
                -1.0f, -1.0f, -1.0f,
                -1.0f, 1.0f, -1.0f,
                1.0f, 1.0f, -1.0f);

        // Left face (x-)
        vertexIndex = addCubeFace(vertexList, vertexIndex,
                -1.0f, -1.0f, -1.0f,
                -1.0f, -1.0f, 1.0f,
                -1.0f, 1.0f, 1.0f,
                -1.0f, 1.0f, -1.0f);

        // Right face (x+)
        vertexIndex = addCubeFace(vertexList, vertexIndex,
                1.0f, -1.0f, -1.0f,
                1.0f, 1.0f, -1.0f,
                1.0f, 1.0f, 1.0f,
                1.0f, -1.0f, 1.0f);

        // Bottom face (y-)
        vertexIndex = addCubeFace(vertexList, vertexIndex,
                -1.0f, -1.0f, -1.0f,
                1.0f, -1.0f, -1.0f,
                1.0f, -1.0f, 1.0f,
                -1.0f, -1.0f, 1.0f);

        // Top face (y+)
        addCubeFace(vertexList, vertexIndex,
                -1.0f, 1.0f, -1.0f,
                -1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f,
                1.0f, 1.0f, -1.0f);

        return new SimpleQuadMesh(vertexList, "star_cube");
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
