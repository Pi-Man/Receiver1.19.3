package pi_man.receiver.client;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;
import pi_man.receiver.Receiver;
import pi_man.receiver.client.model.BakedBBModel;
import pi_man.receiver.world.item.component.AnimationPropertyHandler;

import java.util.List;

public class ReceiverItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static ReceiverItemRenderer instance = null;

    private ReceiverItemRenderer() {
        super(null, null);
    }

    public static ReceiverItemRenderer getInstance() {
        if (instance == null) {
            instance = new ReceiverItemRenderer();
        }
        return instance;
    }
    @Override
    public void renderByItem(ItemStack itemStack, ItemTransforms.TransformType transformType, PoseStack poseStack, MultiBufferSource multiBufferSource, int packedLight, int packedOverlay) {
        renderModel(ForgeRegistries.ITEMS.getKey(itemStack.getItem()), itemStack, poseStack, packedLight);
    }

    public void renderModel(ResourceLocation modelLocation, ItemStack itemStack, PoseStack poseStack, int packedLight) {
        BakedModel bakedModel = Minecraft.getInstance().getModelManager().getModel(new ModelResourceLocation(modelLocation, "inventory"));
        if (bakedModel instanceof BakedBBModel bakedBBModel) {
            poseStack.pushPose();

            Uniform bones = Receiver.ClientModEvents.bbModelShader.getUniform("Bones");
            Uniform UV2 = Receiver.ClientModEvents.bbModelShader.getUniform("UV2");

            if (bones != null) {
                int i = 0;
                List<ItemTransform> itemTransforms = bakedBBModel.getTransformations();
                for (ItemTransform itemTransform : itemTransforms) {
                    poseStack.pushPose();
                    itemTransform.apply(false, poseStack);
                    Matrix4f mat = poseStack.last().pose();
                    float afloat[] = mat.get(new float[16]);
                    for (int j = 0; j < 16; j++) {
                        bones.set(i * 16 + j, afloat[j]);
                    }
                    i++;
                    poseStack.popPose();
                }
                bones.upload();
            }
            if (UV2 != null) {
                int aint[] = new int[2];
                aint[0] = (packedLight & 0xFFFF);
                aint[1] = ((packedLight >> 16) & 0xFFFF);
                UV2.set(aint[0], aint[1]);
                UV2.upload();
            }

            VertexBuffer vertexBuffer = bakedBBModel.getVertexBuffer();

            Receiver.ClientModEvents.ReceiverRenderType.SOLID.setupRenderState();
            vertexBuffer.bind();
            vertexBuffer.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            Receiver.ClientModEvents.ReceiverRenderType.SOLID.clearRenderState();

            bakedBBModel.getAnimator().ifPresent(animator -> {
                animator.getSubModels().forEach(bone -> {
                    if (itemStack.getItem() instanceof AnimationPropertyHandler animationPropertyHandler) {
                        String s = animationPropertyHandler.getProperty(itemStack, bone.getPropertyKey(), 1).getAsString();
                        if (!s.isEmpty()) {
                            ResourceLocation resourceLocation = new ResourceLocation(s);
                            poseStack.pushPose();
                            ItemTransform itemTransform = bakedBBModel.getSubModelTransforms().get(bone.getPropertyKey());
                            if (itemTransform != null) {
                                itemTransform.apply(false, poseStack);
                            }
                            renderModel(resourceLocation, itemStack, poseStack, packedLight);
                            poseStack.popPose();
                        }
                    }
                });
            });

            poseStack.popPose();
        }
    }
}
