package shipwrights.genesis.space.renderer.planet;

import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.layout.FloatRepr;
import dev.engine_room.flywheel.api.layout.LayoutBuilder;
import dev.engine_room.flywheel.lib.instance.AbstractInstance;
import dev.engine_room.flywheel.lib.instance.SimpleInstanceType;
import dev.engine_room.flywheel.lib.util.ExtraMemoryOps;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import shipwrights.genesis.GenesisMod;

public class PlanetInstance extends AbstractInstance {
    public Matrix4fc pose = new Matrix4f();
    public Vector3f localCameraPos = new Vector3f();
    public float halfSize;

    protected PlanetInstance(InstanceType<?> type, InstanceHandle handle) {
        super(type, handle);
    }

    public void setHalfSize(float halfSize) {
        this.halfSize = halfSize;
    }

    public void setTransform(Matrix4fc transform, Vector3f localCameraPos) {
        this.pose = transform;
        this.localCameraPos = localCameraPos;
        this.setChanged();
    }

    public static final InstanceType<PlanetInstance> TYPE
            = SimpleInstanceType.builder(PlanetInstance::new)
            .cullShader(GenesisMod.resource("instance/cull/planet.glsl"))
            .vertexShader(GenesisMod.resource("instance/planet.vert"))
            .layout(LayoutBuilder.create()
                    .matrix("pose", FloatRepr.FLOAT, 4)
                    .vector("localCameraPos", FloatRepr.FLOAT, 3)
                    .scalar("halfSize", FloatRepr.FLOAT)
                    .build()
            )
            .writer((ptr, instance) -> {
                ExtraMemoryOps.putMatrix4f(ptr, instance.pose);

                ExtraMemoryOps.putVector3f(ptr + 64, instance.localCameraPos);

                MemoryUtil.memPutFloat(ptr + 76, instance.halfSize);
            })
            .build();
}
