package pi_man.receiver.world.item.component;

import net.minecraft.world.item.ItemStack;
import pi_man.receiver.Receiver;

public interface AnimationPropertyHandler {
    Property getProperty(ItemStack itemStack, Receiver.PropertyKey key, float pt);
}
