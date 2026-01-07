package shipwrights.genesis.client.blockentityRenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import shipwrights.genesis.content.block.VoidCoreBlock;
import shipwrights.genesis.content.blockentity.VoidCoreBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

public class VoidCoreBlockEntityRenderer implements BlockEntityRenderer<VoidCoreBlockEntity> {
    private static final ResourceLocation PORTAL_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/end_portal.png");
    private static final float PORTAL_SIZE = 0.5f;

    public VoidCoreBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(VoidCoreBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();

        if (state.hasProperty(VoidCoreBlock.DORMANT)) {
            if (state.getValue(VoidCoreBlock.DORMANT)) {
                return;
            }
        }
        poseStack.pushPose();

        // Move to center of block
        poseStack.translate(0.5D, 0.5D, 0.5D);

        float scale = 1.0f;

        // Draw the portal
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.endPortal());
        Matrix4f matrix = poseStack.last().pose();

        // Draw all six faces of the cube
        // Front face (Z+)
        drawPortalFace(vertexConsumer, matrix, scale, PORTAL_SIZE, 0, 0, 1);
        // Back face (Z-)
        drawPortalFace(vertexConsumer, matrix, scale, PORTAL_SIZE, 0, 0, -1);
        // Top face (Y+)
        drawPortalFace(vertexConsumer, matrix, scale, PORTAL_SIZE, 0, 1, 0);
        // Bottom face (Y-)
        drawPortalFace(vertexConsumer, matrix, scale, PORTAL_SIZE, 0, -1, 0);
        // Right face (X+)
        drawPortalFace(vertexConsumer, matrix, scale, PORTAL_SIZE, 1, 0, 0);
        // Left face (X-)
        drawPortalFace(vertexConsumer, matrix, scale, PORTAL_SIZE, -1, 0, 0);

        poseStack.popPose();
    }

    private void drawPortalFace(VertexConsumer consumer, Matrix4f matrix,
                                float scale, float size, float normalX, float normalY, float normalZ) {
        // Rotate the face based on its normal
        PoseStack tempStack = new PoseStack();
        if (normalX != 0) {
            tempStack.mulPose(new org.joml.Quaternionf().rotationY((float) Math.PI / 2));
        } else if (normalY != 0) {
            tempStack.mulPose(new org.joml.Quaternionf().rotationX((float) Math.PI / 2));
        }
        Matrix4f rotatedMatrix = new Matrix4f(matrix).mul(tempStack.last().pose());

        float x = size * normalX;
        float y = size * normalY;
        float z = size * normalZ;

        // Draw quad with full texture coordinates
        // Order vertices based on normal direction to ensure face is visible from outside
        if (normalX > 0 || normalY > 0 || normalZ > 0) {
            // For negative normals, draw clockwise
            consumer.vertex(rotatedMatrix, -size , -size , x + y + z)
                    .color(0, 0, 0, 255)
                    .uv(0, 0)
                    .endVertex();
            consumer.vertex(rotatedMatrix, size , -size , x + y + z)
                    .color(0, 0, 0, 255)
                    .uv(1, 0)
                    .endVertex();
            consumer.vertex(rotatedMatrix, size , size , x + y + z)
                    .color(0, 0, 0, 255)
                    .uv(1, 1)
                    .endVertex();
            consumer.vertex(rotatedMatrix, -size, size, x + y + z)
                    .color(0, 0, 0, 255)
                    .uv(0, 1)
                    .endVertex();
        } else {
            // For positive normals, draw counter-clockwise
            consumer.vertex(rotatedMatrix, -size , -size , x + y + z)
                    .color(0, 0, 0, 255)
                    .uv(0, 0)
                    .endVertex();
            consumer.vertex(rotatedMatrix, -size , size , x + y + z)
                    .color(0, 0, 0, 255)
                    .uv(0, 1)
                    .endVertex();
            consumer.vertex(rotatedMatrix, size , size , x + y + z)
                    .color(0, 0, 0, 255)
                    .uv(1, 1)
                    .endVertex();
            consumer.vertex(rotatedMatrix, size , -size , x + y + z)
                    .color(0, 0, 0, 255)
                    .uv(1, 0)
                    .endVertex();
        }
    }

    @Override
    public boolean shouldRenderOffScreen(VoidCoreBlockEntity blockEntity) {
        return false;
    }
}
