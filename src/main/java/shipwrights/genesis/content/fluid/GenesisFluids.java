package shipwrights.genesis.content.fluid;

import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.util.entry.FluidEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import shipwrights.genesis.GenesisMod;

import java.util.function.Consumer;

/**
 * Fluid registration using Registrate to avoid circular dependency issues.
 * Registrate.create() automatically registers event listeners.
 */
public class GenesisFluids {

    // Registrate.create() auto-registers event listeners via getModEventBus()
    public static final Registrate REGISTRATE = Registrate.create(GenesisMod.MOD_ID);

    private static final ResourceLocation STILL_RL = ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "block/miasma_still");
    private static final ResourceLocation FLOWING_RL = ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "block/miasma_flow");
    private static final ResourceLocation OVERLAY_RL = ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "block/miasma_overlay");
    private static final int TINT_COLOR = 0xCC8B9A32;

    public static final FluidEntry<MiasmaFluid.Flowing> MIASMA = REGISTRATE
            .fluid("miasma", STILL_RL, FLOWING_RL, GenesisFluids::createMiasmaFluidType, MiasmaFluid.Flowing::new)
            .lang("Miasma")
            .properties(p -> p
                    .density(0)
                    .viscosity(0)
                    .canConvertToSource(false)
                    .canPushEntity(false)
                    .fallDistanceModifier(1f)
                    .canSwim(false)
                    .canDrown(false)
                    .supportsBoating(false))
            .fluidProperties(p -> p
                    .levelDecreasePerBlock(2)
                    .slopeFindDistance(3)
                    .tickRate(10))
            .source(MiasmaFluid.Source::new)
            .block(MiasmaLiquidBlock::new)
                .initialProperties(() -> net.minecraft.world.level.block.Blocks.WATER)
                .properties(p -> p
                        .mapColor(MapColor.COLOR_YELLOW)
                        .replaceable()
                        .noCollission()
                        .strength(100.0F)
                        .noLootTable()
                        .liquid())
                .build()
            .bucket()
                .build()
            .register();

    private static FluidType createMiasmaFluidType(FluidType.Properties properties, ResourceLocation stillTexture, ResourceLocation flowingTexture) {
        return new FluidType(properties) {
            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(new IClientFluidTypeExtensions() {
                    @Override
                    public ResourceLocation getStillTexture() {
                        return stillTexture;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return flowingTexture;
                    }

                    @Override
                    public ResourceLocation getOverlayTexture() {
                        return OVERLAY_RL;
                    }

                    @Override
                    public int getTintColor() {
                        return TINT_COLOR;
                    }
                });
            }
        };
    }

    // Helper methods to access source/flowing fluids
    public static ForgeFlowingFluid.Source getSource() {
        return (ForgeFlowingFluid.Source) MIASMA.getSource();
    }

    public static MiasmaFluid.Flowing getFlowing() {
        return MIASMA.get();
    }

    /**
     * Call this from mod constructor to ensure static initialization happens.
     * The actual registration is done automatically by Registrate.create().
     */
    public static void init() {
        GenesisMod.LOGGER.info("Initializing Genesis fluids via Registrate");
    }
}
