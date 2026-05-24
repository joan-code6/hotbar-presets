package de.joancode.invSorter.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class HotbarSettingsScreen extends Screen {
    public HotbarSettingsScreen() {
        super(Text.translatable("screen.invsorter.settings"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int buttonWidth = 220;
        int buttonHeight = 20;
        int spacing = 8;
        int y = centerY - (buttonHeight + spacing);

        this.addDrawableChild(ButtonWidget.builder(getFullInventorySortButtonText(), btn -> {
            boolean enabled = !InvSorterSettings.isFullInventorySortModeEnabled();
            InvSorterSettings.setFullInventorySortModeEnabled(enabled);
            btn.setMessage(getFullInventorySortButtonText());
        }).dimensions(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), btn -> {
            this.client.setScreen(new HotbarMainMenuScreen());
        }).dimensions(centerX - buttonWidth / 2, y + buttonHeight + spacing, buttonWidth, buttonHeight).build());
    }

    private Text getFullInventorySortButtonText() {
        return Text.translatable(
                "screen.invsorter.mode.full_inventory",
                Text.translatable(InvSorterSettings.isFullInventorySortModeEnabled()
                        ? "screen.invsorter.mode.enabled"
                        : "screen.invsorter.mode.disabled")
        );
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawText(
                this.textRenderer,
                this.title,
                (this.width - this.textRenderer.getWidth(this.title)) / 2,
                40,
                0xFFFFFF,
                false
        );
    }
}
