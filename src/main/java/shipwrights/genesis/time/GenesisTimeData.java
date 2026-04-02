package shipwrights.genesis.time;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class GenesisTimeData extends SavedData {

    private static final String DATA_NAME = "genesis_time";
    private long timeOffset = 0;

    public static GenesisTimeData getOrCreate(MinecraftServer server) {

        return server.overworld().getDataStorage().computeIfAbsent(
                GenesisTimeData::load,
                GenesisTimeData::new,
                DATA_NAME
        );
    }

    public static GenesisTimeData load(CompoundTag tag) {
        GenesisTimeData data = new GenesisTimeData();
        data.timeOffset = tag.getLong("timeOffset");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("timeOffset", timeOffset);
        return tag;
    }

    public long getTimeOffset() {
        return timeOffset;
    }

    public void addOffset(long delta) {
        timeOffset += delta;
        setDirty();
    }
}
