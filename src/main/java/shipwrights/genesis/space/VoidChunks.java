package shipwrights.genesis.space;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraftforge.event.level.ChunkDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.config.GenesisCommonConfig;

/// Keeps the chunks of space cheap to fly through. Space has no terrain, so its chunks hold nothing, yet each one
/// crossed into is still generated, sent, kept and saved as any other. So fewer are loaded around each player there
/// (see {@link #viewDistance}), they're made empty outright rather than generated (see {@link #empty}), and those
/// holding nothing are never saved (see {@link #leaveUnsaved}): they're made again, the same, when next needed.
///
/// Applied through {@link shipwrights.genesis.mixin.ChunkMapMixin}
@Mod.EventBusSubscriber(modid = GenesisMod.MOD_ID)
public final class VoidChunks {

    private VoidChunks() {
    }

    /// The view distance {@code level} uses when its server asks for {@code requested}: no more than
    /// {@linkplain GenesisCommonConfig#spaceViewDistance() the configured limit} in space
    public static int viewDistance(ServerLevel level, int requested) {
        if (!GenesisMod.isSpaceLikeDimension(level)) return requested;
        return Math.min(requested, GenesisCommonConfig.spaceViewDistance());
    }

    /// Whether {@code chunk}, in {@code level}, is a chunk of space holding nothing worth saving: no blocks, no block
    /// entities, and no capability data another mod has attached to it. Entities aren't kept in chunks, so they're saved
    /// either way
    public static boolean isVoid(ServerLevel level, ChunkAccess chunk) {
        if (!GenesisMod.isSpaceLikeDimension(level) || !(chunk instanceof LevelChunk levelChunk)) return false;
        if (!chunk.getBlockEntitiesPos().isEmpty()) return false;
        CompoundTag caps = levelChunk.writeCapsToNBT();
        if (caps != null && !caps.isEmpty()) return false;
        for (LevelChunkSection section : chunk.getSections()) {
            if (!section.hasOnlyAir()) return false;
        }
        return true;
    }

    /// Whether {@code level} is space whose generator would only ever make empty chunks: a flat one with no layers,
    /// features or structures, as space's is. A data pack that gives space terrain has it generated as usual
    public static boolean generatesNothing(ServerLevel level) {
        if (!GenesisMod.isSpaceLikeDimension(level)) return false;
        ServerChunkCache chunks = level.getChunkSource();
        if (!(chunks.getGenerator() instanceof FlatLevelSource flat)) return false;
        if (!flat.settings().getLayers().stream().allMatch(state -> state == null || state.isAir())) return false;
        if (!chunks.getGeneratorState().possibleStructureSets().isEmpty()) return false;
        return flat.getBiomeGenerationSettings(flat.settings().getBiome()).features().stream()
                .allMatch(step -> step.size() == 0);
    }

    /// The chunk generating would make at {@code pos} in a level that {@linkplain #generatesNothing generates nothing}:
    /// empty but for its biomes, and whole, as one read from disk is. A chunk that has to be generated needs those
    /// around it part generated first, eleven chunks out, each in a dozen steps. One that's whole already only needs
    /// its neighbours loaded
    public static ChunkAccess empty(ServerLevel level, ChunkPos pos) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        LevelChunk chunk = new LevelChunk(level, pos);
        chunk.fillBiomesFromNoise(generator.getBiomeSource(), level.getChunkSource().randomState().sampler());
        // Nothing in it to light
        chunk.setLightCorrect(true);
        chunk.setUnsaved(false);
        return new ImposterProtoChunk(chunk, false);
    }

    /// Whether {@code chunk}, about to be saved in {@code level}, should be left unsaved instead: it's
    /// {@linkplain #isVoid void}, and there's nothing on disk in its place, which may have held something that has to
    /// be saved as gone. Otherwise it's about to be on disk, and is from then on
    public static boolean leaveUnsaved(ServerLevel level, ChunkAccess chunk) {
        OnDisk onDisk = (OnDisk) chunk;
        if (isVoid(level, chunk) && !onDisk.genesis$isOnDisk()) return true;
        onDisk.genesis$setOnDisk();
        return false;
    }

    /// Every chunk read from disk comes through here, whatever reads it, on whichever thread
    @SubscribeEvent
    public static void onChunkRead(ChunkDataEvent.Load event) {
        ((OnDisk) event.getChunk()).genesis$setOnDisk();
    }

    /// Whether a chunk is on disk, having been read from it or saved to it: kept on the chunk itself, so it's known
    /// without asking the disk. Every chunk is one (see {@link shipwrights.genesis.mixin.ChunkAccessMixin})
    public interface OnDisk {
        boolean genesis$isOnDisk();

        void genesis$setOnDisk();
    }
}
