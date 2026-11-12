package com.oidioomstudio.purplestick;

import com.oidioomstudio.purplestick.config.PurpleStickConfig;
import com.oidioomstudio.purplestick.item.PurpleStickDataComponents;
import com.oidioomstudio.purplestick.item.PurpleStickItems;
import com.oidioomstudio.purplestick.item.StickItem;
import com.oidioomstudio.purplestick.item.StickLock;
import com.oidioomstudio.purplestick.item.StickMode;
import com.oidioomstudio.purplestick.net.ChangeStickModePayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PurpleStickMod implements ModInitializer {
    public static final String MOD_ID = "purplestick";
    public static final String MOD_NAME = "Purple Stick";
    public static final String MOD_VERSION = "1.1.2";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(MOD_ID, "main")
    );

    public static final CreativeModeTab TAB = FabricCreativeModeTab.builder()
            .icon(() -> new ItemStack(PurpleStickItems.NETHERITE_STICK))
            .title(Component.literal("Purple Stick"))
            .displayItems((itemDisplayParameters, output) -> {
                output.accept(PurpleStickItems.WOODEN_STICK);
                output.accept(PurpleStickItems.COPPER_STICK);
                output.accept(PurpleStickItems.IRON_STICK);
                output.accept(PurpleStickItems.DIAMOND_STICK);
                output.accept(PurpleStickItems.NETHERITE_STICK);
            })
            .build();

    @Override
    public void onInitialize() {
        LOGGER.info("[PurpleStick] Initializing {} v{}...", MOD_NAME, MOD_VERSION);
        PurpleStickConfig.load();
        PurpleStickDataComponents.initialize();
        PurpleStickItems.initialize();
        com.oidioomstudio.olib.api.OCreativeTabs.accept(PurpleStickItems.WOODEN_STICK);
        com.oidioomstudio.olib.api.OCreativeTabs.accept(PurpleStickItems.COPPER_STICK);
        com.oidioomstudio.olib.api.OCreativeTabs.accept(PurpleStickItems.IRON_STICK);
        com.oidioomstudio.olib.api.OCreativeTabs.accept(PurpleStickItems.DIAMOND_STICK);
        com.oidioomstudio.olib.api.OCreativeTabs.accept(PurpleStickItems.NETHERITE_STICK);

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, TAB);

        // Network: Mode changing payload
        PayloadTypeRegistry.serverboundPlay().register(ChangeStickModePayload.TYPE, ChangeStickModePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ChangeStickModePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            player.level().getServer().execute(() -> {
                ItemStack stack = payload.isMainHand() ? player.getMainHandItem() : player.getOffhandItem();
                if (stack.getItem() instanceof StickItem stickItem) {
                    StickMode mode = StickMode.fromId(payload.modeId());
                    StickLock lock = StickLock.fromId(payload.lockId());
                    if (lock.getRequiredBlocks() > stickItem.getEffectiveLimit(mode)) {
                        lock = StickLock.NONE;
                    }
                    StickItem.setMode(stack, mode);
                    StickItem.setLock(stack, lock);
                    player.sendOverlayMessage(Component.translatable("purplestick.message.mode_changed", mode.getName(), lock.getName()));

                    if (mode == StickMode.DESTRUCTION && player.level().getServer() != null) {
                        var adv = player.level().getServer().getAdvancements().get(Identifier.fromNamespaceAndPath(MOD_ID, "destruction_mode"));
                        if (adv != null) {
                            player.getAdvancements().award(adv, "enter_destruction_mode");
                        }
                    }
                }
            });
        });

        LOGGER.info("[PurpleStick] Successfully initialized.");
    }
}
