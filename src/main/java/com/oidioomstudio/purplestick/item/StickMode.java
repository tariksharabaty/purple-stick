package com.oidioomstudio.purplestick.item;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public enum StickMode {
    BUILD("build", Component.translatable("purplestick.mode.build"), 0xFF55FF55),
    DESTRUCTION("destruction", Component.translatable("purplestick.mode.destruction"), 0xFFFF4444);

    public static final Codec<StickMode> CODEC = Codec.STRING.xmap(StickMode::fromId, StickMode::getId);
    public static final StreamCodec<ByteBuf, StickMode> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
            i -> values()[i],
            StickMode::ordinal
    );

    private final String id;
    private final Component name;
    private final int outlineColor;

    StickMode(String id, Component name, int outlineColor) {
        this.id = id;
        this.name = name;
        this.outlineColor = outlineColor;
    }

    public String getId() {
        return id;
    }

    public Component getName() {
        return name;
    }

    public int getOutlineColor() {
        return outlineColor;
    }

    public static StickMode fromId(String id) {
        if (id == null) return BUILD;
        for (StickMode m : values()) {
            if (m.id.equalsIgnoreCase(id)) {
                return m;
            }
        }
        return BUILD;
    }
}
