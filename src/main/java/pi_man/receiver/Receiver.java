package pi_man.receiver;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.gui.overlay.GuiOverlayManager;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import pi_man.receiver.client.model.BBModelLoader;
import pi_man.receiver.net.NetUtils;
import pi_man.receiver.net.packets.ActionUpdatePacket;
import pi_man.receiver.sounds.ModSoundEvents;
import pi_man.receiver.world.item.ActionableItem;
import pi_man.receiver.world.item.ModItems;
import pi_man.receiver.world.item.component.Frame;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Receiver.MODID)
public class Receiver
{

    public static final String MODID = "receiver";

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);

    public Receiver()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        //MinecraftForge.EVENT_BUS.addListener(ClientModEvents::onWorldRender);
        MinecraftForge.EVENT_BUS.addListener(ClientModEvents::onRenderOverlay);
        MinecraftForge.EVENT_BUS.addListener(ClientModEvents::onInput);

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ModItems.ITEMS.register(modEventBus);
        ModSoundEvents.SOUND_EVENTS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        NetUtils.makeInstance();
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
    }

    private void addCreative(CreativeModeTabEvent.BuildContents event)
    {
        if (event.getTab() == CreativeModeTabs.BUILDING_BLOCKS)
            event.accept(ModItems.M1_GARAND::get);
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        public static class ReceiverRenderType extends RenderType {
            public static final RenderType SOLID = RenderType.create("receiver:solid", BB_MODEL_VERTEX_FORMAT, VertexFormat.Mode.QUADS, 2097152, true, false, RenderType.CompositeState.builder().setLightmapState(RenderType.LIGHTMAP).setShaderState(new RenderStateShard.ShaderStateShard(()->bbModelShader)).setTextureState(BLOCK_SHEET_MIPPED).createCompositeState(true));

            public ReceiverRenderType(String p_173178_, VertexFormat p_173179_, VertexFormat.Mode p_173180_, int p_173181_, boolean p_173182_, boolean p_173183_, Runnable p_173184_, Runnable p_173185_) {
                super(p_173178_, p_173179_, p_173180_, p_173181_, p_173182_, p_173183_, p_173184_, p_173185_);
            }
        }

        public static ShaderInstance bbModelShader = null;
        public static final VertexFormatElement ELEMENT_BONE_ID = new VertexFormatElement(0, VertexFormatElement.Type.USHORT, VertexFormatElement.Usage.GENERIC, 1);
        public static final VertexFormat BB_MODEL_VERTEX_FORMAT = new VertexFormat(ImmutableMap.<String, VertexFormatElement>builder()
                .put("Position",DefaultVertexFormat.ELEMENT_POSITION)
                .put("Color",DefaultVertexFormat.ELEMENT_COLOR)
                .put("UV0",DefaultVertexFormat.ELEMENT_UV0)
                .put("Normal",DefaultVertexFormat.ELEMENT_NORMAL)
                .put("Padding",DefaultVertexFormat.ELEMENT_PADDING)
                .put("Bone_ID",ELEMENT_BONE_ID)
                .build());

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());

        }

        @SubscribeEvent
        public static void onShaderRegister(RegisterShadersEvent event) throws IOException {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), new ResourceLocation(MODID,"bbmodel"), BB_MODEL_VERTEX_FORMAT), (p_172645_) -> {
                bbModelShader = p_172645_;
            });
        }

        @SubscribeEvent
        public static void onModelLoaderRegister(ModelEvent.RegisterGeometryLoaders event) {
            event.register("bbmodel", new BBModelLoader());
        }


        static IKeyConflictContext RECEIVER_KEY_CONFLICT = new IKeyConflictContext() {
            @Override
            public boolean isActive() {
                return false;//KeyConflictContext.IN_GAME.isActive() && Minecraft.getInstance().player != null && Minecraft.getInstance().player.getMainHandItem().getItem() instanceof ActionableItem && !Minecraft.getInstance().options.keySprint.isDown();
            }

            @Override
            public boolean conflicts(IKeyConflictContext other) {
                return this == other;
            }
        };

        static class ReceiverKeyMapping extends KeyMapping {
            public final int action;
            ReceiverKeyMapping(int action, String name, IKeyConflictContext keyConflictContext, InputConstants.Type type, int keycode, String category) {
                super(name, keyConflictContext, type, keycode, category);
                this.action = action;
            }

            public boolean isUsableAndMatches(InputConstants.Key keyCode) {
                return keyCode != InputConstants.UNKNOWN && keyCode.equals(getKey()) && KeyConflictContext.IN_GAME.isActive() && Minecraft.getInstance().player != null && Minecraft.getInstance().player.getMainHandItem().getItem() instanceof ActionableItem && !Minecraft.getInstance().options.keySprint.isDown();
            }

//            @Override
//            public boolean isActiveAndMatches(InputConstants.Key keyCode) {
//                return keyCode != InputConstants.UNKNOWN && keyCode.equals(getKey()) && getKeyConflictContext().isActive();
//            }
        }

        static KeyMapping shoot = null;
        static KeyMapping ads = null;
        static KeyMapping fireMode = null;
        static KeyMapping insert = null;
        static KeyMapping remove = null;
        static KeyMapping pull = null;
        static KeyMapping lock = null;
        static List<ReceiverKeyMapping> keymappingList = new ArrayList<>();
        @SubscribeEvent
        public static void onKeyMappingRegister(RegisterKeyMappingsEvent event) {
            shoot = registerKeyMapping(event, new ReceiverKeyMapping(ActionableItem.SHOOT, "key.receiver.shoot", RECEIVER_KEY_CONFLICT, InputConstants.Type.MOUSE, InputConstants.MOUSE_BUTTON_LEFT, "category.receiver.gun_control"));
            ads = registerKeyMapping(event, new ReceiverKeyMapping(ActionableItem.ADS, "key.receiver.ads", RECEIVER_KEY_CONFLICT, InputConstants.Type.MOUSE, InputConstants.MOUSE_BUTTON_RIGHT, "category.receiver.gun_control"));
            fireMode = registerKeyMapping(event, new ReceiverKeyMapping(ActionableItem.FIRE_MODE, "key.receiver.fireMode", RECEIVER_KEY_CONFLICT, InputConstants.Type.KEYSYM, InputConstants.KEY_V, "category.receiver.gun_control"));
            insert = registerKeyMapping(event, new ReceiverKeyMapping(ActionableItem.INSERT, "key.receiver.insert", RECEIVER_KEY_CONFLICT, InputConstants.Type.KEYSYM, InputConstants.KEY_Z, "category.receiver.gun_control"));
            remove = registerKeyMapping(event, new ReceiverKeyMapping(ActionableItem.REMOVE, "key.receiver.remove", RECEIVER_KEY_CONFLICT, InputConstants.Type.KEYSYM, InputConstants.KEY_E, "category.receiver.gun_control"));
            pull = registerKeyMapping(event, new ReceiverKeyMapping(ActionableItem.PULL, "key.receiver.pull", RECEIVER_KEY_CONFLICT, InputConstants.Type.KEYSYM, InputConstants.KEY_R, "category.receiver.gun_control"));
            lock = registerKeyMapping(event, new ReceiverKeyMapping(ActionableItem.LOCK, "key.receiver.lock", RECEIVER_KEY_CONFLICT, InputConstants.Type.KEYSYM, InputConstants.KEY_T, "category.receiver.gun_control"));
        }

        static KeyMapping registerKeyMapping(RegisterKeyMappingsEvent event, ReceiverKeyMapping keyMapping) {
            event.register(keyMapping);
            keymappingList.add(keyMapping);
            return keyMapping;
        }

        public static void onInput(InputEvent event) {
            if (event instanceof InputEvent.MouseButton.Pre mouseEvent) {
                InputConstants.Key key = InputConstants.Type.MOUSE.getOrCreate(mouseEvent.getButton());
                if (handleInput(key, mouseEvent.getAction())) mouseEvent.setCanceled(true);
            }
            else if (event instanceof InputEvent.Key keyEvent) {
                InputConstants.Key key = InputConstants.Type.KEYSYM.getOrCreate(keyEvent.getKey());
                if (handleInput(key, keyEvent.getAction())) keyEvent.setCanceled(true);
            }
        }

        private static boolean handleInput(InputConstants.Key key, int action) {
            boolean cancel = false;
            if (action == GLFW.GLFW_REPEAT) return cancel;
            for (ReceiverKeyMapping keyMapping : keymappingList) {
                if (keyMapping.isUsableAndMatches(key)) {
                    if (action == GLFW.GLFW_PRESS) cancel = true;
                    NetUtils.getInstance().sendToServer(new ActionUpdatePacket(keyMapping.action, action == GLFW.GLFW_PRESS));
                }
            }
            if (Minecraft.getInstance().options.keySprint.isActiveAndMatches(key) && action == GLFW.GLFW_PRESS) {
                for (ReceiverKeyMapping keyMapping : keymappingList) {
                    NetUtils.getInstance().sendToServer(new ActionUpdatePacket(keyMapping.action, false));
                }
            }
            return cancel;
        }

        public static void onRenderOverlay(RenderGuiOverlayEvent event) {
            if (event.getOverlay() == GuiOverlayManager.findOverlay(VanillaGuiOverlay.CROSSHAIR.id())
                    && Minecraft.getInstance().player.getInventory().getSelected().getItem() instanceof ActionableItem actionableItem
                    && actionableItem.getComponent(Frame.class).isPresent()) {
                event.setCanceled(true);
            }
        }
//          TODO reference for later
//        public static void onWorldRender(RenderLevelStageEvent event) {
//            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
//                BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
//                bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
//
//                double size = 0.005, offset = -0.03;
//
//                bufferBuilder.vertex(size, size + offset, 0)
//                        .color(1.0f, 0.0f, 0.0f, 0.5f)
//                        .endVertex();
//                bufferBuilder.vertex(size, size + offset, -64)
//                        .color(1.0f, 0.0f, 0.0f, 0.5f)
//                        .endVertex();
//                bufferBuilder.vertex(-size, -size + offset, -64)
//                        .color(1.0f, 0.0f, 0.0f, 0.5f)
//                        .endVertex();
//                bufferBuilder.vertex(-size, -size + offset, 0)
//                        .color(1.0f, 0.0f, 0.0f, 0.5f)
//                        .endVertex();
//
//                bufferBuilder.vertex(size, -size + offset, 0)
//                        .color(1.0f, 0.0f, 0.0f, 0.5f)
//                        .endVertex();
//                bufferBuilder.vertex(size, -size + offset, -64)
//                        .color(1.0f, 0.0f, 0.0f, 0.5f)
//                        .endVertex();
//                bufferBuilder.vertex(-size, size + offset, -64)
//                        .color(1.0f, 0.0f, 0.0f, 0.5f)
//                        .endVertex();
//                bufferBuilder.vertex(-size, size + offset, 0)
//                        .color(1.0f, 0.0f, 0.0f, 0.5f)
//                        .endVertex();
//
//                RenderSystem.enableBlend();
//                RenderSystem.defaultBlendFunc();
//                RenderSystem.setShader(() -> GameRenderer.getPositionColorShader());
//                Tesselator.getInstance().end();
//            }
//        }
    }

    public static class PropertyKey {
        public final String majorKey, minorKey;

        public PropertyKey(String key) {
            if (key.contains(":")) {
                String keys[] = key.split(":", 2);
                majorKey = keys[0];
                minorKey = keys[1];
            }
            else {
                majorKey = "";
                minorKey = key;
            }
        }

        public PropertyKey(String majorKey, String minorKey) {
            this.majorKey = majorKey;
            this.minorKey = minorKey;
        }

        @Override
        public String toString() {
            return majorKey + ":" + minorKey;
        }
    }

    public static long rand(long seed) {
        return (seed * 48271) % 2147483647;
    }

    public static String getItemKey(Item item) {
        ResourceLocation resourceLocation = ForgeRegistries.ITEMS.getKey(item);
        return resourceLocation.equals(ForgeRegistries.ITEMS.getDefaultKey()) ? "" : resourceLocation.toString();
    }
}
