package dude.guns;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import java.util.function.Function;

public class ModItems {
    public static final Item SHOTGUN = register(
            "shotgun",
            ShotgunItem::new,
            new Item.Properties().durability(ModConfig.get().shotgun.durability)
    );

    public static final Item SHOTGUN_SHELL = register(
            "shotgun_shell",
            Item::new,
            new Item.Properties().stacksTo(ModConfig.get().shotgun.shellStackSize)
    );

    public static final Item SNIPER_RIFLE = register(
            "sniper_rifle",
            SniperRifleItem::new,
            new Item.Properties().durability(ModConfig.get().sniperRifle.durability)
    );

    public static final Item SNIPER_ROUND = register(
            "sniper_round",
            Item::new,
            new Item.Properties().stacksTo(ModConfig.get().sniperRifle.roundStackSize)
    );

    public static final Item MACHINE_GUN = register(
            "machine_gun",
            MachineGunItem::new,
            new Item.Properties().durability(ModConfig.get().machineGun.durability)
    );

    public static final Item MACHINE_GUN_ROUND = register(
            "machine_gun_round",
            Item::new,
            new Item.Properties().stacksTo(ModConfig.get().machineGun.roundStackSize)
    );

    public static <T extends Item> T register(
            String name,
            Function<Item.Properties, T> itemFactory,
            Item.Properties properties
    ) {
        ResourceKey<Item> itemKey = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(Guns.MOD_ID, name)
        );

        T item = itemFactory.apply(properties.setId(itemKey));

        Registry.register(BuiltInRegistries.ITEM, itemKey, item);

        return item;
    }

    public static void initialize() {
        Guns.LOGGER.info("Registering Guns items");

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT)
                .register((creativeTab) -> {
                    creativeTab.accept(ModItems.SHOTGUN);
                    creativeTab.accept(ModItems.SHOTGUN_SHELL);
                    creativeTab.accept(ModItems.SNIPER_RIFLE);
                    creativeTab.accept(ModItems.SNIPER_ROUND);
                    creativeTab.accept(ModItems.MACHINE_GUN);
                    creativeTab.accept(ModItems.MACHINE_GUN_ROUND);
                });
    }
}
