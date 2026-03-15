package shipwrights.genesis.commands;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import shipwrights.genesis.GenesisMod;

public final class GenesisCommandArguments {
    private GenesisCommandArguments() {}

    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES =
            DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, GenesisMod.MOD_ID);

    public static final RegistryObject<ArgumentTypeInfo<?, ?>> CELESTIAL =
            ARGUMENT_TYPES.register("celestial", () -> SingletonArgumentInfo.contextFree(CelestialArgument::celestial));

    public static void register(IEventBus bus) {
        ARGUMENT_TYPES.register(bus);
    }
}
