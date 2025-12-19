package de.joancode.invSorter.client;

import com.google.gson.*;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.lang.reflect.Type;

public class ItemStackSerializer implements JsonSerializer<ItemStack>, JsonDeserializer<ItemStack> {

    @Override
    public JsonElement serialize(ItemStack itemStack, Type type, JsonSerializationContext context) {
        if (itemStack.isEmpty()) {
            return JsonNull.INSTANCE;
        }

        JsonObject json = new JsonObject();
        json.addProperty("item", Registries.ITEM.getId(itemStack.getItem()).toString());
        json.addProperty("count", itemStack.getCount());

        // NBT wird vorläufig nicht unterstützt - kann später hinzugefügt werden

        return json;
    }

    @Override
    public ItemStack deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        if (jsonElement.isJsonNull() || !jsonElement.isJsonObject()) {
            return ItemStack.EMPTY;
        }

        JsonObject json = jsonElement.getAsJsonObject();

        try {
            if (!json.has("item") || !json.has("count")) {
                return ItemStack.EMPTY; // Missing required fields
            }

            String itemIdString = json.get("item").getAsString();
            if (itemIdString == null || itemIdString.isEmpty()) {
                return ItemStack.EMPTY; // Invalid item ID
            }

            Identifier itemId = Identifier.of(itemIdString);
            int count = json.get("count").getAsInt();

            if (count <= 0) {
                return ItemStack.EMPTY; // Invalid count
            }

            return new ItemStack(Registries.ITEM.get(itemId), count);
        } catch (Exception e) {
            // Log error and return empty stack instead of crashing
            System.err.println("Error deserializing ItemStack: " + e.getMessage());
            return ItemStack.EMPTY;
        }
    }
}
