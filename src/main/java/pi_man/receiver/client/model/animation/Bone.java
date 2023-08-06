package pi_man.receiver.client.model.animation;

import com.google.gson.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import pi_man.receiver.Receiver;

import java.lang.reflect.Type;

public class Bone {

    private final Predicate predicate;
    private final ResourceKey key;
    private final Vector3f pivot;

    public Bone(Predicate predicate, ResourceKey key, Vector3f pivot) {
        this.predicate = predicate;
        this.key = key;
        this.pivot = pivot;
    }
    public ItemTransform calculate(ItemStack itemStack, ClientLevel clientLevel, LivingEntity livingEntity, int rand) {
        return predicate.calculate(pivot, itemStack, clientLevel, livingEntity, rand);
    }
    public boolean checkGroup(String name) {
        return key.type == ResourceKey.Type.LOCAL && key.location.minorKey.equals(name);
    }

    public boolean isDisplay() {
        return key.type == ResourceKey.Type.DISPLAY;
    }

    public boolean isSubModel() {
        return key.type == ResourceKey.Type.MODEL;
    }

    public Receiver.PropertyKey getPropertyKey() {
        return key.location;
    }

    public boolean checkDisplay(String name) {
        return key.type == ResourceKey.Type.DISPLAY && (name.contains(key.location.minorKey) || key.location.minorKey.equals("all"));
    }

    public static class ResourceKey {
        public enum Type {
            LOCAL,
            MODEL,
            VALUE,
            DISPLAY
        }

        public final ResourceKey.Type type;
        public final Receiver.PropertyKey location;
        ResourceKey(String label) {
            String parts[] = label.split(":", 2);
            switch (parts[0]) {
                case "this" -> type = Type.LOCAL;
                case "value" -> type = Type.VALUE;
                case "display" -> type = Type.DISPLAY;
                default -> type = Type.MODEL;
            }
            location = new Receiver.PropertyKey(parts[1]);
        }
    }

    public static class Deserializer implements JsonDeserializer<Bone> {

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
        public Bone deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject bone = json.getAsJsonObject();
            ResourceKey key = new ResourceKey(GsonHelper.getAsString(bone, "target"));
            Vector3f pivot = getVector3f(bone, "pivot", 0.0f, 0.0f, 0.0f);
            pivot.mul(1.0f / 16.0f);
            Predicate predicate = context.deserialize(bone.get("predicate"), Predicate.class);
            return new Bone(predicate, key, pivot);
        }

        private Vector3f getVector3f(JsonObject jsonObject, String memberName, float... fallback) {

            float[] floats = new float[3];

            if (jsonObject.has(memberName)) {

                JsonArray jsonArray = jsonObject.getAsJsonArray(memberName);

                for (int i = 0; i < 3; i++) {
                    floats[i] = jsonArray.get(i).getAsFloat();
                }

            }
            else {
                floats = fallback;
            }

            return floats == null ? null : new Vector3f(floats);
        }
    }
}
