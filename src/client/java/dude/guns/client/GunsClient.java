package dude.guns.client;

import dude.guns.Guns;
import dude.guns.network.ShotgunRecoilPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class GunsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(_ -> ShotgunRecoilState.tick());
        ClientTickEvents.END_CLIENT_TICK.register(SniperAimState::tick);
        SniperScopeOverlay.initialize();

        ClientPlayNetworking.registerGlobalReceiver(ShotgunRecoilPayload.TYPE, (_, context) -> {
            //noinspection resource
            context.client().execute(() -> {
                ShotgunRecoilState.start();
                Guns.LOGGER.info("Shotgun recoil packet received");
            });
        });
	}
}
