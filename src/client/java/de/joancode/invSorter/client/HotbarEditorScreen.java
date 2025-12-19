package de.joancode.invSorter.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class HotbarEditorScreen extends Screen {
    private final List<ItemStack> hotbarItems = new ArrayList<>();
    private TextFieldWidget nameField;
    private ButtonWidget saveButton;
    private ButtonWidget hotkeyButton;
    private int selectedHotkey = 0;
    private boolean waitingForKey = false;
    private final int HOTBAR_SIZE = 9;
    private final List<ButtonWidget> slotButtons = new ArrayList<>();

    private static final File CONFIG_DIR = new File(System.getProperty("user.home"),
            ".minecraft/config/invsorter");
    private static final File CONFIG_FILE = new File(CONFIG_DIR, "hotbars.json");
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(ItemStack.class, new ItemStackSerializer())
            .create();

    private long lastKeyTime = 0;

    // GLFW callbacks for more reliable key capture while waiting for hotkey
    private GLFWKeyCallbackI tempKeyCallback = null;
    private GLFWMouseButtonCallbackI tempMouseCallback = null;
    private boolean callbackInstalled = false;

    public HotbarEditorScreen() {
        super(Text.translatable("screen.invsorter.hotbar_editor"));
        // Initialisiere die Liste mit leeren ItemStacks
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            hotbarItems.add(ItemStack.EMPTY);
        }
        // Kopiere die aktuelle Hotbar in die UI
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.player != null) {
            for (int i = 0; i < HOTBAR_SIZE; i++) {
                hotbarItems.set(i, mc.player.getInventory().getStack(i).copy());
            }
        }
    }

    @Override
    protected void init() {
        slotButtons.clear();
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int slotSize = 22;
        int spacing = 6;
        int startX = centerX - ((HOTBAR_SIZE * (slotSize + spacing)) / 2);
        int y = centerY - 50;

        // Hotbar-Slots als Buttons mit Item-Anzeige
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            int x = startX + i * (slotSize + spacing);
            final int slotIndex = i;
            ButtonWidget btn = ButtonWidget.builder(Text.literal(""), b -> {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.player != null) {
                    // Kopiere das Item aus dem entsprechenden Hotbar-Slot des Spielers
                    ItemStack stack = mc.player.getInventory().getStack(slotIndex);
                    hotbarItems.set(slotIndex, stack.copy());
                }
            }).dimensions(x, y, slotSize, slotSize).build();
            this.addDrawableChild(btn);
            slotButtons.add(btn);
        }

        // Name-Eingabefeld
        nameField = new TextFieldWidget(this.textRenderer, centerX - 100, y + 50, 200, 20, Text.translatable("screen.invsorter.hotbar_editor.name"));
        nameField.setPlaceholder(Text.literal("Configuration Name..."));
        this.addSelectableChild(nameField);

        // Hotkey-Button (optional)
        String hotkeyText;
        if (waitingForKey) {
            hotkeyText = "Press any key... (ESC to cancel)";
        } else if (selectedHotkey == 0) {
            hotkeyText = "Set Hotkey (Optional)";
        } else {
            hotkeyText = "Hotkey: " + getHotkeyName(selectedHotkey);
        }

        hotkeyButton = ButtonWidget.builder(Text.literal(hotkeyText), btn -> {
            waitingForKey = true;
            lastKeyTime = System.currentTimeMillis();
        }).dimensions(centerX - 100, y + 80, 200, 20).build();
        this.addDrawableChild(hotkeyButton);

        // Speichern-Button
        saveButton = ButtonWidget.builder(Text.literal("Save Configuration"), btn -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                nameField.setPlaceholder(Text.literal("Please enter a name!"));
                return;
            }
            List<ItemStack> items = hotbarItems.stream().map(ItemStack::copy).collect(Collectors.toList());
            HotbarConfig config = new HotbarConfig(name, items, selectedHotkey);
            // Lade bestehende Konfigurationen
            List<HotbarConfig> configs = HotbarStorage.loadConfigs();
            // Überschreibe, falls Name schon existiert
            configs.removeIf(c -> c.getName().equalsIgnoreCase(name));
            configs.add(config);
            HotbarStorage.saveConfigs(configs);
            InvSorterClient.refreshHotbarConfigs(); // Refresh hotkeys
            this.client.setScreen(new HotbarMainMenuScreen());
        }).dimensions(centerX - 100, y + 110, 200, 20).build();
        this.addDrawableChild(saveButton);

        // Cancel button
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), btn -> {
            this.client.setScreen(new HotbarMainMenuScreen());
        }).dimensions(centerX - 100, y + 140, 200, 20).build());

        super.init();
    }

    private String getHotkeyName(int keyCode) {
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
        
        // Title
        context.drawCenteredTextWithShadow(
            this.textRenderer,
            this.title,
            this.width / 2,
            20,
            0xFFFFFF
        );
        
        // Instructions
        String instruction = "Click slots to sync with your current hotbar";
        context.drawCenteredTextWithShadow(
            this.textRenderer,
            Text.literal(instruction),
            this.width / 2,
            this.height / 2 - 70,
            0xAAAAAA
        );
        
        // Item-Icons auf den Buttons rendern
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            ItemStack stack = hotbarItems.get(i);
            ButtonWidget btn = slotButtons.get(i);
            if (!stack.isEmpty()) {
                context.drawItem(stack, btn.getX() + 3, btn.getY() + 3);
            }
        }
        
        if (nameField != null) nameField.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void tick() {
        super.tick();

        if (waitingForKey && this.client != null) {
            long window = this.client.getWindow().getHandle();
            long currentTime = System.currentTimeMillis();

            // Allow some time for the button click to finish before detecting keys
            if (currentTime - lastKeyTime < 100) {
                // Ensure the UI has a tick to update and callback gets installed reliably
                installTemporaryCallbacks(window);
                return;
            }

            // If callbacks are not installed yet, install them now
            installTemporaryCallbacks(window);
        } else {
            // Ensure callbacks are removed if we are not waiting
            if (callbackInstalled) {
                removeTemporaryCallbacks();
            }
        }
    }

    private void installTemporaryCallbacks(long window) {
        if (callbackInstalled) return;
        callbackInstalled = true;

        // Store previous callbacks so we can restore them
        final GLFWKeyCallbackI previousKey = org.lwjgl.glfw.GLFW.glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (action == GLFW.GLFW_PRESS) {
                final int capturedKey = key;
                MinecraftClient.getInstance().execute(() -> {
                    selectedHotkey = capturedKey;
                    waitingForKey = false;
                    removeTemporaryCallbacks();
                    this.clearAndInit();
                });
            }
        });

        final GLFWMouseButtonCallbackI previousMouse = org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (action == GLFW.GLFW_PRESS) {
                final int captured = 1000 + button;
                MinecraftClient.getInstance().execute(() -> {
                    selectedHotkey = captured;
                    waitingForKey = false;
                    removeTemporaryCallbacks();
                    this.clearAndInit();
                });
            }
        });

        // Keep references so removeTemporaryCallbacks can restore
        this.tempKeyCallback = previousKey;
        this.tempMouseCallback = previousMouse;
    }

    private void removeTemporaryCallbacks() {
        if (!callbackInstalled) return;
        callbackInstalled = false;
        try {
            long window = this.client.getWindow().getHandle();
            // Restore previous callbacks (if any) by setting null — LWJGL returns previous callback, restoring by setting null is acceptable
            org.lwjgl.glfw.GLFW.glfwSetKeyCallback(window, tempKeyCallback);
            org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback(window, tempMouseCallback);
        } catch (Exception e) {
            // Ignore restore failures
        } finally {
            tempKeyCallback = null;
            tempMouseCallback = null;
        }
    }

    private int getHotkeyKeyCode(int index) {
        return GLFW.GLFW_KEY_F1 + index;
    }
}
