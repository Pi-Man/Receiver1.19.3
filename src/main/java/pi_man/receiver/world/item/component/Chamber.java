package pi_man.receiver.world.item.component;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import pi_man.receiver.Receiver;
import pi_man.receiver.world.item.ammunition.Ammunition;

import javax.annotation.Nullable;
import java.util.Optional;

public class Chamber extends Component {
    public Chamber(String name) {
        super(name);
    }

    public Optional<Ammunition> get(ItemStack itemStack) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(getTag(itemStack).getString("ammunition")));
        if (item instanceof Ammunition ammunition) {
            return Optional.of(ammunition);
        }
        else return Optional.empty();
    }

    public boolean isEmpty(ItemStack itemStack) {
        return getTag(itemStack).getString("ammunition").isEmpty();
    }

    public void set(ItemStack itemStack, @Nullable Ammunition ammunition) {
        ResourceLocation resourceLocation = ForgeRegistries.ITEMS.getKey(ammunition);
        getTag(itemStack).putString("ammunition", resourceLocation == null || resourceLocation.equals(ForgeRegistries.ITEMS.getDefaultKey()) ? "" : resourceLocation.toString());
    }

    public void strike(Level level, LivingEntity entity, ItemStack itemStack) {
        if (!isEmpty(itemStack)) {
            get(itemStack).ifPresent(ammunition -> {
                if (ammunition.recoils()) {
                    long seed = getTag(itemStack).getLong("seed");

                    if (seed == 0) seed = level.random.nextLong();
                    seed = Receiver.rand(seed);
                    float theta = (float)((double)seed / (double)(1L << 31) * Math.PI * 2.0);
                    seed = Receiver.rand(seed);
                    float phi = (float)((double)seed / (double)(1L << 31) * Math.PI / 256.0);

                    set(itemStack, ammunition.fire(level, entity, theta, phi));
                    
                    getTag(itemStack).putLong("seed", seed);
                    item.getComponent(Bolt.class).ifPresent(bolt -> bolt.recoil(itemStack));
                }
            });
        }
    }
}
