package com.oidioomstudio.purplestick.item;

import com.oidioomstudio.purplestick.PurpleStickMod;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public final class PurpleStickDataComponents {
    public static final DataComponentType<StickMode> STICK_MODE = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(PurpleStickMod.MOD_ID, "stick_mode"),
            DataComponentType.<StickMode>builder()
                    .persistent(StickMode.CODEC)
                    .networkSynchronized(StickMode.STREAM_CODEC)
                    .build()
    );

    public static final DataComponentType<StickLock> STICK_LOCK = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(PurpleStickMod.MOD_ID, "stick_lock"),
            DataComponentType.<StickLock>builder()
                    .persistent(StickLock.CODEC)
                    .networkSynchronized(StickLock.STREAM_CODEC)
                    .build()
    );

    private PurpleStickDataComponents() {
    }

    public static void initialize() {
        // Class loaded
    }
}

