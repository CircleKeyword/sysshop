package com.sysshop.core;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class RestockScreen extends Screen {
    private final String shopId;
    private int sourceSlot = -1;
    public RestockScreen(String shopId) { super(new TextComponent("补货仓库")); this.shopId = shopId == null ? "" : shopId; }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        renderBackground(pose);
        fill(pose, 0, 0, width, height, 0xB9081723);
        fill(pose, 10, 10, width - 10, 34, 0xFF124B62);
        fill(pose, 12, 38, width - 12, height - 91, 0xFF10364A);
        fill(pose, 12, height - 87, width - 12, height - 12, 0xFF10364A);
        fill(pose, 10, 10, width - 10, 12, 0xFF31D0B3);
        drawString(pose, font, "补货仓库 · 54 格", 20, 18, 0xFFF4F8F7);
        drawString(pose, font, "54格：右键选，左键放/取", 132, 18, 0xFF9CBAC4);
        button(pose, width - 74, 15, 54, 16, "返回", mouseX, mouseY);
        ShopData.Shop shop = ShopClientState.snapshot.selected;
        if (shop == null || shop.stock == null || shop.stock.size() != ShopData.STORAGE_SLOTS)
            drawCenteredString(pose, font, "正在读取库存…", width / 2, 86, 0xFFB9C9CE);
        else for (int i = 0; i < ShopData.STORAGE_SLOTS; i++) {
            int x = width / 2 - 81 + (i % 9) * 18, y = 44 + (i / 9) * 18;
            fill(pose, x, y, x + 17, y + 17, 0xFF263D4B);
            fill(pose, x, y, x + 17, y + 1, 0xFF426071);
            ShopData.ItemRef ref = shop.stock.get(i);
            drawItem(pose, ref == null ? null : ref.id, ref == null ? 0 : ref.count, x, y);
        }
        int top = height - 82;
        drawString(pose, font, "玩家物品栏", 20, top + 4, 0xFF8BDCCB);
        if (sourceSlot >= 0) drawString(pose, font, "已选物品槽：" + sourceSlot, 125, top + 4, 0xFFFFD178);
        for (int row = 0; row < 4; row++) for (int col = 0; col < 9; col++) {
            int slot = row < 3 ? 9 + row * 9 + col : col;
            int x = width / 2 - 81 + col * 18, y = top + 13 + row * 18;
            ItemStack stack = Minecraft.getInstance().player.getInventory().getItem(slot);
            fill(pose, x, y, x + 17, y + 17, slot == sourceSlot ? 0xFF9E8737 : 0xFF263D4B);
            if (!stack.isEmpty()) {
                Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(stack, x + 1, y + 1);
                Minecraft.getInstance().getItemRenderer().renderGuiItemDecorations(font, stack, x + 1, y + 1);
            }
        }
        if (ShopClientState.snapshot.notice != null && !ShopClientState.snapshot.notice.isEmpty())
            drawString(pose, font, cut(ShopClientState.snapshot.notice, 40), 20, height - 96, 0xFFFFD178);
        super.render(pose, mouseX, mouseY, partialTick);
    }

    private void drawItem(PoseStack pose, String id, int count, int x, int y) {
        if (id == null || id.isEmpty() || count < 1) return;
        ResourceLocation key = ResourceLocation.tryParse(id);
        Item item = key == null ? null : ForgeRegistries.ITEMS.getValue(key);
        if (item != null) {
            ItemStack stack = new ItemStack(item, Math.min(64, count));
            Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(stack, x + 1, y + 1);
            Minecraft.getInstance().getItemRenderer().renderGuiItemDecorations(font, stack, x + 1, y + 1);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && in(mouseX, mouseY, width - 74, 15, 54, 16)) {
            ShopClientState.send("manager", "shop", shopId); Minecraft.getInstance().setScreen(new ManagerScreen()); return true;
        }
        int target = storageSlot(mouseX, mouseY);
        if (target >= 0) {
            if (sourceSlot >= 0) {
                ItemStack stack = Minecraft.getInstance().player.getInventory().getItem(sourceSlot);
                if (!stack.isEmpty() && !stack.hasTag()) {
                    JsonObject request = ShopClientState.action("restock");
                    request.addProperty("shop", shopId); request.addProperty("source", sourceSlot);
                    request.addProperty("target", target); request.addProperty("count", stack.getCount());
                    ShopClientState.send(request); sourceSlot = -1;
                }
            } else {
                JsonObject request = ShopClientState.action("withdraw"); request.addProperty("shop", shopId); request.addProperty("source", target); ShopClientState.send(request);
            }
            return true;
        }
        if (button == 1) {
            int slot = inventorySlot(mouseX, mouseY);
            if (slot >= 0) {
                ItemStack stack = Minecraft.getInstance().player.getInventory().getItem(slot);
                sourceSlot = !stack.isEmpty() && !stack.hasTag() ? slot : -1;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int storageSlot(double x, double y) {
        for (int i = 0; i < ShopData.STORAGE_SLOTS; i++)
            if (in(x, y, width / 2 - 81 + (i % 9) * 18, 44 + (i / 9) * 18, 17, 17)) return i;
        return -1;
    }
    private int inventorySlot(double x, double y) {
        int top = height - 82;
        for (int row = 0; row < 4; row++) for (int col = 0; col < 9; col++)
            if (in(x, y, width / 2 - 81 + col * 18, top + 13 + row * 18, 17, 17)) return row < 3 ? 9 + row * 9 + col : col;
        return -1;
    }
    private void button(PoseStack pose, int x, int y, int w, int h, String text, int mx, int my) {
        fill(pose, x, y, x + w, y + h, in(mx, my, x, y, w, h) ? 0xFF247D80 : 0xFF1D626D);
        drawCenteredString(pose, font, text, x + w / 2, y + 4, 0xFFEAF8F4);
    }
    private static boolean in(double x, double y, int left, int top, int w, int h) { return x >= left && x < left + w && y >= top && y < top + h; }
    private String cut(String value, int max) { return value == null || value.length() <= max ? value : value.substring(0, max - 1) + "…"; }
    @Override public boolean isPauseScreen() { return false; }
}
