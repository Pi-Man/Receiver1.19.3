package pi_man.receiver.world.item.component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.util.TriConsumer;
import pi_man.receiver.Receiver;
import pi_man.receiver.util.QuadConsumer;
import pi_man.receiver.world.item.ActionableItem;
import pi_man.receiver.world.item.ammunition.Ammunition;

import java.util.Optional;
import java.util.function.BiFunction;

public class AmmoHolderHolder extends Component {

    private final String key;
    private final float insertSpeed;
    private final BiFunction<AmmoHolderHolder, ItemStack, Boolean> canInsert;
    private final QuadConsumer<AmmoHolderHolder, Bolt, ItemStack, LivingEntity> onBoltOpen;
    public AmmoHolderHolder(String name, String key, float insertSpeed, BiFunction<AmmoHolderHolder, ItemStack, Boolean> canInsert, QuadConsumer<AmmoHolderHolder, Bolt, ItemStack, LivingEntity> onBoltOpen) {
        super(name);
        this.key = key;
        this.insertSpeed = insertSpeed;
        this.canInsert = canInsert;
        this.onBoltOpen = onBoltOpen;
    }

    @Override
    public void update(Level level, LivingEntity entity, ItemStack itemStack, boolean held) {
        CompoundTag tag = getTag(itemStack);
        if (held) {
            float insert = tag.getFloat("insert");
            if (tag.getBoolean("inserting")) {
                if (insert + insertSpeed < 1.0f) {
                    insert += insertSpeed;
                }
                else {
                    insert = 1.0f;
                    tag.putBoolean("inserting", false);
                }
            }
            if (tag.getBoolean("removing")) {
                if (insert - insertSpeed > 0.0f) {
                    insert -= insertSpeed;
                }
                else {
                    insert = 0.0f;
                    tag.putBoolean("removing", false);
                    ItemStack itemStack1 = removeAmmoHolder(itemStack);
                    if (entity instanceof Player player) {
                        player.addItem(itemStack1);
                    }
                }
            }
            tag.putFloat("insert", insert);
        }
        else {
            if (tag.getBoolean("inserting")) {
                tag.putFloat("insert", 0.0f);
                tag.putBoolean("inserting", false);
            }
            if (tag.getBoolean("removing")) {
                tag.putFloat("insert", 1.0f);
                tag.putBoolean("removing", false);
            }
        }
    }

    @Override
    public void act(ItemStack itemStack, LivingEntity entity, int action, boolean state) {
        CompoundTag tag = getTag(itemStack);
        if (!tag.getBoolean("inserting") && !tag.getBoolean("removing") && canInsert.apply(this, itemStack)) {
            if (action == ActionableItem.INSERT && state) {
                if (tag.getString(key).isEmpty()) {
                    if (entity instanceof Player player) {
                        ItemStack itemStack1 = findAmmoHolder(player);
                        if (!itemStack1.isEmpty()) {
                            putAmmoHolder(itemStack, itemStack1);
                            itemStack1.grow(-1);
                            tag.putBoolean("inserting", true);
                        }
                    }
                }
            } else if (action == ActionableItem.REMOVE && state) {
                if (!tag.getString(key).isEmpty()) {
                    if (entity instanceof Player) {
                        tag.putBoolean("removing", true);
                    }
                }
            }
        }
    }

    public boolean hasAmmoHolder(ItemStack itemStack) {
        return !getTag(itemStack).getString(key).isEmpty();
    }

    public boolean isEmpty(ItemStack itemStack) {
        return getAmmoHolder(itemStack).map(ammoHolder -> ammoHolder.isEmpty(itemStack)).orElse(true);
    }

    public Optional<Ammunition> strip(ItemStack itemStack) {
        return isEmpty(itemStack) ? Optional.empty() : getAmmoHolder(itemStack).map(ammoHolder -> ammoHolder.popAmmo(itemStack));
    }

    public void onBoltOpen(Bolt bolt, ItemStack itemStack, LivingEntity entity) {
        this.onBoltOpen.accept(this, bolt, itemStack, entity);
    }

    private void putAmmoHolder(ItemStack itemStack, ItemStack ammoHolder) {
        if (ammoHolder.getItem() instanceof ActionableItem actionableItem) {
            actionableItem.getComponent(AmmoHolder.class).ifPresent(ammoHolder1 -> {
                CompoundTag tag = getTag(itemStack);
                CompoundTag ammoHolderTag = getTag(itemStack, ammoHolder1);
                CompoundTag ammoHolderTag1 = getTag(ammoHolder, ammoHolder1);
                tag.putString(key, Receiver.getItemKey(ammoHolder.getItem()));
                ammoHolderTag.merge(ammoHolderTag1);
            });
        }
    }

    public ItemStack removeAmmoHolder(ItemStack itemStack) {
        CompoundTag tag = getTag(itemStack);
        ItemStack ammoHolder = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(tag.getString(key))));
        if (!ammoHolder.isEmpty()) {
            if (ammoHolder.getItem() instanceof ActionableItem actionableItem) {
                actionableItem.getComponent(AmmoHolder.class).ifPresent(ammoHolder1 -> {
                    CompoundTag ammoHolderTag = getTag(itemStack, ammoHolder1);
                    CompoundTag ammoHolderTag1 = getTag(ammoHolder, ammoHolder1);
                    tag.putString(key, "");
                    tag.putFloat("insert", 0.0f);
                    ammoHolderTag1.merge(ammoHolderTag);
                });
            }
        }
        return ammoHolder;
    }

    private ItemStack findAmmoHolder(Player player) {
        ItemStack itemStack = player.getOffhandItem();
        if (itemStack.getItem() instanceof ActionableItem actionableItem) {
            if (actionableItem.getComponent(AmmoHolder.class).isPresent()) {
                return itemStack;
            }
        }
        return ItemStack.EMPTY;
    }

    private Optional<AmmoHolder> getAmmoHolder(ItemStack itemStack) {
        CompoundTag tag = getTag(itemStack);
        ItemStack ammoHolder = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(tag.getString(key))));
        if (!ammoHolder.isEmpty()) {
            if (ammoHolder.getItem() instanceof ActionableItem actionableItem) {
                return actionableItem.getComponent(AmmoHolder.class);
            }
        }
        return Optional.empty();
    }
}
