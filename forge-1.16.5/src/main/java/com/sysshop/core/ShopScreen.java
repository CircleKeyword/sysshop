package com.sysshop.core;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class ShopScreen extends Screen {
    private int shopOffset;
    private int offerOffset;
    public ShopScreen() { super(new StringTextComponent("以物易物")); }

    @Override
    public void render(MatrixStack pose, int mouseX, int mouseY, float partialTick) {
        renderBackground(pose);
        fill(pose, 0, 0, width, height, 0xB9081723);
        fill(pose, 10, 10, width - 10, 34, 0xFF124B62);
        fill(pose, 12, 36, 154, height - 12, 0xFF10364A);
        fill(pose, 158, 36, width - 12, height - 12, 0xFF152F42);
        fill(pose, 10, 10, width - 10, 12, 0xFF31D0B3);
        drawString(pose, font, "以物易物", 20, 18, 0xFFF4F8F7);
        button(pose, width - 196, 15, 58, 16, "创建", mouseX, mouseY, true);
        button(pose, width - 132, 15, 58, 16, "管理", mouseX, mouseY, true);
        button(pose, width - 66, 15, 46, 16, "关闭", mouseX, mouseY, true);
        drawString(pose, font, "全部商店", 20, 43, 0xFF8BDCCB);
        ShopData.Snapshot state = ShopClientState.snapshot;
        if (state.shops == null || state.shops.isEmpty()) {
            drawCenteredString(pose, font, "正在读取商店…", width / 2, height / 2, 0xFFB9C9CE);
            super.render(pose, mouseX, mouseY, partialTick);
            return;
        }
        int cardLimit = 4;
        shopOffset = Math.max(0, Math.min(shopOffset, Math.max(0, state.shops.size() - cardLimit)));
        for (int i = shopOffset; i < Math.min(state.shops.size(), shopOffset + cardLimit); i++) {
            ShopData.Shop shop = state.shops.get(i);
            int y = 56 + (i - shopOffset) * 34;
            boolean selected = shop.id.equals(state.selectedId);
            fill(pose, 18, y, 150, y + 29, selected ? 0xFF1D6676 : 0xFF173F53);
            fill(pose, 18, y, 21, y + 29, shop.system ? 0xFF55E0B8 : 0xFF6E9BB0);
            drawString(pose, font, cut(shop.name, 18), 27, y + 5, 0xFFF2F7F7);
            drawString(pose, font, cut(shop.system ? "系统商店 · 无限" : "店主：" + shop.ownerName, 19), 27, y + 17, 0xFF9CBAC4);
        }
        if (state.shops.size() > cardLimit) {
            button(pose, 18, height - 28, 34, 16, "‹", mouseX, mouseY, shopOffset > 0);
            button(pose, 108, height - 28, 34, 16, "›", mouseX, mouseY, shopOffset + cardLimit < state.shops.size());
            drawCenteredString(pose, font, (shopOffset + 1) + "-" + Math.min(state.shops.size(), shopOffset + cardLimit)
                    + "/" + state.shops.size(), 82, height - 24, 0xFF9CBAC4);
        }
        ShopData.Shop selected = state.selected;
        if (selected == null) for (ShopData.Shop shop : state.shops) if (shop.id.equals(state.selectedId)) selected = shop;
        if (selected == null) drawCenteredString(pose, font, "选择商店查看交易", width / 2 + 55, height / 2, 0xFFB9C9CE);
        else {
            drawString(pose, font, cut(selected.name, 24), 168, 56, 0xFFF0F6F4);
            drawString(pose, font, selected.system ? "无需补货" : "玩家商店 · 现货兑换", 168, 68, 0xFF8FB6C0);
            int count = selected.offers == null ? 0 : selected.offers.size();
            int pageSize = Math.max(1, (height - 116) / 48);
            offerOffset = Math.max(0, Math.min(offerOffset, Math.max(0, count - pageSize)));
            for (int i = offerOffset; i < Math.min(count, offerOffset + pageSize); i++) {
                ShopData.Offer offer = selected.offers.get(i);
                int y = 84 + (i - offerOffset) * 48;
                fill(pose, 166, y, width - 20, y + 42, 0xFF1B4053);
                pair(pose, offer.input, 174, y + 6);
                drawCenteredString(pose, font, "→", 230, y + 13, 0xFF70D6BD);
                pair(pose, offer.output, 246, y + 6);
                String amount = offer.available < 0 ? "∞" : "库存 " + offer.available;
                drawString(pose, font, amount, 174, y + 29, offer.available == 0 ? 0xFFFF9A84 : 0xFF96C3B5);
                button(pose, width - 68, y + 24, 44, 17, "换物", mouseX, mouseY,
                        offer.available != 0 && has(offer.input) && has(offer.output));
            }
            if (count > pageSize) {
                button(pose, 168, height - 28, 30, 16, "‹", mouseX, mouseY, offerOffset > 0);
                button(pose, width - 54, height - 28, 30, 16, "›", mouseX, mouseY, offerOffset + pageSize < count);
                drawCenteredString(pose, font, (offerOffset + 1) + "-" + Math.min(count, offerOffset + pageSize) + "/" + count,
                        width / 2 + 42, height - 24, 0xFF9CBAC4);
            }
        }
        super.render(pose, mouseX, mouseY, partialTick);
    }

    private boolean has(ShopData.ItemRef[] pair) {
        if (pair == null) return false;
        for (ShopData.ItemRef ref : pair) if (ref != null && !ref.isEmpty()) return true;
        return false;
    }
    private void pair(MatrixStack pose, ShopData.ItemRef[] refs, int x, int y) {
        for (int i = 0; i < 2; i++) itemSlot(pose, refs != null && refs.length > i ? refs[i] : null, x + i * 22, y);
    }
    private void itemSlot(MatrixStack pose, ShopData.ItemRef ref, int x, int y) {
        fill(pose, x, y, x + 20, y + 20, 0xFF51B86F);
        fill(pose, x, y, x + 20, y + 2, 0xFF90E69E);
        if (ref != null && !ref.isEmpty()) {
            Item item = resolve(ref.id);
            if (item != null) {
                ItemStack stack = new ItemStack(item, Math.min(64, ref.count));
                Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(stack, x + 2, y + 2);
                Minecraft.getInstance().getItemRenderer().renderGuiItemDecorations(font, stack, x + 2, y + 2);
            }
        } else drawCenteredString(pose, font, "·", x + 10, y + 6, 0xFF174F42);
    }
    private Item resolve(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        return key == null ? null : ForgeRegistries.ITEMS.getValue(key);
    }
    private void button(MatrixStack pose, int x, int y, int w, int h, String text, int mx, int my, boolean enabled) {
        boolean hover = enabled && in(mx, my, x, y, w, h);
        fill(pose, x, y, x + w, y + h, !enabled ? 0xFF334854 : hover ? 0xFF247D80 : 0xFF1D626D);
        drawCenteredString(pose, font, text, x + w / 2, y + (h - 8) / 2, enabled ? 0xFFEAF8F4 : 0xFF84999F);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        if (in(mouseX, mouseY, width - 196, 15, 58, 16)) { ShopClientState.openCreate(); return true; }
        if (in(mouseX, mouseY, width - 132, 15, 58, 16)) { ShopClientState.openManager(); return true; }
        if (in(mouseX, mouseY, width - 66, 15, 46, 16)) { onClose(); return true; }
        ShopData.Snapshot state = ShopClientState.snapshot;
        if (state.shops != null && state.shops.size() > 4) {
            if (in(mouseX, mouseY, 18, height - 28, 34, 16) && shopOffset > 0) { shopOffset = Math.max(0, shopOffset - 4); return true; }
            if (in(mouseX, mouseY, 108, height - 28, 34, 16) && shopOffset + 4 < state.shops.size()) { shopOffset += 4; return true; }
        }
        if (state.shops != null) for (int i = shopOffset; i < Math.min(state.shops.size(), shopOffset + 4); i++) {
            int y = 56 + (i - shopOffset) * 34;
            if (in(mouseX, mouseY, 18, y, 132, 29)) { ShopClientState.send("detail", "shop", state.shops.get(i).id); return true; }
        }
        ShopData.Shop selected = state.selected;
        if (selected != null && selected.offers != null) {
            int count = selected.offers.size();
            int pageSize = Math.max(1, (height - 116) / 48);
            offerOffset = Math.max(0, Math.min(offerOffset, Math.max(0, count - pageSize)));
            if (count > pageSize) {
                if (in(mouseX, mouseY, 168, height - 28, 30, 16) && offerOffset > 0) { offerOffset = Math.max(0, offerOffset - pageSize); return true; }
                if (in(mouseX, mouseY, width - 54, height - 28, 30, 16) && offerOffset + pageSize < count) { offerOffset += pageSize; return true; }
            }
            for (int i = offerOffset; i < Math.min(count, offerOffset + pageSize); i++) {
                int y = 84 + (i - offerOffset) * 48;
                if (in(mouseX, mouseY, width - 68, y + 24, 44, 17)) {
                    JsonObject request = ShopClientState.action("purchase");
                    request.addProperty("shop", selected.id); request.addProperty("row", i);
                    ShopClientState.send(request); return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean in(double x, double y, int left, int top, int w, int h) { return x >= left && x < left + w && y >= top && y < top + h; }
    private String cut(String value, int max) { return value == null || value.length() <= max ? value : value.substring(0, max - 1) + "…"; }
    @Override public boolean isPauseScreen() { return false; }
}
