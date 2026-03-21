package shipwrights.genesis.content.sound;

import shipwrights.genesis.GenesisMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class GenesisSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
        DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, GenesisMod.MOD_ID);

    public static final RegistryObject<SoundEvent> VOID_ENGINE_START = SOUND_EVENTS.register("void_engine_start",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "void_engine_start")));

    public static final RegistryObject<SoundEvent> VOID_ENGINE_TRAVEL = SOUND_EVENTS.register("void_engine_travel",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "void_engine_travel")));

    public static final RegistryObject<SoundEvent> VOID_ENGINE_STOP = SOUND_EVENTS.register("void_engine_stop",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "void_engine_stop")));

    public static final RegistryObject<SoundEvent> WORMHOLE_AMBIANCE = SOUND_EVENTS.register("wormhole_ambiance",
        () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "wormhole_ambiance")));

    public static final RegistryObject<SoundEvent> MIASMA_HISS = SOUND_EVENTS.register("miasma_hiss",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "miasma_hiss")));

    public static final RegistryObject<SoundEvent> VOID_CORE_ORE_SOUND = SOUND_EVENTS.register("void_core_ore_sound",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "void_core_ore_sound")));
}
