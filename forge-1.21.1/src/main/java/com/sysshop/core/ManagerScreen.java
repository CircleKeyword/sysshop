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

public final class ManagerScreen extends Screen {
    private int shopOffset;
    private int offerOffset;
    private ItemStack selectedCopy = ItemStack.EMPTY;

    public ManagerScreen() { super(Component.literal("管理我的商店")); }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(0, 0, width, height, 0xB9081723);
        g.fill(10, 10, width - 10, 34, 0xFF124B62);
        g.fill(12, 36, 126, height - 92, 0xFF10364A);
        g.fill(130, 36, width - 12, height - 92, 0xFF152F42);
        g.fill(10, 10, width - 10, 12, 0xFF31D0B3);
        g.drawString(font, "管理我的商店", 20, 18, 0xFFF4F8F7, false);
        drawButton(g, width - 190, 15, 55, 16, "浏览", mouseX, mouseY);
        drawButton(g, width - 128, 15, 55, 16, "补货", mouseX, mouseY, ShopClientState.snapshot.selected != null && !ShopClientState.snapshot.selected.system);
        drawButton(g, width - 66, 15, 46, 16, "关闭", mouseX, mouseY);

        ShopData.Snapshot state = ShopClientState.snapshot;
        g.drawString(font, "我的商店", 20, 43, 0xFF8BDCCB, false);
        int visibleShops = Math.max(1, (height - 146) / 28);
        int start = Math.min(shopOffset, Math.max(0, state.shops.size() - visibleShops));
        for (int i = start; i < Math.min(state.shops.size(), start + visibleShops); i++) {
            ShopData.Shop shop = state.shops.get(i);
            int y = 56 + (i - start) * 28;
            boolean active = shop.id.equals(state.selectedId);
            boolean hover = inside(mouseX, mouseY, 18, y, 102, 23);
            g.fill(18, y, 120, y + 23, active ? 0xFF1D6676 : hover ? 0xFF1A5366 : 0xFF173F53);
            g.drawString(font, cut(shop.name, 15), 23, y + 7, 0xFFEAF3F3, false);
        }
        if (state.shops == null || state.shops.isEmpty()) {
            g.drawCenteredString(font, "还没有商店", 70, 74, 0xFFB9C9CE);
            g.drawString(font, "先创建一家", 39, 87, 0xFF90B3BE, false);
        }

        ShopData.Shop selected = state.selected;
        if (selected == null) {
            g.drawCenteredString(font, "选择商店后编辑交易", width / 2 + 50, 72, 0xFFB9C9CE);
        } else {
            g.drawString(font, cut(selected.name, 22), 140, 42, 0xFFF0F6F4, false);
            g.drawString(font, selected.system ? "系统商店 · 无限供货" : "右键背包物品复制到交易槽；不扣除背包物品", 140, 53,
                    selected.system ? 0xFF75D8B7 : 0xFF9CBAC4, false);
            int visibleOffers = Math.max(1, (height - 170) / 27);
            int count = selected.offers == null ? 0 : selected.offers.size();
            int offerStart = Math.min(offerOffset, Math.max(0, count - visibleOffers));
            for (int i = offerStart; i < Math.min(count, offerStart + visibleOffers); i++) {
                ShopData.Offer offer = selected.offers.get(i);
                int y = 68 + (i - offerStart) * 27;
                g.fill(138, y, width - 19, y + 24, 0xFF1B4053);
                drawPair(g, offer.input, 146, y + 2, mouseX, mouseY);
                g.drawCenteredString(font, "→", 203, y + 8, 0xFF70D6BD);
                drawPair(g, offer.output, 219, y + 2, mouseX, mouseY);
                drawButton(g, width - 40, y + 3, 18, 18, "×", mouseX, mouseY);
            }
            if (count < ShopData.MAX_OFFERS)
                drawButton(g, 142, Math.min(height - 116, 72 + Math.min(count, visibleOffers) * 27), 76, 18, "+ 交易行", mouseX, mouseY);
            if (selected.system)
                g.drawString(font, "系统商店无需补货", 225, height - 112, 0xFF78D9B7, false);
        }

        drawInventory(g, mouseX, mouseY);
        if (!selectedCopy.isEmpty()) {
            g.drawString(font, "已选物品（复制）", 14, height - 96, 0xFFFFD178, false);
            g.renderItem(selectedCopy, 112, height - 100);
        }
        if (state.notice != null && !state.notice.isEmpty())
            g.drawString(font, cut(state.notice, Math.max(12, width / 7)), 138, height - 101, 0xFFFFD178, false);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawInventory(GuiGraphics g, int mouseX, int mouseY) {
        int top = height - 84;
        g.fill(12, top - 7, width - 12, height - 12, 0xFF10364A);
        g.drawString(font, "物品栏 · 左键选择，交易槽右键上架/清空", 20, top - 4, 0xFF8BDCCB, false);
        for (int row = 0; row < 4; row++) for (int col = 0; col < 9; col++) {
            int slot = row < 3 ? 9 + row * 9 + col : col;
            int x = width / 2 - 81 + col * 18;
            int y = top + 8 + row * 18;
            ItemStack stack = Minecraft.getInstance().player == null ? ItemStack.EMPTY
                    : Minecraft.getInstance().player.getInventory().getItem(slot);
            boolean chosen = !selectedCopy.isEmpty() && !stack.isEmpty() && ItemStack.isSameItem(selectedCopy, stack)
                    && selectedCopy.getCount() == stack.getCount();
            g.fill(x, y, x + 17, y + 17, chosen ? 0xFF9E8737 : 0xFF263D4B);
            g.fill(x, y, x + 17, y + 1, chosen ? 0xFFFFD86B : 0xFF426071);
            if (!stack.isEmpty()) {
                g.renderItem(stack, x + 1, y + 1);
                g.renderItemDecorations(font, stack, x + 1, y + 1);
            }
        }
    }

    private void drawPair(GuiGraphics g, ShopData.ItemRef[] refs, int x, int y, int mouseX, int mouseY) {
        for (int i = 0; i < 2; i++) drawRef(g, refs != null && refs.length > i ? refs[i] : null, x + i * 22, y, mouseX, mouseY);
    }

    private void drawRef(GuiGraphics g, ShopData.ItemRef ref, int x, int y, int mouseX, int mouseY) {
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
        } else g.drawCenteredString(font, "+", x + 10, y + 6, 0xFF174F42);
    }

    private Item resolve(String id) {
        try { ResourceLocation key = ResourceLocation.tryParse(id); return key == null ? null : ForgeRegistries.ITEMS.getValue(key); }
        catch (RuntimeException ex) { return null; }
    }

    private void drawButton(GuiGraphics g, int x, int y, int w, int h, String label, int mouseX, int mouseY) {
        drawButton(g, x, y, w, h, label, mouseX, mouseY, true);
    }
    private void drawButton(GuiGraphics g, int x, int y, int w, int h, String label, int mouseX, int mouseY, boolean enabled) {
        boolean hover = enabled && inside(mouseX, mouseY, x, y, w, h);
        g.fill(x, y, x + w, y + h, !enabled ? 0xFF334854 : hover ? 0xFF247D80 : 0xFF1D626D);
        g.drawCenteredString(font, label, x + w / 2, y + (h - 8) / 2, enabled ? 0xFFEAF8F4 : 0xFF84999F);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ShopData.Snapshot state = ShopClientState.snapshot;
        if (button == 0) {
            int slot = inventorySlot(mouseX, mouseY);
            if (slot >= 0) {
                ItemStack stack = Minecraft.getInstance().player.getInventory().getItem(slot);
                if (stack.isEmpty() || !stack.getComponentsPatch().isEmpty()) selectedCopy = ItemStack.EMPTY;
                else selectedCopy = stack.copy();
                return true;
            }
            if (inside(mouseX, mouseY, width - 190, 15, 55, 16)) { ShopClientState.openBrowse(); return true; }
            if (inside(mouseX, mouseY, width - 128, 15, 55, 16) && state.selected != null && !state.selected.system) {
                ShopClientState.openRestock(state.selected.id); return true;
            }
            if (inside(mouseX, mouseY, width - 66, 15, 46, 16)) { onClose(); return true; }
            int visible = Math.max(1, (height - 146) / 28);
            int start = Math.min(shopOffset, Math.max(0, state.shops.size() - visible));
            for (int i = start; i < Math.min(state.shops.size(), start + visible); i++) {
                int y = 56 + (i - start) * 28;
                if (inside(mouseX, mouseY, 18, y, 102, 23)) {
                    ShopClientState.send("manager", "shop", state.shops.get(i).id); return true;
                }
            }
            if (state.selected != null) {
                int visibleOffers = Math.max(1, (height - 170) / 27);
                int count = state.selected.offers == null ? 0 : state.selected.offers.size();
                int offerStart = Math.min(offerOffset, Math.max(0, count - visibleOffers));
                for (int i = offerStart; i < Math.min(count, offerStart + visibleOffers); i++) {
                    int y = 68 + (i - offerStart) * 27;
                    if (inside(mouseX, mouseY, width - 40, y + 3, 18, 18)) {
                        JsonObject request = ShopClientState.action("delete_offer");
                        request.addProperty("shop", state.selected.id); request.addProperty("row", i);
                        ShopClientState.send(request); return true;
                    }
                }
                int addY = Math.min(height - 116, 72 + Math.min(count, visibleOffers) * 27);
                if (count < ShopData.MAX_OFFERS && inside(mouseX, mouseY, 142, addY, 76, 18)) {
                    ShopClientState.send("add_offer", "shop", state.selected.id); return true;
                }
            }
        }
        if (button == 1) {
            if (state.selected != null && state.selected.offers != null) {
                int visibleOffers = Math.max(1, (height - 170) / 27);
                int count = state.selected.offers.size();
                int offerStart = Math.min(offerOffset, Math.max(0, count - visibleOffers));
                for (int i = offerStart; i < Math.min(count, offerStart + visibleOffers); i++) {
                    int y = 68 + (i - offerStart) * 27;
                    if (inside(mouseX, mouseY, 146, y + 2, 42, 20)) { setTrade(i, "input", (int) ((mouseX - 146) / 22)); return true; }
                    if (inside(mouseX, mouseY, 219, y + 2, 42, 20)) { setTrade(i, "output", (int) ((mouseX - 219) / 22)); return true; }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void setTrade(int row, String side, int slot) {
        ShopData.Shop selected = ShopClientState.snapshot.selected;
        if (selected == null) return;
        JsonObject request = ShopClientState.action("set_offer");
        request.addProperty("shop", selected.id); request.addProperty("row", row);
        request.addProperty("side", side); request.addProperty("slot", Math.max(0, Math.min(1, slot)));
        if (selectedCopy.isEmpty()) { request.addProperty("item", ""); request.addProperty("count", 0); }
        else {
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(selectedCopy.getItem());
            request.addProperty("item", key == null ? "" : key.toString());
            request.addProperty("count", selectedCopy.getCount());
            selectedCopy = ItemStack.EMPTY;
        }
        ShopClientState.send(request);
    }

    private int inventorySlot(double mouseX, double mouseY) {
        int top = height - 84;
        for (int row = 0; row < 4; row++) for (int col = 0; col < 9; col++) {
            int x = width / 2 - 81 + col * 18;
            int y = top + 8 + row * 18;
            if (inside(mouseX, mouseY, x, y, 17, 17)) return row < 3 ? 9 + row * 9 + col : col;
        }
        return -1;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX < 130) shopOffset = Math.max(0, shopOffset - (verticalAmount > 0 ? 1 : -1));
        else offerOffset = Math.max(0, offerOffset - (verticalAmount > 0 ? 1 : -1));
        return true;
    }

    private static boolean inside(double x, double y, int left, int top, int w, int h) { return x >= left && x < left + w && y >= top && y < top + h; }
    private String cut(String text, int max) { return text == null || text.length() <= max ? text : text.substring(0, Math.max(0, max - 1)) + "…"; }
    @Override public boolean isPauseScreen() { return false; }
}
