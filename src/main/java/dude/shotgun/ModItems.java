package dude.shotgun;

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
            new Item.Properties().durability(600)
    );

    public static final Item SHOTGUN_SHELL = register(
            "shotgun_shell",
            Item::new,
            new Item.Properties().stacksTo(64)
    );

    public static <T extends Item> T register(
            String name,
            Function<Item.Properties, T> itemFactory,
            Item.Properties properties
    ) {
        ResourceKey<Item> itemKey = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(Shotgun.MOD_ID, name)
        );

        T item = itemFactory.apply(properties.setId(itemKey));

        Registry.register(BuiltInRegistries.ITEM, itemKey, item);

        return item;
    }

    public static void initialize() {
        Shotgun.LOGGER.info("Registering Shotgun items");

        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT)
                .register((creativeTab) -> {
                    creativeTab.accept(ModItems.SHOTGUN);
                    creativeTab.accept(ModItems.SHOTGUN_SHELL);
                });
    }
}