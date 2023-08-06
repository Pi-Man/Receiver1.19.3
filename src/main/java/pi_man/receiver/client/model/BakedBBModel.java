package pi_man.receiver.client.model;

import com.electronwill.nightconfig.core.Config;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.math.Transformation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.IForgeBakedModel;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import pi_man.receiver.Receiver;
import pi_man.receiver.client.model.animation.Animator;

import java.util.*;

public class BakedBBModel implements BakedModel {

    private final BufferBuilder.RenderedBuffer buffer;
    private final List<ItemTransform> transformations;
    private final Map<Receiver.PropertyKey, ItemTransform> subModelTransforms = new HashMap<>();
    private final Overrides overrides = new Overrides();
    private final ItemTransforms baseItemTransforms;
    private ItemTransforms itemTransforms;
    private final BBModel model;

    private VertexBuffer vertexBuffer = null;

    public BakedBBModel(BufferBuilder.RenderedBuffer buffer, ItemTransforms itemTransforms, BBModel model) {
        transformations = new ArrayList<>(Collections.nCopies(16, ItemTransform.NO_TRANSFORM));
        this.buffer = buffer;
        this.baseItemTransforms = itemTransforms;
        this.itemTransforms = itemTransforms;
        this.model = model;
    }

    public Optional<Animator> getAnimator() {
        return model.getAnimator();
    }

    public List<ItemTransform> getTransformations() {
        return ImmutableList.copyOf(transformations);
    }

    public Map<Receiver.PropertyKey, ItemTransform> getSubModelTransforms() {
        return subModelTransforms;
    }

    /**
     * @param p_235039_
     * @param p_235040_
     * @param p_235041_
     * @deprecated Forge: Use {@link IForgeBakedModel#getQuads(BlockState, Direction, RandomSource, ModelData, RenderType)}
     */
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState p_235039_, @Nullable Direction p_235040_, RandomSource p_235041_) {
        return null;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData data, @Nullable RenderType renderType) {
        return BakedModel.super.getQuads(state, side, rand, data, renderType);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return true;
    }

    @Override
    public ItemTransforms getTransforms() {
        return itemTransforms;
    }

    /**
     * @deprecated Forge: Use {@link IForgeBakedModel#getParticleIcon(ModelData)}
     */
    @Override
    public TextureAtlasSprite getParticleIcon() {
        return null;
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }

    public VertexBuffer getVertexBuffer() {
        if (vertexBuffer == null) {
            vertexBuffer = new VertexBuffer();
            vertexBuffer.bind();
            vertexBuffer.upload(buffer);
        }
        return vertexBuffer;
    }

    public static class Overrides extends ItemOverrides {
        @Nullable
        @Override
        public BakedModel resolve(BakedModel bakedModel, ItemStack itemStack, @Nullable ClientLevel clientLevel, @Nullable LivingEntity livingEntity, int rand) {
            if (bakedModel instanceof BakedBBModel bakedBBModel) {
                bakedBBModel.transformations.clear();
                bakedBBModel.transformations.addAll(bakedBBModel.model.calculateBones(itemStack, clientLevel, livingEntity, rand));
                bakedBBModel.itemTransforms = bakedBBModel.model.calculateItemTransforms(bakedBBModel.baseItemTransforms, itemStack, clientLevel, livingEntity, rand);
                bakedBBModel.subModelTransforms.clear();
                bakedBBModel.subModelTransforms.putAll(bakedBBModel.model.calculateSubModelBones(itemStack, clientLevel, livingEntity, rand));
            }
            return bakedModel;
        }
    }

}
