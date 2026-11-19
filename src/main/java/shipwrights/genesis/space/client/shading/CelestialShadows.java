package shipwrights.genesis.space.client.shading;

import com.mojang.blaze3d.shaders.AbstractUniform;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.Registry;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4dc;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.Time;
import shipwrights.genesis.space.math.OBB;

import java.util.List;

/// What the components that draw shadows on a celestial have in common
public final class CelestialShadows {
    private CelestialShadows() {
    }

    // Must match shadow_volumes.glsl
    private static final int MAX_VOLUMES = 4;

    /// The cube a celestial is
    public static OBB cube(Registry<Celestial> celestials, Celestial celestial, Time time) {
        return OBB.createCube(
                celestial.size(),
                celestial.transformProvider().getRotation(celestials, time),
                celestial.transformProvider().getPosition(celestials, time)
        );
    }

    /// Every celestial that casts shadows, other than {@code except}
    public static List<OBB> casters(Registry<Celestial> celestials, Celestial except, Time time) {
        return celestials.stream()
                .filter(it -> it.castsShadows() && it != except)
                .map(it -> cube(celestials, it, time))
                .toList();
    }

    /// Tells a shader that includes {@code shadow_volumes.glsl} what shades the thing it's about to draw
    ///
    /// @param toShaderSpace from the local space the volumes are in to the space the shader works out shadows in.
    ///                      It may only rotate and translate, so that distances are the same in both.
    public static void setUniforms(ShaderInstance shader, List<ShadowVolume> volumes, Matrix4f toShaderSpace) {
        // More occluders than this at once would leave the rest's shadows undrawn
        int volumeCount = Math.min(volumes.size(), MAX_VOLUMES);
        shader.safeGetUniform("ShadowVolumeCount").set((float) volumeCount);
        for (int volumeIndex = 0; volumeIndex < volumeCount; volumeIndex++) {
            ShadowVolume volume = volumes.get(volumeIndex);
            List<Vector4dc> planes = volume.planes();
            for (int i = 0; i < ShadowVolume.MAX_PLANES; i++) {
                AbstractUniform uniform = shader.safeGetUniform("ShadowPlane[" + (volumeIndex * ShadowVolume.MAX_PLANES + i) + "]");
                if (i >= planes.size()) {
                    // Everywhere is fully inside an unused plane
                    uniform.set(0f, 0f, 0f, 1f);
                    continue;
                }
                // Scaled so that the shader gets depths as a fraction of the soft edge's width
                Vector4dc plane = planes.get(i);
                Vector3f normal = toShaderSpace.transformDirection(
                        new Vector3f((float) plane.x(), (float) plane.y(), (float) plane.z()));
                normal.div((float) volume.edgeWidth());
                uniform.set(normal.x, normal.y, normal.z, (float) (plane.w() / volume.edgeWidth()));
            }
        }
    }
}
