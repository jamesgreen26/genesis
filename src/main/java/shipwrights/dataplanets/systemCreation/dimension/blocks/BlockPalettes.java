package shipwrights.dataplanets.systemCreation.dimension.blocks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber
public class BlockPalettes {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockPalettes.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Set<BlockInfo> SOLIDS = new HashSet<>();
    public static Set<BlockInfo> FLUIDS = new HashSet<>();

    record BlockPalette(List<BlockInfo> values, Boolean replace) {
        public static final Codec<BlockPalette> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        BlockInfo.CODEC.listOf().fieldOf("values").forGetter(BlockPalette::values),
                        Codec.BOOL.fieldOf("replace").forGetter(BlockPalette::replace)
                ).apply(instance, BlockPalette::new)
        );
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new SimpleJsonResourceReloadListener(GSON, "block_palettes/solids") {
            @Override
            protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
                SOLIDS.clear();

                for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
                    try {
                        BlockPalette palette = BlockPalette.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                                .resultOrPartial(error -> LOGGER.error("Failed to parse block palette {}: {}", entry.getKey(), error))
                                .orElse(null);

                        if (palette != null) {
                            if (palette.replace()) {
                                SOLIDS.clear();
                            }
                            SOLIDS.addAll(palette.values());
                            LOGGER.info("Loaded {} solid block entries from {}", palette.values().size(), entry.getKey());
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error loading solid block palette {}", entry.getKey(), e);
                    }
                }

                LOGGER.info("Total solid blocks loaded: {}", SOLIDS.size());
            }
        });

        event.addListener(new SimpleJsonResourceReloadListener(GSON, "block_palettes/fluids") {
            @Override
            protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
                FLUIDS.clear();

                for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
                    try {
                        BlockPalette palette = BlockPalette.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                                .resultOrPartial(error -> LOGGER.error("Failed to parse block palette {}: {}", entry.getKey(), error))
                                .orElse(null);

                        if (palette != null) {
                            if (palette.replace()) {
                                FLUIDS.clear();
                            }
                            FLUIDS.addAll(palette.values());
                            LOGGER.info("Loaded {} fluid block entries from {}", palette.values().size(), entry.getKey());
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error loading fluid block palette {}", entry.getKey(), e);
                    }
                }

                LOGGER.info("Total fluid blocks loaded: {}", FLUIDS.size());
            }
        });
    }

}
