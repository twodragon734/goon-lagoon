package com.example.magicwand;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public class MagicWandMod implements ModInitializer {
    public static final String MOD_ID = "magicwand";

    public static Item MAGIC_WAND;

    @Override
    public void onInitialize() {
        Identifier id = Identifier.of(MOD_ID, "magic_wand");
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);

        MAGIC_WAND = Registry.register(Registries.ITEM, key,
                new MagicWandItem(new Item.Settings()
                        .registryKey(key)
                        .maxCount(1)
                        .rarity(Rarity.EPIC)
                        .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)));

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(MAGIC_WAND));
    }
}
