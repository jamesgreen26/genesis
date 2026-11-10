package shipwrights.genesis;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.ModList;

import java.nio.file.Path;

/// A built-in data pack, from the {@code overrides} folder in our resources, that replaces other mods' data (such as
/// ZPS's beam collector recipe). It's required and sits at the top, so it always outranks every mod's data pack, in
/// existing worlds too, which keep the order of mod data packs they were created with.
public final class GenesisOverridesPack {
    private static final String ID = "genesis_overrides";

    private GenesisOverridesPack() {
    }

    public static void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) {
            return;
        }
        Path root = ModList.get().getModFileById(GenesisMod.MOD_ID).getFile().findResource("overrides");
        Pack pack = Pack.readMetaAndCreate(ID, Component.literal("Genesis Overrides"), true,
                id -> new PathPackResources(id, root, true), PackType.SERVER_DATA, Pack.Position.TOP, PackSource.BUILT_IN);
        if (pack != null) {
            event.addRepositorySource(consumer -> consumer.accept(pack));
        }
    }
}
