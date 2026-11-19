package shipwrights.genesis.space.client.map;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.resource.ResourcePackLoader;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.OrbitingTransformProvider;
import shipwrights.genesis.space.Time;
import org.joml.Quaterniondc;
import org.joml.Vector3dc;

import java.io.Reader;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Celestials loaded straight from data packs, for when there's no world to have them synced from, as at the title
/// screen.
///
/// Reads the packs a new world would start with: vanilla's, every mod's, and any that are required. A world's own
/// data packs aren't included.
///
/// @param registry every celestial that could be placed (see {@link #registryOf})
/// @param json     the JSON each was read from, by id
public record OfflineCelestials(Registry<Celestial> registry, Map<ResourceLocation, JsonElement> json) {
    private static final Logger LOGGER = LogUtils.getLogger();

    /// Every celestial in the default data packs
    public static OfflineCelestials load() {
        // As a new world's are found: vanilla's, one for each mod, and any that mods add
        PackRepository repository = new PackRepository(new ServerPacksSource());
        ResourcePackLoader.loadResourcePacks(repository, ServerLifecycleHooks::buildPackFinder);
        ModLoader.get().postEvent(new AddPackFindersEvent(PackType.SERVER_DATA, repository::addPackFinder));
        repository.reload();
        List<String> defaults = ForgeHooks.getModPacksWithVanilla();
        repository.setSelected(repository.getAvailablePacks().stream()
                .filter(pack -> pack.isRequired() || defaults.contains(pack.getId()))
                .map(Pack::getId)
                .toList());

        Map<ResourceLocation, Celestial> decoded = new LinkedHashMap<>();
        Map<ResourceLocation, JsonElement> jsons = new LinkedHashMap<>();
        FileToIdConverter converter = FileToIdConverter.json(ForgeHooks.prefixNamespace(Celestial.REGISTRY_KEY.location()));
        try (var resources = new MultiPackResourceManager(PackType.SERVER_DATA, repository.openAllSelected())) {
            for (Map.Entry<ResourceLocation, Resource> entry : converter.listMatchingResources(resources).entrySet()) {
                ResourceLocation id = converter.fileToId(entry.getKey());
                try (Reader reader = entry.getValue().openAsReader()) {
                    JsonElement json = JsonParser.parseReader(reader);
                    Celestial.CODEC.parse(JsonOps.INSTANCE, json)
                            .resultOrPartial(error -> LOGGER.error("Couldn't load celestial {}: {}", id, error))
                            .ifPresent(celestial -> {
                                decoded.put(id, celestial);
                                jsons.put(id, json);
                            });
                } catch (Exception e) {
                    LOGGER.error("Couldn't read celestial {}", id, e);
                }
            }
        }

        return new OfflineCelestials(registryOf(decoded), Map.copyOf(jsons));
    }

    /// A frozen registry of {@code celestials}, leaving out those orbiting a parent that's missing, which couldn't be
    /// placed
    public static Registry<Celestial> registryOf(Map<ResourceLocation, Celestial> celestials) {
        MappedRegistry<Celestial> registry = new MappedRegistry<>(Celestial.REGISTRY_KEY, Lifecycle.stable());
        celestials.forEach((id, celestial) -> {
            if (isPlaceable(id, celestials, new HashSet<>())) {
                registry.register(ResourceKey.create(Celestial.REGISTRY_KEY, id), celestial, Lifecycle.stable());
            } else {
                LOGGER.debug("Leaving out celestial {}, which orbits a celestial that doesn't exist", id);
            }
        });
        registry.freeze();
        return registry;
    }

    /// Those of {@code celestials} that can be placed at {@code time} among the rest: whose positions and rotations
    /// work out, rather than failing or coming to infinity, such as for orbiting a celestial that isn't there
    public static Map<ResourceLocation, Celestial> placeable(Map<ResourceLocation, Celestial> celestials, Time time) {
        Registry<Celestial> registry = registryOf(celestials);
        Map<ResourceLocation, Celestial> placeable = new LinkedHashMap<>();
        registry.entrySet().forEach(entry -> {
            try {
                Celestial celestial = entry.getValue();
                Vector3dc position = celestial.transformProvider().getPosition(registry, time);
                Quaterniondc rotation = celestial.transformProvider().getRotation(registry, time);
                if (position.isFinite() && Double.isFinite(rotation.w()) && Double.isFinite(celestial.size())) {
                    placeable.put(entry.getKey().location(), celestial);
                }
            } catch (RuntimeException e) {
                // Can't be placed
            }
        });
        return placeable;
    }

    /// Whether every parent up the chain from {@code id} exists, without looping
    private static boolean isPlaceable(ResourceLocation id, Map<ResourceLocation, Celestial> celestials,
                                       Set<ResourceLocation> visited) {
        Celestial celestial = celestials.get(id);
        if (celestial == null || !visited.add(id)) return false;
        if (celestial.transformProvider() instanceof OrbitingTransformProvider orbit) {
            return isPlaceable(orbit.parent(), celestials, visited);
        }
        return true;
    }
}
