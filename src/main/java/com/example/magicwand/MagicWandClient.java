package com.example.magicwand;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/** Client side only: mana bar, upgrade levels and the U key. */
public class MagicWandClient implements ClientModInitializer {
    private static int mana = ManaManager.MAX_MANA;
    private static int max = ManaManager.MAX_MANA;
    private static final Map<String, Integer> LEVELS = new HashMap<>();

    private static KeyBinding openKey;

    public static int level(String id) {
        return LEVELS.getOrDefault(id, 0);
    }

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ManaPayload.ID, (payload, context) -> {
            mana = payload.mana();
            max = payload.max();
        });

        ClientPlayNetworking.registerGlobalReceiver(UpgradesPayload.ID, (payload, context) -> {
            LEVELS.clear();
            for (String part : payload.data().split(";")) {
                String[] kv = part.split("=");
                if (kv.length == 2) {
                    try {
                        LEVELS.put(kv[0], Integer.parseInt(kv[1]));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.execute(() -> {
                if (mc.currentScreen instanceof UpgradeScreen screen) {
                    screen.refresh();
                }
            });
        });

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.magicwand.upgrades", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_U, "category.magicwand"));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.wasPressed()) {
                if (mc.currentScreen == null && mc.player != null) {
                    mc.setScreen(new UpgradeScreen());
                }
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> renderMana(drawContext));
    }

    private static void renderMana(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden || mc.player.isSpectator()) {
            return;
        }

        int barWidth = 81;
        int x = ctx.getScaledWindowWidth() / 2 - 91; // lines up with the left of the hotbar
        int y = ctx.getScaledWindowHeight() - 59;    // just above the armor row

        int filled = max <= 0 ? 0 : (int) (barWidth * (mana / (float) max));

        ctx.fill(x - 1, y - 1, x + barWidth + 1, y + 7, 0xFF000000); // border
        ctx.fill(x, y, x + barWidth, y + 6, 0xFF14143C);             // empty part
        if (filled > 0) {
            ctx.fill(x, y, x + filled, y + 6, 0xFF3A7BFF);           // mana
            ctx.fill(x, y, x + filled, y + 1, 0xFF9CC4FF);           // highlight
        }
        ctx.drawTextWithShadow(mc.textRenderer, mana + "/" + max, x + barWidth + 4, y - 1, 0xFF66AAFF);
    }
}
