package com.sysshop.core;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;

public final class CreateShopScreen extends Screen {
    private EditBox nameField;
    private EditBox ownerField;

    public CreateShopScreen() { super(new TextComponent("创建商店")); }

    @Override
    protected void init() {
        int left = width / 2 - 112;
        nameField = new EditBox(font, left + 12, 81, 200, 22, new TextComponent("店铺名称"));
        nameField.setMaxLength(ShopData.MAX_NAME_LENGTH);
        addRenderableWidget(nameField);
        ownerField = new EditBox(font, left + 12, 121, 200, 22, new TextComponent("店主显示名称"));
        ownerField.setMaxLength(ShopData.MAX_NAME_LENGTH);
        ownerField.setValue(Minecraft.getInstance().player == null ? "" : Minecraft.getInstance().player.getName().getString());
        addRenderableWidget(ownerField);
        addRenderableWidget(new Button(left + 12, 158, 96, 22, new TextComponent("创建商店"), button -> submit()));
        addRenderableWidget(new Button(left + 116, 158, 96, 22, new TextComponent("返回"), button -> ShopClientState.openBrowse()));
    }

    private void submit() {
        JsonObject request = ShopClientState.action("create");
        request.addProperty("name", nameField.getValue());
        request.addProperty("ownerName", ownerField.getValue());
        ShopClientState.send(request);
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        renderBackground(pose);
        fill(pose, 0, 0, width, height, 0xB9081723);
        int left = width / 2 - 124;
        fill(pose, left, 42, left + 248, 192, 0xFF143D50);
        fill(pose, left, 42, left + 248, 45, 0xFF45D0B1);
        drawCenteredString(pose, font, title, width / 2, 54, 0xFFF2F7F7);
        drawString(pose, font, "商店名称", left + 12, 66, 0xFF9DDBD1);
        drawString(pose, font, "店主显示名称（仅管理员可自定义）", left + 12, 106, 0xFF9DDBD1);
        drawCenteredString(pose, font, "普通玩家的店主名称由服务器账号确定。", width / 2, 186, 0xFF9CB6BF);
        super.render(pose, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() { return false; }
}
