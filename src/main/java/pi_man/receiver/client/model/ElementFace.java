package pi_man.receiver.client.model;

import java.lang.reflect.Type;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import net.minecraft.util.GsonHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector2f;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public class ElementFace {

    public final Vector4f uv;
    public final String texture;

    public ElementFace(Vector4f uv, String texture) {
        this.uv = uv;
        this.texture = texture;
    }

    public Vector2f getUV(int index) {
        return new Vector2f(
                index == 0 || index == 3 ? uv.get(0) : uv.get(2),
                index == 0 || index == 1 ? uv.get(1) : uv.get(3)
        );
    }

    public static class Deserializer implements JsonDeserializer<ElementFace> {

        public ElementFace deserialize(JsonElement baseElement, Type type, JsonDeserializationContext context) throws JsonParseException {

            JsonObject jsonObject = (JsonObject) baseElement;

            if (jsonObject.get("texture") == null || jsonObject.get("texture").isJsonNull()) {
                return null;
            }

            Vector4f uv = this.getVector4f(jsonObject, "uv", 0, 0, 16, 16);

            String texture = GsonHelper.getAsString(jsonObject, "texture", "missingno");

            return new ElementFace(uv, texture);

        }

        private Vector4f getVector4f(JsonObject jsonObject, String memberName, float... fallback) {

            float[] floats = fallback;

            if (jsonObject.has(memberName)) {

                JsonArray jsonArray = jsonObject.getAsJsonArray(memberName);

                for (int i = 0; i < 4; i++) {
                    floats[i] = jsonArray.get(i).getAsFloat();
                }

            }

            return new Vector4f(floats);
        }

    }

}