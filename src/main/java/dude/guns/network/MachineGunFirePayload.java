package dude.guns.network;

import dude.guns.Guns;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record MachineGunFirePayload() implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(
            Guns.MOD_ID,
            "machine_gun_fire"
    );

    public static final Type<MachineGunFirePayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, MachineGunFirePayload> CODEC =
            StreamCodec.unit(new MachineGunFirePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
