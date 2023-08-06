package pi_man.receiver.world.item;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.nbt.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import pi_man.receiver.Receiver;
import pi_man.receiver.client.ReceiverItemRenderer;
import pi_man.receiver.world.item.component.AnimationPropertyHandler;
import pi_man.receiver.world.item.component.Component;
import pi_man.receiver.world.item.component.Frame;
import pi_man.receiver.world.item.component.Property;

import java.util.*;
import java.util.function.Consumer;

public class ActionableItem extends Item implements AnimationPropertyHandler {

    // start actions
    public static final int SHOOT = 0;
    public static final int ADS = 1;
    public static final int FIRE_MODE = 2;
    public static final int INSERT = 3;
    public static final int REMOVE = 4;
    public static final int PULL = 5;
    public static final int LOCK = 6;
    // end actions

    protected final Map<String, Component> components = new HashMap<>();
    protected final Map<Class<? extends Component>, String> componentNames = new HashMap<>();

    public ActionableItem(Properties properties, Component... components) {
        super(properties);
        Arrays.stream(components).forEach(component -> {
            component.setItem(this);
            this.components.put(component.getName(), component);
            this.componentNames.put(component.getClass(), component.getName());
        });
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            /**
             * Queries this item's renderer.
             * <p>
             * Only used if {@link BakedModel#isCustomRenderer()} returns {@code true} or {@link BlockState#getRenderShape()}
             * returns {@link RenderShape#ENTITYBLOCK_ANIMATED}.
             * <p>
             * By default, returns vanilla's block entity renderer.
             */
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return ReceiverItemRenderer.getInstance();
            }
        });
    }



    @Override
    public Property getProperty(ItemStack itemStack, Receiver.PropertyKey key, float pt) {
        CompoundTag newNBT = itemStack.getOrCreateTagElement(Receiver.MODID);
        CompoundTag oldNBT = newNBT.getCompound("prev");

        List<String> path = new ArrayList<>(Arrays.asList(key.minorKey.split("[/\\\\.]")));

        path.add(0, key.majorKey);

        for(int i = 0; i < path.size() - 1; i++) {
            String part = path.get(i);
            int index = part.indexOf('[');
            if (index != -1) {
                String firstPart = part.substring(0, index);
                Tag newT = newNBT.get(firstPart);
                Tag oldT = oldNBT.get(firstPart);
                ListTag newList = !(newT instanceof ListTag) ? new ListTag() : (ListTag) newT;
                ListTag oldList = !(oldT instanceof ListTag) ? new ListTag() : (ListTag) oldT;

                index = Integer.parseInt(part.substring(index + 1, part.indexOf(']')));
                newNBT = newList.getCompound(index);
                oldNBT = oldList.getCompound(index);
            }
            else {
                newNBT = newNBT.getCompound(part);
                oldNBT = oldNBT.getCompound(part);
            }
        }

        Tag newTag = null;
        Tag oldTag = null;

        String part = path.get(path.size() - 1);
        int index = part.indexOf('[');
        if (index != -1) {
            String firstPart = part.substring(0, index);
            Tag newT = newNBT.get(firstPart);
            Tag oldT = oldNBT.get(firstPart);
            ListTag newList = !(newT instanceof ListTag) ? new ListTag() : (ListTag) newT;
            ListTag oldList = !(oldT instanceof ListTag) ? new ListTag() : (ListTag) oldT;

            index = Integer.parseInt(part.substring(index + 1, part.indexOf(']')));
            if (index >= 0 && index < newList.size() && index < oldList.size()) {
                newTag = newList.get(index);
                oldTag = oldList.get(index);
            }
        }
        else {
            newTag = newNBT.get(part);
            oldTag = oldNBT.get(part);
        }

        if (newTag == null || oldTag == null) {
            return Property.EMPTY;
        }

        return switch (newTag.getId()) {
            case Tag.TAG_BYTE -> makeByteProperty(newTag, oldTag, pt);
            case Tag.TAG_SHORT -> makeShortProperty(newTag, oldTag, pt);
            case Tag.TAG_INT -> makeIntProperty(newTag, oldTag, pt);
            case Tag.TAG_LONG -> makeLongProperty(newTag, oldTag, pt);
            case Tag.TAG_FLOAT -> makeFloatProperty(newTag, oldTag, pt);
            case Tag.TAG_DOUBLE -> makeDoubleProperty(newTag, oldTag, pt);
            case Tag.TAG_BYTE_ARRAY -> makeByteArrayProperty(newTag, oldTag, pt);
            case Tag.TAG_STRING -> makeStringProperty(newTag, oldTag, pt);
            case Tag.TAG_LIST -> makeListProperty(newTag, oldTag, pt);
            case Tag.TAG_COMPOUND -> makeCompoundProperty(newTag, oldTag, pt);
            case Tag.TAG_INT_ARRAY -> makeIntArrayProperty(newTag, oldTag, pt);
            case Tag.TAG_LONG_ARRAY -> makeLongArrayProperty(newTag, oldTag, pt);
            default -> Property.EMPTY;
        };
    }

    public <T extends Component> Optional<T> getComponent(Class<T> tClass) {
        Component c = components.get(componentNames.get(tClass));
        if (c != null && c.getClass() == tClass) {
            return Optional.of((T)c);
        }
        return Optional.empty();
    }

    private void update(Level level, LivingEntity entity, ItemStack itemStack, boolean held) {
        CompoundTag tag = itemStack.getOrCreateTagElement(Receiver.MODID);
        CompoundTag oldTag = tag.copy();
        oldTag.remove("prev");
        tag.put("prev", oldTag);
        components.forEach((s, component) -> component.preUpdate(level, entity, itemStack, held));
        components.forEach((s, component) -> component.update(level, entity, itemStack, held));
        components.forEach((s, component) -> component.postUpdate(level, entity, itemStack, held));
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        update(entity.level, null, stack, false);
        return false;
    }

    @Override
    public void inventoryTick(ItemStack p_41404_, Level p_41405_, Entity p_41406_, int p_41407_, boolean p_41408_) {
        update(p_41405_, p_41406_ instanceof LivingEntity ? (LivingEntity) p_41406_ : null, p_41404_, p_41408_);
    }

    @Override
    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {

        if (oldStack.getItem() != newStack.getItem()) {
            return true;
        }

        UUID oldUUID = getComponent(Frame.class).map(frame -> frame.getUUID(oldStack)).orElse(new UUID(0, 0));
        UUID newUUID = getComponent(Frame.class).map(frame -> frame.getUUID(oldStack)).orElse(new UUID(0, 0));

        return !oldUUID.equals(newUUID);

    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return oldStack.getItem() != newStack.getItem() || slotChanged;
    }

    public void act(ItemStack itemStack, LivingEntity entity, int action, boolean state) {
        components.forEach((key, value) -> value.act(itemStack, entity, action, state));
    }






    private static Property makeByteProperty(Tag newTag, Tag oldTag, float pt) {
        float f = Mth.lerp(pt, ((ByteTag)oldTag).getAsFloat(), ((ByteTag)newTag).getAsFloat());
        return new Property() {
            @Override
            public float getAsFloat() {
                return f;
            }
            @Override
            public String getAsString() {
                return Float.toString(getAsFloat());
            }
        };
    }

    private static Property makeShortProperty(Tag newTag, Tag oldTag, float pt) {
        float f = Mth.lerp(pt, ((ShortTag)oldTag).getAsFloat(), ((ShortTag)newTag).getAsFloat());
        return new Property() {
            @Override
            public float getAsFloat() {
                return f;
            }
            @Override
            public String getAsString() {
                return Float.toString(getAsFloat());
            }
        };
    }

    private static Property makeIntProperty(Tag newTag, Tag oldTag, float pt) {
        float f = Mth.lerp(pt, ((IntTag)oldTag).getAsFloat(), ((IntTag)newTag).getAsFloat());
        return new Property() {
            @Override
            public float getAsFloat() {
                return f;
            }
            @Override
            public String getAsString() {
                return Float.toString(getAsFloat());
            }
        };
    }

    private static Property makeLongProperty(Tag newTag, Tag oldTag, float pt) {
        float f = Mth.lerp(pt, ((LongTag)oldTag).getAsFloat(), ((LongTag)newTag).getAsFloat());
        return new Property() {
            @Override
            public float getAsFloat() {
                return f;
            }
            @Override
            public String getAsString() {
                return Float.toString(getAsFloat());
            }
        };
    }

    private static Property makeFloatProperty(Tag newTag, Tag oldTag, float pt) {
        float f = Mth.lerp(pt, ((FloatTag)oldTag).getAsFloat(), ((FloatTag)newTag).getAsFloat());
        return new Property() {
            @Override
            public float getAsFloat() {
                return f;
            }
            @Override
            public String getAsString() {
                return Float.toString(getAsFloat());
            }
        };
    }

    private static Property makeDoubleProperty(Tag newTag, Tag oldTag, float pt) {
        float f = Mth.lerp(pt, ((DoubleTag)oldTag).getAsFloat(), ((DoubleTag)newTag).getAsFloat());
        return new Property() {
            @Override
            public float getAsFloat() {
                return f;
            }
            @Override
            public String getAsString() {
                return Float.toString(getAsFloat());
            }
        };
    }

    private static Property makeByteArrayProperty(Tag newTag, Tag oldTag, float pt) {
        float f = Mth.lerp(pt, (float)((ByteArrayTag)oldTag).size(), (float)((ByteArrayTag)newTag).size());
        return new Property() {
            @Override
            public float getAsFloat() {
                return f;
            }

            @Override
            public String getAsString() {
                return newTag.getAsString();
            }
        };
    }

    private static Property makeStringProperty(Tag newTag, Tag oldTag, float pt) {
        float f = Mth.lerp(pt, oldTag.getAsString().isEmpty() ? 0.0f : 1.0f, newTag.getAsString().isEmpty() ? 0.0f : 1.0f);
        return new Property() {
            @Override
            public float getAsFloat() {
                return f;
            }

            @Override
            public String getAsString() {
                return newTag.getAsString();
            }
        };
    }

    private static Property makeListProperty(Tag newTag, Tag oldTag, float pt) {
        float f = Mth.lerp(pt, (float)((ListTag)oldTag).size(), (float)((ListTag)newTag).size());
        return new Property() {
            @Override
            public float getAsFloat() {
                return f;
            }

            @Override
            public String getAsString() {
                return newTag.getAsString();
            }
        };
    }

    private static Property makeCompoundProperty(Tag newTag, Tag oldTag, float pt) {
        float f = Mth.lerp(pt, ((CompoundTag)oldTag).isEmpty() ? 0.0f : 1.0f, ((CompoundTag)newTag).isEmpty() ? 0.0f : 1.0f);
        return new Property() {
            @Override
            public float getAsFloat() {
                return f;
            }

            @Override
            public String getAsString() {
                return newTag.getAsString();
            }
        };
    }

    private static Property makeIntArrayProperty(Tag newTag, Tag oldTag, float pt) {
        float f = Mth.lerp(pt, (float)((IntArrayTag)oldTag).size(), (float)((IntArrayTag)newTag).size());
        return new Property() {
            @Override
            public float getAsFloat() {
                return f;
            }

            @Override
            public String getAsString() {
                return newTag.getAsString();
            }
        };
    }

    private static Property makeLongArrayProperty(Tag newTag, Tag oldTag, float pt) {
        float f = Mth.lerp(pt, (float)((LongArrayTag)oldTag).size(), (float)((LongArrayTag)newTag).size());
        return new Property() {
            @Override
            public float getAsFloat() {
                return f;
            }

            @Override
            public String getAsString() {
                return newTag.getAsString();
            }
        };
    }
}
