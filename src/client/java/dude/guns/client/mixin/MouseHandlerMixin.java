package dude.guns.client.mixin;

import dude.guns.client.MachineGunAimState;
import dude.guns.client.SniperAimState;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @ModifyArg(
            method = "turnPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"
            ),
            index = 0
    )
    private double guns$scaleSniperMouseX(double value) {
        return scaleSniperMouse(value);
    }

    @ModifyArg(
            method = "turnPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"
            ),
            index = 1
    )
    private double guns$scaleSniperMouseY(double value) {
        return scaleSniperMouse(value);
    }

    private static double scaleSniperMouse(double value) {
        if (!SniperAimState.isActive()) {
            if (MachineGunAimState.isActive()) {
                return value * MachineGunAimState.getMouseSensitivityMultiplier();
            }

            return value;
        }

        return value * SniperAimState.getMouseSensitivityMultiplier();
    }
}
