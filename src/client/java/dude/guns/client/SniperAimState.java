package dude.guns.client;

import dude.guns.ModConfig;
import dude.guns.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class SniperAimState {
    private static float previousZoomProgress = 0.0f;
    private static float zoomProgress = 0.0f;

    private SniperAimState() {
    }

    public static void tick(Minecraft client) {
        boolean aiming = shouldAim(client);
        float step = ModConfig.get().sniperRifle.zoomStep;

        previousZoomProgress = zoomProgress;

        if (aiming) {
            zoomProgress = Math.min(1.0f, zoomProgress + step);
        } else {
            zoomProgress = Math.max(0.0f, zoomProgress - step);
        }
    }

    public static boolean isActive() {
        return zoomProgress > 0.0f;
    }

    public static float getZoomMultiplier(float tickProgress) {
        float target = ModConfig.get().sniperRifle.zoomFovMultiplier;
        float progress = Mth.lerp(tickProgress, previousZoomProgress, zoomProgress);
        return 1.0f + (target - 1.0f) * smoothStep(progress);
    }

    public static double getMouseSensitivityMultiplier() {
        return getZoomMultiplier(1.0f);
    }

    private static boolean shouldAim(Minecraft client) {
        LocalPlayer player = client.player;

        if (player == null || client.level == null || client.screen != null) {
            return false;
        }

        if (!client.options.getCameraType().isFirstPerson()) {
            return false;
        }

        if (!client.options.keyUse.isDown()) {
            return false;
        }

        return isSniper(player.getMainHandItem()) || isSniper(player.getOffhandItem());
    }

    private static boolean isSniper(ItemStack stack) {
        return stack.is(ModItems.SNIPER_RIFLE);
    }

    private static float smoothStep(float value) {
        return value * value * (3.0f - 2.0f * value);
    }
}
