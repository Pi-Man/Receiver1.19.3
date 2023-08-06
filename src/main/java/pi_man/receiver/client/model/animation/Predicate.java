package pi_man.receiver.client.model.animation;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import pi_man.receiver.Receiver;
import pi_man.receiver.world.item.component.AnimationPropertyHandler;

import java.lang.reflect.Type;
import java.util.*;

public class Predicate {

    private final Map<Channel, TreeMap<Float, Keyframe>> keyframes;
    private final Receiver.PropertyKey key;

    public Predicate(Map<Channel, TreeMap<Float, Keyframe>> keyframes, Receiver.PropertyKey key) {
        this.keyframes = keyframes;
        this.key = key;
    }

    public ItemTransform calculate(Vector3f pivot, ItemStack itemStack, ClientLevel clientLevel, LivingEntity livingEntity, int rand) {

        float value = 0;
        if (itemStack.getItem() instanceof AnimationPropertyHandler animationPropertyHandler) {
            value = animationPropertyHandler.getProperty(itemStack, key, Minecraft.getInstance().getPartialTick()).getAsFloat();
        }

        Vector3f translation = calculate(Channel.POSITION, value, new Vector3f(0));
        Vector3f rotation = calculate(Channel.ROTATION, value, new Vector3f(0));
        Vector3f scale = calculate(Channel.SCALE, value, new Vector3f(1));

        rotation.x *= -1;

        translation.mul(1.0f / 16.0f);

        return new Animator.PivotItemTransform(rotation, translation, scale, new Vector3f(), pivot);

    }

    private Vector3f calculate(Channel channel, float value, Vector3f fallback) {
        Map.Entry<Float, Keyframe> lowEntry = keyframes.get(channel).floorEntry(value);
        Map.Entry<Float, Keyframe> highEntry = keyframes.get(channel).higherEntry(value);

        Keyframe lowKeyframe;
        Keyframe highKeyframe;

        if (lowEntry == null) {
            lowKeyframe = new Keyframe(0, fallback);
        }
        else {
            lowKeyframe = lowEntry.getValue();
        }

        if (highEntry == null) {
            if (lowEntry == null) {
                return fallback;
            }
            else return new Vector3f(lowKeyframe.data());
        }
        else {
            highKeyframe = highEntry.getValue();
        }

        return Keyframe.lerp(lowKeyframe, highKeyframe, value);
    }

    public enum Channel {
        POSITION,
        ROTATION,
        SCALE
    }

    public static class Deserializer implements JsonDeserializer<Predicate> {

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
        public Predicate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

            JsonObject predicate = json.getAsJsonObject();

            Receiver.PropertyKey key = new Receiver.PropertyKey(GsonHelper.getAsString(predicate, "property"));

            JsonObject keyframesObject = GsonHelper.getAsJsonObject(predicate, "keyframes");

            Map<Channel, TreeMap<Float, Keyframe>> keyframes = new HashMap<>();

            keyframes.put(Channel.POSITION, new TreeMap<>());
            keyframes.put(Channel.ROTATION, new TreeMap<>());
            keyframes.put(Channel.SCALE, new TreeMap<>());

            for (Map.Entry<String, JsonElement> entry : keyframesObject.entrySet()) {
                float value = Float.parseFloat(entry.getKey());
                JsonObject keyframe = entry.getValue().getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry1 : keyframe.entrySet()) {
                    String channel = entry1.getKey();
                    Keyframe keyframe1 = new Keyframe(value, getVector3f(keyframe, channel, 0.0f, 0.0f, 0.0f));
                    Channel channel1;
                    switch (channel) {
                        case "position" -> {
                            channel1 = Channel.POSITION;
                            keyframe1.data().x *= -1;
                        }
                        case "rotation" -> channel1 = Channel.ROTATION;
                        case "scale" -> channel1 = Channel.SCALE;
                        default -> throw new IllegalArgumentException(channel + " is not a valid channel");
                    }
                    keyframes.get(channel1).put(value, keyframe1);
                }
            }

            return new Predicate(keyframes, key);
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