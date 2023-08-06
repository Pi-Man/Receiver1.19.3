package pi_man.receiver.client.model;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.*;

import net.minecraft.client.renderer.block.model.ItemOverride;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.joml.Vector3f;
import pi_man.receiver.client.model.animation.Animator;
import pi_man.receiver.client.model.animation.Bone;
import pi_man.receiver.client.model.animation.Predicate;

@OnlyIn(Dist.CLIENT)
public class RawBBModel
{
    private static final Logger LOGGER = LogManager.getLogger();
    @VisibleForTesting
    static final Gson SERIALIZER = new GsonBuilder()
            .registerTypeAdapter(RawBBModel.class, new Deserializer())
            .registerTypeAdapter(ModelElement.class, new ModelElement.Deserializer())
            .registerTypeAdapter(ElementFace.class, new ElementFace.Deserializer())
            .registerTypeAdapter(ModelGroup.class, new ModelGroup.Deserializer())
            .registerTypeAdapter(ItemTransform.class, new ItemTransformDeserializer())
            .registerTypeAdapter(ItemTransforms.class, new ItemTransforms.Deserializer())
            .registerTypeAdapter(Animator.class, new Animator.Deserializer())
            .registerTypeAdapter(Bone.class, new Bone.Deserializer())
            .registerTypeAdapter(Predicate.class, new Predicate.Deserializer())
            .create();
    private final Map<UUID, ModelElement> elements;
    private final List<ModelGroup> groups;
    public Vector3f rotation = new Vector3f();
    public Vector3f origin = new Vector3f();
    private final ItemTransforms cameraTransforms;
    public String name = "";
    @VisibleForTesting
    public final Map<String, String> textures;
    public final Animator animator;

    public static RawBBModel deserialize(Reader readerIn)
    {
        return GsonHelper.fromJson(SERIALIZER, readerIn, RawBBModel.class, false);
    }

    public RawBBModel(Map<UUID, ModelElement> elementsIn, List<ModelGroup> groups, Map<String, String> texturesIn, ItemTransforms cameraTransformsIn, Animator animator)
    {
        this.elements = elementsIn;
        this.groups = groups;
        this.textures = texturesIn;
        this.cameraTransforms = cameraTransformsIn;
        this.animator = animator;
    }

    public Map<UUID, ModelElement> getElements()
    {
        return this.elements;
    }

    public List<ModelGroup> getGroups() {
        return groups;
    }

    public ItemTransforms getAllTransforms()
    {
        ItemTransform itemtransformvec3f = this.getTransform(ItemTransforms.TransformType.THIRD_PERSON_LEFT_HAND);
        ItemTransform itemtransformvec3f1 = this.getTransform(ItemTransforms.TransformType.THIRD_PERSON_RIGHT_HAND);
        ItemTransform itemtransformvec3f2 = this.getTransform(ItemTransforms.TransformType.FIRST_PERSON_LEFT_HAND);
        ItemTransform itemtransformvec3f3 = this.getTransform(ItemTransforms.TransformType.FIRST_PERSON_RIGHT_HAND);
        ItemTransform itemtransformvec3f4 = this.getTransform(ItemTransforms.TransformType.HEAD);
        ItemTransform itemtransformvec3f5 = this.getTransform(ItemTransforms.TransformType.GUI);
        ItemTransform itemtransformvec3f6 = this.getTransform(ItemTransforms.TransformType.GROUND);
        ItemTransform itemtransformvec3f7 = this.getTransform(ItemTransforms.TransformType.FIXED);
        return new ItemTransforms(itemtransformvec3f, itemtransformvec3f1, itemtransformvec3f2, itemtransformvec3f3, itemtransformvec3f4, itemtransformvec3f5, itemtransformvec3f6, itemtransformvec3f7);
    }

    private ItemTransform getTransform(ItemTransforms.TransformType type)
    {
        return this.cameraTransforms.getTransform(type);
    }

    public static class Deserializer implements JsonDeserializer<RawBBModel>
    {
        public RawBBModel deserialize(JsonElement baseElement, Type type, JsonDeserializationContext context) throws JsonParseException
        {

            JsonObject baseObject = (JsonObject) baseElement;

            this.checkVersion(baseElement);

            Map<UUID, ModelElement> elements = this.getElements(baseObject, context);

            List<ModelGroup> groups = this.getGroups(baseObject, context);

            Map<String, String> textures = this.getTextures(baseObject);

            ItemTransforms itemcameratransforms = ItemTransforms.NO_TRANSFORMS;

            if (baseObject.has("display"))
            {
                JsonObject jsonobject1 = GsonHelper.getAsJsonObject(baseObject, "display");
                itemcameratransforms = context.deserialize(jsonobject1, ItemTransforms.class);
            }

            Animator animator = context.deserialize(baseObject.get("animation_data"), Animator.class);

            return new RawBBModel(elements, groups, textures, itemcameratransforms, animator);

        }

        private void checkVersion(JsonElement element) {

            JsonObject object = element.getAsJsonObject();

            JsonObject meta = object.getAsJsonObject("meta");

            //TODO: do proper version check
            //if (!GsonHelper.getAsString(meta, "format_version", "").startsWith("3")) {
            //    throw new JsonParseException("Version must be at least 3.0");
            //}

            if (!GsonHelper.getAsString(meta, "model_format", "").equals("free")) {
                throw new JsonParseException("Model must be free Model");
            }

            if (GsonHelper.getAsBoolean(meta, "box_uv", true)) {
                throw new JsonParseException("Box UV not supported");
            }
        }

        private Map<String, String> getTextures(JsonObject object)
        {
            Map<String, String> map = Maps.<String, String>newHashMap();

            if (object.has("textures"))
            {
                JsonArray texturesArray = object.getAsJsonArray("textures");

                for (JsonElement textureElement : texturesArray)
                {
                    JsonObject textureObject = (JsonObject) textureElement;
                    map.put(textureObject.get("id").getAsString(), new ResourceLocation(textureObject.get("namespace").getAsString(), textureObject.get("folder").getAsString() + "/" + textureObject.get("name").getAsString().replace(".png", "")).toString());
                }
            }

            return map;
        }

        private Map<UUID, ModelElement> getElements(JsonObject jsonObject, JsonDeserializationContext context) {

            JsonArray elementsArray = jsonObject.getAsJsonArray("elements");

            Map<UUID, ModelElement> map = new HashMap<>();

            for (JsonElement jsonElement : elementsArray) {
                ModelElement modelElement = context.deserialize(jsonElement, ModelElement.class);
                map.put(modelElement.uuid, modelElement);
            }

            return map;
        }

        private List<ModelGroup> getGroups(JsonObject baseObject, JsonDeserializationContext context) {

            JsonArray groupsArray = baseObject.getAsJsonArray("outliner");

            List<ModelGroup> list = new ArrayList<>();

            for (JsonElement jsonElement : groupsArray) {
                if (jsonElement.isJsonObject()) {
                    ModelGroup group = context.deserialize(jsonElement, ModelGroup.class);
                    list.add(group);
                }
            }

            return list;
        }

    }
    public static class ItemTransformDeserializer implements JsonDeserializer<ItemTransform> {
        public static final Vector3f DEFAULT_ROTATION = new Vector3f(0.0F, 0.0F, 0.0F);
        public static final Vector3f DEFAULT_TRANSLATION = new Vector3f(0.0F, 0.0F, 0.0F);
        public static final Vector3f DEFAULT_SCALE = new Vector3f(1.0F, 1.0F, 1.0F);

        @Override
        public ItemTransform deserialize(JsonElement p_111775_, Type p_111776_, JsonDeserializationContext p_111777_) throws JsonParseException {
            JsonObject jsonobject = p_111775_.getAsJsonObject();
            Vector3f vector3f = this.getVector3f(jsonobject, "rotation", DEFAULT_ROTATION);
            Vector3f vector3f1 = this.getVector3f(jsonobject, "translation", DEFAULT_TRANSLATION);
            vector3f1.mul(0.0625F);
            vector3f1.set(vector3f1.x, vector3f1.y, vector3f1.z);
            Vector3f vector3f2 = this.getVector3f(jsonobject, "scale", DEFAULT_SCALE);
            vector3f2.set(vector3f2.x, vector3f2.y, vector3f2.z);
            Vector3f rightRotation = this.getVector3f(jsonobject, "right_rotation", DEFAULT_ROTATION);
            return new ItemTransform(vector3f, vector3f1, vector3f2, rightRotation);
        }

        private Vector3f getVector3f(JsonObject p_111779_, String p_111780_, Vector3f p_253777_) {
            if (!p_111779_.has(p_111780_)) {
                return p_253777_;
            } else {
                JsonArray jsonarray = GsonHelper.getAsJsonArray(p_111779_, p_111780_);
                if (jsonarray.size() != 3) {
                    throw new JsonParseException("Expected 3 " + p_111780_ + " values, found: " + jsonarray.size());
                } else {
                    float[] afloat = new float[3];

                    for(int i = 0; i < afloat.length; ++i) {
                        afloat[i] = GsonHelper.convertToFloat(jsonarray.get(i), p_111780_ + "[" + i + "]");
                    }

                    return new Vector3f(afloat[0], afloat[1], afloat[2]);
                }
            }
        }
    }
}