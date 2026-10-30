# Magic Wands (Fabric, Minecraft 1.21.4)

All wands: ender pearl/other item in the top-right slot, blaze rod in the middle and bottom-left.

| Wand | Top-right item | Right-click | Sneak + right-click |
|---|---|---|---|
| Magic Wand | ender pearl | fireball | teleport 12 blocks |
| Lightning Wand | lightning rod | lightning where you aim | same |
| Ice Wand | blue ice | freeze mob / water, snow on ground | same |
| Wind Wand | breeze rod | launch yourself up | shockwave knockback |
| Heal Wand | golden apple | heal what you aim at (or yourself) | same |

## Spell scrolls (infinite use, never consumed)

Recipe: paper in the middle, 8 of the surrounding item.

| Scroll | Surround with | Effect | Cooldown |
|---|---|---|---|
| Speedster Scroll | 8 sugar | Speed III for 60 seconds | 90 seconds |
| Drill Scroll | 8 iron ingots | Summons a drill that tunnels (about 3 wide, 30 long) where you face | 15 seconds |

## Mana

You have 100 mana (blue bar above your armor). It refills 3 per second and resets to full when you rejoin.
Every spell costs mana; at 0 you can't cast.

| Item | Mana cost |
|---|---|
| Magic Wand: fireball / teleport | 10 / 25 |
| Lightning Wand | 30 |
| Ice Wand | 15 |
| Wind Wand: launch / shockwave | 20 / 25 |
| Heal Wand | 25 |
| Speedster Scroll | 40 |
| Drill Scroll | 50 |

## Upgrade tree (press U)

Spend XP levels to unlock and improve powers. Each wand and scroll has its own path, plus a Mana path.
The fireball is free; every other wand/scroll power must be unlocked first.
Upgrades are saved in the world folder (magicwand_upgrades.json). Creative mode buys upgrades for free.
Handy for testing: /xp add @s 50 levels

## Mana potions

| Potion | Recipe | Effect |
|---|---|---|
| Mana Potion | glass bottle + 2 lapis lazuli + glowstone dust (shapeless) | drink: +50 mana, returns the bottle |
| Splash Mana Potion | mana potion + gunpowder (shapeless) | throw: +40 mana to everyone within 4 blocks of the splash |

## Nuke Wand (creative only)

No crafting recipe: take it from the Tools tab of the creative inventory. Survival players cannot use it.
Its upgrade tab ("Nuke") only appears in the U menu while you are holding the wand.

| Action | Effect | Mana |
|---|---|---|
| Right-click | Big Blast (power 6, TNT is 4) where you aim | 40 |
| Sneak + right-click | Nuke (power 25), needs the Nuke upgrade | 100 |

Upgrades: Blast Power (+2 per level), Incendiary (fire), Nuke (unlock), Nuke Yield (+5 per level).

## Spell Book, mana armor and trinkets

| Item | Recipe | Effect |
|---|---|---|
| Spell Book | book + 2 amethyst shards + 2 lapis (shapeless) | Pulls in every scroll in your inventory. Right-click: cast selected scroll. Sneak + right-click: next scroll. Scrolls must stay in your inventory. |
| Mana Crown (helmet slot) | gold ingots, amethyst shard, lapis (see recipe file) | +30 max mana while worn |
| Mana Robe (chestplate slot) | 7 lapis + amethyst shard | +1.5 mana/sec regen while worn |
| Mana Charm | string, lapis, amethyst | +20 max mana while anywhere in your inventory |
| Mana Ring | gold ingots + lapis | +1 mana/sec regen while anywhere in your inventory |

Each trinket only counts once. Bonuses stack with each other and with the upgrade tree.
