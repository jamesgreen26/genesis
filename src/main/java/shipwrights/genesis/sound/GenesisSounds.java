package shipwrights.genesis.sound;

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
}
