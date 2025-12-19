package de.joancode.invSorter.client;

import net.minecraft.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class HotbarConfig {
    private String name;
    private List<ItemStack> items;
    private int keyCode; // 0 = kein Hotkey

    // No-arg constructor needed for Gson deserialization
    public HotbarConfig() {
        this.name = "";
        this.items = new ArrayList<>();
        this.keyCode = 0;
    }

    public HotbarConfig(String name, List<ItemStack> items, int keyCode) {
        this.name = name;
        this.items = normalizeItems(items);
        this.keyCode = keyCode;
    }

    // Ensure the list has exactly 9 entries (HOTBAR_SIZE)
    private List<ItemStack> normalizeItems(List<ItemStack> items) {
        final int HOTBAR_SIZE = 9;
        List<ItemStack> normalized = new ArrayList<>();
        if (items == null) {
            for (int i = 0; i < HOTBAR_SIZE; i++) normalized.add(ItemStack.EMPTY);
            return normalized;
        }
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            if (i < items.size() && items.get(i) != null) normalized.add(items.get(i));
            else normalized.add(ItemStack.EMPTY);
        }
        return normalized;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<ItemStack> getItems() { return items; }
    public void setItems(List<ItemStack> items) { this.items = normalizeItems(items); }

    public int getKeyCode() { return keyCode; }
    public void setKeyCode(int keyCode) { this.keyCode = keyCode; }
}
