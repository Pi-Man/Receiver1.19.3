package pi_man.receiver.client.model;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import pi_man.receiver.Receiver;
import pi_man.receiver.client.model.animation.Animator;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class BBModel implements IUnbakedGeometry<BBModel> {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final ResourceLocation modelLocation;

    private RawBBModel rawModel = null;

    public BBModel(ResourceLocation modelLocation) {
        this.modelLocation = modelLocation;
    }

    private RawBBModel getModel(ResourceLocation location) {

        AtomicReference<RawBBModel> model = new AtomicReference<>(null);

        ResourceLocation trueModelLocation = new ResourceLocation(location.getNamespace(), "models/item/".concat(location.getPath().concat(".bbmodel")));
        Optional<Resource> resourceOptional = Minecraft.getInstance().getResourceManager().getResource(trueModelLocation);
        resourceOptional.ifPresentOrElse(resource -> {
            try {
                model.set(RawBBModel.deserialize(resource.openAsReader()));
            } catch (IOException e) {
                LOGGER.warn("Could not find model: {} ", trueModelLocation);
            }
        }, () -> {
            LOGGER.warn("Could not find model: {} ", trueModelLocation);
        });

        return model.get();
    }

    public void load() {
        rawModel = getModel(modelLocation);
    }

    public Optional<Animator> getAnimator() {
        if (rawModel.animator == null) {
            return Optional.empty();
        }
        else {
            return Optional.of(rawModel.animator);
        }
    }

    @Override
    public BakedBBModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation modelLocation) {

        BufferBuilder bufferBuilder = new BufferBuilder(256);
        bufferBuilder.begin(VertexFormat.Mode.QUADS, Receiver.ClientModEvents.BB_MODEL_VERTEX_FORMAT);

        Set<UUID> elements = new HashSet<>(rawModel.getElements().keySet());

        PoseStack poseStack = new PoseStack();

        apply(poseStack, rawModel.origin, rawModel.rotation);

        for (ModelGroup modelGroup : rawModel.getGroups()) {

            bakeGroup(poseStack, bufferBuilder, modelGroup, elements, spriteGetter);

        }

        for (UUID uuid : elements) {
            bakeElement(poseStack, bufferBuilder, rawModel.getElements().get(uuid), spriteGetter);
        }

        return new BakedBBModel(bufferBuilder.end(), rawModel.getAllTransforms(), this);
    }

    private static void apply(PoseStack poseStack, Vector3f origin, Vector3f rotation) {
        poseStack.translate(origin.x, origin.y, origin.z);
        poseStack.mulPose(new Quaternionf().rotationXYZ((float)(rotation.x * Math.PI / 180.0), (float)(rotation.y * Math.PI / 180.0), (float)(rotation.z * Math.PI / 180.0)));
        poseStack.translate(-origin.x, -origin.y, -origin.z);
    }

    private void bakeGroup(PoseStack poseStack, BufferBuilder bufferBuilder, ModelGroup modelGroup, Set<UUID> elements, Function<Material, TextureAtlasSprite> spriteGetter) {

        poseStack.pushPose();

        apply(poseStack, modelGroup.origin, modelGroup.rotation);

        for (ModelGroup subGroup : modelGroup.subGroups) {
            bakeGroup(poseStack, bufferBuilder, subGroup, elements, spriteGetter);
        }

        for (UUID uuid: modelGroup.elements) {
            elements.remove(uuid);
            bakeElement(poseStack, bufferBuilder, rawModel.getElements().get(uuid), spriteGetter);
        }

        poseStack.popPose();
    }

    private void bakeElement(PoseStack poseStack, BufferBuilder bufferBuilder, ModelElement modelElement, Function<Material, TextureAtlasSprite> spriteGetter) {

        poseStack.pushPose();

        apply(poseStack, modelElement.origin, modelElement.rotation);

        List<ModelGroup> groups = getModelGroup(rawModel.getGroups(), modelElement);
        int boneID;
        if (groups.isEmpty() || rawModel.animator == null) {
            boneID = 0;
        }
        else {
            boneID = rawModel.animator.getBoneID(groups);
        }

        for (Map.Entry<Direction, ElementFace> entry : modelElement.faces.entrySet()) {

            Direction direction = entry.getKey();
            ElementFace face = entry.getValue();

            float vertexInfo[] = new float[Direction.values().length];

            vertexInfo[Direction.WEST.get3DDataValue()] = modelElement.start.x;
            vertexInfo[Direction.DOWN.get3DDataValue()] = modelElement.start.y;
            vertexInfo[Direction.NORTH.get3DDataValue()] = modelElement.start.z;
            vertexInfo[Direction.EAST.get3DDataValue()] = modelElement.end.x;
            vertexInfo[Direction.UP.get3DDataValue()] = modelElement.end.y;
            vertexInfo[Direction.SOUTH.get3DDataValue()] = modelElement.end.z;

            Vector3f normal = new Vector3f(direction.getNormal().getX(), direction.getNormal().getY(), direction.getNormal().getZ());
            normal = poseStack.last().normal().transform(normal);

            ResourceLocation textureLocation = new ResourceLocation(rawModel.textures.get(face.texture));
            Material material = new Material(InventoryMenu.BLOCK_ATLAS, textureLocation);
            TextureAtlasSprite atlasSprite = spriteGetter.apply(material);

            for (int i = 0; i < 4; i++) {
                Vector3f vertex = new Vector3f(
                        vertexInfo[FaceInfo.fromFacing(direction).getVertexInfo(i).xFace],
                        vertexInfo[FaceInfo.fromFacing(direction).getVertexInfo(i).yFace],
                        vertexInfo[FaceInfo.fromFacing(direction).getVertexInfo(i).zFace]
                );
                vertex = poseStack.last().pose().transformPosition(vertex);
                Vector2f uv = face.getUV(i);
                uv.x = atlasSprite.getU(uv.x);
                uv.y = atlasSprite.getV(uv.y);
                bufferBuilder.vertex(vertex.x, vertex.y, vertex.z);
                bufferBuilder.color(0xFFFFFFFF);
                bufferBuilder.uv(uv.x, uv.y);
                bufferBuilder.normal(normal.x, normal.y, normal.z);
                bufferBuilder.putShort(0, (short) boneID);
                bufferBuilder.nextElement();
                bufferBuilder.endVertex();
            }
        }

        poseStack.popPose();
    }

    private static List<ModelGroup> getModelGroup(List<ModelGroup> groups, ModelElement modelElement) {
        List<ModelGroup> list = new ArrayList<>();
        for (ModelGroup group : groups) {
            for (UUID uuid : group.elements) {
                if (uuid.equals(modelElement.uuid)) {
                    list.add(group);
                    return list;
                }
            }
            List<ModelGroup> list1 = getModelGroup(group.subGroups, modelElement);
            if (!list1.isEmpty()) {
                list.addAll(list1);
                list.add(group);
                return list;
            }
        }
        return list;
    }

    /**
     * Resolve parents of nested {@link BlockModel}s which are later used in
     * {@link IUnbakedGeometry#bake(IGeometryBakingContext, ModelBaker, Function, ModelState, ItemOverrides, ResourceLocation)}
     * via {@link BlockModel#resolveParents(Function)}
     *
     * @param modelGetter
     * @param context
     */
    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, IGeometryBakingContext context) {
        IUnbakedGeometry.super.resolveParents(modelGetter, context);
    }

    /**
     * {@return a set of all the components whose visibility may be configured via {@link IGeometryBakingContext}}
     */
    @Override
    public Set<String> getConfigurableComponentNames() {
        return Set.of();
    }

    public List<ItemTransform> calculateBones(ItemStack itemStack, ClientLevel clientLevel, LivingEntity livingEntity, int rand) {
        return rawModel.animator == null ? List.of(ItemTransform.NO_TRANSFORM) : rawModel.animator.calculateBones(itemStack, clientLevel, livingEntity, rand);
    }

    public ItemTransforms calculateItemTransforms(ItemTransforms baseItemTransforms, ItemStack itemStack, ClientLevel clientLevel, LivingEntity livingEntity, int rand) {
        return rawModel.animator == null ? baseItemTransforms : rawModel.animator.calculateItemTransforms(baseItemTransforms, itemStack, clientLevel, livingEntity, rand);
    }

    public Map<Receiver.PropertyKey, ItemTransform> calculateSubModelBones(ItemStack itemStack, ClientLevel clientLevel, LivingEntity livingEntity, int rand) {
        return rawModel.animator == null ? Map.of() : rawModel.animator.calculateSubModelBones(itemStack, clientLevel, livingEntity, rand);
    }
}
