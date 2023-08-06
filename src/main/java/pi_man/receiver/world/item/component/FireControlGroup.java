package pi_man.receiver.world.item.component;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import pi_man.receiver.world.item.ActionableItem;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class FireControlGroup extends Component {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final boolean externalHammer;
    private final boolean doubleAction;
    private final List<FireMode> fireModes;

    public FireControlGroup(String name, boolean externalHammer, boolean doubleAction, FireMode... fireModes) {
        super(name);
        this.externalHammer = externalHammer;
        this.doubleAction = doubleAction;
        this.fireModes = Arrays.asList(fireModes);
    }

    @Override
    public void update(Level level, LivingEntity entity, ItemStack itemStack, boolean held) {
        CompoundTag tag = getTag(itemStack);
        Optional<Bolt> boltOptional = item.getComponent(Bolt.class);
        boolean seerFlag = boltOptional.map(bolt -> bolt.canResetSeer(itemStack)).orElse(true);

        boolean trigger = tag.getBoolean("trigger") && held;
        FireMode fireMode = fireModes.get(tag.getInt("fireMode"));

        boolean fire = false;

        switch (fireMode) {
            case SAFE:
                break;
            case SEMI:
                boolean seer = tag.getBoolean("seer");
                if (!trigger && seerFlag) {
                    seer = true;
                }
                if (seer) {
                    if (trigger) {
                        seer = false;
                        fire = true;
                    }
                }
                tag.putBoolean("seer", seer);
                break;
            case BURST:
                boolean burstSeer = tag.getBoolean("seer");
                int burstCount = tag.getInt("burstCount");
                if (!level.isClientSide()) LOGGER.info("burstCount: " + burstCount);
                if (seerFlag) {
                    if (burstCount < 3) {
                        burstSeer = true;
                    }
                    else if (!trigger) {
                        burstSeer = true;
                        burstCount = 0;
                    }
                }
                if (burstSeer) {
                    if (trigger) {
                        burstSeer = false;
                        burstCount++;
                        fire = true;
                    }
                }
                tag.putBoolean("seer", burstSeer);
                tag.putInt("burstCount", burstCount);
                break;
            case AUTO:
                boolean autoSeer = tag.getBoolean("seer");
                if (seerFlag) {
                    autoSeer = true;
                }
                if (autoSeer) {
                    if (trigger) {
                        autoSeer = false;
                        fire = true;
                    }
                }
                tag.putBoolean("seer", autoSeer);
                break;
        }

        if (fire) {
            item.getComponent(Chamber.class).ifPresent(chamber -> chamber.strike(level, entity, itemStack));
        }
    }

    @Override
    public void act(ItemStack itemStack, LivingEntity entity, int action, boolean state) {
        CompoundTag tag = getTag(itemStack);
        if (action == ActionableItem.SHOOT) {
            tag.putBoolean("trigger", state);
        }
        if (action == ActionableItem.FIRE_MODE && state) {
            tag.putInt("fireMode", (tag.getInt("fireMode") + 1) % fireModes.size());
        }
    }

    public enum FireMode {
        SAFE,
        SEMI,
        BURST,
        AUTO
    }
}
