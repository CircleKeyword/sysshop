package com.sysshop.core;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.StringTextComponent;

/** Client-only screen routing and the most recent server snapshot. */
public final class ShopClientState {
    private static final Gson GSON = new Gson();
    public static ShopData.Snapshot snapshot = new ShopData.Snapshot();

    private ShopClientState() { }

    public static void openBrowse() {
        Minecraft.getInstance().setScreen(new ShopScreen());
        send("open");
    }

    public static void openCreate() {
        Minecraft.getInstance().setScreen(new CreateShopScreen());
    }

    public static void openManager() {
        Minecraft.getInstance().setScreen(new ManagerScreen());
        send("manager");
    }

    public static void openRestock(String shopId) {
        Minecraft.getInstance().setScreen(new RestockScreen(shopId));
        send("restock_view", "shop", shopId);
    }

    public static void send(String action) {
        JsonObject request = new JsonObject();
        request.addProperty("action", action);
        ShopNetwork.sendToServer(request.toString());
    }

    public static void send(String action, String key, String value) {
        JsonObject request = new JsonObject();
        request.addProperty("action", action);
        request.addProperty(key, value == null ? "" : value);
        ShopNetwork.sendToServer(request.toString());
    }

    public static void send(JsonObject request) {
        ShopNetwork.sendToServer(request.toString());
    }

    public static JsonObject action(String action) {
        JsonObject request = new JsonObject();
        request.addProperty("action", action);
        return request;
    }

    public static void receive(String json) {
        try {
            ShopData.Snapshot next = GSON.fromJson(json, ShopData.Snapshot.class);
            if (next == null) return;
            snapshot = next;
            Minecraft minecraft = Minecraft.getInstance();
            if (next.notice != null && !next.notice.isEmpty() && minecraft.player != null)
                minecraft.player.displayClientMessage(new StringTextComponent(next.notice), false);
            if ("manager".equals(next.mode) && !(minecraft.screen instanceof ManagerScreen))
                minecraft.setScreen(new ManagerScreen());
            else if ("browse".equals(next.mode) && !(minecraft.screen instanceof ShopScreen))
                minecraft.setScreen(new ShopScreen());
            else if ("restock".equals(next.mode) && !(minecraft.screen instanceof RestockScreen))
                minecraft.setScreen(new RestockScreen(next.selectedId));
        } catch (RuntimeException ex) {
            Minecraft.getInstance().player.displayClientMessage(new StringTextComponent("商店数据无法读取。"), false);
        }
    }
}
