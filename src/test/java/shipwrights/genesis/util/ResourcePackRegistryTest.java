package shipwrights.genesis.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.InactiveProfiler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.MinecraftBootstrap;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourcePackRegistryTest {

    private static final ResourceKey<Registry<Integer>> KEY = ResourceKey.createRegistryKey(GenesisMod.id("numbers"));

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftBootstrap.bootstrap();
    }

    @Test
    void emptyBeforeFirstLoad() {
        var registry = new ResourcePackRegistry<>(KEY, Codec.INT);
        assertEquals(0, registry.registry().size());
    }

    @Test
    void loadsValidEntriesAndSkipsInvalidOnes() {
        var registry = new ResourcePackRegistry<>(KEY, Codec.INT);
        ResourceLocation one = GenesisMod.id("one"), bad = GenesisMod.id("bad");

        registry.apply(Map.<ResourceLocation, JsonElement>of(
                one, new JsonPrimitive(1),
                bad, new JsonPrimitive("not a number")
        ), null, InactiveProfiler.INSTANCE);

        assertEquals(1, registry.registry().size());
        assertEquals(1, registry.registry().get(one));
        assertTrue(registry.registry().getOptional(bad).isEmpty());
    }

    @Test
    void reloadReplacesPreviousEntries() {
        var registry = new ResourcePackRegistry<>(KEY, Codec.INT);
        ResourceLocation one = GenesisMod.id("one"), two = GenesisMod.id("two");

        registry.apply(Map.of(one, new JsonPrimitive(1)), null, InactiveProfiler.INSTANCE);
        registry.apply(Map.of(two, new JsonPrimitive(2)), null, InactiveProfiler.INSTANCE);

        assertTrue(registry.registry().getOptional(one).isEmpty());
        assertEquals(2, registry.registry().get(two));
    }
}
