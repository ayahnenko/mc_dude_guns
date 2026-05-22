package dude.guns;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class WeaponAmmo {
    private WeaponAmmo() {
    }

    public static boolean hasAmmo(Player player, boolean usesAmmo, Item ammoItem) {
        if (!usesAmmo || player.getAbilities().instabuild) {
            return true;
        }

        return findAmmoStack(player, ammoItem) != null;
    }

    public static boolean consumeAmmo(Player player, boolean usesAmmo, Item ammoItem) {
        if (!usesAmmo || player.getAbilities().instabuild) {
            return true;
        }

        ItemStack stack = findAmmoStack(player, ammoItem);

        if (stack == null) {
            return false;
        }

        stack.shrink(1);
        return true;
    }

    private static ItemStack findAmmoStack(Player player, Item ammoItem) {
        Inventory inventory = player.getInventory();

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);

            if (stack.is(ammoItem)) {
                return stack;
            }
        }

        return null;
    }
}
