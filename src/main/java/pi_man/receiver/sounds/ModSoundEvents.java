package pi_man.receiver.sounds;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import pi_man.receiver.Receiver;

public class ModSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Receiver.MODID);

    public static final RegistryObject<SoundEvent> M1_GARAND_PING = SOUND_EVENTS.register("m1_garand_ping", () ->
            SoundEvent.createVariableRangeEvent(new ResourceLocation(Receiver.MODID, "m1_garand_ping")));

    public static final RegistryObject<SoundEvent> RIFLE_SHOT = SOUND_EVENTS.register("rifle_shot", () ->
            SoundEvent.createVariableRangeEvent(new ResourceLocation(Receiver.MODID, "rifle_shot")));
}
