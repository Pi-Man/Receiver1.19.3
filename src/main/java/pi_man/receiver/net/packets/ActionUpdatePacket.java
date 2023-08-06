package pi_man.receiver.net.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import pi_man.receiver.world.item.ActionableItem;
import pi_man.receiver.world.item.ammunition.Ammunition;

import java.util.Objects;
import java.util.function.Supplier;

public class ActionUpdatePacket extends ModPacket {

    private final int action;
    private final boolean state;

    public ActionUpdatePacket(int action, boolean state) {
        super(null);
        this.action = action;
        this.state = state;
    }

    public ActionUpdatePacket(FriendlyByteBuf byteBuf) {
        super(byteBuf);
        this.action = byteBuf.readInt();
        this.state = byteBuf.readBoolean();
    }

    @Override
    public void serialize(FriendlyByteBuf byteBuf) {
        byteBuf.writeInt(action);
        byteBuf.writeBoolean(state);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer serverPlayer = ctx.get().getSender();
            if (serverPlayer != null && serverPlayer.getMainHandItem().getItem() instanceof ActionableItem actionableItem) {
                actionableItem.act(serverPlayer.getMainHandItem(), serverPlayer, action, state);
            }
        });
    }
}
