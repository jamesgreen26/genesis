package shipwrights.genesis.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.blockentity.NavProjectorBlockEntity;
import shipwrights.genesis.planets.PlanetData;
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

    static final PlanetData sunData = new PlanetData(ResourceLocation.fromNamespaceAndPath(GenesisMod.MOD_ID, "sun") , 15, 0, 1, 1, 1);

    static {
        sunData.rot = new Vector3d(0, 0, 0);
    }

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

        // Render planets from Genesis planet registry
        for (PlanetData planet : GenesisMod.planets) {
            renderPlanetProjection(poseStack, bufferSource, packedLight, packedOverlay, planet, isOnShip, shipPos, pos, scale_factor, blockRenderer, ticks, partialTick);
        }

        renderPlanetProjection(poseStack, bufferSource, packedLight, packedOverlay, sunData, isOnShip, shipPos, pos, scale_factor, blockRenderer, ticks, partialTick);


        poseStack.popPose();
    }

    private static void renderPlanetProjection(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, PlanetData planet, boolean isOnShip, Vector3dc shipPos, BlockPos pos, int scale_factor, BlockRenderDispatcher blockRenderer, long ticks, float partialTick) {
        Vector3d planetPos = planet.getCurrentPos(ticks, partialTick);

        if (isOnShip) {
            if (planetPos.sub(new Vector3d(shipPos), new Vector3d()).length() > 120000) return;
        } else {
            if (planetPos.sub(new Vector3d(pos.getX(), pos.getY(), pos.getZ()), new Vector3d()).length() > 120000)
                return;
        }

        float scale = (float) (2 * Math.sqrt(planet.size) / Math.sqrt(scale_factor));
        if (scale > 0) {
            poseStack.translate(planetPos.x / scale_factor, planetPos.y / scale_factor, planetPos.z / scale_factor);
            poseStack.scale(scale, scale, scale);

            Quaternionf rot = new Quaternionf().rotateXYZ((float) planet.rot.x, (float) planet.rot.y, (float) planet.rot.z);
            poseStack.mulPose(rot);

            poseStack.translate(-0.5D, -0.5D, -0.5D);

            if (planetPos.lengthSquared() == 0) {
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
            poseStack.translate(-planetPos.x / scale_factor, -planetPos.y / scale_factor, -planetPos.z / scale_factor);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(NavProjectorBlockEntity blockEntity) {
        return true;
    }
}
