package dude.shotgun;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {
    public static final SoundEvent SHOTGUN_FIRE = registerSound("shotgun_fire");

    private static SoundEvent registerSound(String id) {
        Identifier identifier = Identifier.fromNamespaceAndPath(Shotgun.MOD_ID, id);

        return Registry.register(
                BuiltInRegistries.SOUND_EVENT,
                identifier,
                SoundEvent.createVariableRangeEvent(identifier)
        );
    }

    public static void initialize() {
        Shotgun.LOGGER.info("Registering Shotgun sounds");
    }
}