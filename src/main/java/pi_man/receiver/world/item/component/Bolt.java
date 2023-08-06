package pi_man.receiver.world.item.component;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import pi_man.receiver.Receiver;
import pi_man.receiver.world.item.ActionableItem;
import pi_man.receiver.world.item.ModItems;

public class Bolt extends Component {

    private static final Logger LOGGER =LogUtils.getLogger();

    public Bolt(String name) {
        super(name);
    }

    @Override
    public void preUpdate(Level level, LivingEntity entity, ItemStack itemStack, boolean held) {

        CompoundTag tag = getTag(itemStack);

        float boltPos = tag.getFloat("boltPos");
        boolean recoil = tag.getBoolean("recoil");
        boolean lock = tag.getBoolean("lock");

        boolean flag1 = boltPos > 0.8f;
        boolean flag2 = boltPos > 0.85f;
        boolean flag3 = boltPos > 0.9f;

        if (recoil) {
            boltPos = 1.0f;
            tag.putBoolean("recoil", false);
        }
        else {
            if (tag.getBoolean("pulling")) {
                float limit = lock ? 0.75f : 1.0f;
                if (boltPos < limit) {
                    boltPos += 0.25f;
                    if (boltPos > limit) {
                        boltPos = limit;
                    }
                }
                if (boltPos > limit) {
                    boltPos += 0.25f;
                    if (boltPos > 1.0f) {
                        boltPos = 1.0f;
                    }
                }
                if (!flag2 && boltPos > 0.85f) {
                    tag.putBoolean("lock", false);
                }
            }
            else {
                float limit = lock ? 0.85f : 0.0f;
                if (boltPos > limit) {
                    boltPos -= 1.0f;
                    if (boltPos < limit) {
                        boltPos = limit;
                    }
                    if (flag1 && boltPos <= 0.8f) {
                        item.getComponent(AmmoHolderHolder.class).flatMap(ammoHolderHolder -> ammoHolderHolder.strip(itemStack)).ifPresent(ammunition -> tag.putString("ammunition", Receiver.getItemKey(ammunition)));
                    }
                }
                if (boltPos < limit) {
                    boltPos -= 1.0f;
                    if (boltPos < 0) {
                        boltPos = 0;
                    }
                }
            }
        }

        if (!flag1 && boltPos > 0.8f) {
            item.getComponent(Chamber.class).ifPresent(chamber -> {
                chamber.get(itemStack).ifPresent(ammunition -> {
                    if (!level.isClientSide()) {
                        Vec3 look = entity.getLookAngle();
                        look = new Vec3(-look.z, 0, look.x).normalize().scale(0.2f);
                        ItemEntity itemEntity = new ItemEntity(level, entity.getEyePosition().x, entity.getEyePosition().y, entity.getEyePosition().z, new ItemStack(ammunition), look.x, 0.2, look.z);
                        itemEntity.setDefaultPickUpDelay();
                        level.addFreshEntity(itemEntity);
                    }
                    chamber.set(itemStack, null);
                    tag.putString("ammunition", "");
                });
            });
        }
        if (!flag3 && boltPos > 0.9f) {
            item.getComponent(AmmoHolderHolder.class).ifPresent(ammoHolderHolder -> {
                ammoHolderHolder.onBoltOpen(this, itemStack, entity);
            });
        }

        if (boltPos == 0.0f) {
            item.getComponent(Chamber.class).ifPresent(chamber -> {
                chamber.getTag(itemStack).putString("ammunition", tag.getString("ammunition"));
            });
        }

        tag.putFloat("boltPos", boltPos);

    }

    @Override
    public void update(Level level, LivingEntity entity, ItemStack itemStack, boolean held) {

    }

    @Override
    public void act(ItemStack itemStack, LivingEntity entity, int action, boolean state) {
        if (action == ActionableItem.PULL) {
            getTag(itemStack).putBoolean("pulling", state);
        }
        else if (action == ActionableItem.LOCK) {
            getTag(itemStack).putBoolean("lock", state);
        }
    }

    public boolean canResetSeer(ItemStack itemStack) {
        return getTag(itemStack).getFloat("boltPos") == 0.0f;
    }

    public void recoil(ItemStack itemStack) {
        getTag(itemStack).putBoolean("recoil", true);
    }

    public void lock(ItemStack itemStack) {
        getTag(itemStack).putBoolean("lock", true);
    }
}
