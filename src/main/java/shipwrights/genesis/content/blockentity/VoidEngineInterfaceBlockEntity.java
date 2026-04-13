package shipwrights.genesis.content.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.content.block.VoidCoreBlock;
import shipwrights.genesis.networking.*;
import shipwrights.genesis.teleportation.DimensionTravelTeleporter;
import shipwrights.genesis.teleportation.TravelDirection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class VoidEngineInterfaceBlockEntity extends BlockEntity {
    private static final int MAX_ENERGY = 8192;
    private static final int ENERGY_PER_TICK = 512;
    private final EnergyStorage energyStorage = new EnergyStorage(MAX_ENERGY);
    private final LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energyStorage);

    public VoidEngineInterfaceBlockEntity(BlockPos pos, BlockState state) {
        super(GenesisBlockEntities.VOID_ENGINE_INTERFACE.get(), pos, state);
    }

    private int chargeUpTicks = 0;
    boolean active = false;
    private ResourceLocation returningDim = ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyCapability.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energyStorage.deserializeNBT(tag.get("energy"));
        chargeUpTicks = tag.getInt("chargeUpTicks");
        active = tag.getBoolean("active");
        returningDim = ResourceLocation.parse(tag.getString("returningDim"));
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("energy", energyStorage.serializeNBT());
        tag.putInt("chargeUpTicks", chargeUpTicks);
        tag.putBoolean("active", active);
        tag.putString("returningDim", returningDim.toString());
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T blockEntity) {
        if (blockEntity instanceof VoidEngineInterfaceBlockEntity voidEngineInterface) {
            // Try to receive energy from adjacent blocks
            if (voidEngineInterface.energyStorage.getEnergyStored() < voidEngineInterface.energyStorage.getMaxEnergyStored()) {
                for (Direction direction : Direction.values()) {
                    BlockEntity neighbor = level.getBlockEntity(pos.relative(direction));
                    if (neighbor != null) {
                        neighbor.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).ifPresent(energy -> {
                            int toExtract = Math.min(128, voidEngineInterface.energyStorage.getMaxEnergyStored() - voidEngineInterface.energyStorage.getEnergyStored());
                            int extracted = energy.extractEnergy(toExtract, false);
                            voidEngineInterface.energyStorage.receiveEnergy(extracted, false);
                        });
                    }
                }
            }

            //GenesisEvents.message = Component.literal("chargeUpTicks = " + voidEngineInterface.chargeUpTicks + " active = " + voidEngineInterface.active);

            BlockState core = level.getBlockState(pos.offset(state.getValue(BlockStateProperties.HORIZONTAL_FACING).getNormal().multiply(-1)));
            if (core.hasProperty(VoidCoreBlock.DORMANT) && !core.getValue(VoidCoreBlock.DORMANT)) {
                Ship ship = VSGameUtilsKt.getShipManagingPos(level, pos);

                Vec3 center = pos.getCenter();
                boolean isPowered = level.getBlockState(pos).hasProperty(BlockStateProperties.POWERED) && level.getBlockState(pos).getValue(BlockStateProperties.POWERED);
                boolean hasEnergy = voidEngineInterface.energyStorage.getEnergyStored() >= ENERGY_PER_TICK;

                if (isPowered && hasEnergy) {
                    voidEngineInterface.energyStorage.extractEnergy(ENERGY_PER_TICK, false);

                    if (!voidEngineInterface.active && voidEngineInterface.chargeUpTicks >= 0) {
                        voidEngineInterface.active = true;
                        GenesisMod.LOGGER.info("Current dimension id: {}", level.dimension().location());
                        if (!level.dimension().location().equals(GenesisMod.WORMHOLE_DIM)) {
                            GenesisNetworking.sendToAll(GenesisNetworking.INSTANCE, new StopVoidEngineStartSoundPacket());
                            GenesisNetworking.sendToAll(GenesisNetworking.INSTANCE, new VoidEngineSoundPacket(pos));
                        }
                    }

                    if (!level.dimension().location().equals(GenesisMod.WORMHOLE_DIM) && level.getServer() != null) {
                        voidEngineInterface.chargeUpTicks++;

                        // Check if we should teleport to wormhole dimension
                        if (voidEngineInterface.chargeUpTicks == 244) {
                            if (ship == null || !level.dimension().location().equals(ResourceLocation.fromNamespaceAndPath("genesis", "great_unknown"))) {
                                if (level.getBlockState(pos).hasProperty(BlockStateProperties.POWERED) && level.getBlockState(pos).getValue(BlockStateProperties.POWERED)) {
                                    explode(level, center);
                                }
                            } else {
                                voidEngineInterface.chargeUpTicks = 2;

                                // Save current dimension for return
                                voidEngineInterface.returningDim = level.dimension().location();

                                // Get wormhole level
                                ServerLevel wormholeLevel = level.getServer().getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, GenesisMod.WORMHOLE_DIM));
                                if (wormholeLevel != null) {
                                    // Teleport ship to wormhole - scale position down

                                    GenesisNetworking.INSTANCE.send(
                                            PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)),
                                            new EnteringWarpPacket()
                                    );

                                    Vector3dc targetPos = ship.getTransform().getPositionInWorld().mul(1 / 32.0, new Vector3d());
                                    DimensionTravelTeleporter.teleportShip((ServerShip) ship, TravelDirection.PLANET_TO_SPACE, (ServerLevel) level, wormholeLevel, targetPos, new Quaterniond());

                                    //sendWormholeTravelPacket
                                    GenesisNetworking.sendToAll(GenesisNetworking.INSTANCE, new WormholeTravelSoundPacket(pos));
                                }
                                return;
                            }
                        }
                    } else {
                        voidEngineInterface.chargeUpTicks = 32;
                    }
                } else {
                    GenesisNetworking.sendToAll(GenesisNetworking.INSTANCE, new StopVoidEngineStartSoundPacket());
                    if (voidEngineInterface.chargeUpTicks > 0) {
                        voidEngineInterface.chargeUpTicks--;
                    }
                    if (level.dimension().location().equals(GenesisMod.WORMHOLE_DIM) && level.getServer() != null) {
                        if (voidEngineInterface.chargeUpTicks <= 0) {
                            voidEngineInterface.chargeUpTicks = -64;
                            // Auto-return to saved dimension when in wormhole
                            ServerLevel returnLevel = level.getServer().getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, voidEngineInterface.returningDim));
                            returnFromWormhole(level, pos, returnLevel, ship, false);
                        }
                    } else {
                        if (voidEngineInterface.chargeUpTicks > 0) {
                            voidEngineInterface.chargeUpTicks = 0;
                            GenesisNetworking.sendToAll(GenesisNetworking.INSTANCE, new StopVoidEngineStartSoundPacket());
                        }
                    }
                }
            }
            if (voidEngineInterface.chargeUpTicks < 0) {
                voidEngineInterface.chargeUpTicks++;
            }
            if (voidEngineInterface.active) {
                 if (voidEngineInterface.chargeUpTicks == 0) {
                    voidEngineInterface.active = false;
                }
            }
        }
    }

    public static void returnFromWormhole(Level level, BlockPos pos, ServerLevel returnLevel, Ship ship, boolean unstable) {
        if (ship != null && returnLevel != null) {
            // Teleport ship back - scale position up
            Vector3dc targetPos = ship.getTransform().getPositionInWorld().mul(32.0, new Vector3d());

            GenesisNetworking.INSTANCE.send(
                    PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)),
                    new EnteringWarpPacket()
            );

            GenesisNetworking.sendToAll(GenesisNetworking.INSTANCE, new WormholeTravelSoundPacket(pos));
            DimensionTravelTeleporter.teleportShip((ServerShip) ship, TravelDirection.SPACE_TO_PLANET, (ServerLevel) level, returnLevel, targetPos, new Quaterniond());
            GenesisNetworking.sendToAll(GenesisNetworking.INSTANCE, new WormholeTravelSoundPacket(pos));
            if (unstable) {
                Vec3 center = new Vec3(targetPos.x(), targetPos.y(), targetPos.z());
                explode(returnLevel, center);
            }
        }
    }

    private static void explode(Level level, Vec3 center) {
        level.explode(null, center.x, center.y, center.z, 16f, Level.ExplosionInteraction.BLOCK);
    }
}
