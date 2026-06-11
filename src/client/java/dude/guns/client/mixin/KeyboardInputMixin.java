package dude.guns.client.mixin;

import dude.guns.client.MachineGunAimState;
import dude.guns.client.SniperAimState;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {
    @Inject(method = "tick", at = @At("RETURN"))
    private void guns$slowMovementWhileAiming(CallbackInfo ci) {
        if (!SniperAimState.isActive()) {
            if (MachineGunAimState.isActive()) {
                ClientInputAccessor accessor = (ClientInputAccessor) this;
                Vec2 moveVector = accessor.guns$getMoveVector();
                float multiplier = (float) MachineGunAimState.getMovementMultiplier();
                accessor.guns$setMoveVector(new Vec2(moveVector.x * multiplier, moveVector.y * multiplier));
            }

            return;
        }

        ClientInputAccessor accessor = (ClientInputAccessor) this;
        Vec2 moveVector = accessor.guns$getMoveVector();
        float multiplier = (float) SniperAimState.getMouseSensitivityMultiplier();
        accessor.guns$setMoveVector(new Vec2(moveVector.x * multiplier, moveVector.y * multiplier));
    }
}
