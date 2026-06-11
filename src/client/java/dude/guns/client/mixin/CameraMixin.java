package dude.guns.client.mixin;

import dude.guns.client.MachineGunAimState;
import dude.guns.client.SniperAimState;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class CameraMixin {
    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void guns$applySniperZoom(float tickProgress, CallbackInfoReturnable<Float> cir) {
        if (!SniperAimState.isActive()) {
            if (MachineGunAimState.isActive()) {
                cir.setReturnValue(cir.getReturnValue() * MachineGunAimState.getZoomMultiplier(tickProgress));
            }

            return;
        }

        cir.setReturnValue(cir.getReturnValue() * SniperAimState.getZoomMultiplier(tickProgress));
    }
}
