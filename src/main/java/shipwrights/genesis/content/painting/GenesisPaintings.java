package shipwrights.genesis.content.painting;

import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import shipwrights.genesis.GenesisMod;

public class GenesisPaintings {
    public static final DeferredRegister<PaintingVariant> PAINTING_VARIANTS =
            DeferredRegister.create(ForgeRegistries.PAINTING_VARIANTS, GenesisMod.MOD_ID);

    public static final RegistryObject<PaintingVariant> SPACE_0 = PAINTING_VARIANTS.register("space_0",
            () -> new PaintingVariant(64, 64));
}
