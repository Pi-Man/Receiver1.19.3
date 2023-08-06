package pi_man.receiver.client.model.animation;

import net.minecraft.util.Mth;
import org.joml.Vector3f;

public record Keyframe(float value, Vector3f data) {

    public static Vector3f lerp(Keyframe lowerKeyframe, Keyframe upperKeyframe, float value) {
        return span(value, lowerKeyframe.value, upperKeyframe.value, lowerKeyframe.data, upperKeyframe.data);
    }

    public static float span(float in, float inLow, float inHigh, float outLow, float outHigh) {
        return Mth.lerp(Mth.inverseLerp(in, inLow, inHigh), outLow, outHigh);
    }

    public static Vector3f span(float in, float inLow, float inHigh, Vector3f outLow, Vector3f outHigh) {
        float x = span(in, inLow, inHigh, outLow.x, outHigh.x);
        float y = span(in, inLow, inHigh, outLow.y, outHigh.y);
        float z = span(in, inLow, inHigh, outLow.z, outHigh.z);
        return new Vector3f(x, y, z);
    }

}