package pi_man.receiver.client.model;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class ModelElement {

    public final String name;
    public final Vector3f start, end, rotation, origin;
    public final Map<Direction, ElementFace> faces;
    public final UUID uuid;

    public ModelElement(String name, Vector3f start, Vector3f end, Vector3f rotation, Vector3f origin, Map<Direction, ElementFace> faces, UUID uuid) {
        this.name = name;
        this.start = start;
        this.end = end;
        this.rotation = rotation;
        this.origin = origin;
        this.faces = faces;
        this.uuid = uuid;
    }

    public static class Deserializer implements JsonDeserializer<ModelElement> {

        public ModelElement deserialize(JsonElement baseElement, Type type, JsonDeserializationContext context) throws JsonParseException {

            JsonObject jsonObject = (JsonObject) baseElement;

            String name = GsonHelper.getAsString(jsonObject, "name", "");

            Vector3f start = this.getVector3f(jsonObject, "from", 0, 0, 0);

            Vector3f end = this.getVector3f(jsonObject, "to", 0, 0, 0);

            Vector3f rotation = this.getVector3f(jsonObject, "rotation", 0, 0, 0);

            Vector3f origin = this.getVector3f(jsonObject, "origin", 0f, 0f, 0f);

            start.mul(1f/16f);
            end.mul(1f/16f);
            origin.mul(1f/16f);

            rotation.y *= -1;

            JsonObject facesObject = jsonObject.getAsJsonObject("faces");

            Map<Direction, ElementFace> faces = new HashMap<>();

            for (Entry<String, JsonElement> entry : facesObject.entrySet()) {
                Direction facing = Direction.byName(entry.getKey());
                ElementFace face = context.deserialize(entry.getValue(), ElementFace.class);
                if (face != null) {
                    faces.put(facing, face);
                }
            }

            UUID uuid = this.getUUID(jsonObject, "uuid");

            return new ModelElement(name, start, end, rotation, origin, faces, uuid);

        }

        private Vector3f getVector3f(JsonObject jsonObject, String memberName, float... fallback) {

            float[] floats = fallback;

            if (jsonObject.has(memberName)) {

                JsonArray jsonArray = jsonObject.getAsJsonArray(memberName);

                for (int i = 0; i < 3; i++) {
                    floats[i] = jsonArray.get(i).getAsFloat();
                }

            }

            return new Vector3f(floats);
        }

        private UUID getUUID(JsonObject jsonObject, String memberName) {

            String uuid = GsonHelper.getAsString(jsonObject, memberName, "");

            return UUID.fromString(uuid);

        }

    }


}