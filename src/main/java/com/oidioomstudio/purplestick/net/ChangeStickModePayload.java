package com.oidioomstudio.purplestick.net;

import com.oidioomstudio.purplestick.PurpleStickMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ChangeStickModePayload(boolean isMainHand, String modeId, String lockId) implements CustomPacketPayload {
    public static final Type<ChangeStickModePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(PurpleStickMod.MOD_ID, "change_mode")
    );

    public static final StreamCodec<ByteBuf, ChangeStickModePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ChangeStickModePayload::isMainHand,
            ByteBufCodecs.STRING_UTF8, ChangeStickModePayload::modeId,
            ByteBufCodecs.STRING_UTF8, ChangeStickModePayload::lockId,
            ChangeStickModePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
