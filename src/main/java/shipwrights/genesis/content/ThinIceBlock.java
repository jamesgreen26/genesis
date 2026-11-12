package shipwrights.genesis.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.function.Consumer;

/// A thin layer of ice, such as over the crevasses in the moon's ice caverns. As Terraria's thin ice: it holds anything
/// walking across it, but shatters under anything that jumps or falls onto it, which falls on through at the same speed.
/// The ice doesn't break the fall: it takes the damage of the whole fall wherever it lands below (see
/// {@link shipwrights.genesis.mixin.EntityMixin}). Frost Walker, as Terraria's ice skates, lets the wearer land on it
/// safely. Only drops with Silk Touch (see its loot table).
public class ThinIceBlock extends HalfTransparentBlock {
    /// How far something must fall onto it to break it: enough for a jump or a drop of a block, but not for stepping
    /// down from a slab
    private static final float BREAKING_FALL_DISTANCE = 0.75F;

    public ThinIceBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (fallDistance <= BREAKING_FALL_DISTANCE || !(entity instanceof LivingEntity living)
                || hasFrostWalker(level, living)) {
            super.fallOn(level, state, pos, entity, fallDistance);
            return;
        }

        if (!level.isClientSide) {
            forEachThinIceUnder(level, pos, entity, icePos -> level.destroyBlock(icePos, false, entity));
        } else if (entity.isControlledByLocalInstance()) {
            // The client moves its own player, so break the ice there too rather than wait for the server, or the
            // player would land on it, losing their speed, before hearing it had gone
            forEachThinIceUnder(level, pos, entity, icePos ->
                    level.setBlock(icePos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL));
        }

        // The fall carries on through the ice, to take its damage wherever it ends. But if something else, such as a
        // block beside the ice, still holds it up, it ends here
        if (!brokeThrough(entity, pos)) {
            super.fallOn(level, state, pos, entity, fallDistance);
        }
    }

    /// Whether the entity, having landed on thin ice at the given position, broke it and has nothing else to stand
    /// on, so is still falling
    public static boolean brokeThrough(Entity entity, BlockPos landedOn) {
        Level level = entity.level();
        return !(level.getBlockState(landedOn).getBlock() instanceof ThinIceBlock)
                && level.noCollision(entity, entity.getBoundingBox().move(0.0D, -1.0E-6D, 0.0D));
    }

    /// Visits all the thin ice in the layer under the entity, rather than only the block it landed on, so that landing
    /// across several breaks them all, instead of leaving it standing on the rest
    private void forEachThinIceUnder(Level level, BlockPos landedOn, Entity entity, Consumer<BlockPos> action) {
        // Shrunk a little so that blocks only touched at the edges of its feet are left alone
        AABB feet = entity.getBoundingBox().deflate(1.0E-7D, 0.0D, 1.0E-7D);
        int y = landedOn.getY();
        for (BlockPos icePos : BlockPos.betweenClosed(
                Mth.floor(feet.minX), y, Mth.floor(feet.minZ), Mth.floor(feet.maxX), y, Mth.floor(feet.maxZ))) {
            if (level.getBlockState(icePos).is(this)) {
                action.accept(icePos.immutable());
            }
        }
    }

    /// Once the ice has broken under it, whatever broke through keeps falling at the speed it hit it, rather than
    /// stopping as on landing. This runs just after {@link #fallOn}, while the block state the entity landed on is
    /// still the ice's
    @Override
    public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        if (!brokeThrough(entity, entity.getOnPosLegacy())) {
            super.updateEntityAfterFallOn(level, entity);
        }
    }

    private static boolean hasFrostWalker(Level level, LivingEntity living) {
        return level.registryAccess().lookup(Registries.ENCHANTMENT)
                .flatMap(enchantments -> enchantments.get(Enchantments.FROST_WALKER))
                .map(frostWalker -> EnchantmentHelper.getEnchantmentLevel(frostWalker, living) > 0)
                .orElse(false);
    }
}
