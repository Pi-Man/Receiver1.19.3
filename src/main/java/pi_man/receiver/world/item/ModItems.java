package pi_man.receiver.world.item;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import pi_man.receiver.Receiver;
import pi_man.receiver.sounds.ModSoundEvents;
import pi_man.receiver.world.item.ammunition.Ammunition;
import pi_man.receiver.world.item.component.*;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Receiver.MODID);

    public static final RegistryObject<ActionableItem> M1_GARAND = ITEMS.register("m1_garand", () ->
            new ActionableItem(
                    new Item.Properties(),
                    new Frame("Frame"),
                    new FireControlGroup("FireControlGroup", false, false, FireControlGroup.FireMode.SAFE, FireControlGroup.FireMode.SEMI, FireControlGroup.FireMode.BURST, FireControlGroup.FireMode.AUTO),
                    new Bolt("Bolt"),
                    new Chamber("Chamber"),
                    new AmmoHolderHolder("InternalMag", "clip", 0.25f, (ammoHolderHolder, itemStack) -> {
                        return ammoHolderHolder.getItem().getComponent(Bolt.class).map(bolt -> {
                            return bolt.getTag(itemStack).getFloat("boltPos") > 0.8f;
                        }).orElse(false);
                    }, ((ammoHolderHolder, bolt, itemStack, entity) -> {
                        if (ammoHolderHolder.hasAmmoHolder(itemStack) && ammoHolderHolder.isEmpty(itemStack)) {
                            if (!entity.level.isClientSide()) {
                                ItemStack itemStack1 = ammoHolderHolder.removeAmmoHolder(itemStack);
                                ItemEntity itemEntity = new ItemEntity(entity.level, entity.getEyePosition().x, entity.getEyePosition().y, entity.getEyePosition().z, itemStack1, 0.0, 0.2, 0.0);
                                itemEntity.setDefaultPickUpDelay();
                                entity.level.addFreshEntity(itemEntity);
                                entity.level.playSound(null, entity.blockPosition(), ModSoundEvents.M1_GARAND_PING.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);
                            }
                        }
                        if (!ammoHolderHolder.hasAmmoHolder(itemStack)) {
                            bolt.lock(itemStack);
                        }
                    }))
            )
    );

    public static final RegistryObject<ActionableItem> M1_GARAND_CLIP = ITEMS.register("m1_garand_clip", () ->
            new ActionableItem(
                    new Item.Properties(),
                    new AmmoHolder("Clip", 8, 0.15f)
            )
    );

    public static final RegistryObject<Ammunition> _30_06_Cartridge = ITEMS.register("30-06_cartridge", () ->
            new Ammunition(
                    new Item.Properties(),
                    true
            )
    );

    public static final RegistryObject<Ammunition> _30_06_Casing = ITEMS.register("30-06_casing", () ->
            new Ammunition(
                    new Item.Properties(),
                    false
            )
    );

}
