package com.example.magicwand;

import java.util.ArrayList;
import java.util.List;

/** The list of every upgrade in the tree. Costs are in XP levels, one entry per upgrade level. */
public final class Upgrades {
    private Upgrades() {}

    public enum Path {
        MANA("Mana"),
        MAGIC_WAND("Magic Wand"),
        LIGHTNING("Lightning"),
        ICE("Ice"),
        WIND("Wind"),
        HEAL("Heal"),
        SPEEDSTER("Speedster"),
        DRILL("Drill"),
        GROWTH("Growth"),
        RECALL("Recall"),
        CLONE("Clone"),
        BRIDGE("Bridge"),
        NUKE("Nuke"); // only shown while holding the Nuke Wand

        public final String label;

        Path(String label) {
            this.label = label;
        }
    }

    public record Upgrade(String id, Path path, String name, String description, String requires, int[] costs) {
        public int maxLevel() {
            return costs.length;
        }
    }

    private static Upgrade u(String id, Path path, String name, String desc, String requires, int... costs) {
        return new Upgrade(id, path, name, desc, requires, costs);
    }

    // ---- Edit costs and text here ----
    public static final List<Upgrade> ALL = List.of(
            // MANA
            u("mana_capacity", Path.MANA, "Mana Capacity", "+20 maximum mana per level.", null, 3, 4, 6, 8, 10),
            u("mana_regen", Path.MANA, "Mana Regeneration", "+1 mana per second per level.", null, 3, 5, 7, 9, 12),
            u("mana_efficiency", Path.MANA, "Mana Efficiency", "All spells cost 10% less mana per level.", null, 5, 8, 12),

            // MAGIC WAND
            u("teleport", Path.MAGIC_WAND, "Teleport", "Unlocks sneak + right-click teleport.", null, 5),
            u("blink_range", Path.MAGIC_WAND, "Long Blink", "+4 blocks of teleport range per level.", "teleport", 4, 6, 8),
            u("quick_fire", Path.MAGIC_WAND, "Quick Fire", "Fireballs recharge faster each level.", null, 3, 5, 7),

            // LIGHTNING
            u("lightning_strike", Path.LIGHTNING, "Lightning Strike", "Unlocks the Lightning Wand.", null, 6),
            u("storm_caller", Path.LIGHTNING, "Storm Caller", "+1 extra bolt near your target per level.", "lightning_strike", 8, 12),
            u("long_reach", Path.LIGHTNING, "Long Reach", "+20 blocks of range per level.", "lightning_strike", 4, 6, 8),

            // ICE
            u("frost_bolt", Path.ICE, "Frost Bolt", "Unlocks the Ice Wand.", null, 5),
            u("deep_freeze", Path.ICE, "Deep Freeze", "Targets stay frozen 3 seconds longer per level.", "frost_bolt", 3, 5, 7),
            u("blizzard", Path.ICE, "Blizzard", "+1 block of freezing radius per level.", "frost_bolt", 4, 7),

            // WIND
            u("gust", Path.WIND, "Gust", "Unlocks the Wind Wand launch.", null, 5),
            u("tempest", Path.WIND, "Tempest", "Unlocks sneak + right-click shockwave.", "gust", 7),
            u("lift_power", Path.WIND, "Strong Winds", "+20% launch and knockback power per level.", "gust", 3, 5, 7),

            // HEAL
            u("healing_touch", Path.HEAL, "Healing Touch", "Unlocks the Heal Wand.", null, 5),
            u("greater_heal", Path.HEAL, "Greater Heal", "Heals 2 extra hearts per level.", "healing_touch", 4, 6, 8),
            u("renewal", Path.HEAL, "Renewal", "Stronger regeneration after healing.", "healing_touch", 4, 7),

            // SPEEDSTER SCROLL
            u("swift_step", Path.SPEEDSTER, "Swift Step", "Unlocks the Speedster Scroll.", null, 5),
            u("extended_haste", Path.SPEEDSTER, "Extended Haste", "+30 seconds of speed per level.", "swift_step", 3, 5, 7),
            u("hyper_speed", Path.SPEEDSTER, "Hyper Speed", "+1 speed level per level.", "swift_step", 6, 10),

            // DRILL SCROLL
            u("drill_bore", Path.DRILL, "Drill", "Unlocks the Drill Scroll.", null, 6),
            u("deep_bore", Path.DRILL, "Deep Bore", "+15 blocks of drill distance per level.", "drill_bore", 4, 6, 8),
            u("wide_bore", Path.DRILL, "Wide Bore", "Wider tunnel, +0.5 radius per level.", "drill_bore", 6, 10),

            // GROWTH SCROLL
            u("verdant_touch", Path.GROWTH, "Verdant Touch", "Unlocks the Growth Scroll.", null, 5),
            u("wide_bloom", Path.GROWTH, "Wide Bloom", "+2 blocks of growth radius per level.", "verdant_touch", 4, 6, 8),
            u("overgrowth", Path.GROWTH, "Overgrowth", "Grows everything one extra time per level.", "verdant_touch", 5, 8),

            // RECALL SCROLL
            u("homeward", Path.RECALL, "Homeward", "Unlocks the Recall Scroll.", null, 6),
            u("quick_return", Path.RECALL, "Quick Return", "Recall cooldown 20% shorter per level.", "homeward", 4, 6, 8),
            u("safe_return", Path.RECALL, "Safe Return", "Get Resistance and Regeneration after recalling.", "homeward", 6),

            // CLONE SCROLL
            u("mirror_image", Path.CLONE, "Mirror Image", "Unlocks the Clone Scroll.", null, 6),
            u("twin_clones", Path.CLONE, "Twin Clones", "+1 extra clone per level.", "mirror_image", 8, 12),
            u("lasting_illusion", Path.CLONE, "Lasting Illusion", "Clones last 5 seconds longer per level.", "mirror_image", 4, 6, 8),

            // BRIDGE SCROLL
            u("skybridge", Path.BRIDGE, "Skybridge", "Unlocks the Bridge Scroll.", null, 5),
            u("long_bridge", Path.BRIDGE, "Long Bridge", "+4 blocks of bridge length per level.", "skybridge", 4, 6, 8),
            u("lasting_bridge", Path.BRIDGE, "Lasting Bridge", "Bridge lasts 5 seconds longer per level.", "skybridge", 3, 5, 7),

            // NUKE WAND (hidden unless you are holding the Nuke Wand)
            u("blast_power", Path.NUKE, "Blast Power", "+2 explosion power per level for the Big Blast.", null, 5, 8, 12),
            u("incendiary", Path.NUKE, "Incendiary", "Blasts and nukes set the ground on fire.", null, 6),
            u("nuke", Path.NUKE, "Nuke", "Unlocks sneak + right-click: a huge nuclear blast.", null, 15),
            u("nuke_yield", Path.NUKE, "Nuke Yield", "+5 nuke explosion power per level.", "nuke", 12, 18)
    );
    // ----------------------------------

    public static Upgrade get(String id) {
        for (Upgrade up : ALL) {
            if (up.id().equals(id)) return up;
        }
        return null;
    }

    public static List<Upgrade> forPath(Path path) {
        List<Upgrade> out = new ArrayList<>();
        for (Upgrade up : ALL) {
            if (up.path() == path) out.add(up);
        }
        return out;
    }
}
