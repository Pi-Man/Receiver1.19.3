package pi_man.receiver.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.client.model.geometry.IGeometryLoader;

public class BBModelLoader implements IGeometryLoader<BBModel> {
    @Override
    public BBModel read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) throws JsonParseException {
        ResourceLocation resourceLocation = new ResourceLocation(GsonHelper.getAsString(jsonObject, "model", "missingno"));
        BBModel bbModel = new BBModel(resourceLocation);
        bbModel.load();
        return bbModel;
    }
}
