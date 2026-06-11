package dude.guns.client;

import dude.guns.ModConfig;
import dude.guns.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public final class MachineGunAimState {
    private static float previousAimProgress = 0.0f;
    private static float aimProgress = 0.0f;

    private MachineGunAimState() {
    }

    public static void tick(Minecraft client) {
        boolean aiming = shouldAim(client);
        float step = ModConfig.get().machineGun.zoomStep;

        previousAimProgress = aimProgress;

        if (aiming) {
            aimProgress = Math.min(1.0f, aimProgress + step);
        } else {
            aimProgress = Math.max(0.0f, aimProgress - step);
        }
    }

    public static boolean isActive() {
        return aimProgress > 0.0f;
    }

    public static float getZoomMultiplier(float tickProgress) {
        float target = ModConfig.get().machineGun.zoomFovMultiplier;
        float progress = Mth.lerp(tickProgress, previousAimProgress, aimProgress);
        return 1.0f + (target - 1.0f) * smoothStep(progress);
    }

    public static double getMouseSensitivityMultiplier() {
        return getZoomMultiplier(1.0f);
    }

    public static double getMovementMultiplier() {
        float target = ModConfig.get().machineGun.movementMultiplier;
        return 1.0f + (target - 1.0f) * smoothStep(aimProgress);
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

        return isMachineGun(player.getMainHandItem()) || isMachineGun(player.getOffhandItem());
    }

    private static boolean isMachineGun(ItemStack stack) {
        return stack.is(ModItems.MACHINE_GUN);
    }

    private static float smoothStep(float value) {
        return value * value * (3.0f - 2.0f * value);
    }
}
