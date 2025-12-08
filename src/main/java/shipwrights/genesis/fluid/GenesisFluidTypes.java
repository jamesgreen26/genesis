package shipwrights.genesis.fluid;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import shipwrights.genesis.GenesisMod;

public class GenesisFluidTypes {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, GenesisMod.MOD_ID);

    private static final ResourceLocation STILL_RL = ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "block/miasma_still");
    private static final ResourceLocation FLOWING_RL = ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "block/miasma_flow");
    private static final ResourceLocation OVERLAY_RL = ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "block/miasma_overlay");

    public static final RegistryObject<FluidType> MIASMA_FLUID_TYPE = FLUID_TYPES.register("miasma",
            () -> new BaseFluidType(
                    FluidType.Properties.create()
                            .descriptionId("fluid.genesis.miasma")
                            .density(0)
                            .viscosity(800)
                            .canSwim(false)
                            .canDrown(false)
                            .supportsBoating(false)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY),
                    STILL_RL, FLOWING_RL, OVERLAY_RL,
                    0xCC8B9A32
            ));

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
    }
}
