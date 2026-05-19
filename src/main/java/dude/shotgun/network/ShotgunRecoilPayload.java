package dude.shotgun.network;

import dude.shotgun.Shotgun;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record ShotgunRecoilPayload() implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(
            Shotgun.MOD_ID,
            "recoil"
    );

    public static final Type<ShotgunRecoilPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ShotgunRecoilPayload> CODEC =
            StreamCodec.unit(new ShotgunRecoilPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}