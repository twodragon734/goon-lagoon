package com.example.magicwand;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ConsumableComponents;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.function.Function;

public class MagicWandMod implements ModInitializer {
    public static final String MOD_ID = "magicwand";

    public static Item MAGIC_WAND;
    public static Item LIGHTNING_WAND;
    public static Item ICE_WAND;
    public static Item WIND_WAND;
    public static Item HEAL_WAND;
    public static Item NUKE_WAND;
    public static Item SPEEDSTER_SCROLL;
    public static Item DRILL_SCROLL;
    public static Item MANA_POTION;
    public static Item SPELL_BOOK;
    public static Item MANA_CHARM;
    public static Item MANA_RING;
    public static Item MANA_CROWN;
    public static Item MANA_ROBE;
    public static Item SPLASH_MANA_POTION;

    private static Item register(String name, Function<Item.Settings, Item> factory) {
        return register(name, Rarity.EPIC, factory);
    }

    private static Item register(String name, Rarity rarity, Function<Item.Settings, Item> factory) {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, name));
        Item.Settings settings = new Item.Settings()
                .registryKey(key)
                .maxCount(1)
                .rarity(rarity)
                .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return Registry.register(Registries.ITEM, key, factory.apply(settings));
    }

    @Override
    public void onInitialize() {
        MAGIC_WAND = register("magic_wand", MagicWandItem::new);
        LIGHTNING_WAND = register("lightning_wand", s -> new ElementWandItem(s, ElementWandItem.Kind.LIGHTNING));
        ICE_WAND = register("ice_wand", s -> new ElementWandItem(s, ElementWandItem.Kind.ICE));
        WIND_WAND = register("wind_wand", s -> new ElementWandItem(s, ElementWandItem.Kind.WIND));
        HEAL_WAND = register("heal_wand", s -> new ElementWandItem(s, ElementWandItem.Kind.HEAL));

        NUKE_WAND = register("nuke_wand", NukeWandItem::new); // no recipe: creative inventory only
        SPEEDSTER_SCROLL = register("speedster_scroll", Rarity.RARE, s -> new ScrollItem(s, ScrollItem.Kind.SPEED));
        DRILL_SCROLL = register("drill_scroll", Rarity.RARE, s -> new ScrollItem(s, ScrollItem.Kind.DRILL));

        MANA_POTION = register("mana_potion", Rarity.UNCOMMON, s -> new ManaPotionItem(
                s.maxCount(16)
                        .component(DataComponentTypes.CONSUMABLE, ConsumableComponents.drink().build())
                        .useRemainder(Items.GLASS_BOTTLE),
                50));
        SPLASH_MANA_POTION = register("splash_mana_potion", Rarity.UNCOMMON,
                s -> new SplashManaPotionItem(s.maxCount(16)));

        SPELL_BOOK = register("spell_book", Rarity.EPIC, SpellBookItem::new);
        MANA_CHARM = register("mana_charm", Rarity.RARE, Item::new);
        MANA_RING = register("mana_ring", Rarity.RARE, Item::new);
        MANA_CROWN = register("mana_crown", Rarity.RARE, s -> new Item(
                s.component(DataComponentTypes.EQUIPPABLE, EquippableComponent.builder(EquipmentSlot.HEAD).build())));
        MANA_ROBE = register("mana_robe", Rarity.RARE, s -> new Item(
                s.component(DataComponentTypes.EQUIPPABLE, EquippableComponent.builder(EquipmentSlot.CHEST).build())));

        DrillManager.init();

        PayloadTypeRegistry.playS2C().register(ManaPayload.ID, ManaPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(UpgradesPayload.ID, UpgradesPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PurchasePayload.ID, PurchasePayload.CODEC);
        ManaManager.init();
        UpgradeManager.init();

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(MAGIC_WAND);
            entries.add(LIGHTNING_WAND);
            entries.add(ICE_WAND);
            entries.add(WIND_WAND);
            entries.add(HEAL_WAND);
            entries.add(NUKE_WAND);
            entries.add(SPEEDSTER_SCROLL);
            entries.add(DRILL_SCROLL);
            entries.add(SPELL_BOOK);
            entries.add(MANA_CROWN);
            entries.add(MANA_ROBE);
            entries.add(MANA_CHARM);
            entries.add(MANA_RING);
            entries.add(MANA_POTION);
            entries.add(SPLASH_MANA_POTION);
        });
    }
}
