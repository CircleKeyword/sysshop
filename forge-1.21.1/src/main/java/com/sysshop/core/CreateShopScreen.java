package com.sysshop.core;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class CreateShopScreen extends Screen {
    private EditBox nameField;
    private EditBox ownerField;

    public CreateShopScreen() { super(Component.literal("创建商店")); }

    @Override
    protected void init() {
        int left = width / 2 - 112;
        nameField = new EditBox(font, left + 12, 81, 200, 22, Component.literal("店铺名称"));
        nameField.setMaxLength(ShopData.MAX_NAME_LENGTH);
        nameField.setHint(Component.literal("给商店起个名字"));
        addRenderableWidget(nameField);
        ownerField = new EditBox(font, left + 12, 121, 200, 22, Component.literal("店主显示名称"));
        ownerField.setMaxLength(ShopData.MAX_NAME_LENGTH);
        ownerField.setValue(Minecraft.getInstance().player == null ? "" : Minecraft.getInstance().player.getName().getString());
        addRenderableWidget(ownerField);
        addRenderableWidget(Button.builder(Component.literal("创建商店"), button -> submit())
                .bounds(left + 12, 158, 96, 22).build());
        addRenderableWidget(Button.builder(Component.literal("返回"), button -> ShopClientState.openBrowse())
                .bounds(left + 116, 158, 96, 22).build());
    }

    private void submit() {
        JsonObject request = ShopClientState.action("create");
        request.addProperty("name", nameField.getValue());
        request.addProperty("ownerName", ownerField.getValue());
        ShopClientState.send(request);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(0, 0, width, height, 0xB9081723);
        int left = width / 2 - 124;
        g.fill(left, 42, left + 248, 192, 0xFF143D50);
        g.fill(left, 42, left + 248, 45, 0xFF45D0B1);
        g.drawCenteredString(font, "创建商店", width / 2, 54, 0xFFF2F7F7);
        g.drawString(font, "商店名称", left + 12, 66, 0xFF9DDBD1, false);
        g.drawString(font, "店主显示名称（仅管理员可自定义）", left + 12, 106, 0xFF9DDBD1, false);
        g.drawCenteredString(font, "普通玩家的店主名称由服务器账号确定。", width / 2, 186, 0xFF9CB6BF);
        if (ShopClientState.snapshot.notice != null && !ShopClientState.snapshot.notice.isEmpty())
            g.drawCenteredString(font, ShopClientState.snapshot.notice, width / 2, 201, 0xFFFFD178);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() { return false; }
}
