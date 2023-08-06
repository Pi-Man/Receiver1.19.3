package pi_man.receiver.net;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import pi_man.receiver.Receiver;
import pi_man.receiver.net.packets.ActionUpdatePacket;

import java.util.Optional;

public class NetUtils {

    private static NetUtils INSTANCE = null;

    public static NetUtils getInstance() {
        return INSTANCE;
    }

    public static void makeInstance() {
        if (INSTANCE != null) throw new IllegalStateException("Instance already exists!");
        INSTANCE = new NetUtils();
    }

    private NetUtils() {
        CHANNEL = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(Receiver.MODID, "main"))
                .networkProtocolVersion(() -> VERSION)
                .clientAcceptedVersions(NetUtils::compareVersions)
                .serverAcceptedVersions(NetUtils::compareVersions)
                .simpleChannel();

        CHANNEL.registerMessage(0, ActionUpdatePacket.class, ActionUpdatePacket::serialize, ActionUpdatePacket::new, ActionUpdatePacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static final String VERSION = "1.0.0";

    public final SimpleChannel CHANNEL;

    public void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    public void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    private static int[] decomposeVersion(String version) {
        int aint[] = new int[3];
        String s[] = version.split("\\.", 3);
        for (int i = 0; i < 3; i++) {
            aint[i] = i < s.length ? Integer.parseInt(s[i]) : 0;
        }
        return aint;
    }

    private static boolean compareVersions(String version) {
        if (version.equals("ABSENT 🤔")) return true;
        int a[] = decomposeVersion(version);
        int b[] = decomposeVersion(VERSION);
        if (a[0] != b[0]) return false; // major version
        if (a[1] < b[1]) return false; // older versions can't read this version but this version can read older versions
        if (a[2] > b[2]) return false; // this version can't read older versions but older versions can read this version
        return true;
    }

}
