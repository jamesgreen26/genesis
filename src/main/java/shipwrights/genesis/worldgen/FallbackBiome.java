package shipwrights.genesis.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import shipwrights.genesis.GenesisMod;

import java.util.Optional;

/// The biome the client takes a dimension to have where it hasn't loaded the chunks, for the fog and sky colours there
/// (see {@link shipwrights.genesis.mixin.ClientLevelMixin}), rather than vanilla's plains. Keyed by the dimension's id,
/// and synced to clients, which are never sent the dimension's own definition.
@EventBusSubscriber(modid = GenesisMod.MOD_ID)
public record FallbackBiome(ResourceKey<Biome> biome) {
    public static final ResourceKey<Registry<FallbackBiome>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(GenesisMod.id("fallback_biome"));

    public static final Codec<FallbackBiome> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceKey.codec(Registries.BIOME).fieldOf("biome").forGetter(FallbackBiome::biome)
    ).apply(instance, FallbackBiome::new));

    @SubscribeEvent
    public static void registerDataPackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(REGISTRY_KEY, CODEC, CODEC);
    }

    public static Optional<FallbackBiome> of(Level level) {
        return level.registryAccess().registryOrThrow(REGISTRY_KEY).getOptional(level.dimension().location());
    }
}
