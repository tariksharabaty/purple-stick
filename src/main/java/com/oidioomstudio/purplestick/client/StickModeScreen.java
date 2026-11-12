package com.oidioomstudio.purplestick.client;

import com.oidioomstudio.purplestick.item.StickItem;
import com.oidioomstudio.purplestick.item.StickLock;
import com.oidioomstudio.purplestick.item.StickMode;
import com.oidioomstudio.purplestick.net.ChangeStickModePayload;
import com.oidioomstudio.olib.gui.theme.OTheme;
import com.oidioomstudio.olib.gui.widgets.StoneButtonWidget;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class StickModeScreen extends Screen {
    private final boolean isMainHand;
    private final ItemStack stickStack;
    private StickMode currentMode;
    private StickLock currentLock;

    private StoneButtonWidget modeButton;
    private StoneButtonWidget lockButton;

    public StickModeScreen(boolean isMainHand, ItemStack stickStack) {
        super(Component.translatable("purplestick.screen.title"));
        this.isMainHand = isMainHand;
        this.stickStack = stickStack;
        this.currentMode = StickItem.getMode(stickStack);
        StickLock initialLock = StickItem.getLock(stickStack);
        int limit = (stickStack.getItem() instanceof StickItem si) ? si.getEffectiveLimit(currentMode) : 64;
        if (initialLock.getRequiredBlocks() > limit) {
            this.currentLock = StickLock.NONE;
        } else {
            this.currentLock = initialLock;
        }
    }

    private List<StickLock> getAvailableLocks() {
        int limit = (stickStack.getItem() instanceof StickItem si) ? si.getEffectiveLimit(currentMode) : 64;
        List<StickLock> list = new ArrayList<>();
        for (StickLock lock : StickLock.values()) {
            if (lock.getRequiredBlocks() <= limit) {
                list.add(lock);
            }
        }
        if (list.isEmpty()) {
            list.add(StickLock.NONE);
        }
        return list;
    }

    @Override
    protected void init() {
        Component modeText = getModeButtonText();
        Component lockText = getLockButtonText();
        Component doneText = Component.translatable("purplestick.gui.done");

        int maxTextWidth = Math.max(160, this.font.width(modeText));
        maxTextWidth = Math.max(maxTextWidth, this.font.width(lockText));
        maxTextWidth = Math.max(maxTextWidth, this.font.width(doneText));

        int buttonWidth = Math.min(this.width - 32, maxTextWidth + 30);
        int panelWidth = Math.max(240, buttonWidth + 40);
        int panelHeight = 175;
        int x = (this.width - panelWidth) / 2;
        int y = (this.height - panelHeight) / 2;

        int buttonHeight = 22;
        int startX = x + (panelWidth - buttonWidth) / 2;

        // Button 1: Mode toggle
        this.modeButton = new StoneButtonWidget(startX, y + 46, buttonWidth, buttonHeight, modeText, () -> {
            currentMode = (currentMode == StickMode.BUILD) ? StickMode.DESTRUCTION : StickMode.BUILD;
            List<StickLock> available = getAvailableLocks();
            if (!available.contains(currentLock)) {
                currentLock = StickLock.NONE;
            }
            this.modeButton.setMessage(getModeButtonText());
            this.lockButton.setMessage(getLockButtonText());
            saveChanges();
        });
        this.addRenderableWidget(modeButton);

        // Button 2: Lock restriction toggle
        this.lockButton = new StoneButtonWidget(startX, y + 76, buttonWidth, buttonHeight, lockText, () -> {
            List<StickLock> available = getAvailableLocks();
            int idx = available.indexOf(currentLock);
            int next = (idx + 1) % available.size();
            currentLock = available.get(next);
            this.lockButton.setMessage(getLockButtonText());
            saveChanges();
        });
        this.addRenderableWidget(lockButton);

        // Button 3: Done / Close
        StoneButtonWidget doneButton = new StoneButtonWidget(startX, y + 134, buttonWidth, buttonHeight, doneText, this::onClose);
        this.addRenderableWidget(doneButton);
    }

    private Component getModeButtonText() {
        return Component.translatable("purplestick.screen.mode_format", currentMode.getName());
    }

    private Component getLockButtonText() {
        return Component.translatable("purplestick.screen.lock_format", currentLock.getName());
    }

    private void saveChanges() {
        StickItem.setMode(stickStack, currentMode);
        StickItem.setLock(stickStack, currentLock);
        ClientPlayNetworking.send(new ChangeStickModePayload(isMainHand, currentMode.getId(), currentLock.getId()));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTicks) {
        // Draw background darkening
        extractor.fill(0, 0, this.width, this.height, 0x90000000);

        int panelWidth = 240;
        int panelHeight = 175;
        int x = (this.width - panelWidth) / 2;
        int y = (this.height - panelHeight) / 2;

        // Draw stone themed panel background
        extractor.fill(x, y, x + panelWidth, y + panelHeight, 0xEE1A1A20);
        OTheme.drawBorder(extractor, x, y, panelWidth, panelHeight, 0xFF555565);
        OTheme.drawBorder(extractor, x + 1, y + 1, panelWidth - 2, panelHeight - 2, 0xFF252530);

        Font font = this.font;
        // Panel Header (Item Name)
        Component headerName = this.stickStack.isEmpty() ? this.title : this.stickStack.getHoverName();
        extractor.centeredText(font, headerName, x + panelWidth / 2, y + 14, 0xFFE0C0FF);

        // Limit indicator
        int limit = (stickStack.getItem() instanceof StickItem si) ? si.getEffectiveLimit(currentMode) : 64;
        extractor.centeredText(font, Component.translatable("purplestick.screen.limit_label", limit), x + panelWidth / 2, y + 110, 0xFF88AA88);

        super.extractRenderState(extractor, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
