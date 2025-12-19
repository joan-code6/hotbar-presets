package de.joancode.invSorter.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class HotbarConfigDetailScreen extends Screen {
    private final HotbarConfig config;
    private final int configIndex;
    private boolean waitingForKey = false;
    private long lastKeyTime = 0;

    public HotbarConfigDetailScreen(HotbarConfig config, int configIndex) {
        super(Text.literal("Configuration: " + config.getName()));
        this.config = config;
        this.configIndex = configIndex;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int buttonWidth = 160;
        int buttonHeight = 20;

        // Apply Button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Apply Configuration"), btn -> {
            applyConfiguration();
            this.client.setScreen(null);
        }).dimensions(centerX - buttonWidth / 2, centerY - 40, buttonWidth, buttonHeight).build());

        // Change Hotkey Button
        String hotkeyText;
        if (waitingForKey) {
            hotkeyText = "Press any key... (ESC to cancel)";
        } else if (config.getKeyCode() == 0) {
            hotkeyText = "Set Hotkey";
        } else {
            hotkeyText = "Hotkey: " + getKeyName(config.getKeyCode()) + " (Click to change)";
        }

        this.addDrawableChild(ButtonWidget.builder(Text.literal(hotkeyText), btn -> {
            waitingForKey = true;
            lastKeyTime = System.currentTimeMillis();
            this.clearAndInit(); // Refresh the UI to show "Press any key..." message
        }).dimensions(centerX - buttonWidth / 2, centerY - 10, buttonWidth, buttonHeight).build());

        // Delete Button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Delete"), btn -> {
            deleteConfiguration();
            this.client.setScreen(new HotbarConfigListScreen());
        }).dimensions(centerX - buttonWidth / 2, centerY + 20, buttonWidth, buttonHeight).build());

        // Back Button
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), btn -> {
            this.client.setScreen(new HotbarConfigListScreen());
        }).dimensions(centerX - buttonWidth / 2, centerY + 50, buttonWidth, buttonHeight).build());
    }

    @Override
    public void tick() {
        super.tick();

        if (waitingForKey && this.client != null) {
            long window = this.client.getWindow().getHandle();
            long currentTime = System.currentTimeMillis();

            // Allow some time for the button click to finish before detecting keys
            if (currentTime - lastKeyTime < 200) {
                return;
            }

            // Check for ESC key
            if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
                waitingForKey = false;
                this.clearAndInit();
                return;
            }

            // Check for any other key
            for (int key = GLFW.GLFW_KEY_SPACE; key <= GLFW.GLFW_KEY_LAST; key++) {
                if (GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS) {
                    config.setKeyCode(key);
                    saveConfiguration();
                    waitingForKey = false;
                    this.clearAndInit();
                    return;
                }
            }

            // Check for mouse buttons
            for (int button = 0; button <= 7; button++) {
                if (GLFW.glfwGetMouseButton(window, button) == GLFW.GLFW_PRESS) {
                    config.setKeyCode(1000 + button);
                    saveConfiguration();
                    waitingForKey = false;
                    this.clearAndInit();
                    return;
                }
            }
        }
    }

    private void applyConfiguration() {
        if (this.client != null && this.client.player != null) {
            // Implementiere hier die Hotbar-Umsortierung
            for (int i = 0; i < Math.min(config.getItems().size(), 9); i++) {
                // TODO: Implementiere Hotbar-Umsortierung
            }
        }
    }

    private void deleteConfiguration() {
        List<HotbarConfig> configs = HotbarStorage.loadConfigs();
        if (configIndex >= 0 && configIndex < configs.size()) {
            configs.remove(configIndex);
            HotbarStorage.saveConfigs(configs);
        }
    }

    private void saveConfiguration() {
        List<HotbarConfig> configs = HotbarStorage.loadConfigs();
        if (configIndex >= 0 && configIndex < configs.size()) {
            configs.set(configIndex, config);
            HotbarStorage.saveConfigs(configs);
            InvSorterClient.refreshHotbarConfigs(); // Refresh hotkeys
        }
    }

    private String getKeyName(int keyCode) {
        if (keyCode == 0) return "None";
        if (keyCode >= 1000) return "Mouse" + (keyCode - 1000);
        String name = GLFW.glfwGetKeyName(keyCode, 0);
        return name != null ? name.toUpperCase() : "Key" + keyCode;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        String titleString = this.title.getString();
        context.drawText(
            this.textRenderer,
            this.title,
            (this.width - this.textRenderer.getWidth(titleString)) / 2,
            40,
            0xFFFFFF,
            false
        );

        if (waitingForKey) {
            context.drawText(
                this.textRenderer,
                Text.literal("Press a key for hotkey (ESC to cancel)"),
                this.width / 2 - 100,
                this.height / 2 - 60,
                0xFFFF00,
                false
            );
        }
    }
}
