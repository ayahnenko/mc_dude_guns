package dude.guns.network;

import dude.guns.Guns;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record SniperFirePayload() implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(
            Guns.MOD_ID,
            "sniper_fire"
    );

    public static final Type<SniperFirePayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, SniperFirePayload> CODEC =
            StreamCodec.unit(new SniperFirePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
