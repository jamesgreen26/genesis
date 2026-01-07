package shipwrights.genesis.client.blockentityRenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import shipwrights.genesis.content.block.RadarDisplayBlock;
import shipwrights.genesis.content.blockentity.RadarDisplayBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class RadarDisplayBlockEntityRenderer implements BlockEntityRenderer<RadarDisplayBlockEntity> {


    private static final double depthNear = 0d;
    private static final double depthFar = 25_000d;

    public RadarDisplayBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(RadarDisplayBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        double[][] data = blockEntity.getDisplayableData();
        int resolution = data.length;

        if (resolution == 0) return;

        // ---- Setup transform ----
        poseStack.pushPose();

        // Move to block center
        poseStack.translate(0.5, 0.5, 0.5);

        // Rotate to match block facing
        Direction facing = blockEntity.getBlockState().getValue(RadarDisplayBlock.FACING);
        rotateToFacing(poseStack, facing);

        // Move the quad surface outward so it floats slightly off the face
        poseStack.translate(0, 0, 0.501);  // slight offset to avoid z-fighting

        // Now the "screen" exists in X/Y plane, Z forward from block face

        // Screen spans -0.5..0.5 in both X and Y
        float pixelSize = 1.0f / resolution;

        VertexConsumer vc = bufferSource.getBuffer(RenderType.debugQuads());

        for (int x = 0; x < resolution; x++) {
            for (int y = 0; y < resolution; y++) {

                int[] rgb = getColor(data[x][y]);
                float r = rgb[0] / 255f;
                float g = rgb[1] / 255f;
                float b = rgb[2] / 255f;

                // Convert to -0.5..0.5 screen space
                float x0 = 0.5f - x * pixelSize;
                float y0 = -0.5f + y * pixelSize;
                float x1 = x0 - pixelSize;
                float y1 = y0 + pixelSize;

                // Draw pixel quad
                addQuad(
                        poseStack.last().pose(),
                        vc,
                        x0, y0, 0,
                        x1, y1, 0,
                        r, g, b,
                        packedLight
                );
            }
        }

        poseStack.popPose();
    }

    private int[] getColor(double depth) {
        // Handle zero depth (no data)
        if (depth <= 0) {
            return new int[]{0, 0, 0};
        }

        // Normalize depth to 0-1 range
        double t = (depth - depthNear) / (depthFar - depthNear);

        // Beyond far plane is black
        if (t >= 1.0) {
            return new int[]{0, 0, 0};
        }

        // Clamp to valid range
        t = Math.max(0, Math.min(1, t));

        // Define color stops: red -> orange -> yellow -> green -> blue -> black
        int r, g, b;

        if (t < 0.2) {
            // Red to Orange (255,0,0) -> (255,165,0)
            double localT = t / 0.2;
            r = 255;
            g = (int)(165 * localT);
            b = 0;
        } else if (t < 0.4) {
            // Orange to Yellow (255,165,0) -> (255,255,0)
            double localT = (t - 0.2) / 0.2;
            r = 255;
            g = (int)(165 + (255 - 165) * localT);
            b = 0;
        } else if (t < 0.6) {
            // Yellow to Green (255,255,0) -> (0,255,0)
            double localT = (t - 0.4) / 0.2;
            r = (int)(255 * (1 - localT));
            g = 255;
            b = 0;
        } else if (t < 0.8) {
            // Green to Blue (0,255,0) -> (0,0,255)
            double localT = (t - 0.6) / 0.2;
            r = 0;
            g = (int)(255 * (1 - localT));
            b = (int)(255 * localT);
        } else {
            // Blue to Black (0,0,255) -> (0,0,0)
            double localT = (t - 0.8) / 0.2;
            r = 0;
            g = 0;
            b = (int)(255 * (1 - localT));
        }

        return new int[]{r, g, b};
    }

    private void rotateToFacing(PoseStack poseStack, Direction facing) {
        switch (facing.getOpposite()) {
            case NORTH -> {}
            case EAST  -> poseStack.mulPose(Axis.YP.rotationDegrees(-90));
            case SOUTH  -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case WEST  -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case UP    -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
            case DOWN  -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
        }
    }


    private void addQuad(Matrix4f m, VertexConsumer vc,
                         float x0, float y0, float z,
                         float x1, float y1, float z2,
                         float r, float g, float b,
                         int light) {

        vc.vertex(m, x0, y0, z).color(r, g, b, 1f).endVertex();
        vc.vertex(m, x1, y0, z).color(r, g, b, 1f).endVertex();
        vc.vertex(m, x1, y1, z2).color(r, g, b, 1f).endVertex();
        vc.vertex(m, x0, y1, z2).color(r, g, b, 1f).endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(RadarDisplayBlockEntity blockEntity) {
        return false;
    }
}
