package shipwrights.dataplanets.naming;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generates fantasy system names by picking from a datapack-configurable list
 */
public class FantasySystemNameGenerator extends SimpleJsonResourceReloadListener implements SystemNameGenerator {

    private static final Gson GSON = new GsonBuilder().create();
    private final List<String> names = new ArrayList<>();

    // Default names in case no datapack provides any
    private static final List<String> DEFAULT_NAMES = List.of(
        "Aetheria", "Celestia", "Draconis", "Elysium", "Hyperion",
        "Mystara", "Nexus", "Olympus", "Pandora", "Solaris",
        "Tartarus", "Umbra", "Valhalla", "Xanadu", "Zephyr"
    );

    public FantasySystemNameGenerator() {
        super(GSON, "system_names");
        names.addAll(DEFAULT_NAMES);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> data, ResourceManager resourceManager, ProfilerFiller profiler) {
        names.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : data.entrySet()) {
            try {
                JsonElement json = entry.getValue();

                if (json.isJsonObject() && json.getAsJsonObject().has("names")) {
                    var namesArray = json.getAsJsonObject().getAsJsonArray("names");
                    for (JsonElement nameElement : namesArray) {
                        if (nameElement.isJsonPrimitive() && nameElement.getAsJsonPrimitive().isString()) {
                            names.add(nameElement.getAsString());
                        }
                    }
                }
            } catch (JsonParseException e) {
                // Log error but continue loading other files
                System.err.println("Failed to parse system names from " + entry.getKey() + ": " + e.getMessage());
            }
        }

        // If no names were loaded, use defaults
        if (names.isEmpty()) {
            names.addAll(DEFAULT_NAMES);
        }
    }

    @Override
    public String generate(RandomSource random) {
        if (names.isEmpty()) {
            return "Unknown";
        }
        return names.get(random.nextInt(names.size())) + "-" + random.nextInt(10);
    }

    /**
     * Get the current list of available names (for debugging/testing)
     */
    public List<String> getAvailableNames() {
        return new ArrayList<>(names);
    }
}
