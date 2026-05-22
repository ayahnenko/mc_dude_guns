package dude.guns.client;

import dude.guns.Guns;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class SniperScopeOverlay {
    private static final Identifier ELEMENT_ID = Identifier.fromNamespaceAndPath(Guns.MOD_ID, "sniper_scope");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            Guns.MOD_ID,
            "textures/gui/sniper_scope.png"
    );
    private static final int TEXTURE_SIZE = 256;

    private SniperScopeOverlay() {
    }

    public static void initialize() {
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.CROSSHAIR,
                ELEMENT_ID,
                SniperScopeOverlay::render
        );
    }

    private static void render(GuiGraphicsExtractor gui, DeltaTracker deltaTracker) {
        if (!SniperAimState.isActive()) {
            return;
        }

        int width = gui.guiWidth();
        int height = gui.guiHeight();
        int size = Math.max(width, height);
        int x = (width - size) / 2;
        int y = (height - size) / 2;

        if (x > 0) {
            gui.fill(0, 0, x, height, 0xFF000000);
            gui.fill(x + size, 0, width, height, 0xFF000000);
        }

        if (y > 0) {
            gui.fill(0, 0, width, y, 0xFF000000);
            gui.fill(0, y + size, width, height, 0xFF000000);
        }

        gui.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                x,
                y,
                0.0f,
                0.0f,
                size,
                size,
                TEXTURE_SIZE,
                TEXTURE_SIZE,
                TEXTURE_SIZE,
                TEXTURE_SIZE
        );
    }
}
