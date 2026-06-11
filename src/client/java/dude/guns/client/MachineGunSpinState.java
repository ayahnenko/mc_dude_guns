package dude.guns.client;

import dude.guns.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;

public final class MachineGunSpinState {
    private static final int SPIN_HOLD_TICKS = 3;

    private static int shotCounter = 0;
    private static int activeTicks = 0;

    private MachineGunSpinState() {
    }

    public static void tick(Minecraft client) {
        if (activeTicks <= 0) {
            setHeldMachineGunPhase(client, 0);
            return;
        }

        activeTicks--;
    }

    public static void onFirePacketSent(Minecraft client) {
        shotCounter++;
        activeTicks = SPIN_HOLD_TICKS;
        setHeldMachineGunPhase(client, shotCounter % 2);
    }

    private static void setHeldMachineGunPhase(Minecraft client, int phase) {
        if (client.player == null) {
            return;
        }

        ItemStack mainHand = client.player.getItemInHand(InteractionHand.MAIN_HAND);

        if (mainHand.is(ModItems.MACHINE_GUN)) {
            setPhase(mainHand, phase);
            return;
        }

        ItemStack offHand = client.player.getItemInHand(InteractionHand.OFF_HAND);

        if (offHand.is(ModItems.MACHINE_GUN)) {
            setPhase(offHand, phase);
        }
    }

    private static void setPhase(ItemStack stack, int phase) {
        stack.set(
                DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(
                        List.of((float) phase),
                        List.of(),
                        List.of(),
                        List.of()
                )
        );
    }
}
