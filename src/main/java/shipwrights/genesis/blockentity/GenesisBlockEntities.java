package shipwrights.genesis.blockentity;

import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.GenesisBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class GenesisBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, GenesisMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<NavProjectorBlockEntity>> NAV_PROJECTOR =
        BLOCK_ENTITIES.register("nav_projector",
            () -> BlockEntityType.Builder.of(NavProjectorBlockEntity::new,
                GenesisBlocks.NAV_PROJECTOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<VoidEngineInterfaceBlockEntity>> VOID_ENGINE_INTERFACE =
        BLOCK_ENTITIES.register("void_engine_interface",
            () -> BlockEntityType.Builder.of(VoidEngineInterfaceBlockEntity::new,
                GenesisBlocks.VOID_ENGINE_INTERFACE.get()).build(null));

    public static final RegistryObject<BlockEntityType<VoidCoreBlockEntity>> VOID_CORE =
        BLOCK_ENTITIES.register("void_core",
            () -> BlockEntityType.Builder.of(VoidCoreBlockEntity::new,
                GenesisBlocks.VOID_CORE.get()).build(null));

    public static final RegistryObject<BlockEntityType<WarpstoneCatalyzerBlockEntity>> WARPSTONE_CATALYZER_BLOCK_ENTITY = BLOCK_ENTITIES.register("warpstone_catalyzer_block",
            () -> BlockEntityType.Builder.of(WarpstoneCatalyzerBlockEntity::new, GenesisBlocks.WARPSTONE_CATALYZER_BLOCK.get()).build(null));
}
