package com.example.magicwand;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Client side only: receives your mana and draws the mana bar. */
public class MagicWandClient implements ClientModInitializer {
    private static int mana = ManaManager.MAX_MANA;
    private static int max = ManaManager.MAX_MANA;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ManaPayload.ID, (payload, context) -> {
            mana = payload.mana();
            max = payload.max();
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
        ctx.drawTextWithShadow(mc.textRenderer, mana + "/" + max, x + barWidth + 4, y - 1, 0x66AAFF);
    }
}
