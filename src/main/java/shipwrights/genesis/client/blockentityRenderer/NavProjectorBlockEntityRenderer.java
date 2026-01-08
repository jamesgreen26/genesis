package shipwrights.genesis.client.blockentityRenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.content.blockentity.NavProjectorBlockEntity;
import shipwrights.genesis.space.Orbitable;
import shipwrights.genesis.space.OrbitingBody;
import shipwrights.genesis.space.Star;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import org.joml.*;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.lang.Math;
import java.util.Objects;

public class NavProjectorBlockEntityRenderer implements BlockEntityRenderer<NavProjectorBlockEntity> {
    public NavProjectorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(NavProjectorBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                      MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();

        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();

        Level level = blockEntity.getLevel();

        if (level == null) {
            return;
        }

        long ticks = level.getGameTime();

        // Move to center of block
        poseStack.translate(0.5D, 1.5D, 0.5D);
        poseStack.scale(0.02f, 0.02f, 0.02f);
        BlockPos pos = blockEntity.getBlockPos();

        Ship ship = VSGameUtilsKt.getShipManagingPos(level, pos);
        boolean isOnShip = ship != null;

        Vector3dc shipPos = null;

        int scale_factor = 1000;

        poseStack.translate(-0.5D, -0.5D, -0.5D);

        blockRenderer.renderSingleBlock(Blocks.WHITE_CONCRETE.defaultBlockState(),
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        poseStack.translate(0.5D, 0.5D, 0.5D);

        if(!isOnShip) {
            poseStack.translate((float) -pos.getX() / scale_factor, (float) -pos.getY() / scale_factor, (float) -pos.getZ() / scale_factor);
        } else {
            Quaterniondc rot = ship.getTransform().getShipToWorldRotation().invert(new Quaterniond());
            poseStack.mulPose(new Quaternionf(rot.x(), rot.y(), rot.z(), rot.w()));
            shipPos = ship.getWorldAABB().center(new Vector3d());
            ResourceLocation currentDimension = Objects.requireNonNull(blockEntity.getLevel()).dimension().location();

            if (currentDimension.toString().equals(GenesisMod.WORMHOLE_DIM.toString())) {
                shipPos = shipPos.mul(32.0, new Vector3d());
            }

            poseStack.translate((float) -shipPos.x() / scale_factor, (float) -shipPos.y() / scale_factor, (float) -shipPos.z() / scale_factor);
        }

        // Render planets from Genesis space registry
        for (OrbitingBody body : GenesisMod.SPACE_REGISTRY.getAllOrbitingBodies()) {
            renderCelestialProjection(poseStack, bufferSource, packedLight, packedOverlay, body, isOnShip, shipPos, pos, scale_factor, blockRenderer, ticks, partialTick, false);
        }

        // Render stars
        for (Star star : GenesisMod.SPACE_REGISTRY.getAllStars()) {
            renderCelestialProjection(poseStack, bufferSource, packedLight, packedOverlay, star, isOnShip, shipPos, pos, scale_factor, blockRenderer, ticks, partialTick, true);
        }


        poseStack.popPose();
    }

    private static void renderCelestialProjection(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, Orbitable.Celestial celestial, boolean isOnShip, Vector3dc shipPos, BlockPos pos, int scale_factor, BlockRenderDispatcher blockRenderer, long ticks, float partialTick, boolean isStar) {
        Vector3d celestialPos = new Vector3d(celestial.getCurrentPos(ticks, partialTick));

        if (isOnShip) {
            if (celestialPos.sub(new Vector3d(shipPos), new Vector3d()).length() > 120000) return;
        } else {
            if (celestialPos.sub(new Vector3d(pos.getX(), pos.getY(), pos.getZ()), new Vector3d()).length() > 120000)
                return;
        }

        float scale = (float) (2 * Math.sqrt(celestial.getActualSize()) / Math.sqrt(scale_factor));
        if (scale > 0) {
            poseStack.translate(celestialPos.x / scale_factor, celestialPos.y / scale_factor, celestialPos.z / scale_factor);
            poseStack.scale(scale, scale, scale);

            Quaternionf rot = new Quaternionf(celestial.getRotation(ticks, partialTick));
            poseStack.mulPose(rot);

            poseStack.translate(-0.5D, -0.5D, -0.5D);

            if (isStar) {
                blockRenderer.renderSingleBlock(Blocks.WHITE_STAINED_GLASS.defaultBlockState(),
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay
                );
            } else {
                blockRenderer.renderSingleBlock(Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(),
                        poseStack,
                        bufferSource,
                        packedLight,
                        packedOverlay
                );
            }

            poseStack.translate(0.5D, 0.5D, 0.5D);

            poseStack.mulPose(rot.invert());
            poseStack.scale(1 / scale, 1 / scale, 1 / scale);
            poseStack.translate(-celestialPos.x / scale_factor, -celestialPos.y / scale_factor, -celestialPos.z / scale_factor);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(NavProjectorBlockEntity blockEntity) {
        return true;
    }
}
