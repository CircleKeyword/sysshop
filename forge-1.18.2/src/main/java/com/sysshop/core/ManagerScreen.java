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

public final class ManagerScreen extends Screen {
    private int shopOffset;
    private int offerOffset;
    private ItemStack selectedCopy = ItemStack.EMPTY;
    public ManagerScreen() { super(new TextComponent("管理我的商店")); }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        renderBackground(pose);
        fill(pose, 0, 0, width, height, 0xB9081723);
        fill(pose, 10, 10, width - 10, 34, 0xFF124B62);
        fill(pose, 12, 36, 126, height - 92, 0xFF10364A);
        fill(pose, 130, 36, width - 12, height - 92, 0xFF152F42);
        fill(pose, 10, 10, width - 10, 12, 0xFF31D0B3);
        drawString(pose, font, "管理我的商店", 20, 18, 0xFFF4F8F7);
        button(pose, width - 190, 15, 55, 16, "浏览", mouseX, mouseY, true);
        ShopData.Snapshot state = ShopClientState.snapshot;
        button(pose, width - 128, 15, 55, 16, "补货", mouseX, mouseY, state.selected != null && !state.selected.system);
        button(pose, width - 66, 15, 46, 16, "关闭", mouseX, mouseY, true);
        drawString(pose, font, "我的商店", 20, 43, 0xFF8BDCCB);
        shopOffset = Math.max(0, Math.min(shopOffset, Math.max(0, state.shops.size() - 2)));
        int shopEnd = state.shops == null ? 0 : Math.min(state.shops.size(), shopOffset + 2);
        for (int i = shopOffset; i < shopEnd; i++) {
            ShopData.Shop shop = state.shops.get(i);
            int y = 56 + (i - shopOffset) * 28;
            fill(pose, 18, y, 120, y + 23, shop.id.equals(state.selectedId) ? 0xFF1D6676 : 0xFF173F53);
            drawString(pose, font, cut(shop.name, 15), 23, y + 7, 0xFFEAF3F3);
        }
        if (state.shops != null && state.shops.size() > 2) {
            button(pose, 18, height - 112, 34, 16, "‹", mouseX, mouseY, shopOffset > 0);
            button(pose, 78, height - 112, 34, 16, "›", mouseX, mouseY, shopOffset + 2 < state.shops.size());
            drawCenteredString(pose, font, (shopOffset + 1) + "-" + Math.min(state.shops.size(), shopOffset + 2)
                    + "/" + state.shops.size(), 65, height - 108, 0xFF9CBAC4);
        }
        if (state.selected == null) drawCenteredString(pose, font, "选择商店后编辑交易", width / 2 + 44, 75, 0xFFB9C9CE);
        else {
            ShopData.Shop selected = state.selected;
            drawString(pose, font, cut(selected.name, 22), 140, 42, 0xFFF0F6F4);
            drawString(pose, font, selected.system ? "系统商店 · 无限供货" : "右键背包物品复制到槽位，不扣除物品", 140, 53, 0xFF9CBAC4);
            int count = selected.offers == null ? 0 : selected.offers.size();
            int pageSize = 2;
            offerOffset = Math.max(0, Math.min(offerOffset, Math.max(0, count - pageSize)));
            for (int i = offerOffset; i < Math.min(count, offerOffset + pageSize); i++) {
                ShopData.Offer offer = selected.offers.get(i);
                int y = 68 + (i - offerOffset) * 27;
                fill(pose, 138, y, width - 19, y + 24, 0xFF1B4053);
                pair(pose, offer.input, 146, y + 2);
                drawCenteredString(pose, font, "→", 203, y + 8, 0xFF70D6BD);
                pair(pose, offer.output, 219, y + 2);
                button(pose, width - 40, y + 3, 18, 18, "×", mouseX, mouseY, true);
            }
            if (selected.offers == null || selected.offers.size() < ShopData.MAX_OFFERS)
                button(pose, 142, 126, 76, 18, "+ 交易行", mouseX, mouseY, true);
            if (count > pageSize) {
                button(pose, 225, 126, 22, 18, "‹", mouseX, mouseY, offerOffset > 0);
                button(pose, 274, 126, 22, 18, "›", mouseX, mouseY, offerOffset + pageSize < count);
                drawCenteredString(pose, font, (offerOffset + 1) + "-" + Math.min(count, offerOffset + pageSize) + "/" + count,
                        260, 131, 0xFF9CBAC4);
            }
        }
        drawInventory(pose);
        if (!selectedCopy.isEmpty()) drawString(pose, font, "已选复制物品：" + selectedCopy.getHoverName().getString(), 14, height - 97, 0xFFFFD178);
        if (state.notice != null && !state.notice.isEmpty()) drawString(pose, font, cut(state.notice, 28), 138, height - 97, 0xFFFFD178);
        super.render(pose, mouseX, mouseY, partialTick);
    }

    private void drawInventory(PoseStack pose) {
        int top = height - 84;
        fill(pose, 12, top - 7, width - 12, height - 12, 0xFF10364A);
        drawString(pose, font, "物品栏 · 左键选择，交易槽右键上架/清空", 20, top - 4, 0xFF8BDCCB);
        for (int row = 0; row < 4; row++) for (int col = 0; col < 9; col++) {
            int slot = row < 3 ? 9 + row * 9 + col : col;
            int x = width / 2 - 81 + col * 18, y = top + 8 + row * 18;
            ItemStack stack = Minecraft.getInstance().player.getInventory().getItem(slot);
            fill(pose, x, y, x + 17, y + 17, !selectedCopy.isEmpty() && stack.getItem() == selectedCopy.getItem()
                    && stack.getCount() == selectedCopy.getCount() ? 0xFF9E8737 : 0xFF263D4B);
            if (!stack.isEmpty()) {
                Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(stack, x + 1, y + 1);
                Minecraft.getInstance().getItemRenderer().renderGuiItemDecorations(font, stack, x + 1, y + 1);
            }
        }
    }

    private void pair(PoseStack pose, ShopData.ItemRef[] refs, int x, int y) {
        for (int i = 0; i < 2; i++) itemSlot(pose, refs != null && refs.length > i ? refs[i] : null, x + i * 22, y);
    }
    private void itemSlot(PoseStack pose, ShopData.ItemRef ref, int x, int y) {
        fill(pose, x, y, x + 20, y + 20, 0xFF51B86F); fill(pose, x, y, x + 20, y + 2, 0xFF90E69E);
        if (ref != null && !ref.isEmpty()) {
            Item item = resolve(ref.id);
            if (item != null) {
                ItemStack stack = new ItemStack(item, Math.min(64, ref.count));
                Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(stack, x + 2, y + 2);
                Minecraft.getInstance().getItemRenderer().renderGuiItemDecorations(font, stack, x + 2, y + 2);
            }
        } else drawCenteredString(pose, font, "+", x + 10, y + 6, 0xFF174F42);
    }
    private Item resolve(String id) { ResourceLocation key = ResourceLocation.tryParse(id); return key == null ? null : ForgeRegistries.ITEMS.getValue(key); }
    private void button(PoseStack pose, int x, int y, int w, int h, String text, int mx, int my, boolean enabled) {
        fill(pose, x, y, x + w, y + h, !enabled ? 0xFF334854 : in(mx, my, x, y, w, h) ? 0xFF247D80 : 0xFF1D626D);
        drawCenteredString(pose, font, text, x + w / 2, y + (h - 8) / 2, enabled ? 0xFFEAF8F4 : 0xFF84999F);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ShopData.Snapshot state = ShopClientState.snapshot;
        if (button == 0) {
            int inventory = inventorySlot(mouseX, mouseY);
            if (inventory >= 0) {
                ItemStack stack = Minecraft.getInstance().player.getInventory().getItem(inventory);
                if (stack.isEmpty() || stack.hasTag()) selectedCopy = ItemStack.EMPTY;
                else selectedCopy = stack.copy();
                return true;
            }
            if (in(mouseX, mouseY, width - 190, 15, 55, 16)) { ShopClientState.openBrowse(); return true; }
            if (in(mouseX, mouseY, width - 128, 15, 55, 16) && state.selected != null && !state.selected.system) { ShopClientState.openRestock(state.selected.id); return true; }
            if (in(mouseX, mouseY, width - 66, 15, 46, 16)) { onClose(); return true; }
            if (state.shops != null && state.shops.size() > 2) {
                if (in(mouseX, mouseY, 18, height - 112, 34, 16) && shopOffset > 0) { shopOffset = Math.max(0, shopOffset - 2); return true; }
                if (in(mouseX, mouseY, 78, height - 112, 34, 16) && shopOffset + 2 < state.shops.size()) { shopOffset += 2; return true; }
            }
            if (state.shops != null) for (int i = shopOffset; i < Math.min(state.shops.size(), shopOffset + 2); i++) {
                int y = 56 + (i - shopOffset) * 28;
                if (in(mouseX, mouseY, 18, y, 102, 23)) { ShopClientState.send("manager", "shop", state.shops.get(i).id); return true; }
            }
            if (state.selected != null) {
                int count = state.selected.offers == null ? 0 : state.selected.offers.size();
                int pageSize = 2;
                offerOffset = Math.max(0, Math.min(offerOffset, Math.max(0, count - pageSize)));
                if (count > pageSize) {
                    if (in(mouseX, mouseY, 225, 126, 22, 18) && offerOffset > 0) { offerOffset = Math.max(0, offerOffset - pageSize); return true; }
                    if (in(mouseX, mouseY, 274, 126, 22, 18) && offerOffset + pageSize < count) { offerOffset += pageSize; return true; }
                }
                for (int i = offerOffset; i < Math.min(count, offerOffset + pageSize); i++) {
                    int y = 68 + (i - offerOffset) * 27;
                    if (in(mouseX, mouseY, width - 40, y + 3, 18, 18)) {
                        JsonObject request = ShopClientState.action("delete_offer"); request.addProperty("shop", state.selected.id); request.addProperty("row", i); ShopClientState.send(request); return true;
                    }
                }
                if ((state.selected.offers == null || count < ShopData.MAX_OFFERS) && in(mouseX, mouseY, 142, 126, 76, 18)) {
                    ShopClientState.send("add_offer", "shop", state.selected.id); return true;
                }
            }
        }
        if (button == 1) {
            if (state.selected != null && state.selected.offers != null) {
                int count = state.selected.offers.size();
                int pageSize = 2;
                offerOffset = Math.max(0, Math.min(offerOffset, Math.max(0, count - pageSize)));
                for (int i = offerOffset; i < Math.min(count, offerOffset + pageSize); i++) {
                    int y = 68 + (i - offerOffset) * 27;
                    if (in(mouseX, mouseY, 146, y + 2, 42, 20)) { setTrade(i, "input", (int)((mouseX - 146) / 22)); return true; }
                    if (in(mouseX, mouseY, 219, y + 2, 42, 20)) { setTrade(i, "output", (int)((mouseX - 219) / 22)); return true; }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void setTrade(int row, String side, int slot) {
        ShopData.Shop shop = ShopClientState.snapshot.selected;
        if (shop == null) return;
        JsonObject request = ShopClientState.action("set_offer");
        request.addProperty("shop", shop.id); request.addProperty("row", row); request.addProperty("side", side);
        request.addProperty("slot", Math.max(0, Math.min(1, slot)));
        if (selectedCopy.isEmpty()) { request.addProperty("item", ""); request.addProperty("count", 0); }
        else { ResourceLocation key = ForgeRegistries.ITEMS.getKey(selectedCopy.getItem()); request.addProperty("item", key == null ? "" : key.toString()); request.addProperty("count", selectedCopy.getCount()); selectedCopy = ItemStack.EMPTY; }
        ShopClientState.send(request);
    }
    private int inventorySlot(double x, double y) {
        int top = height - 84;
        for (int row = 0; row < 4; row++) for (int col = 0; col < 9; col++)
            if (in(x, y, width / 2 - 81 + col * 18, top + 8 + row * 18, 17, 17)) return row < 3 ? 9 + row * 9 + col : col;
        return -1;
    }
    private static boolean in(double x, double y, int left, int top, int w, int h) { return x >= left && x < left + w && y >= top && y < top + h; }
    private String cut(String value, int max) { return value == null || value.length() <= max ? value : value.substring(0, max - 1) + "…"; }
    @Override public boolean isPauseScreen() { return false; }
}
