package pi_man.receiver.world.item.component;

import net.minecraft.nbt.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import pi_man.receiver.Receiver;
import pi_man.receiver.world.item.ActionableItem;

public class Component {

    protected ActionableItem item = null;
    protected final String name;

    public Component(String name) {
        this.name = name;
    }

    public void setItem(ActionableItem item) {
        if (this.item != null) throw new IllegalStateException("item has already been set");
        this.item = item;
    }

    public ActionableItem getItem() {
        return item;
    }

    public String getName() {
        return name;
    }
    public void preUpdate(Level level, LivingEntity entity, ItemStack itemStack, boolean held) {}

    public void update(Level level, LivingEntity entity, ItemStack itemStack, boolean held) {}

    public void postUpdate(Level level, LivingEntity entity, ItemStack itemStack, boolean held) {}

    public void act(ItemStack itemStack, LivingEntity entity, int action, boolean state) {}

    public CompoundTag getTag(ItemStack itemStack) {
        CompoundTag tag = itemStack.getOrCreateTagElement(Receiver.MODID);
        if (!tag.contains(getName(), Tag.TAG_COMPOUND)) {
            tag.put(getName(), new CompoundTag());
        }
        return tag.getCompound(getName());
    }

    protected CompoundTag getTag(ItemStack itemStack, Component component) {
        CompoundTag tag = itemStack.getOrCreateTagElement(Receiver.MODID);
        if (!tag.contains(component.getName(), Tag.TAG_COMPOUND)) {
            tag.put(component.getName(), new CompoundTag());
        }
        return tag.getCompound(component.getName());
    }
}
