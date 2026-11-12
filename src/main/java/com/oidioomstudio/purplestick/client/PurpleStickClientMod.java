package com.oidioomstudio.purplestick.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.oidioomstudio.purplestick.PurpleStickMod;
import com.oidioomstudio.purplestick.config.PurpleStickConfig;
import com.oidioomstudio.purplestick.item.PurpleStickItems;
import com.oidioomstudio.purplestick.item.StickItem;
import com.oidioomstudio.purplestick.item.StickLock;
import com.oidioomstudio.purplestick.item.StickMode;
import com.oidioomstudio.olib.api.OConfigCategory;
import com.oidioomstudio.olib.api.OConfigRegistry;
import com.oidioomstudio.olib.api.OModConfig;
import com.oidioomstudio.olib.api.options.BooleanOption;
import com.oidioomstudio.olib.api.options.ColorOption;
import com.oidioomstudio.olib.api.options.IntegerSliderOption;
import com.oidioomstudio.olib.util.render.CachedEdge;
import com.oidioomstudio.olib.util.render.OBlockOutlineRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.*;

@Environment(EnvType.CLIENT)
public class PurpleStickClientMod implements ClientModInitializer {

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(PurpleStickMod.MOD_ID, "main")
    );

    public static final KeyMapping KEY_MODE_SCREEN = new KeyMapping(
            "key.purplestick.mode_screen",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_V,
            CATEGORY
    );

    private List<BlockPos> previewBlocks = Collections.emptyList();
    private BlockPos cachedPos = null;
    private List<CachedEdge> cachedEdges = null;
    private int currentOutlineColor = 0xFF55FF55;

    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(KEY_MODE_SCREEN);
        registerConfig();

        // Client Tick: handle Shift+V mode screen & preview updates
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

        // Level Render: 3D Block Outline Rendering
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(context -> {
            renderInGame(context.poseStack(), context.submitNodeCollector(), context.levelState());
        });
    }

    private void onClientTick(Minecraft client) {
        if (client.player == null) return;

        // Check Shift+V to open StickModeScreen
        while (KEY_MODE_SCREEN.consumeClick()) {
            boolean isShift = client.player.isShiftKeyDown()
                    || InputConstants.isKeyDown(InputConstants.KEY_LSHIFT)
                    || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);
            if (!isShift) {
                continue;
            }
            ItemStack mainHand = client.player.getMainHandItem();
            ItemStack offHand = client.player.getOffhandItem();

            if (mainHand.getItem() instanceof StickItem) {
                client.setScreenAndShow(new StickModeScreen(true, mainHand));
                return;
            } else if (offHand.getItem() instanceof StickItem) {
                client.setScreenAndShow(new StickModeScreen(false, offHand));
                return;
            }
        }

        updatePreview(client);
    }

    private void updatePreview(Minecraft client) {
        if (!PurpleStickConfig.get().renderPreview || client.level == null || client.player == null) {
            clearPreview();
            return;
        }

        ItemStack mainHand = client.player.getMainHandItem();
        ItemStack offHand = client.player.getOffhandItem();
        ItemStack stickStack;
        InteractionHand stickHand;
        if (mainHand.getItem() instanceof StickItem) {
            stickStack = mainHand;
            stickHand = InteractionHand.MAIN_HAND;
        } else if (offHand.getItem() instanceof StickItem) {
            stickStack = offHand;
            stickHand = InteractionHand.OFF_HAND;
        } else {
            clearPreview();
            return;
        }

        StickItem stickItem = (StickItem) stickStack.getItem();

        HitResult hit = client.hitResult;
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos clickedPos = blockHit.getBlockPos();
            Direction face = blockHit.getDirection();
            BlockState clickedState = client.level.getBlockState(clickedPos);

            if (clickedState.isAir()) {
                clearPreview();
                return;
            }

            StickMode mode = StickItem.getMode(stickStack);
            StickLock lock = StickItem.getLock(stickStack);
            if (lock.getRequiredBlocks() > stickItem.getEffectiveLimit(mode)) {
                lock = StickLock.NONE;
            }

            int maxBlocks = stickItem.getEffectiveLimit(mode);

            if (mode == StickMode.DESTRUCTION) {
                if (clickedState.getDestroySpeed(client.level, clickedPos) < 0) {
                    clearPreview();
                    return;
                }

                List<BlockPos> destructions = StickItem.calculateDestruction(client.level, clickedPos, face, clickedState, lock, maxBlocks);
                if (!destructions.isEmpty()) {
                    currentOutlineColor = StickMode.DESTRUCTION.getOutlineColor(); // Red
                    if (cachedPos == null || !cachedPos.equals(clickedPos) || !previewBlocks.equals(destructions)) {
                        previewBlocks = destructions;
                        cachedPos = clickedPos;
                        updateEdges();
                    }
                    return;
                }
            } else {
                StickItem.PlacementMaterial material = StickItem.resolvePlacementMaterial(client.player, stickHand, clickedState);
                if (material == null) {
                    clearPreview();
                    return;
                }

                int available = client.player.isCreative()
                        ? maxBlocks
                        : Math.min(maxBlocks, StickItem.countBlockItems(client.player, material.item()));

                if (available <= 0) {
                    clearPreview();
                    return;
                }

                List<BlockPos> placements = StickItem.calculatePlacements(client.level, clickedPos, face, clickedState, material.placeState(), lock, available);
                if (!placements.isEmpty()) {
                    currentOutlineColor = PurpleStickConfig.get().outlineColor;
                    if (cachedPos == null || !cachedPos.equals(clickedPos) || !previewBlocks.equals(placements)) {
                        previewBlocks = placements;
                        cachedPos = clickedPos;
                        updateEdges();
                    }
                    return;
                }
            }
        }

        clearPreview();
    }

    private void clearPreview() {
        previewBlocks = Collections.emptyList();
        cachedPos = null;
        cachedEdges = null;
    }

    private void updateEdges() {
        if (previewBlocks.isEmpty() || cachedPos == null) {
            cachedEdges = Collections.emptyList();
            return;
        }
        cachedEdges = OBlockOutlineRenderer.buildEdges(previewBlocks, cachedPos);
    }

    private void renderInGame(PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, net.minecraft.client.renderer.state.level.LevelRenderState levelState) {
        if (!PurpleStickConfig.get().renderPreview) {
            return;
        }

        OBlockOutlineRenderer.render(
                poseStack,
                collector,
                levelState,
                this.cachedPos,
                this.cachedEdges,
                this.currentOutlineColor,
                2.5f
        );
    }

    private static void registerConfig() {
        PurpleStickConfig.Data cfg = PurpleStickConfig.get();

        OConfigRegistry.register(
                OModConfig.builder(PurpleStickMod.MOD_ID, Component.translatable("purplestick.name"), PurpleStickMod.MOD_VERSION)
                        .category(OConfigCategory.builder("general", Component.translatable("purplestick.config.general"))
                                .group("building", Component.translatable("purplestick.config.building"), g -> g
                                        .add(IntegerSliderOption.builder("max_blocks", Component.translatable("purplestick.config.option.max_blocks"))
                                                .description(Component.translatable("purplestick.config.option.max_blocks.desc"))
                                                .range(16, 4096, 16)
                                                .defaultValue(cfg.maxBlocks)
                                                .getter(() -> cfg.maxBlocks)
                                                .setter(v -> cfg.maxBlocks = v)
                                                .build())
                                        .add(BooleanOption.builder("durability_enabled", Component.translatable("purplestick.config.option.durability"))
                                                .description(Component.translatable("purplestick.config.option.durability.desc"))
                                                .defaultValue(cfg.durabilityEnabled)
                                                .getter(() -> cfg.durabilityEnabled)
                                                .setter(v -> cfg.durabilityEnabled = v)
                                                .build())
                                        .add(BooleanOption.builder("sound_enabled", Component.translatable("purplestick.config.option.sound"))
                                                .description(Component.translatable("purplestick.config.option.sound.desc"))
                                                .defaultValue(cfg.soundEnabled)
                                                .getter(() -> cfg.soundEnabled)
                                                .setter(v -> cfg.soundEnabled = v)
                                                .build())
                                        .add(BooleanOption.builder("render_preview", Component.translatable("purplestick.config.option.render_preview"))
                                                .description(Component.translatable("purplestick.config.option.render_preview.desc"))
                                                .defaultValue(cfg.renderPreview)
                                                .getter(() -> cfg.renderPreview)
                                                .setter(v -> cfg.renderPreview = v)
                                                .build())
                                        .add(ColorOption.builder("outline_color", Component.translatable("purplestick.config.option.outline_color"))
                                                .description(Component.translatable("purplestick.config.option.outline_color.desc"))
                                                .defaultValue(cfg.outlineColor)
                                                .getter(() -> cfg.outlineColor)
                                                .setter(v -> cfg.outlineColor = v)
                                                .build())
                                )
                                .build())
                        .onSave(PurpleStickConfig::save)
                        .build()
        );
    }
}
