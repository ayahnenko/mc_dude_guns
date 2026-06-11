package dude.guns.client.mixin;

import dude.guns.client.MachineGunAimState;
import dude.guns.client.MachineGunSpinState;
import dude.guns.client.SniperAimState;
import dude.guns.network.MachineGunFirePayload;
import dude.guns.network.SniperFirePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void guns$fireSniperInsteadOfAttacking(CallbackInfoReturnable<Boolean> cir) {
        if (!SniperAimState.isActive()) {
            if (MachineGunAimState.isActive()) {
                ClientPlayNetworking.send(new MachineGunFirePayload());
                MachineGunSpinState.onFirePacketSent((Minecraft) (Object) this);
                cir.setReturnValue(true);
            }

            return;
        }

        ClientPlayNetworking.send(new SniperFirePayload());
        cir.setReturnValue(true);
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void guns$stopDestroyingBlocksWhileAiming(boolean attacking, CallbackInfo ci) {
        if (SniperAimState.isActive()) {
            ci.cancel();
            return;
        }

        if (MachineGunAimState.isActive()) {
            if (attacking) {
                ClientPlayNetworking.send(new MachineGunFirePayload());
                MachineGunSpinState.onFirePacketSent((Minecraft) (Object) this);
            }

            ci.cancel();
        }
    }
}
