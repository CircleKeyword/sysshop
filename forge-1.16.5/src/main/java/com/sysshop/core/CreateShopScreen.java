package com.sysshop.core;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

public final class CreateShopScreen extends Screen {
    private TextFieldWidget nameField;
    private TextFieldWidget ownerField;

    public CreateShopScreen() { super(new StringTextComponent("创建商店")); }

    @Override
    protected void init() {
        int left = width / 2 - 112;
        nameField = new TextFieldWidget(font, left + 12, 81, 200, 22, new StringTextComponent("店铺名称"));
        nameField.setMaxLength(ShopData.MAX_NAME_LENGTH);
        addButton(nameField);
        ownerField = new TextFieldWidget(font, left + 12, 121, 200, 22, new StringTextComponent("店主显示名称"));
        ownerField.setMaxLength(ShopData.MAX_NAME_LENGTH);
        ownerField.setValue(Minecraft.getInstance().player == null ? "" : Minecraft.getInstance().player.getName().getString());
        addButton(ownerField);
        addButton(new Button(left + 12, 158, 96, 22, new StringTextComponent("创建商店"), button -> submit()));
        addButton(new Button(left + 116, 158, 96, 22, new StringTextComponent("返回"), button -> ShopClientState.openBrowse()));
    }

    private void submit() {
        JsonObject request = ShopClientState.action("create");
        request.addProperty("name", nameField.getValue());
        request.addProperty("ownerName", ownerField.getValue());
        ShopClientState.send(request);
    }

    @Override
    public void render(MatrixStack matrix, int mouseX, int mouseY, float partialTick) {
        renderBackground(matrix);
        fill(matrix, 0, 0, width, height, 0xB9081723);
        int left = width / 2 - 124;
        fill(matrix, left, 42, left + 248, 192, 0xFF143D50);
        fill(matrix, left, 42, left + 248, 45, 0xFF45D0B1);
        drawCenteredString(matrix, font, title, width / 2, 54, 0xFFF2F7F7);
        drawString(matrix, font, "商店名称", left + 12, 66, 0xFF9DDBD1);
        drawString(matrix, font, "店主显示名称（仅管理员可自定义）", left + 12, 106, 0xFF9DDBD1);
        drawCenteredString(matrix, font, "普通玩家的店主名称由服务器账号确定。", width / 2, 186, 0xFF9CB6BF);
        if (ShopClientState.snapshot.notice != null && !ShopClientState.snapshot.notice.isEmpty())
            drawCenteredString(matrix, font, ShopClientState.snapshot.notice, width / 2, 201, 0xFFFFD178);
        super.render(matrix, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() { return false; }
}
