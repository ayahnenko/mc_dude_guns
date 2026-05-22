package dude.guns;

import dude.guns.network.ShotgunRecoilPayload;
import dude.guns.network.SniperFirePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Guns implements ModInitializer {
	public static final String MOD_ID = "guns";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

        LOGGER.info("Guns is initializing");

        ModConfig.initialize();

        PayloadTypeRegistry.clientboundPlay().register(
                ShotgunRecoilPayload.TYPE,
                ShotgunRecoilPayload.CODEC
        );

        PayloadTypeRegistry.serverboundPlay().register(
                SniperFirePayload.TYPE,
                SniperFirePayload.CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(SniperFirePayload.TYPE, (_, context) ->
                context.server().execute(() -> SniperRifleItem.fireFromPacket(context.player()))
        );

        ModItems.initialize();
        ModSounds.initialize();
        TemporaryMuzzleLights.initialize();
	}
}
