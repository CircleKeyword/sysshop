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

public final class ShopScreen extends Screen {
    private int shopOffset;
    private int offerOffset;

    public ShopScreen() { super(Component.literal("以物易物")); }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(0, 0, width, height, 0xB9081723);
        g.fill(10, 10, width - 10, 34, 0xFF124B62);
        g.fill(12, 36, 154, height - 12, 0xFF10364A);
        g.fill(158, 36, width - 12, height - 12, 0xFF152F42);
        g.fill(10, 10, width - 10, 12, 0xFF31D0B3);
        g.drawString(font, "以物易物", 20, 18, 0xFFF4F8F7, false);
        drawButton(g, width - 196, 15, 58, 16, "创建", mouseX, mouseY);
        drawButton(g, width - 132, 15, 58, 16, "管理", mouseX, mouseY);
        drawButton(g, width - 66, 15, 46, 16, "关闭", mouseX, mouseY);

        g.drawString(font, "全部商店", 20, 43, 0xFF8BDCCB, false);
        ShopData.Snapshot state = ShopClientState.snapshot;
        if (state.notice != null && !state.notice.isEmpty())
            g.drawString(font, truncate(state.notice, Math.max(10, width / 7)), 166, 43, 0xFFFFD178, false);
        if (state.shops == null || state.shops.isEmpty()) {
            g.drawCenteredString(font, "还没有可浏览的商店", width / 2 + 60, height / 2, 0xFFB9C9CE);
        } else {
            drawShopCards(g, state, mouseX, mouseY);
            drawOffers(g, state, mouseX, mouseY);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawShopCards(GuiGraphics g, ShopData.Snapshot state, int mouseX, int mouseY) {
        int visible = Math.max(1, (height - 68) / 36);
        int start = Math.min(shopOffset, Math.max(0, state.shops.size() - visible));
        for (int i = start; i < Math.min(state.shops.size(), start + visible); i++) {
            ShopData.Shop shop = state.shops.get(i);
            int y = 58 + (i - start) * 36;
            boolean selected = shop.id.equals(state.selectedId);
            boolean hover = inside(mouseX, mouseY, 18, y, 136, 31);
            g.fill(18, y, 154, y + 31, selected ? 0xFF1D6676 : hover ? 0xFF1A5366 : 0xFF173F53);
            g.fill(18, y, 21, y + 31, shop.system ? 0xFF55E0B8 : 0xFF6E9BB0);
            g.drawString(font, truncate(shop.name, 18), 27, y + 5, 0xFFF2F7F7, false);
            String owner = shop.system ? "系统供货 · 无限" : "店主：" + shop.ownerName;
            g.drawString(font, truncate(owner, 19), 27, y + 18, 0xFF9CBAC4, false);
        }
        if (state.shops.size() > visible)
            g.drawString(font, (start + 1) + "–" + Math.min(state.shops.size(), start + visible) + "/" + state.shops.size(), 24, height - 25, 0xFF8AA7B2, false);
    }

    private void drawOffers(GuiGraphics g, ShopData.Snapshot state, int mouseX, int mouseY) {
        ShopData.Shop selected = state.selected;
        if (selected == null) for (ShopData.Shop shop : state.shops)
            if (shop.id.equals(state.selectedId)) { selected = shop; break; }
        if (selected == null) {
            g.drawCenteredString(font, "选择一家商店查看换物", width / 2 + 60, height / 2, 0xFFB9C9CE);
            return;
        }
        int x = 168;
        g.drawString(font, selected.name, x, 57, 0xFFF0F6F4, false);
        g.drawString(font, selected.system ? "系统商店 · 无需补货" : "玩家商店 · 按库存供货", x, 70, 0xFF8FB6C0, false);
        int rowHeight = 54;
        int visible = Math.max(1, (height - 112) / rowHeight);
        int count = selected.offers == null ? 0 : selected.offers.size();
        int start = Math.min(offerOffset, Math.max(0, count - visible));
        if (count == 0) {
            g.drawCenteredString(font, "这家商店还没有可用交易", width / 2 + 60, height / 2, 0xFF9FB4BA);
            return;
        }
        for (int i = start; i < Math.min(count, start + visible); i++) {
            ShopData.Offer offer = selected.offers.get(i);
            int y = 91 + (i - start) * rowHeight;
            g.fill(x, y, width - 21, y + 48, 0xFF1B4053);
            g.fill(x, y, width - 21, y + 1, 0xFF2A6A78);
            drawPair(g, offer.input, x + 8, y + 6, mouseX, mouseY);
            g.drawCenteredString(font, "→", x + 62, y + 12, 0xFF70D6BD);
            drawPair(g, offer.output, x + 78, y + 6, mouseX, mouseY);
            String stock = offer.available < 0 ? "∞" : "库存 " + offer.available;
            g.drawString(font, stock, x + 9, y + 31, offer.available == 0 ? 0xFFFF9A84 : 0xFF96C3B5, false);
            boolean canBuy = offer.available != 0 && hasItems(offer.input) && hasItems(offer.output);
            drawButton(g, width - 68, y + 25, 44, 18, "换物", mouseX, mouseY, canBuy);
        }
        if (count > visible) g.drawString(font, "滚轮查看更多交易", x, height - 23, 0xFF8AA7B2, false);
    }

    private boolean hasItems(ShopData.ItemRef[] items) {
        if (items == null) return false;
        for (ShopData.ItemRef item : items) if (item != null && !item.isEmpty()) return true;
        return false;
    }

    private void drawPair(GuiGraphics g, ShopData.ItemRef[] items, int x, int y, int mouseX, int mouseY) {
        for (int slot = 0; slot < 2; slot++) {
            ShopData.ItemRef ref = items != null && items.length > slot ? items[slot] : null;
            drawItemSlot(g, ref, x + slot * 23, y, mouseX, mouseY);
        }
    }

    private void drawItemSlot(GuiGraphics g, ShopData.ItemRef ref, int x, int y, int mouseX, int mouseY) {
        g.fill(x, y, x + 20, y + 20, 0xFF51B86F);
        g.fill(x, y, x + 20, y + 2, 0xFF90E69E);
        if (ref != null && !ref.isEmpty()) {
            Item item = resolve(ref.id);
            if (item != null) {
                ItemStack stack = new ItemStack(item, Math.min(64, Math.max(1, ref.count)));
                g.renderItem(stack, x + 2, y + 2);
                g.renderItemDecorations(font, stack, x + 2, y + 2);
            }
            if (inside(mouseX, mouseY, x, y, 20, 20)) g.renderTooltip(font, Component.literal(ref.id + " × " + ref.count), mouseX, mouseY);
        } else g.drawCenteredString(font, "·", x + 10, y + 6, 0xFF174F42);
    }

    private Item resolve(String id) {
        try {
            ResourceLocation key = ResourceLocation.tryParse(id);
            return key == null ? null : ForgeRegistries.ITEMS.getValue(key);
        } catch (RuntimeException ex) { return null; }
    }

    private void drawButton(GuiGraphics g, int x, int y, int w, int h, String label, int mouseX, int mouseY) {
        drawButton(g, x, y, w, h, label, mouseX, mouseY, true);
    }

    private void drawButton(GuiGraphics g, int x, int y, int w, int h, String label, int mouseX, int mouseY, boolean enabled) {
        boolean hover = enabled && inside(mouseX, mouseY, x, y, w, h);
        int color = !enabled ? 0xFF334854 : hover ? 0xFF247D80 : 0xFF1D626D;
        g.fill(x, y, x + w, y + h, color);
        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, enabled ? 0xFFEAF8F4 : 0xFF84999F);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        if (inside(mouseX, mouseY, width - 196, 15, 58, 16)) { ShopClientState.openCreate(); return true; }
        if (inside(mouseX, mouseY, width - 132, 15, 58, 16)) { ShopClientState.openManager(); return true; }
        if (inside(mouseX, mouseY, width - 66, 15, 46, 16)) { onClose(); return true; }
        ShopData.Snapshot state = ShopClientState.snapshot;
        if (state.shops != null) {
            int visible = Math.max(1, (height - 68) / 36);
            int start = Math.min(shopOffset, Math.max(0, state.shops.size() - visible));
            for (int i = start; i < Math.min(state.shops.size(), start + visible); i++) {
                int y = 58 + (i - start) * 36;
                if (inside(mouseX, mouseY, 18, y, 136, 31)) {
                    ShopClientState.send("detail", "shop", state.shops.get(i).id);
                    return true;
                }
            }
        }
        ShopData.Shop selected = state.selected;
        if (selected == null && state.shops != null) for (ShopData.Shop shop : state.shops)
            if (shop.id.equals(state.selectedId)) { selected = shop; break; }
        if (selected != null && selected.offers != null) {
            int visible = Math.max(1, (height - 112) / 54);
            int start = Math.min(offerOffset, Math.max(0, selected.offers.size() - visible));
            for (int i = start; i < Math.min(selected.offers.size(), start + visible); i++) {
                int y = 91 + (i - start) * 54;
                if (inside(mouseX, mouseY, width - 68, y + 25, 44, 18)) {
                    JsonObject request = ShopClientState.action("purchase");
                    request.addProperty("shop", selected.id);
                    request.addProperty("row", i);
                    ShopClientState.send(request);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX < 158) shopOffset = Math.max(0, shopOffset - (verticalAmount > 0 ? 1 : -1));
        else offerOffset = Math.max(0, offerOffset - (verticalAmount > 0 ? 1 : -1));
        return true;
    }

    private static boolean inside(double x, double y, int left, int top, int w, int h) {
        return x >= left && x < left + w && y >= top && y < top + h;
    }
    private String truncate(String text, int max) { return text == null || text.length() <= max ? text : text.substring(0, Math.max(0, max - 1)) + "…"; }

    @Override public boolean isPauseScreen() { return false; }
}
