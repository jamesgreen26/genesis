package shipwrights.genesis.space.renderer.star;

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

public class StarInstance extends AbstractInstance {
    public Matrix4fc pose = new Matrix4f();
    public Vector3f localCameraPos = new Vector3f();
    public float halfSize;

    protected StarInstance(InstanceType<?> type, InstanceHandle handle) {
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

    public static final InstanceType<StarInstance> TYPE
            = SimpleInstanceType.builder(StarInstance::new)
            .cullShader(GenesisMod.resource("instance/cull/star.glsl"))
            .vertexShader(GenesisMod.resource("instance/star.vert"))
            .layout(LayoutBuilder.create()
                    .matrix("pose", FloatRepr.FLOAT, 4)
                    .vector("localCameraPos", FloatRepr.FLOAT, 3)
                    .scalar("halfSize", FloatRepr.FLOAT)
                    .build()
            )
            .writer((ptr, instance) -> {
                // mat4 (64 bytes)
                ExtraMemoryOps.putMatrix4f(ptr, instance.pose);

                // vec3 (12 bytes)
                MemoryUtil.memPutFloat(ptr + 64, instance.localCameraPos.x);
                MemoryUtil.memPutFloat(ptr + 68, instance.localCameraPos.y);
                MemoryUtil.memPutFloat(ptr + 72, instance.localCameraPos.z);

                // float (4 bytes)
                MemoryUtil.memPutFloat(ptr + 76, instance.halfSize);
            })
            .build();
}
