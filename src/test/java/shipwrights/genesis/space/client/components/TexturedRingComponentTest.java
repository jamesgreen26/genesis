package shipwrights.genesis.space.client.components;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import shipwrights.genesis.space.client.components.TexturedRingComponent.Axis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TexturedRingComponentTest {

    private static TexturedRingComponent parse(String json) {
        return TexturedRingComponent.CODEC.codec().parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    @Test
    void codec_ringIsAroundTheYAxisUnlessToldOtherwise() {
        assertEquals(new TexturedRingComponent(1.5, 2.5, Axis.Y), parse("{\"innerSize\": 1.5, \"outerSize\": 2.5}"));
    }

    @Test
    void codec_readsTheAxis() {
        assertEquals(Axis.X, parse("{\"innerSize\": 1.5, \"outerSize\": 2.5, \"axis\": \"x\"}").axis());
        assertEquals(Axis.Z, parse("{\"innerSize\": 1.5, \"outerSize\": 2.5, \"axis\": \"z\"}").axis());
    }

    @Test
    void codec_rejectsWhatIsNotAnAxis() {
        var result = TexturedRingComponent.CODEC.codec().parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"innerSize\": 1.5, \"outerSize\": 2.5, \"axis\": \"w\"}"));
        assertTrue(result.isError());
    }

    @Test
    void codec_leavesOutTheAxisWhenItIsY() {
        JsonElement aroundY = TexturedRingComponent.CODEC.codec()
                .encodeStart(JsonOps.INSTANCE, new TexturedRingComponent(1.5, 2.5, Axis.Y)).getOrThrow();
        JsonElement aroundX = TexturedRingComponent.CODEC.codec()
                .encodeStart(JsonOps.INSTANCE, new TexturedRingComponent(1.5, 2.5, Axis.X)).getOrThrow();

        assertFalse(aroundY.getAsJsonObject().has("axis"));
        assertEquals("x", aroundX.getAsJsonObject().get("axis").getAsString());
    }

    @Test
    void axis_ringIsFlatAlongTheAxisItIsAround() {
        for (Axis axis : Axis.values()) {
            Vector3f point = axis.place(3, 5);
            assertEquals(0, point.get(axis.ordinal()), axis + " should be left at zero");
            assertEquals(34, point.lengthSquared(), 1e-6, "Both of the plane's directions should be kept");
        }
    }

    @Test
    void axis_planeTurnsTheSameWayAroundEachAxis() {
        // The plane's first direction crossed with its second points the same way along each axis
        for (Axis axis : Axis.values()) {
            Vector3f normal = axis.place(1, 0).cross(axis.place(0, 1));
            assertEquals(-1, normal.get(axis.ordinal()), 1e-6, axis.toString());
        }
    }
}
