package pi_man.receiver.world.item.component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import pi_man.receiver.world.item.ActionableItem;

import java.util.UUID;

public class Frame extends Component {

    public Frame(String name) {
        super(name);
    }

    public UUID getUUID(ItemStack itemStack) {
        CompoundTag tag = getTag(itemStack);
        if (!tag.contains("uuid")) {
            tag.putUUID("uuid", UUID.randomUUID());
        }
        return getTag(itemStack).getUUID("uuid");
    }

    @Override
    public void update(Level level, LivingEntity entity, ItemStack itemStack, boolean held) {

        CompoundTag tag = getTag(itemStack);

        float ads = tag.getFloat("ads");
        boolean aiming = tag.getBoolean("aiming") && held;

        if (aiming) {
            ads += 4.0f / 20.0f * ((1.0f - ads) / (0.3f + (1.0f - ads)));
        }
        else {
            ads += 3.0f / 20.0f * (-ads / (0.8f + ads));
        }

        if (ads > 1.0f) {
            ads = 1.0f;
        }
        else if (ads < 0.0f) {
            ads = 0.0f;
        }

        tag.putFloat("ads", ads);
        tag.putBoolean("aiming", aiming);
    }

    @Override
    public void act(ItemStack itemStack, LivingEntity entity, int action, boolean state) {
        CompoundTag tag = getTag(itemStack);
        if (action == ActionableItem.ADS) {
            tag.putBoolean("aiming", state);
        }
    }
}
