package com.sysshop.core;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

/** Forge 1.16.5 packet adapter. Requests and snapshots use bounded UTF-8 JSON. */
public final class ShopNetwork {
    private static final String PROTOCOL = "1";
    private static final int MAX_PACKET_BYTES = 131072;
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SysShopMod.MOD_ID, "shops"), () -> PROTOCOL,
            PROTOCOL::equals, PROTOCOL::equals);

    static {
        CHANNEL.registerMessage(0, ShopPacket.class, ShopPacket::encode, ShopPacket::decode, ShopPacket::handle);
    }

    private ShopNetwork() { }
    public static void init() { }
    public static void sendToServer(String payload) { CHANNEL.sendToServer(new ShopPacket(payload)); }
    public static void sendTo(ServerPlayerEntity player, String payload) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ShopPacket(payload));
    }

    private static final class ShopPacket {
        private final String payload;
        private ShopPacket(String payload) { this.payload = payload == null ? "{}" : payload; }
        private static void encode(ShopPacket message, PacketBuffer buffer) {
            byte[] bytes = message.payload.getBytes(StandardCharsets.UTF_8);
            if (bytes.length > MAX_PACKET_BYTES) throw new IllegalArgumentException("Shop packet too large");
            buffer.writeVarInt(bytes.length);
            buffer.writeBytes(bytes);
        }
        private static ShopPacket decode(PacketBuffer buffer) {
            int size = buffer.readVarInt();
            if (size < 0 || size > MAX_PACKET_BYTES || size > buffer.readableBytes())
                throw new IllegalArgumentException("Invalid shop packet length");
            byte[] bytes = new byte[size];
            buffer.readBytes(bytes);
            return new ShopPacket(new String(bytes, StandardCharsets.UTF_8));
        }
        private static void handle(ShopPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                ServerPlayerEntity sender = context.getSender();
                if (sender != null) ShopStore.handle(sender, message.payload);
                else DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ShopClientState.receive(message.payload));
            });
            context.setPacketHandled(true);
        }
    }
}
