package de.joancode.invSorter.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

public class HotbarMainMenuScreen extends Screen {
    public HotbarMainMenuScreen() {
        super(Text.translatable("screen.invsorter.main_menu"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int buttonWidth = 180;
        int buttonHeight = 20;
        int spacing = 8;
        int y = centerY - (buttonHeight * 2 + spacing * 2);

        // Button: Neue Konfiguration erstellen
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.invsorter.create_config"), btn -> {
            this.client.setScreen(new HotbarEditorScreen());
        }).dimensions(centerX - buttonWidth / 2, y, buttonWidth, buttonHeight).build());

        // Button: Bestehende Konfiguration verwalten
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.invsorter.manage_configs"), btn -> {
            this.client.setScreen(new HotbarConfigListScreen());
        }).dimensions(centerX - buttonWidth / 2, y + buttonHeight + spacing, buttonWidth, buttonHeight).build());

        // Button: Einstellungen
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("screen.invsorter.open_settings"), btn -> {
            this.client.setScreen(new HotbarSettingsScreen());
        }).dimensions(centerX - buttonWidth / 2, y + 2 * (buttonHeight + spacing), buttonWidth, buttonHeight).build());

        // Button: Schließen
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), btn -> {
            this.close();
        }).dimensions(centerX - buttonWidth / 2, y + 3 * (buttonHeight + spacing), buttonWidth, buttonHeight).build());
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (this.title != null && this.textRenderer != null) {
            String titleString = this.title.getString();
            int titleWidth = 0;
            try {
                titleWidth = this.textRenderer.getWidth(titleString);
            } catch (Exception e) {
                // Fehler beim Berechnen der Breite ignorieren
            }
            int x = (this.width - titleWidth) / 2;
            context.drawText(
                this.textRenderer,
                this.title,
                x,
                40,
                0xFFFFFF,
                false
            );
        }
    }
}
