package pi_man.receiver.world.item.component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import pi_man.receiver.Receiver;
import pi_man.receiver.world.item.ActionableItem;
import pi_man.receiver.world.item.ammunition.Ammunition;

public class AmmoHolder extends Component {

    private final int capacity;
    private final float insertSpeed;

    public AmmoHolder(String name, int capacity, float insertSpeed) {
        super(name);
        this.capacity = capacity;
        this.insertSpeed = insertSpeed;
    }

    @Override
    public void update(Level level, LivingEntity entity, ItemStack itemStack, boolean held) {
        CompoundTag tag = getTag(itemStack);
        float fill = tag.getFloat("fill");
        int size = tag.getInt("size");
        if (tag.getBoolean("filling")) {
            if (fill + insertSpeed < size) {
                fill += insertSpeed;
            }
            else {
                fill = size;
                tag.putBoolean("filling", false);
            }
        }
        if (tag.getBoolean("removing")) {
            if (fill - insertSpeed > size - 1) {
                fill -= insertSpeed;
            }
            else if (size > 0) {
                Ammunition ammunition = popAmmo(itemStack);
                if (entity instanceof Player player) {
                    player.addItem(new ItemStack(ammunition));
                }
                fill = size - 1;
                tag.putBoolean("removing", false);
            }
        }
        tag.putFloat("fill", fill);
    }

    @Override
    public void act(ItemStack itemStack, LivingEntity entity, int action, boolean state) {
        CompoundTag tag = getTag(itemStack);
        if (!tag.getBoolean("filling") && !tag.getBoolean("removing")) {
            int size = tag.getInt("size");
            if (action == ActionableItem.INSERT && state) {
                if (size < capacity) {
                    if (entity instanceof Player player) {
                        ItemStack itemStack1 = findAmmo(player);
                        if (!itemStack1.isEmpty()) {
                            pushAmmo(itemStack, (Ammunition) itemStack1.getItem());
                            itemStack1.grow(-1);
                            tag.putBoolean("filling", true);
                        }
                    }
                }
            } else if (action == ActionableItem.REMOVE && state) {
                if (size > 0) {
                    if (entity instanceof Player) {
                        getTag(itemStack).putBoolean("removing", true);
                    }
                }
            }
        }
    }

    public boolean isEmpty(ItemStack itemStack) {
        ListTag ammo = getAmmo(itemStack);
        return ammo.getString(0).isEmpty();
    }

    protected ItemStack findAmmo(Player player) {
        ItemStack itemStack = player.getOffhandItem();
        return itemStack.getItem() instanceof Ammunition ? itemStack : ItemStack.EMPTY;
    }

    protected ListTag getAmmo(ItemStack itemStack) {
        CompoundTag tag = getTag(itemStack);
        if (!tag.contains("ammunition", Tag.TAG_LIST)) {
            ListTag listTag = new ListTag();
            for (int i = 0; i < capacity; i++) {
                listTag.add(StringTag.valueOf(""));
            }
            tag.put("ammunition", listTag);
        }
        return (ListTag) tag.get("ammunition");
    }

    protected void pushAmmo(ItemStack itemStack, Ammunition ammunition) {
        CompoundTag tag = getTag(itemStack);
        int size = tag.getInt("size");
        tag.putInt("size", size + 1);
        ListTag ammo = getAmmo(itemStack);
        ammo.set(size, StringTag.valueOf(Receiver.getItemKey(ammunition)));
    }

    protected Ammunition popAmmo(ItemStack itemStack) {
        CompoundTag tag = getTag(itemStack);
        int size = tag.getInt("size");
        tag.putInt("size", size - 1);
        tag.putFloat("fill", size -1 );
        ListTag ammo = getAmmo(itemStack);
        return (Ammunition) ForgeRegistries.ITEMS.getValue(new ResourceLocation(ammo.set(size - 1, StringTag.valueOf("")).getAsString()));
    }
}
