package shipwrights.genesis.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.joml.Vector3d;
import shipwrights.genesis.extension.FallingBlockEntityExtension;
import shipwrights.genesis.mixin.FallingBlockEntityAccessor;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class AsteroidBlock extends Block {

    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 9);
    public static final IntegerProperty PALETTE = IntegerProperty.create("palette", 0, 1);
    private static final ConcurrentHashMap<BlockPos, Damage> damageMap = new ConcurrentHashMap<>();
    private static final long DAMAGE_TIMEOUT_MS = 120_000; // 1 minute
    private static final int DESTROY_THRESHOLD = 64;
    private static final int CLEANUP_INTERVAL = 20;
    private static final AtomicInteger damageCounter = new AtomicInteger(0);

    private static class Damage {
        private final AtomicLong lastUpdatedTime;
        private final AtomicInteger numHits;

        public Damage(long lastUpdatedTime) {
            this.lastUpdatedTime = new AtomicLong(lastUpdatedTime);
            this.numHits = new AtomicInteger(0);
        }

        public void update(long time, int amount) {
            lastUpdatedTime.set(time);
            numHits.addAndGet(amount);
        }

        public int getHits() {
            return numHits.get();
        }

        public long getLastUpdatedTime() {
            return lastUpdatedTime.get();
        }
    }

    public static void applyDamage(Level level, BlockPos pos, int amount) {
        if (level.isClientSide()) {
            return;
        }

        long now = System.currentTimeMillis();

        Damage damage = damageMap.computeIfAbsent(pos, k -> new Damage(now));
        damage.update(now, amount);

        if (damage.getHits() > DESTROY_THRESHOLD) {
            if (damageMap.remove(pos) != null) {
                level.destroyBlock(pos, false);
            }
        }

        if (damageCounter.incrementAndGet() % CLEANUP_INTERVAL == 0) {
            cleanupExpiredDamage(now);
        }
    }

    private static void cleanupExpiredDamage(long now) {
        long cutoffTime = now - DAMAGE_TIMEOUT_MS;
        damageMap.entrySet().removeIf(entry ->
                entry.getValue().getLastUpdatedTime() < cutoffTime
        );
    }

    public AsteroidBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(VARIANT, 0).setValue(PALETTE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(VARIANT, PALETTE);
    }

    public static Vector3d getRotation(int variant) {
        return switch (variant) {
            case 0 -> new Vector3d();
            case 1 -> new Vector3d(0, Math.toRadians(22.5), 0);
            case 2 -> new Vector3d(0, Math.toRadians(45), 0);
            case 3 -> new Vector3d(0, Math.toRadians(-22.5), 0);
            case 4 -> new Vector3d(Math.toRadians(22.5), 0, 0);
            case 5 -> new Vector3d(Math.toRadians(45), 0, 0);
            case 6 -> new Vector3d(Math.toRadians(-22.5), 0, 0);
            case 7 -> new Vector3d(0, 0, Math.toRadians(22.5));
            case 8 -> new Vector3d(0, 0, Math.toRadians(45));
            case 9 -> new Vector3d(0, 0, Math.toRadians(-22.5));
            default -> new Vector3d(0, 0, 0);
        };
    }

    public void onProjectileHit(Level level, BlockState blockState, BlockHitResult arg3, Projectile arg4) {
        double speed = arg4.getDeltaMovement().length();
        applyDamage(level, arg3.getBlockPos(), (int) Math.ceil(speed * 10));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            spawnFallingBlocks(level, pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static Vector3d rotatePoint(double x, double y, double z, Vector3d eulerAngles) {
        double cosX = Math.cos(eulerAngles.x);
        double sinX = Math.sin(eulerAngles.x);
        double cosY = Math.cos(eulerAngles.y);
        double sinY = Math.sin(eulerAngles.y);
        double cosZ = Math.cos(eulerAngles.z);
        double sinZ = Math.sin(eulerAngles.z);

        double y1 = y * cosX - z * sinX;
        double z1 = y * sinX + z * cosX;

        double x2 = x * cosY + z1 * sinY;
        double z2 = -x * sinY + z1 * cosY;

        double x3 = x2 * cosZ - y1 * sinZ;
        double y3 = x2 * sinZ + y1 * cosZ;

        return new Vector3d(x3, y3, z2);
    }

    private static class WeightedBlockState {
        final BlockState state;
        final int weight;

        WeightedBlockState(BlockState state, int weight) {
            this.state = state;
            this.weight = weight;
        }
    }

    private static final List<List<WeightedBlockState>> PALETTE_BLOCKS = List.of(
        // Palette 0: Stone-based
        List.of(
            new WeightedBlockState(Blocks.STONE.defaultBlockState(), 1),
            new WeightedBlockState(Blocks.ANDESITE.defaultBlockState(), 1),
            new WeightedBlockState(Blocks.GRAVEL.defaultBlockState(), 1),
            new WeightedBlockState(Blocks.COAL_ORE.defaultBlockState(), 1)
        ),
        // Palette 1: Deepslate-based
        List.of(
            new WeightedBlockState(Blocks.SMOOTH_BASALT.defaultBlockState(), 24),
            new WeightedBlockState(Blocks.DEEPSLATE.defaultBlockState(), 24),
            new WeightedBlockState(Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 24),
            new WeightedBlockState(Blocks.DEEPSLATE_COAL_ORE.defaultBlockState(), 24),
            new WeightedBlockState(Blocks.DEEPSLATE_IRON_ORE.defaultBlockState(), 4)
        )
    );

    private static BlockState selectWeightedRandom(List<WeightedBlockState> blocks, net.minecraft.util.RandomSource random) {
        int totalWeight = blocks.stream().mapToInt(w -> w.weight).sum();
        int randomValue = random.nextInt(totalWeight);

        int currentWeight = 0;
        for (WeightedBlockState weighted : blocks) {
            currentWeight += weighted.weight;
            if (randomValue < currentWeight) {
                return weighted.state;
            }
        }

        return blocks.get(0).state;
    }

    private void spawnFallingBlocks(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        int variant = state.getValue(VARIANT);
        int palette = state.getValue(PALETTE);
        Vector3d rotation = getRotation(variant);

        List<WeightedBlockState> paletteBlocks = PALETTE_BLOCKS.get(palette);

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    boolean isOuterLayer = x < 2 || x >= 14 || y < 2 || y >= 14 || z < 2 || z >= 14;
                    if (!isOuterLayer) {
                        continue;
                    }

                    if (serverLevel.random.nextDouble() > 0.125) {
                        continue;
                    }

                    double relX = (x + 0.5) - 8.0;
                    double relY = (y + 0.5) - 8.0;
                    double relZ = (z + 0.5) - 8.0;

                    Vector3d rotated = rotatePoint(relX, relY, relZ, rotation);

                    double offsetX = (rotated.x + 8.0) / 16.0;
                    double offsetY = (rotated.y + 8.0) / 16.0;
                    double offsetZ = (rotated.z + 8.0) / 16.0;

                    BlockState blockState = selectWeightedRandom(paletteBlocks, serverLevel.random);

                    FallingBlockEntity fallingBlock = FallingBlockEntityAccessor.invokeConstructor(
                            serverLevel,
                            pos.getX() + offsetX,
                            pos.getY() + offsetY,
                            pos.getZ() + offsetZ,
                            blockState
                    );

                    double dirX = rotated.x;
                    double dirY = rotated.y;
                    double dirZ = rotated.z;

                    double length = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
                    double driftStrength = 0.25 + (level.random.nextDouble() / 4);

                    if (length > 0) {
                        dirX = (dirX / length) * driftStrength;
                        dirY = (dirY / length) * driftStrength;
                        dirZ = (dirZ / length) * driftStrength;
                    }

                    fallingBlock.setDeltaMovement(dirX, dirY, dirZ);

                    ((FallingBlockEntityExtension) fallingBlock).genesis$setRotation(new Vector3d(rotation));
                    fallingBlock.dropItem = false;

                    fallingBlock.time = 400 + level.random.nextInt(0, 100);

                    serverLevel.addFreshEntity(fallingBlock);
                }
            }
        }
    }
}
