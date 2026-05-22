package dude.guns.client.mixin;

import dude.guns.client.SniperAimState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {
    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void guns$hideCrosshairWhileAiming(
            GuiGraphicsExtractor gui,
            DeltaTracker deltaTracker,
            CallbackInfo ci
    ) {
        if (SniperAimState.isActive()) {
            ci.cancel();
        }
    }
}
