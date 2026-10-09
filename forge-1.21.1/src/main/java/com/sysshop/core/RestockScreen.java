package com.sysshop.core;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class RestockScreen extends Screen {
    private final String shopId;
    private int sourceSlot = -1;

    public RestockScreen(String shopId) {
        super(Component.literal("商店库存"));
        this.shopId = shopId == null ? "" : shopId;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(0, 0, width, height, 0xB9081723);
        g.fill(10, 10, width - 10, 34, 0xFF124B62);
        g.fill(12, 38, width - 12, height - 91, 0xFF10364A);
        g.fill(12, height - 87, width - 12, height - 12, 0xFF10364A);
        g.fill(10, 10, width - 10, 12, 0xFF31D0B3);
        g.drawString(font, "补货仓库 · 54 格", 20, 18, 0xFFF4F8F7, false);
        g.drawString(font, "54格：右键选，左键放/取", 132, 18, 0xFF9CBAC4, false);
        drawButton(g, width - 74, 15, 54, 16, "返回", mouseX, mouseY);

        ShopData.Shop shop = ShopClientState.snapshot.selected;
        if (shop == null || shop.stock == null || shop.stock.size() != ShopData.STORAGE_SLOTS) {
            g.drawCenteredString(font, "正在读取库存…", width / 2, 92, 0xFFB9C9CE);
        } else {
            int gridX = width / 2 - 81;
            int gridY = 44;
            for (int i = 0; i < ShopData.STORAGE_SLOTS; i++) {
                int x = gridX + (i % 9) * 18;
                int y = gridY + (i / 9) * 18;
                g.fill(x, y, x + 17, y + 17, 0xFF263D4B);
                g.fill(x, y, x + 17, y + 1, 0xFF426071);
                ShopData.ItemRef ref = shop.stock.get(i);
                drawItem(g, ref == null ? null : ref.id, ref == null ? 0 : ref.count, x, y);
            }
        }

        int top = height - 82;
        g.drawString(font, "玩家物品栏", 20, top + 4, 0xFF8BDCCB, false);
        if (sourceSlot >= 0 && Minecraft.getInstance().player != null) {
            ItemStack selected = Minecraft.getInstance().player.getInventory().getItem(sourceSlot);
            String label = selected.isEmpty() ? "未选择" : selected.getHoverName().getString() + " × " + selected.getCount();
            g.drawString(font, "已选：" + label, 125, top + 4, 0xFFFFD178, false);
        }
        for (int row = 0; row < 4; row++) for (int col = 0; col < 9; col++) {
            int slot = row < 3 ? 9 + row * 9 + col : col;
            int x = width / 2 - 81 + col * 18;
            int y = top + 13 + row * 18;
            ItemStack stack = Minecraft.getInstance().player == null ? ItemStack.EMPTY
                    : Minecraft.getInstance().player.getInventory().getItem(slot);
            g.fill(x, y, x + 17, y + 17, slot == sourceSlot ? 0xFF9E8737 : 0xFF263D4B);
            g.fill(x, y, x + 17, y + 1, slot == sourceSlot ? 0xFFFFD86B : 0xFF426071);
            if (!stack.isEmpty()) {
                g.renderItem(stack, x + 1, y + 1);
                g.renderItemDecorations(font, stack, x + 1, y + 1);
            }
        }
        if (ShopClientState.snapshot.notice != null && !ShopClientState.snapshot.notice.isEmpty())
            g.drawCenteredString(font, ShopClientState.snapshot.notice, width / 2, 36, 0xFFFFD178);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawItem(GuiGraphics g, String id, int count, int x, int y) {
        if (id == null || id.isEmpty() || count < 1) return;
        try {
            ResourceLocation key = ResourceLocation.tryParse(id);
            Item item = key == null ? null : ForgeRegistries.ITEMS.getValue(key);
            if (item != null) {
                ItemStack stack = new ItemStack(item, Math.min(64, count));
                g.renderItem(stack, x + 1, y + 1);
                g.renderItemDecorations(font, stack, x + 1, y + 1);
            }
        } catch (RuntimeException ignored) { }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && inside(mouseX, mouseY, width - 74, 15, 54, 16)) {
            ShopClientState.send("manager", "shop", shopId);
            Minecraft.getInstance().setScreen(new ManagerScreen());
            return true;
        }
        int storageIndex = storageSlot(mouseX, mouseY);
        if (storageIndex >= 0) {
            if (sourceSlot >= 0 && Minecraft.getInstance().player != null) {
                ItemStack stack = Minecraft.getInstance().player.getInventory().getItem(sourceSlot);
                if (!stack.isEmpty() && stack.getComponentsPatch().isEmpty()) {
                    JsonObject request = ShopClientState.action("restock");
                    request.addProperty("shop", shopId); request.addProperty("source", sourceSlot);
                    request.addProperty("target", storageIndex); request.addProperty("count", stack.getCount());
                    ShopClientState.send(request);
                    sourceSlot = -1;
                }
            } else {
                JsonObject request = ShopClientState.action("withdraw");
                request.addProperty("shop", shopId); request.addProperty("source", storageIndex);
                ShopClientState.send(request);
            }
            return true;
        }
        if (button == 1) {
            int slot = inventorySlot(mouseX, mouseY);
            if (slot >= 0) {
                ItemStack stack = Minecraft.getInstance().player.getInventory().getItem(slot);
                sourceSlot = !stack.isEmpty() && stack.getComponentsPatch().isEmpty() ? slot : -1;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int storageSlot(double mouseX, double mouseY) {
        int x0 = width / 2 - 81, y0 = 44;
        for (int i = 0; i < ShopData.STORAGE_SLOTS; i++)
            if (inside(mouseX, mouseY, x0 + (i % 9) * 18, y0 + (i / 9) * 18, 17, 17)) return i;
        return -1;
    }

    private int inventorySlot(double mouseX, double mouseY) {
        int top = height - 82;
        for (int row = 0; row < 4; row++) for (int col = 0; col < 9; col++) {
            int x = width / 2 - 81 + col * 18;
            int y = top + 13 + row * 18;
            if (inside(mouseX, mouseY, x, y, 17, 17)) return row < 3 ? 9 + row * 9 + col : col;
        }
        return -1;
    }

    private void drawButton(GuiGraphics g, int x, int y, int w, int h, String label, int mouseX, int mouseY) {
        boolean hover = inside(mouseX, mouseY, x, y, w, h);
        g.fill(x, y, x + w, y + h, hover ? 0xFF247D80 : 0xFF1D626D);
        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, 0xFFEAF8F4);
    }

    private static boolean inside(double x, double y, int left, int top, int w, int h) { return x >= left && x < left + w && y >= top && y < top + h; }
    @Override public boolean isPauseScreen() { return false; }
}
