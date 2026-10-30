package com.example.magicwand;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** The upgrade tree. Open it with the U key. */
public class UpgradeScreen extends Screen {
    private static Upgrades.Path selected = Upgrades.Path.MANA; // remembers your last tab

    private static final int PANEL_W = 420;
    private static final int PANEL_H = 224;
    private static final int ROW_H = 46;

    private int left;
    private int top;

    public UpgradeScreen() {
        super(Text.literal("Upgrade Tree"));
    }

    /** The Nuke tab only exists while you are holding the Nuke Wand. */
    private static boolean holdingNukeWand() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;
        return mc.player.getMainHandStack().isOf(MagicWandMod.NUKE_WAND)
                || mc.player.getOffHandStack().isOf(MagicWandMod.NUKE_WAND);
    }

    private static List<Upgrades.Path> visiblePaths() {
        List<Upgrades.Path> out = new ArrayList<>();
        for (Upgrades.Path path : visiblePaths()) {
            if (path == Upgrades.Path.NUKE && !holdingNukeWand()) continue;
            out.add(path);
        }
        return out;
    }

    public void refresh() {
        this.clearAndInit();
    }

    @Override
    protected void init() {
        left = (this.width - PANEL_W) / 2;
        top = (this.height - PANEL_H) / 2;

        if (selected == Upgrades.Path.NUKE && !holdingNukeWand()) {
            selected = Upgrades.Path.MANA;
        }

        // Tabs down the left side
        int ty = top + 24;
        for (Upgrades.Path path : Upgrades.Path.values()) {
            ButtonWidget tab = ButtonWidget.builder(Text.literal(path.label), btn -> {
                selected = path;
                this.clearAndInit();
            }).dimensions(left + 6, ty, 100, 12).build();
            tab.active = path != selected;
            this.addDrawableChild(tab);
            ty += 14;
        }

        // Upgrade buttons on the right side
        int ry = top + 30;
        for (Upgrades.Upgrade up : Upgrades.forPath(selected)) {
            int lvl = MagicWandClient.level(up.id());
            boolean maxed = lvl >= up.maxLevel();
            boolean locked = up.requires() != null && MagicWandClient.level(up.requires()) < 1;

            String label = maxed ? "MAX" : locked ? "Locked" : "Buy: " + up.costs()[lvl] + " XP";
            ButtonWidget buy = ButtonWidget.builder(Text.literal(label),
                    btn -> ClientPlayNetworking.send(new PurchasePayload(up.id())))
                    .dimensions(left + PANEL_W - 106, ry + 10, 100, 20).build();
            buy.active = !maxed && !locked;
            this.addDrawableChild(buy);

            ry += ROW_H;
        }
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.renderBackground(ctx, mouseX, mouseY, delta);
        ctx.fill(left - 2, top - 2, left + PANEL_W + 2, top + PANEL_H + 2, 0xFF000000);
        ctx.fill(left, top, left + PANEL_W, top + PANEL_H, 0xEE1B1B2F);
        ctx.fill(left + 112, top + 24, left + 113, top + PANEL_H - 8, 0xFF555577);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta); // background and buttons

        MinecraftClient mc = MinecraftClient.getInstance();
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("Upgrade Tree  (press U to close)"),
                left + 8, top + 8, 0xFFFFFFFF);
        if (mc.player != null) {
            ctx.drawTextWithShadow(this.textRenderer, Text.literal("XP Levels: " + mc.player.experienceLevel),
                    left + PANEL_W - 90, top + 8, 0xFF7CFC00);
        }

        int ry = top + 30;
        for (Upgrades.Upgrade up : Upgrades.forPath(selected)) {
            int lvl = MagicWandClient.level(up.id());
            boolean maxed = lvl >= up.maxLevel();
            boolean locked = up.requires() != null && MagicWandClient.level(up.requires()) < 1;

            int nameColor = maxed ? 0xFFFFD700 : locked ? 0xFF888888 : 0xFFFFFFFF;
            ctx.drawTextWithShadow(this.textRenderer,
                    Text.literal(up.name() + "  (" + lvl + "/" + up.maxLevel() + ")"),
                    left + 120, ry, nameColor);

            List<OrderedText> lines = this.textRenderer.wrapLines(Text.literal(up.description()), 180);
            int ly = ry + 11;
            for (int i = 0; i < Math.min(2, lines.size()); i++) {
                ctx.drawTextWithShadow(this.textRenderer, lines.get(i), left + 120, ly, 0xFFAAAAAA);
                ly += 10;
            }

            if (locked) {
                Upgrades.Upgrade req = Upgrades.get(up.requires());
                String reqName = req == null ? up.requires() : req.name();
                ctx.drawTextWithShadow(this.textRenderer, Text.literal("Requires: " + reqName),
                        left + 120, ry + 33, 0xFFFF5555);
            }

            ry += ROW_H;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_U) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
