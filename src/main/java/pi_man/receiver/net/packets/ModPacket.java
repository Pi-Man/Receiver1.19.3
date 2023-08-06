package pi_man.receiver.net.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public abstract class ModPacket {

    ModPacket(FriendlyByteBuf byteBuf){}

    abstract void serialize(FriendlyByteBuf byteBuf);

    abstract void handle(Supplier<NetworkEvent.Context> ctx);

}
