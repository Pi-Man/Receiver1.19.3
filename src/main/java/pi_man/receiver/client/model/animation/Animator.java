package pi_man.receiver.client.model.animation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import pi_man.receiver.Receiver;
import pi_man.receiver.client.model.ModelGroup;

import java.lang.reflect.Type;
import java.util.*;

public class Animator {

    private final List<Bone> bones, displays, subModels;

    public Animator(List<Bone> bones, List<Bone> displays, List<Bone> subModels) {
        this.bones = bones;
        this.displays = displays;
        this.subModels = subModels;
    }

    public List<ItemTransform> calculateBones(ItemStack itemStack, ClientLevel clientLevel, LivingEntity livingEntity, int rand) {
        List<ItemTransform> transformations = new ArrayList<>();
        transformations.add(ItemTransform.NO_TRANSFORM);
        transformations.addAll(bones.stream().map(bone -> bone.calculate(itemStack, clientLevel, livingEntity, rand)).toList());
        return transformations;
    }

    public int getBoneID(List<ModelGroup> groups) {
        for (ModelGroup group : groups) {
            int i = 1;
            for (Bone bone : bones) {
                if (bone.checkGroup(group.name)) return i;
                i++;
            }
        }
        return 0;
    }

    public ItemTransforms calculateItemTransforms(ItemTransforms baseItemTransforms, ItemStack itemStack, ClientLevel clientLevel, LivingEntity livingEntity, int rand) {
        ItemTransform thirdPersonLeftHand = ItemTransform.NO_TRANSFORM;
        ItemTransform thirdPersonRightHand = ItemTransform.NO_TRANSFORM;
        ItemTransform firstPersonLeftHand = ItemTransform.NO_TRANSFORM;
        ItemTransform firstPersonRightHand = ItemTransform.NO_TRANSFORM;
        ItemTransform head = ItemTransform.NO_TRANSFORM;
        ItemTransform gui = ItemTransform.NO_TRANSFORM;
        ItemTransform ground = ItemTransform.NO_TRANSFORM;
        ItemTransform fixed = ItemTransform.NO_TRANSFORM;
        Map<ItemTransforms.TransformType, ItemTransform> modded = new HashMap<>();
        for (ItemTransforms.TransformType type : ItemTransforms.TransformType.values()) {
            List<ItemTransform> itemTransforms = new ArrayList<>();
            itemTransforms.add(baseItemTransforms.getTransform(type));
            for (Bone displayBone : displays) {
                if (displayBone.checkDisplay(type.getSerializeName())) {
                    itemTransforms.add(displayBone.calculate(itemStack, clientLevel, livingEntity, rand));
                }
            }
            switch (type) {
                case NONE -> {}
                case THIRD_PERSON_LEFT_HAND -> thirdPersonLeftHand = new MultiItemTransform(itemTransforms);
                case THIRD_PERSON_RIGHT_HAND -> thirdPersonRightHand = new MultiItemTransform(itemTransforms);
                case FIRST_PERSON_LEFT_HAND -> firstPersonLeftHand = new MultiItemTransform(itemTransforms);
                case FIRST_PERSON_RIGHT_HAND -> firstPersonRightHand = new MultiItemTransform(itemTransforms);
                case HEAD -> head = new MultiItemTransform(itemTransforms);
                case GUI -> gui = new MultiItemTransform(itemTransforms);
                case GROUND -> ground = new MultiItemTransform(itemTransforms);
                case FIXED -> fixed = new MultiItemTransform(itemTransforms);
                default -> modded.put(type, new MultiItemTransform(itemTransforms));
            }
        }
        return new ItemTransforms(thirdPersonLeftHand, thirdPersonRightHand, firstPersonLeftHand, firstPersonRightHand, head, gui, ground, fixed, ImmutableMap.copyOf(modded));
    }

    public List<Bone> getSubModels() {
        return subModels;
    }

    public Map<Receiver.PropertyKey, ItemTransform> calculateSubModelBones(ItemStack itemStack, ClientLevel clientLevel, LivingEntity livingEntity, int rand) {
        Map<Receiver.PropertyKey, ItemTransform> map = new HashMap<>();
        for (Bone bone : subModels) {
            map.put(bone.getPropertyKey(), bone.calculate(itemStack, clientLevel, livingEntity, rand));
        }
        return map;
    }

    public static class PivotItemTransform extends ItemTransform {

        public final Vector3f pivot;
        public final Vector3f negPivot;
        public PivotItemTransform(Vector3f p_254427_, Vector3f p_254496_, Vector3f p_254022_, Vector3f rightRotation, Vector3f pivot) {
            super(p_254427_, p_254496_, p_254022_, rightRotation);
            this.pivot = new Vector3f(pivot);
            this.negPivot = pivot.negate(new Vector3f());
        }

        @Override
        public void apply(boolean p_111764_, PoseStack p_111765_) {
            if (this != NO_TRANSFORM) {
                float f = this.rotation.x();
                float f1 = this.rotation.y();
                float f2 = this.rotation.z();
                if (p_111764_) {
                    f1 = -f1;
                    f2 = -f2;
                }

                int i = p_111764_ ? -1 : 1;
                p_111765_.translate((float)i * this.translation.x(), this.translation.y(), this.translation.z());
                Quaternionf rot = (new Quaternionf()).rotationXYZ(f * ((float)Math.PI / 180F), f1 * ((float)Math.PI / 180F), f2 * ((float)Math.PI / 180F));

                p_111765_.translate((float)i * this.pivot.x(), this.pivot.y(), this.pivot.z());
                p_111765_.mulPose(rot);
                p_111765_.translate((float)i * this.negPivot.x(), this.negPivot.y(), this.negPivot.z());

                p_111765_.scale(this.scale.x(), this.scale.y(), this.scale.z());
                p_111765_.mulPose(net.minecraftforge.common.util.TransformationHelper.quatFromXYZ(rightRotation.x(), rightRotation.y() * (p_111764_ ? -1 : 1), rightRotation.z() * (p_111764_ ? -1 : 1), true));
            }
        }
    }

    public static class MultiItemTransform extends ItemTransform {

        public final ImmutableList<ItemTransform> itemTransforms;

        public MultiItemTransform(Collection<ItemTransform> transforms) {
            super(new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f());
            itemTransforms = ImmutableList.copyOf(transforms);
        }
        public MultiItemTransform(ItemTransform... transforms) {
            super(new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f());
            itemTransforms = ImmutableList.copyOf(transforms);
        }

        @Override
        public void apply(boolean p_111764_, PoseStack p_111765_) {
            itemTransforms.forEach(itemTransform -> itemTransform.apply(p_111764_, p_111765_));
        }
    }

    public static class Deserializer implements JsonDeserializer<Animator> {

        /**
         * Gson invokes this call-back method during deserialization when it encounters a field of the
         * specified type.
         * <p>In the implementation of this call-back method, you should consider invoking
         * {@link JsonDeserializationContext#deserialize(JsonElement, Type)} method to create objects
         * for any non-trivial field of the returned object. However, you should never invoke it on the
         * the same type passing {@code json} since that will cause an infinite loop (Gson will call your
         * call-back method again).
         *
         * @param json    The Json data being deserialized
         * @param typeOfT The type of the Object to deserialize to
         * @param context
         * @return a deserialized object of the specified type typeOfT which is a subclass of {@code T}
         * @throws JsonParseException if json is not in the expected format of {@code typeofT}
         */
        @Override
        public Animator deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

            JsonArray array = json.getAsJsonArray();

            List<Bone> bones = new ArrayList<>(), displays = new ArrayList<>(), subModels = new ArrayList<>();

            for (JsonElement bone : array) {
                Bone bone1 = context.deserialize(bone, Bone.class);
                if (bone1.isDisplay()) {
                    displays.add(bone1);
                }
                else if (bone1.isSubModel()) {
                    subModels.add(bone1);
                }
                else {
                    bones.add(bone1);
                }
            }

            return new Animator(bones, displays, subModels);
        }
    }
}
