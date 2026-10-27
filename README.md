# Magic Wand (Fabric, Minecraft 1.21.4)

- Right-click: shoot a fireball
- Sneak + right-click: teleport up to 12 blocks forward (stops before walls)
- Recipe: blaze rod, blaze rod, ender pearl placed diagonally (see data/magicwand/recipe/magic_wand.json)

## Build
1. Install JDK 21.
2. In this folder run: `gradle wrapper --gradle-version 8.12` (one time), then `./gradlew build`
   (Windows: `gradlew.bat build`).
3. Your mod jar is in `build/libs/magic-wand-1.0.0.jar`. Drop it in `.minecraft/mods`
   together with Fabric API.

## Test without installing
`./gradlew runClient`
