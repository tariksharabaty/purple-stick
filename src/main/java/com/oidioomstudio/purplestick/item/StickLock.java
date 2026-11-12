package com.oidioomstudio.purplestick.item;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public enum StickLock {
    NONE("none", Component.translatable("purplestick.lock.none")),
    AREA_1X1("area_1x1", Component.translatable("purplestick.lock.area_1x1")),
    AREA_3X3("area_3x3", Component.translatable("purplestick.lock.area_3x3")),
    AREA_5X5("area_5x5", Component.translatable("purplestick.lock.area_5x5")),
    AREA_9X9("area_9x9", Component.translatable("purplestick.lock.area_9x9")),
    HORIZONTAL("horizontal", Component.translatable("purplestick.lock.horizontal")),
    VERTICAL("vertical", Component.translatable("purplestick.lock.vertical")),
    NORTH_SOUTH("north_south", Component.translatable("purplestick.lock.north_south")),
    EAST_WEST("east_west", Component.translatable("purplestick.lock.east_west"));

    public static final Codec<StickLock> CODEC = Codec.STRING.xmap(StickLock::fromId, StickLock::getId);
    public static final StreamCodec<ByteBuf, StickLock> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(
            i -> values()[i],
            StickLock::ordinal
    );

    private final String id;
    private final Component name;

    StickLock(String id, Component name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public Component getName() {
        return name;
    }

    public boolean isArea() {
        return this == AREA_1X1 || this == AREA_3X3 || this == AREA_5X5 || this == AREA_9X9;
    }

    public int getAreaRadius() {
        return switch (this) {
            case AREA_1X1 -> 0;
            case AREA_3X3 -> 1;
            case AREA_5X5 -> 2;
            case AREA_9X9 -> 4;
            default -> -1;
        };
    }

    public int getRequiredBlocks() {
        return switch (this) {
            case AREA_9X9 -> 81;
            case AREA_5X5 -> 25;
            case AREA_3X3 -> 9;
            default -> 1;
        };
    }

    public static StickLock fromId(String id) {
        if (id == null) return NONE;
        for (StickLock l : values()) {
            if (l.id.equalsIgnoreCase(id)) {
                return l;
            }
        }
        return NONE;
    }

    public Direction[] filterDirections(Direction face) {
        if (this == NONE || isArea()) {
            return StickItem.getPlaneDirections(face);
        }

        Direction[] planeDirs = StickItem.getPlaneDirections(face);
        return switch (this) {
            case HORIZONTAL -> {
                if (face.getAxis() == Direction.Axis.Y) {
                    yield new Direction[]{Direction.EAST, Direction.WEST};
                } else {
                    yield new Direction[]{face.getClockWise(), face.getCounterClockWise()};
                }
            }
            case VERTICAL -> {
                if (face.getAxis() == Direction.Axis.Y) {
                    yield new Direction[]{Direction.NORTH, Direction.SOUTH};
                } else {
                    yield new Direction[]{Direction.UP, Direction.DOWN};
                }
            }
            case NORTH_SOUTH -> new Direction[]{Direction.NORTH, Direction.SOUTH};
            case EAST_WEST -> new Direction[]{Direction.EAST, Direction.WEST};
            default -> planeDirs;
        };
    }
}
