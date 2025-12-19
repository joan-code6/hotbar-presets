package de.joancode.invSorter.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

public class HotbarConfigListScreen extends Screen {
    private final List<HotbarConfig> configs;

    public HotbarConfigListScreen() {
        super(Text.translatable("screen.invsorter.manage_configs"));
        this.configs = HotbarStorage.loadConfigs();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = 60;
        int buttonWidth = 180;
        int buttonHeight = 20;
        int smallButtonWidth = 50;
        int spacing = 4;

        // Für jede gespeicherte Konfiguration einen Button anzeigen
        for (int i = 0; i < configs.size(); i++) {
            HotbarConfig config = configs.get(i);
            final int configIndex = i;

            String buttonText = config.getName();
            if (config.getKeyCode() != 0) {
                buttonText += " [" + getKeyName(config.getKeyCode()) + "]";
            }

            // Main button to edit config
            this.addDrawableChild(ButtonWidget.builder(Text.literal(buttonText), btn -> {
                this.client.setScreen(new HotbarConfigDetailScreen(config, configIndex));
            }).dimensions(centerX - buttonWidth / 2 - smallButtonWidth - 2, y, buttonWidth, buttonHeight).build());

            // Apply button
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Apply"), btn -> {
                applyConfiguration(config);
                this.client.setScreen(null); // Close screen after applying
            }).dimensions(centerX + buttonWidth / 2 + 2, y, smallButtonWidth, buttonHeight).build());

            y += buttonHeight + spacing;
        }

        // Wenn keine Konfigurationen vorhanden sind
        if (configs.isEmpty()) {
            // Diese Nachricht wird im render() angezeigt
        }

        // Zurück-Button
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), btn -> {
            this.client.setScreen(new HotbarMainMenuScreen());
        }).dimensions(centerX - 100, this.height - 40, 200, buttonHeight).build());
    }

    private void applyConfiguration(HotbarConfig config) {
        // Trigger the configuration application through InvSorterClient
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            // We need to make the apply method accessible, so we'll use a workaround
            // by creating a temporary instance and calling the method
            InvSorterClient.applyConfigurationDirectly(config);
        }
    }

    private String getKeyName(int keyCode) {
        if (keyCode == 0) return "None";
        return org.lwjgl.glfw.GLFW.glfwGetKeyName(keyCode, 0);
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
            20,
            0xFFFFFF,
            false
        );

        // Wenn keine Konfigurationen vorhanden sind, zeige die Nachricht an
        if (configs.isEmpty()) {
            context.drawText(
                this.textRenderer,
                Text.literal("No configurations found"),
                (this.width - this.textRenderer.getWidth("No configurations found")) / 2,
                this.height / 2,
                0xFFFFFF,
                false
            );
        }
    }
}
