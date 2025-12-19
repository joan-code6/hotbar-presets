package de.joancode.invSorter.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.KeyBinding.Category;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class InvSorterClient implements ClientModInitializer {
    private static KeyBinding openHotbarEditorKeyBinding;
    private static List<HotbarConfig> registeredConfigs = new ArrayList<>();
    private static boolean isSwapping = false; // Flag to prevent multiple simultaneous operations

    @Override
    public void onInitializeClient() {
        Category invSorterCategory = Category.create(Identifier.of("hotbarpresets", "category.hotbarpresets"));
        openHotbarEditorKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hotbarpresets.open_hotbar_editor",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                invSorterCategory
        ));

        // Register client tick event to listen for key press
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openHotbarEditorKeyBinding.wasPressed()) {
                MinecraftClient.getInstance().setScreen(new HotbarMainMenuScreen());
            }

            // Check for hotbar configuration hotkeys
            checkHotbarHotkeys(client);
        });

        // Load saved configurations at startup
        loadHotbarConfigs();
    }

    private void checkHotbarHotkeys(MinecraftClient client) {
        if (client == null || client.player == null || registeredConfigs == null || registeredConfigs.isEmpty()) {
            return; // Safety checks
        }

        try {
            long window = client.getWindow().getHandle();

            for (HotbarConfig config : registeredConfigs) {
                if (config == null || config.getKeyCode() == 0) continue; // No hotkey assigned

                boolean isPressed = false;

                if (config.getKeyCode() >= 1000) {
                    // Mouse button
                    int mouseButton = config.getKeyCode() - 1000;
                    isPressed = GLFW.glfwGetMouseButton(window, mouseButton) == GLFW.GLFW_PRESS;
                } else {
                    // Keyboard key
                    isPressed = GLFW.glfwGetKey(window, config.getKeyCode()) == GLFW.GLFW_PRESS;
                }

                if (isPressed) {
                    applyHotbarConfiguration(config, client);
                    // Small delay to prevent multiple triggers
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // Ignore interruption
                    }
                    break; // Only apply one configuration per tick
                }
            }
        } catch (Exception e) {
            // Log error but don't crash
            System.err.println("Error checking hotbar hotkeys: " + e.getMessage());
        }
    }

    private void applyHotbarConfiguration(HotbarConfig config, MinecraftClient client) {
        if (client.player == null) return;

        // Find matching items in inventory and rearrange hotbar
        List<ItemStack> targetItems = config.getItems();
        if (targetItems == null) return; // Safety check

        // Build a mapping of what should go where
        boolean[] slotProcessed = new boolean[36]; // Track which slots we've already planned to move
        List<SwapOperation> operations = new ArrayList<>();

        for (int targetSlot = 0; targetSlot < Math.min(targetItems.size(), 9); targetSlot++) {
            ItemStack targetItem = targetItems.get(targetSlot);
            if (targetItem == null || targetItem.isEmpty()) continue;

            // Check if the correct item is already in the target slot
            ItemStack currentItemInSlot = client.player.getInventory().getStack(targetSlot);
            if (areItemStacksEqual(currentItemInSlot, targetItem)) {
                System.out.println("[HotbarPresets] Slot " + targetSlot + " already has correct item, skipping");
                slotProcessed[targetSlot] = true;
                continue; // Already correct, skip
            }

            // Find this item in the player's inventory (but not in already processed slots)
            int sourceSlot = findItemInInventoryExcluding(client.player, targetItem, slotProcessed);
            if (sourceSlot != -1) {
                operations.add(new SwapOperation(sourceSlot, targetSlot));
                slotProcessed[sourceSlot] = true;
                slotProcessed[targetSlot] = true;
            }
        }

        if (operations.isEmpty()) {
            System.out.println("[HotbarPresets] Hotbar already matches configuration!");
            return;
        }

        // Execute operations sequentially with delays
        executeSwapOperations(operations, client);
    }

    private int findItemInInventoryExcluding(ClientPlayerEntity player, ItemStack targetItem, boolean[] exclude) {
        // Check hotbar first (slots 0-8)
        for (int i = 0; i < 9; i++) {
            if (exclude[i]) continue;
            ItemStack stack = player.getInventory().getStack(i);
            if (areItemStacksEqual(stack, targetItem)) {
                return i;
            }
        }

        // Check main inventory (slots 9-35)
        for (int i = 9; i < 36; i++) {
            if (exclude[i]) continue;
            ItemStack stack = player.getInventory().getStack(i);
            if (areItemStacksEqual(stack, targetItem)) {
                return i;
            }
        }

        return -1; // Item not found
    }

    private static class SwapOperation {
        final int sourceSlot;
        final int targetSlot;

        SwapOperation(int sourceSlot, int targetSlot) {
            this.sourceSlot = sourceSlot;
            this.targetSlot = targetSlot;
        }
    }

    private void executeSwapOperations(List<SwapOperation> operations, MinecraftClient client) {
        if (operations.isEmpty()) return;

        // Prevent multiple operations from running simultaneously
        if (isSwapping) {
            System.out.println("[HotbarPresets] Already swapping items, ignoring request");
            return;
        }

        isSwapping = true;

        new Thread(() -> {
            try {
                for (int i = 0; i < operations.size(); i++) {
                    SwapOperation op = operations.get(i);
                    try {
                        // Execute on main thread
                        final int opIndex = i;
                        client.execute(() -> {
                            if (client.player != null) {
                                System.out.println("[InvSorter] Swapping slot " + op.sourceSlot + " -> " + op.targetSlot);
                                swapItems(client.player, op.sourceSlot, op.targetSlot);
                            }
                        });
                        // Wait between operations to allow server sync (increased to 100ms for better reliability)
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        System.err.println("[InvSorter] Swap operations interrupted");
                        break;
                    } catch (Exception e) {
                        System.err.println("[InvSorter] Error during swap operation: " + e.getMessage());
                    }
                }
            } finally {
                // Always reset the flag when done
                isSwapping = false;
                System.out.println("[HotbarPresets] Swap operations completed");
            }
        }).start();
    }

    private int findItemInInventory(ClientPlayerEntity player, ItemStack targetItem) {
        // Check hotbar first (slots 0-8)
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (areItemStacksEqual(stack, targetItem)) {
                return i;
            }
        }

        // Check main inventory (slots 9-35)
        for (int i = 9; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (areItemStacksEqual(stack, targetItem)) {
                return i;
            }
        }

        return -1; // Item not found
    }

    // Helper method for item comparison that works across different Minecraft versions
    private boolean areItemStacksEqual(ItemStack stack1, ItemStack stack2) {
        if (stack1.isEmpty() || stack2.isEmpty()) {
            return false;
        }
        return stack1.getItem() == stack2.getItem();
    }

    private void swapItems(ClientPlayerEntity player, int sourceSlot, int targetSlot) {
        if (sourceSlot == targetSlot) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.interactionManager == null || player.currentScreenHandler == null) return;

        int syncId = player.currentScreenHandler.syncId;

        // Convert inventory slot indices to screen handler slot indices
        // In player inventory screen handler:
        // Slots 0-8 are hotbar
        // Slots 9-35 are main inventory (3 rows of 9)
        // But when accessing through player.getInventory():
        // Slots 0-8 are hotbar
        // Slots 9-35 are main inventory
        // So the mapping is actually the same for player inventory!

        int screenSourceSlot = convertToScreenSlot(sourceSlot);
        int screenTargetSlot = convertToScreenSlot(targetSlot);

        // Use the SWAP action for hotbar swaps when target is in hotbar
        if (targetSlot >= 0 && targetSlot <= 8 && sourceSlot >= 9 && sourceSlot <= 35) {
            // SWAP action: swap main inventory slot with hotbar slot
            // The button parameter is the hotbar slot (0-8)
            client.interactionManager.clickSlot(syncId, screenSourceSlot, targetSlot, net.minecraft.screen.slot.SlotActionType.SWAP, player);
        } else if (sourceSlot >= 0 && sourceSlot <= 8 && targetSlot >= 9 && targetSlot <= 35) {
            // Reverse: source is in hotbar, target is in main inventory
            client.interactionManager.clickSlot(syncId, screenTargetSlot, sourceSlot, net.minecraft.screen.slot.SlotActionType.SWAP, player);
        } else if (sourceSlot >= 0 && sourceSlot <= 35 && targetSlot >= 0 && targetSlot <= 35) {
            // Both in player inventory - use pickup/swap method with delays
            client.interactionManager.clickSlot(syncId, screenSourceSlot, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, player);
            client.interactionManager.clickSlot(syncId, screenTargetSlot, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, player);
            client.interactionManager.clickSlot(syncId, screenSourceSlot, 0, net.minecraft.screen.slot.SlotActionType.PICKUP, player);
        }
    }

    private int convertToScreenSlot(int inventorySlot) {
        // For the player's own inventory screen handler, the slot mapping is direct
        // Hotbar: inventory 0-8 = screen 0-8
        // Main inventory: inventory 9-35 = screen 9-35
        return inventorySlot;
    }

    private void loadHotbarConfigs() {
        registeredConfigs = HotbarStorage.loadConfigs();
    }

    // Method to refresh configs when they're updated
    public static void refreshHotbarConfigs() {
        registeredConfigs = HotbarStorage.loadConfigs();
    }

    // Public method to apply a configuration directly (e.g., from UI button)
    public static void applyConfigurationDirectly(HotbarConfig config) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || config == null) return;

        // Prevent multiple operations from running simultaneously
        if (isSwapping) {
            System.out.println("[HotbarPresets] Already swapping items, ignoring button click");
            return;
        }

        List<ItemStack> targetItems = config.getItems();
        if (targetItems == null) return;

        // Build a mapping of what should go where
        boolean[] slotProcessed = new boolean[36];
        List<SwapOperation> operations = new ArrayList<>();

        for (int targetSlot = 0; targetSlot < Math.min(targetItems.size(), 9); targetSlot++) {
            ItemStack targetItem = targetItems.get(targetSlot);
            if (targetItem == null || targetItem.isEmpty()) continue;

            // Check if the correct item is already in the target slot
            ItemStack currentItemInSlot = client.player.getInventory().getStack(targetSlot);
            if (areItemStacksEqualStatic(currentItemInSlot, targetItem)) {
                System.out.println("[HotbarPresets] Slot " + targetSlot + " already has correct item, skipping");
                slotProcessed[targetSlot] = true;
                continue;
            }

            int sourceSlot = findItemInInventoryStaticExcluding(client.player, targetItem, slotProcessed);
            if (sourceSlot != -1) {
                operations.add(new SwapOperation(sourceSlot, targetSlot));
                slotProcessed[sourceSlot] = true;
                slotProcessed[targetSlot] = true;
            }
        }

        if (operations.isEmpty()) {
            System.out.println("[HotbarPresets] Hotbar already matches configuration!");
            return;
        }

        new InvSorterClient().executeSwapOperations(operations, client);
    }

    private static int findItemInInventoryStaticExcluding(ClientPlayerEntity player, ItemStack targetItem, boolean[] exclude) {
        for (int i = 0; i < 9; i++) {
            if (exclude[i]) continue;
            ItemStack stack = player.getInventory().getStack(i);
            if (areItemStacksEqualStatic(stack, targetItem)) {
                return i;
            }
        }
        for (int i = 9; i < 36; i++) {
            if (exclude[i]) continue;
            ItemStack stack = player.getInventory().getStack(i);
            if (areItemStacksEqualStatic(stack, targetItem)) {
                return i;
            }
        }
        return -1;
    }

    private static int findItemInInventoryStatic(ClientPlayerEntity player, ItemStack targetItem) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (areItemStacksEqualStatic(stack, targetItem)) {
                return i;
            }
        }
        for (int i = 9; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (areItemStacksEqualStatic(stack, targetItem)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean areItemStacksEqualStatic(ItemStack stack1, ItemStack stack2) {
        if (stack1.isEmpty() || stack2.isEmpty()) {
            return false;
        }
        return stack1.getItem() == stack2.getItem();
    }
}
