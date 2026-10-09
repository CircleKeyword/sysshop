package com.sysshop.core;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

import java.nio.charset.StandardCharsets;

/** Forge 1.21.1 SimpleChannel adapter with explicitly directional packets. */
public final class ShopNetwork {
    private static final int MAX_PACKET_BYTES = 131072;
    private static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(SysShopMod.MOD_ID, "shops"))
            .networkProtocolVersion(1)
            .clientAcceptedVersions(Channel.VersionTest.exact(1))
            .serverAcceptedVersions(Channel.VersionTest.exact(1))
            .simpleChannel();

    static {
        register(0, PacketFlow.SERVERBOUND);
        register(1, PacketFlow.CLIENTBOUND);
    }

    private ShopNetwork() { }
    public static void init() { }

    private static void register(int id, PacketFlow direction) {
        CHANNEL.messageBuilder(ShopPacket.class, id)
                .direction(direction)
                .encoder(ShopPacket::encode)
                .decoder(ShopPacket::decode)
                .consumerMainThread(ShopPacket::handle)
                .add();
    }

    public static void sendToServer(String payload) {
        CHANNEL.send(new ShopPacket(payload), PacketDistributor.SERVER.noArg());
    }

    public static void sendTo(ServerPlayer player, String payload) {
        CHANNEL.send(new ShopPacket(payload), PacketDistributor.PLAYER.with(player));
    }

    private static final class ShopPacket {
        private final String payload;
        private ShopPacket(String payload) { this.payload = payload == null ? "{}" : payload; }

        private static void encode(ShopPacket message, FriendlyByteBuf buffer) {
            byte[] bytes = message.payload.getBytes(StandardCharsets.UTF_8);
            if (bytes.length > MAX_PACKET_BYTES) throw new IllegalArgumentException("Shop packet too large");
            buffer.writeVarInt(bytes.length);
            buffer.writeBytes(bytes);
        }

        private static ShopPacket decode(FriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            if (size < 0 || size > MAX_PACKET_BYTES || size > buffer.readableBytes())
                throw new IllegalArgumentException("Invalid shop packet length");
            byte[] bytes = new byte[size];
            buffer.readBytes(bytes);
            return new ShopPacket(new String(bytes, StandardCharsets.UTF_8));
        }

        private static void handle(ShopPacket message, CustomPayloadEvent.Context context) {
            ServerPlayer sender = context.getSender();
            if (sender != null) ShopStore.handle(sender, message.payload);
            else DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ShopClientState.receive(message.payload));
            context.setPacketHandled(true);
        }
    }
}
