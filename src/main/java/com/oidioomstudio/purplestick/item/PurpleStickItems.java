package com.oidioomstudio.purplestick.item;

import com.oidioomstudio.purplestick.PurpleStickMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public final class PurpleStickItems {
    public static final Item WOODEN_STICK = register("wooden_stick",
            key -> new StickItem(new Item.Properties().setId(key).durability(128), 16));

    public static final Item COPPER_STICK = register("copper_stick",
            key -> new StickItem(new Item.Properties().setId(key).durability(256), 64));

    public static final Item IRON_STICK = register("iron_stick",
            key -> new StickItem(new Item.Properties().setId(key).durability(512), 256));

    public static final Item DIAMOND_STICK = register("diamond_stick",
            key -> new StickItem(new Item.Properties().setId(key).durability(2048), 512));

    public static final Item NETHERITE_STICK = register("netherite_stick",
            key -> new StickItem(new Item.Properties().setId(key).durability(4096).fireResistant(), 1024));

    private static Item register(String name, Function<ResourceKey<Item>, Item> factory) {
        Identifier id = Identifier.fromNamespaceAndPath(PurpleStickMod.MOD_ID, name);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        Item item = factory.apply(key);
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void initialize() {
        // Class loaded
    }
}
