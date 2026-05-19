package dude.shotgun.client;

import dude.shotgun.Shotgun;
import dude.shotgun.network.ShotgunRecoilPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ShotgunClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(_ -> ShotgunRecoilState.tick());

        ClientPlayNetworking.registerGlobalReceiver(ShotgunRecoilPayload.TYPE, (_, context) -> {
            //noinspection resource
            context.client().execute(() -> {
                ShotgunRecoilState.start();
                Shotgun.LOGGER.info("Shotgun recoil packet received");
            });
        });
	}
}