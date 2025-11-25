package shipwrights.genesis.client.blockentityRenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.genesis.block.RadarDisplayBlock;
import shipwrights.genesis.blockentity.RadarDisplayBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import shipwrights.genesis.radar.PixelFrustumFactory;

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
                float x0 = -0.5f + x * pixelSize;
                float y0 = -0.5f + y * pixelSize;
                float x1 = x0 + pixelSize;
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
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180));
            case SOUTH -> {}  // default orientation (facing viewer)
            case WEST  -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
            case EAST  -> poseStack.mulPose(Axis.YP.rotationDegrees(-90));
            case UP    -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            case DOWN  -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
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

    /**
     * Debug function to render frustum visualization.
     * Call this to render a specific pixel's frustum as a transparent wireframe pyramid.
     *
     * @param blockEntity The radar display block entity
     * @param pixelX The X coordinate of the pixel (0 to resolution-1)
     * @param pixelY The Y coordinate of the pixel (0 to resolution-1)
     * @param frustumDepth How far to extend the frustum visualization
     * @param poseStack The pose stack
     * @param bufferSource The buffer source
     */
    public static void renderFrustumDebug(RadarDisplayBlockEntity blockEntity, int pixelX, int pixelY,
                                    double frustumDepth, PoseStack poseStack, MultiBufferSource bufferSource) {
        if (blockEntity == null || blockEntity.display == null) return;

        PixelFrustumFactory factory = blockEntity.display.getFrustumFactory();
        Vector3dc camera = blockEntity.display.debugCamera;

        if (camera == null || factory.getCamera() == null) return;

        // Get camera basis vectors
        Vector3dc C = factory.getCamera();
        Vector3dc F = factory.getForward();
        Vector3dc R = factory.getRight();
        Vector3dc U = factory.getUp();
        double tanHalfFov = factory.getTanHalfFov();

        // Calculate equal angular spacing for this pixel (same logic as factory)
        int res = blockEntity.display.resolution;
        double halfFov = Math.atan(tanHalfFov);
        double anglePerPixelRad = (halfFov * 2.0) / res;

        // Angular boundaries for this pixel
        double angleX0 = (pixelX - res / 2.0) * anglePerPixelRad;
        double angleX1 = (pixelX + 1 - res / 2.0) * anglePerPixelRad;
        double angleY0 = (pixelY - res / 2.0) * anglePerPixelRad;
        double angleY1 = (pixelY + 1 - res / 2.0) * anglePerPixelRad;

        // Convert to tangent values
        double tanX0 = Math.tan(angleX0);
        double tanX1 = Math.tan(angleX1);
        double tanY0 = Math.tan(angleY0);
        double tanY1 = Math.tan(angleY1);

        // Calculate corner rays
        Vector3d d00 = new Vector3d(F).add(new Vector3d(R).mul(tanX0)).add(new Vector3d(U).mul(tanY0)).normalize();
        Vector3d d10 = new Vector3d(F).add(new Vector3d(R).mul(tanX1)).add(new Vector3d(U).mul(tanY0)).normalize();
        Vector3d d01 = new Vector3d(F).add(new Vector3d(R).mul(tanX0)).add(new Vector3d(U).mul(tanY1)).normalize();
        Vector3d d11 = new Vector3d(F).add(new Vector3d(R).mul(tanX1)).add(new Vector3d(U).mul(tanY1)).normalize();

        // Calculate far plane corner positions
        Vector3d p00 = new Vector3d(C).add(new Vector3d(d00).mul(frustumDepth));
        Vector3d p10 = new Vector3d(C).add(new Vector3d(d10).mul(frustumDepth));
        Vector3d p01 = new Vector3d(C).add(new Vector3d(d01).mul(frustumDepth));
        Vector3d p11 = new Vector3d(C).add(new Vector3d(d11).mul(frustumDepth));

        // Get player position for relative rendering
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Vector3d playerPos = new Vector3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());

        // Render frustum wireframe
        poseStack.pushPose();

        // Get transparent line render type
        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());
        Matrix4f matrix = poseStack.last().pose();

        float r = 0.0f, g = 1.0f, b = 0.0f, a = 0.5f; // Green semi-transparent

        // Render edges from camera to far corners
        renderLine(lineConsumer, matrix, C, p00, playerPos, r, g, b, a);
        renderLine(lineConsumer, matrix, C, p10, playerPos, r, g, b, a);
        renderLine(lineConsumer, matrix, C, p01, playerPos, r, g, b, a);
        renderLine(lineConsumer, matrix, C, p11, playerPos, r, g, b, a);

        // Render far plane rectangle
        renderLine(lineConsumer, matrix, p00, p10, playerPos, r, g, b, a);
        renderLine(lineConsumer, matrix, p10, p11, playerPos, r, g, b, a);
        renderLine(lineConsumer, matrix, p11, p01, playerPos, r, g, b, a);
        renderLine(lineConsumer, matrix, p01, p00, playerPos, r, g, b, a);

        // Render the 4 frustum planes with transparency
        VertexConsumer quadConsumer = bufferSource.getBuffer(RenderType.debugQuads());
        r = 0.0f; g = 1.0f; b = 0.0f; a = 0.1f; // Very transparent green

        // Left face
        renderTransparentQuad(quadConsumer, matrix, C, p01, p00, playerPos, r, g, b, a);
        // Right face
        renderTransparentQuad(quadConsumer, matrix, C, p10, p11, playerPos, r, g, b, a);
        // Top face
        renderTransparentQuad(quadConsumer, matrix, C, p00, p10, playerPos, r, g, b, a);
        // Bottom face
        renderTransparentQuad(quadConsumer, matrix, C, p11, p01, playerPos, r, g, b, a);

        poseStack.popPose();
    }

    private static void renderLine(VertexConsumer consumer, Matrix4f matrix, Vector3dc from, Vector3dc to,
                           Vector3dc offset, float r, float g, float b, float a) {
        consumer.vertex(matrix,
            (float)(from.x() - offset.x()),
            (float)(from.y() - offset.y()),
            (float)(from.z() - offset.z()))
            .color(r, g, b, a)
            .normal(0, 1, 0)
            .endVertex();
        consumer.vertex(matrix,
            (float)(to.x() - offset.x()),
            (float)(to.y() - offset.y()),
            (float)(to.z() - offset.z()))
            .color(r, g, b, a)
            .normal(0, 1, 0)
            .endVertex();
    }

    private static void renderTransparentQuad(VertexConsumer consumer, Matrix4f matrix,
                                      Vector3dc apex, Vector3dc corner1, Vector3dc corner2,
                                      Vector3dc offset, float r, float g, float b, float a) {
        // Render a triangle from apex to two corners
        consumer.vertex(matrix,
            (float)(apex.x() - offset.x()),
            (float)(apex.y() - offset.y()),
            (float)(apex.z() - offset.z()))
            .color(r, g, b, a)
            .endVertex();
        consumer.vertex(matrix,
            (float)(corner1.x() - offset.x()),
            (float)(corner1.y() - offset.y()),
            (float)(corner1.z() - offset.z()))
            .color(r, g, b, a)
            .endVertex();
        consumer.vertex(matrix,
            (float)(corner2.x() - offset.x()),
            (float)(corner2.y() - offset.y()),
            (float)(corner2.z() - offset.z()))
            .color(r, g, b, a)
            .endVertex();
        consumer.vertex(matrix,
            (float)(corner2.x() - offset.x()),
            (float)(corner2.y() - offset.y()),
            (float)(corner2.z() - offset.z()))
            .color(r, g, b, a)
            .endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(RadarDisplayBlockEntity blockEntity) {
        return false;
    }
}
