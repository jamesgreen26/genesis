package shipwrights.genesis.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.worldgen.FallbackBiome;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Unique
    private static final ResourceKey<Biome> GENESIS$GEOTHERMAL_DEPTHS =
            ResourceKey.create(Registries.BIOME, GenesisMod.id("moon_geothermal_depths"));
    @Unique
    private static final float GENESIS$STEAM_CHANCE = 0.6F;
    /// ZPS's buoyant steam puff, as its vents use. Looked up by id, as its type lives in Kelvin, which we don't compile
    /// against
    @Unique
    private static final ResourceLocation GENESIS$STEAM_PARTICLE = ResourceLocation.fromNamespaceAndPath("zps", "steam");

    /// Steam rising off still water in the moon's geothermal depths. Rides vanilla's ambient tick, which samples random
    /// blocks around the player each tick, so it's spread over the nearby pools the way ambient block effects are
    @Inject(method = "doAnimateTick", at = @At("TAIL"))
    private void genesis$steamOffGeothermalPools(int x, int y, int z, int range, RandomSource random, Block block,
                                                 BlockPos.MutableBlockPos pos, CallbackInfo ci) {
        ClientLevel level = (ClientLevel) (Object) this;
        if (random.nextFloat() >= GENESIS$STEAM_CHANCE || !level.getFluidState(pos).is(Fluids.WATER)
                || !level.getBlockState(pos.above()).isAir() || !level.getBiome(pos).is(GENESIS$GEOTHERMAL_DEPTHS)) {
            return;
        }
        if (!(BuiltInRegistries.PARTICLE_TYPE.get(GENESIS$STEAM_PARTICLE) instanceof ParticleOptions steam)) {
            return;
        }
        level.addParticle(steam,
                pos.getX() + random.nextDouble(), pos.getY() + 0.95, pos.getZ() + random.nextDouble(),
                0.0, 0.02 + random.nextDouble() * 0.02, 0.0);
    }

    /// Where it hasn't loaded the chunks, the client takes the biome to be plains, tinting the fog and sky there plains'
    /// blue. Take it to be the dimension's {@link FallbackBiome} instead, where it has one
    @Inject(method = "getUncachedNoiseBiome", at = @At("HEAD"), cancellable = true)
    private void genesis$fallBackToDimensionBiome(int x, int y, int z, CallbackInfoReturnable<Holder<Biome>> cir) {
        ClientLevel level = (ClientLevel) (Object) this;
        FallbackBiome.of(level).flatMap(fallback -> level.registryAccess().registryOrThrow(Registries.BIOME)
                .getHolder(fallback.biome())).ifPresent(cir::setReturnValue);
    }
}
