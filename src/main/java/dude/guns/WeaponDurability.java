package dude.guns;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class WeaponDurability {
    private WeaponDurability() {
    }

    public static void hurtHeldItem(ServerLevel level, ServerPlayer player, InteractionHand hand, int amount) {
        hurtItem(level, player, player.getItemInHand(hand), hand, amount);
    }

    public static void hurtItem(
            ServerLevel level,
            ServerPlayer player,
            ItemStack stack,
            InteractionHand hand,
            int amount
    ) {
        if (amount <= 0 || player.getAbilities().instabuild) {
            return;
        }

        EquipmentSlot slot = hand == InteractionHand.MAIN_HAND
                ? EquipmentSlot.MAINHAND
                : EquipmentSlot.OFFHAND;

        stack.hurtAndBreak(
                amount,
                level,
                player,
                item -> player.onEquippedItemBroken(item, slot)
        );
    }
}
