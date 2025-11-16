package shipwrights.genesis.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;
import org.joml.primitives.AABBi;
import org.joml.primitives.AABBic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.util.AABBdUtilKt;
import org.valkyrienskies.core.util.VectorConversionsKt;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

@Mixin(value = ClientLevel.class, priority = 1500)
public class VSClientLevelMixin {

    @Unique
    private final RandomSource genesis$vsRandom = RandomSource.create();

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private void trySpawnDripParticles(BlockPos blockPos, BlockState blockState, ParticleOptions particleOptions, boolean bl2) {
    }

    @TargetHandler(
            mixin = "org.valkyrienskies.mod.mixin.client.world.MixinClientLevel",
            name = "afterAnimatedTick"
    )
    @WrapMethod(
            method = "@MixinSquared:Handler"
    )
    private void genesis$tickFewerBlocksOnMiniShips(int posX, int posY, int posZ, CallbackInfo ci, Operation<Void> original) {
        boolean holdingBarrierItem = false;
        if (this.minecraft.gameMode.getPlayerMode() == GameType.CREATIVE) {
            for (final ItemStack itemStack : this.minecraft.player.getHandSlots()) {
                if (itemStack.getItem() == Blocks.BARRIER.asItem()) {
                    holdingBarrierItem = true;
                    break;
                }
            }
        }

        // use more precise player position, since ship blocks aren't locked to the grid of integer coordinates in world space
        final Vec3 pos = this.minecraft.player.position();

        final AABBdc origin = new AABBd(pos.x, pos.y, pos.z, pos.x, pos.y, pos.z);
        final AABBdc shipIntersectBB = AABBdUtilKt.expand(new AABBd(origin), 32.0);
        final double biggerBBProbability = 668.0 / (32.0 * 32.0 * 32.0);
        final double smallerBBProbability = 668.0 / (16.0 * 16.0 * 16.0);

        final AABBd temp0 = new AABBd();
        final AABBi temp1 = new AABBi();
        final AABBd temp2 = new AABBd();
        final AABBi temp3 = new AABBi();
        final AABBi temp4 = new AABBi();
        final AABBi temp5 = new AABBi();
        final AABBd temp6 = new AABBd();
        final AABBd temp7 = new AABBd();
        for (final Ship ship : VSGameUtilsKt.getShipsIntersecting(ClientLevel.class.cast(this), shipIntersectBB)) {
            final AABBic shipVoxelAABB = ship.getShipAABB();
            if (shipVoxelAABB == null) {
                continue;
            }

            // This reverses any scaling that would happen when transforming to ship space.
            // We do this to ensure that the same number of blocks are ticked regardless of ship scale
            // Otherwise, mini ships tick too many blocks and cause lag
            final double distanceScaling = ship.getTransform().getShipToWorldScaling().x();
            final AABBdc biggerBB = AABBdUtilKt.expand(temp6.set(origin), 32.0 * distanceScaling);
            final AABBdc smallerBB = AABBdUtilKt.expand(temp7.set(origin), 16.0 * distanceScaling);

            // Only spawn particles in the intersection of the ship bounding box and the particle spawning bounding
            // boxes surrounding the player
            final AABBic biggerBBTransformed =
                    VectorConversionsKt.toAABBi(biggerBB.transform(ship.getWorldToShip(), temp0), temp1);
            final AABBic smallerBBTransformed =
                    VectorConversionsKt.toAABBi(smallerBB.transform(ship.getWorldToShip(), temp2), temp3);

            // Expand [shipVoxelAABB] by 1 on each side to account for blocks like torches not expanding the voxel AABB
            final AABBic biggerBBIntersection =
                    VectorConversionsKt.expand(shipVoxelAABB, 1, temp4).intersection(biggerBBTransformed);
            final AABBic smallerBBIntersection =
                    VectorConversionsKt.expand(shipVoxelAABB, 1, temp5).intersection(smallerBBTransformed);

            if (biggerBBIntersection.isValid()) {
                genesis$animateTickVS(biggerBBIntersection, biggerBBProbability, holdingBarrierItem);
            }
            if (smallerBBIntersection.isValid()) {
                genesis$animateTickVS(smallerBBIntersection, smallerBBProbability, holdingBarrierItem);
            }
        }
    }

    @Unique
    private void genesis$animateTickVS(
            final AABBic region,
            final double regionBlockProbability,
            final boolean holdingBarrierItem
    ) {
        final int volume = (region.maxX() - region.minX() + 1) * (region.maxY() - region.minY() + 1)
                * (region.maxZ() - region.minZ() + 1);
        final double blocksToTickAsDouble = volume * regionBlockProbability;
        int blocksToTick = (int) Math.floor(blocksToTickAsDouble);
        // Handle the case of partial blocks to tick
        if (genesis$vsRandom.nextDouble() > blocksToTickAsDouble - blocksToTick) {
            blocksToTick++;
        }
        final ClientLevel thisAsClientLevel = ClientLevel.class.cast(this);
        final BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < blocksToTick; i++) {
            final int posX = region.minX() + genesis$vsRandom.nextInt(region.maxX() - region.minX() + 1);
            final int posY = region.minY() + genesis$vsRandom.nextInt(region.maxY() - region.minY() + 1);
            final int posZ = region.minZ() + genesis$vsRandom.nextInt(region.maxZ() - region.minZ() + 1);

            mutableBlockPos.set(posX, posY, posZ);
            final BlockState blockState = thisAsClientLevel.getBlockState(mutableBlockPos);
            blockState.getBlock().animateTick(blockState, thisAsClientLevel, mutableBlockPos, genesis$vsRandom);
            final FluidState fluidState = thisAsClientLevel.getFluidState(mutableBlockPos);
            if (!fluidState.isEmpty()) {
                fluidState.animateTick(thisAsClientLevel, mutableBlockPos, genesis$vsRandom);
                final ParticleOptions particleOptions = fluidState.getDripParticle();
                if (particleOptions != null && genesis$vsRandom.nextInt(10) == 0) {
                    final boolean bl2 = blockState.isFaceSturdy(thisAsClientLevel, mutableBlockPos, Direction.DOWN);
                    final BlockPos blockPos = mutableBlockPos.below();
                    this.trySpawnDripParticles(blockPos, thisAsClientLevel.getBlockState(blockPos), particleOptions,
                            bl2);
                }
            }

            if (holdingBarrierItem && blockState.is(Blocks.BARRIER)) {
                thisAsClientLevel.addParticle(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, blockState),
                        (double) posX + 0.5, (double) posY + 0.5,
                        (double) posZ + 0.5, 0.0, 0.0, 0.0);
            }
        }
    }
}