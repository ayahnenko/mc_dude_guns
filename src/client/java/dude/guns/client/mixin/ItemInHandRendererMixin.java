package dude.guns.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dude.guns.ModItems;
import dude.guns.client.ShotgunRecoilState;
import dude.guns.client.SniperAimState;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    @Unique
    private boolean shotgun$recoilPosePushed = false;

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void shotgun$applyRecoilTransform(
            AbstractClientPlayer player,
            float frameInterp,
            float xRot,
            InteractionHand hand,
            float attack,
            ItemStack itemStack,
            float inverseArmHeight,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            CallbackInfo ci
    ) {
        shotgun$recoilPosePushed = false;

        if (itemStack.is(ModItems.SNIPER_RIFLE) && SniperAimState.isActive()) {
            ci.cancel();
            return;
        }

        if (!itemStack.is(ModItems.SHOTGUN)) {
            return;
        }

        if (!ShotgunRecoilState.isActive()) {
            return;
        }

        shotgun$recoilPosePushed = true;
        poseStack.pushPose();

        float progress = ShotgunRecoilState.getProgress();

        // progress: 1.0 сразу после выстрела, потом плавно к 0.0.
        float kick = progress * progress;

        // Сдвиг модели: чуть назад, вниз и немного вправо.
        poseStack.translate(
                0.0f,
                0.0f,
                0.75f * kick
        );

        // Небольшой подброс ствола вверх.
        poseStack.mulPose(new Quaternionf(new AxisAngle4f(
                (float) Math.toRadians(-8.0f * kick),
                1.0f,
                0.0f,
                0.0f
        )));
    }

    @Inject(method = "renderArmWithItem", at = @At("RETURN"))
    private void shotgun$removeRecoilTransform(
            AbstractClientPlayer player,
            float frameInterp,
            float xRot,
            InteractionHand hand,
            float attack,
            ItemStack itemStack,
            float inverseArmHeight,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            CallbackInfo ci
    ) {
        if (shotgun$recoilPosePushed) {
            poseStack.popPose();
            shotgun$recoilPosePushed = false;
        }
    }
}
