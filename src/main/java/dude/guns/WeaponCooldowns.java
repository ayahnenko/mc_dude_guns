package dude.guns;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class WeaponCooldowns {
    private WeaponCooldowns() {
    }

    public static boolean isCoolingDown(Player player, ItemStack stack) {
        return player.getCooldowns().isOnCooldown(stack);
    }

    public static void start(Player player, ItemStack stack, int ticks) {
        if (ticks <= 0) {
            return;
        }

        player.getCooldowns().addCooldown(stack, ticks);
    }

    public static void startEmpty(Player player, ItemStack stack, int ticks) {
        start(player, stack, ticks);
    }

    public static void startOverheated(Player player, ItemStack stack, int ticks) {
        start(player, stack, ticks);
    }
}
