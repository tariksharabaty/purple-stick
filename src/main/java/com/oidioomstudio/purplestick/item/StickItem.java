package com.oidioomstudio.purplestick.item;

import com.oidioomstudio.purplestick.config.PurpleStickConfig;
import com.oidioomstudio.olib.util.item.OInventoryHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.function.Consumer;

public class StickItem extends Item {
    private final int blockLimit;

    public StickItem(Properties properties, int blockLimit) {
        super(properties);
        this.blockLimit = blockLimit;
    }

    public int getBlockLimit() {
        return blockLimit;
    }

        public static StickMode getMode(ItemStack stack) {
        StickMode mode = stack.get(PurpleStickDataComponents.STICK_MODE);
        if (mode != null) {
            return mode;
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            return tag.getString("purplestick_mode").map(StickMode::fromId).orElse(StickMode.BUILD);
        }
        return StickMode.BUILD;
    }

    public static void setMode(ItemStack stack, StickMode mode) {
        stack.set(PurpleStickDataComponents.STICK_MODE, mode);
    }

    public static StickLock getLock(ItemStack stack) {
        StickLock lock = stack.get(PurpleStickDataComponents.STICK_LOCK);
        if (lock != null) {
            return lock;
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            return tag.getString("purplestick_lock").map(StickLock::fromId).orElse(StickLock.NONE);
        }
        return StickLock.NONE;
    }

    public static void setLock(ItemStack stack, StickLock lock) {
        stack.set(PurpleStickDataComponents.STICK_LOCK, lock);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Direction face = context.getClickedFace();
        Player player = context.getPlayer();
        ItemStack stick = context.getItemInHand();

        if (player == null) {
            return InteractionResult.PASS;
        }

        BlockState clickedState = level.getBlockState(clickedPos);
        if (clickedState.isAir()) {
            return InteractionResult.PASS;
        }

        StickMode mode = getMode(stick);
        StickLock lock = getLock(stick);
        if (lock.getRequiredBlocks() > getEffectiveLimit(mode)) {
            lock = StickLock.NONE;
        }

        if (mode == StickMode.DESTRUCTION) {
            return handleDestruction(context, level, clickedPos, face, player, stick, clickedState, lock);
        } else {
            return handlePlacement(context, level, clickedPos, face, player, stick, clickedState, lock);
        }
    }

    public record PlacementMaterial(Item item, BlockState placeState) {}

    public static boolean isDirtLike(BlockState s) {
        return s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT) || s.is(Blocks.COARSE_DIRT) || s.is(Blocks.ROOTED_DIRT)
                || s.is(Blocks.PODZOL) || s.is(Blocks.DIRT_PATH) || s.is(Blocks.FARMLAND) || s.is(Blocks.MUD)
                || s.is(Blocks.MYCELIUM);
    }

    public static boolean isStoneLike(BlockState s) {
        return s.is(Blocks.STONE) || s.is(Blocks.COBBLESTONE) || s.is(Blocks.ANDESITE) || s.is(Blocks.DIORITE) || s.is(Blocks.GRANITE)
                || s.is(Blocks.DEEPSLATE) || s.is(Blocks.COBBLED_DEEPSLATE);
    }

    public static boolean isMatchingSurface(BlockState a, BlockState b) {
        if (a.getBlock() == b.getBlock()) return true;
        if (isDirtLike(a) && isDirtLike(b)) return true;
        if (isStoneLike(a) && isStoneLike(b)) return true;
        return false;
    }

    public static boolean isMatchingDestruction(BlockState a, BlockState b) {
        if (a.getBlock() == b.getBlock()) return true;
        if (isDirtLike(a) && isDirtLike(b)) return true;
        if (isStoneLike(a) && isStoneLike(b)) return true;
        return false;
    }

    public static PlacementMaterial resolvePlacementMaterial(Player player, InteractionHand stickHand, BlockState clickedState) {
        if (player == null) return null;

        // 1. Other hand block item priority (user explicitly holds a block in their other hand)
        ItemStack otherHand = (stickHand == InteractionHand.MAIN_HAND) ? player.getOffhandItem() : player.getMainHandItem();
        if (!otherHand.isEmpty() && otherHand.getItem() instanceof BlockItem blockItem) {
            BlockState state = blockItem.getBlock().defaultBlockState();
            if (!state.hasBlockEntity()) {
                return new PlacementMaterial(blockItem, state);
            }
        }

        // 2. Direct match: does player have the exact clicked block item in inventory or is creative?
        Item directItem = clickedState.getBlock().asItem();
        if (directItem != null && directItem != Items.AIR) {
            if (player.isCreative() || countBlockItems(player, directItem) > 0) {
                return new PlacementMaterial(directItem, clickedState.getBlock().defaultBlockState());
            }
        }

        // 3. Survival natural fallbacks if player does not have direct item:
        if (isDirtLike(clickedState)) {
            if (countBlockItems(player, Items.DIRT) > 0) {
                return new PlacementMaterial(Items.DIRT, Blocks.DIRT.defaultBlockState());
            }
        }
        if (isStoneLike(clickedState)) {
            if (countBlockItems(player, Items.COBBLESTONE) > 0) {
                return new PlacementMaterial(Items.COBBLESTONE, Blocks.COBBLESTONE.defaultBlockState());
            }
            if (countBlockItems(player, Items.COBBLED_DEEPSLATE) > 0) {
                return new PlacementMaterial(Items.COBBLED_DEEPSLATE, Blocks.COBBLED_DEEPSLATE.defaultBlockState());
            }
            if (countBlockItems(player, Items.STONE) > 0) {
                return new PlacementMaterial(Items.STONE, Blocks.STONE.defaultBlockState());
            }
        }

        // 4. Hotbar fallback (slots 0..8): if player has any placeable BlockItem in hotbar
        for (int i = 0; i < 9; i++) {
            ItemStack hotbarStack = player.getInventory().getItem(i);
            if (!hotbarStack.isEmpty() && hotbarStack.getItem() instanceof BlockItem bi) {
                BlockState bs = bi.getBlock().defaultBlockState();
                if (!bs.hasBlockEntity()) {
                    return new PlacementMaterial(bi, bs);
                }
            }
        }

        // 5. Main inventory fallback (slots 9..35)
        for (int i = 9; i < player.getInventory().getContainerSize(); i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (!invStack.isEmpty() && invStack.getItem() instanceof BlockItem bi) {
                BlockState bs = bi.getBlock().defaultBlockState();
                if (!bs.hasBlockEntity()) {
                    return new PlacementMaterial(bi, bs);
                }
            }
        }

        // 6. Creative mode fallback
        if (player.isCreative()) {
            if (directItem != null && directItem != Items.AIR) {
                return new PlacementMaterial(directItem, clickedState.getBlock().defaultBlockState());
            }
            return new PlacementMaterial(Items.DIRT, Blocks.DIRT.defaultBlockState());
        }

        return null;
    }

    public ItemStack getHarvestTool(Player player, ItemStack stick, BlockState state) {
        ItemStack otherHand = (player.getMainHandItem() == stick) ? player.getOffhandItem() : player.getMainHandItem();
        if (!otherHand.isEmpty() && (otherHand.has(DataComponents.TOOL) || otherHand.isCorrectToolForDrops(state))) {
            return otherHand;
        }

        if (this == PurpleStickItems.NETHERITE_STICK) {
            return new ItemStack(Items.NETHERITE_PICKAXE);
        } else if (this == PurpleStickItems.DIAMOND_STICK) {
            return new ItemStack(Items.DIAMOND_PICKAXE);
        } else if (this == PurpleStickItems.IRON_STICK) {
            return new ItemStack(Items.IRON_PICKAXE);
        } else if (this == PurpleStickItems.COPPER_STICK) {
            return new ItemStack(Items.STONE_PICKAXE);
        } else {
            return new ItemStack(Items.WOODEN_PICKAXE);
        }
    }

    private InteractionResult handleDestruction(UseOnContext context, Level level, BlockPos clickedPos, Direction face, Player player, ItemStack stick, BlockState clickedState, StickLock lock) {
        if (clickedState.getDestroySpeed(level, clickedPos) < 0) {
            return InteractionResult.FAIL;
        }

        int maxBlocks = getEffectiveLimit(StickMode.DESTRUCTION);
        List<BlockPos> destroyPositions = calculateDestruction(level, clickedPos, face, clickedState, lock, maxBlocks);

        if (destroyPositions.isEmpty()) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        boolean isCreative = player.isCreative();

        int destroyedCount = 0;
        SoundType soundType = clickedState.getSoundType();
        ItemStack harvestTool = getHarvestTool(player, stick, clickedState);

        for (BlockPos targetPos : destroyPositions) {
            BlockState state = level.getBlockState(targetPos);
            if (state.isAir() || state.getDestroySpeed(level, targetPos) < 0) continue;

            if (!isCreative) {
                // Drop resources matching the tool or block entity before removal
                Block.dropResources(state, level, targetPos, level.getBlockEntity(targetPos), serverPlayer, harvestTool);
            }
            level.destroyBlock(targetPos, false, serverPlayer);
            destroyedCount++;

            if (!isCreative && PurpleStickConfig.get().durabilityEnabled && stick.isDamageableItem()) {
                OInventoryHelper.damageHeldItem(serverPlayer, context.getHand(), 1);
                if (stick.isEmpty()) {
                    break;
                }
            }
        }

        if (destroyedCount > 0) {
            if (PurpleStickConfig.get().soundEnabled) {
                level.playSound(null, clickedPos, soundType.getBreakSound(), SoundSource.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
            }
            serverPlayer.containerMenu.broadcastChanges();
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    private InteractionResult handlePlacement(UseOnContext context, Level level, BlockPos clickedPos, Direction face, Player player, ItemStack stick, BlockState clickedState, StickLock lock) {
        PlacementMaterial material = resolvePlacementMaterial(player, context.getHand(), clickedState);
        if (material == null) {
            if (!player.isCreative()) {
                player.sendOverlayMessage(Component.translatable("purplestick.message.no_item").withStyle(ChatFormatting.RED));
            }
            return InteractionResult.FAIL;
        }

        Item requiredItem = material.item();
        BlockState placeState = material.placeState();

        int maxBlocks = getEffectiveLimit(StickMode.BUILD);
        int available = player.isCreative() ? maxBlocks : Math.min(maxBlocks, countBlockItems(player, requiredItem));
        if (available <= 0) {
            if (!player.isCreative()) {
                player.sendOverlayMessage(Component.translatable("purplestick.message.no_item").withStyle(ChatFormatting.RED));
            }
            return InteractionResult.FAIL;
        }

        List<BlockPos> placementPositions = calculatePlacements(level, clickedPos, face, clickedState, placeState, lock, available);
        if (placementPositions.isEmpty()) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ServerPlayer serverPlayer = (ServerPlayer) player;
        boolean isCreative = player.isCreative();

        int placedCount = 0;
        SoundType soundType = placeState.getSoundType();

        for (BlockPos targetPos : placementPositions) {
            if (!isCreative) {
                if (!consumeBlockItem(serverPlayer, requiredItem)) {
                    break;
                }
            }

            level.setBlock(targetPos, placeState, Block.UPDATE_ALL);
            placedCount++;

            if (!isCreative && PurpleStickConfig.get().durabilityEnabled && stick.isDamageableItem()) {
                OInventoryHelper.damageHeldItem(serverPlayer, context.getHand(), 1);
                if (stick.isEmpty()) {
                    break;
                }
            }
        }

        if (placedCount > 0) {
            if (PurpleStickConfig.get().soundEnabled) {
                level.playSound(null, clickedPos, soundType.getPlaceSound(), SoundSource.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
            }
            serverPlayer.containerMenu.broadcastChanges();
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    public int getEffectiveLimit(StickMode mode) {
        int baseLimit = (this == PurpleStickItems.NETHERITE_STICK)
                ? PurpleStickConfig.get().maxBlocks
                : this.blockLimit;

        if (mode == StickMode.DESTRUCTION) {
            if (this == PurpleStickItems.WOODEN_STICK) return 9;
            if (this == PurpleStickItems.COPPER_STICK) return 25;
            if (this == PurpleStickItems.IRON_STICK) return 49;
            if (this == PurpleStickItems.DIAMOND_STICK) return 64;
            if (this == PurpleStickItems.NETHERITE_STICK) return Math.min(81, baseLimit);
            return Math.min(81, Math.max(9, baseLimit / 4));
        }
        return baseLimit;
    }

    public int getEffectiveLimit() {
        return getEffectiveLimit(StickMode.BUILD);
    }

    public static List<BlockPos> calculatePlacements(Level level, BlockPos origin, Direction face, BlockState surfaceState, BlockState placeState, StickLock lock, int maxBlocks) {
        if (placeState == null || placeState.isAir() || placeState.hasBlockEntity()) {
            return Collections.emptyList(); // Safety: do not duplicate container / block entities
        }

        List<BlockPos> validPlacements = new ArrayList<>();

        if (lock.isArea()) {
            int radius = lock.getAreaRadius();
            for (int u = -radius; u <= radius; u++) {
                for (int v = -radius; v <= radius; v++) {
                    BlockPos current = getPlaneOffset(origin, face, u, v);
                    if (level.isOutsideBuildHeight(current)) continue;

                    if (level.hasChunkAt(current) && !level.getBlockState(current).isAir()) {
                        BlockPos placePos = current.relative(face);
                        if (!level.isOutsideBuildHeight(placePos) && level.getBlockState(placePos).canBeReplaced()) {
                            validPlacements.add(placePos);
                            if (validPlacements.size() >= maxBlocks) return validPlacements;
                        }
                    }
                }
            }
            return validPlacements;
        }

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();

        BlockPos firstPlacePos = origin.relative(face);
        if (!level.isOutsideBuildHeight(firstPlacePos) && level.getBlockState(firstPlacePos).canBeReplaced()) {
            queue.add(origin);
            visited.add(origin);
        }

        Direction[] planeDirs = lock.filterDirections(face);

        while (!queue.isEmpty() && validPlacements.size() < maxBlocks) {
            BlockPos current = queue.poll();
            BlockPos placePos = current.relative(face);

            if (!level.isOutsideBuildHeight(placePos) && level.getBlockState(placePos).canBeReplaced()) {
                validPlacements.add(placePos);
                if (validPlacements.size() >= maxBlocks) break;
            }

            for (Direction dir : planeDirs) {
                mut.setWithOffset(current, dir);
                if (!level.isOutsideBuildHeight(mut) && !visited.contains(mut)) {
                    BlockPos neighbor = mut.immutable();
                    visited.add(neighbor);

                    if (level.hasChunkAt(neighbor) && isMatchingSurface(level.getBlockState(neighbor), surfaceState)) {
                        queue.add(neighbor);
                    }
                }
            }
        }

        return validPlacements;
    }

    public static List<BlockPos> calculateDestruction(Level level, BlockPos origin, Direction face, BlockState targetState, StickLock lock, int maxBlocks) {
        if (targetState == null || targetState.isAir()) {
            return Collections.emptyList();
        }

        List<BlockPos> validDestructions = new ArrayList<>();

        if (lock.isArea()) {
            int radius = lock.getAreaRadius();
            for (int u = -radius; u <= radius; u++) {
                for (int v = -radius; v <= radius; v++) {
                    BlockPos current = getPlaneOffset(origin, face, u, v);
                    if (level.isOutsideBuildHeight(current)) continue;

                    if (level.hasChunkAt(current)) {
                        BlockState state = level.getBlockState(current);
                        if (!state.isAir() && state.getDestroySpeed(level, current) >= 0) {
                            validDestructions.add(current);
                            if (validDestructions.size() >= maxBlocks) return validDestructions;
                        }
                    }
                }
            }
            return validDestructions;
        }

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();

        queue.add(origin);
        visited.add(origin);

        Direction[] planeDirs = lock.filterDirections(face);

        while (!queue.isEmpty() && validDestructions.size() < maxBlocks) {
            BlockPos current = queue.poll();
            if (level.isOutsideBuildHeight(current)) continue;

            BlockState curState = level.getBlockState(current);

            if (isMatchingDestruction(curState, targetState) && curState.getDestroySpeed(level, current) >= 0) {
                validDestructions.add(current);
                if (validDestructions.size() >= maxBlocks) break;

                for (Direction dir : planeDirs) {
                    mut.setWithOffset(current, dir);
                    if (!level.isOutsideBuildHeight(mut) && !visited.contains(mut)) {
                        BlockPos neighbor = mut.immutable();
                        visited.add(neighbor);
                        if (level.hasChunkAt(neighbor) && isMatchingDestruction(level.getBlockState(neighbor), targetState)) {
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        return validDestructions;
    }

    public static BlockPos getPlaneOffset(BlockPos origin, Direction face, int u, int v) {
        if (face.getAxis() == Direction.Axis.Y) {
            return origin.offset(u, 0, v);
        } else if (face.getAxis() == Direction.Axis.X) {
            return origin.offset(0, u, v);
        } else {
            return origin.offset(u, v, 0);
        }
    }

    public static Direction[] getPlaneDirections(Direction face) {
        if (face.getAxis() == Direction.Axis.Y) {
            return new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        } else if (face.getAxis() == Direction.Axis.X) {
            return new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH};
        } else {
            return new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST};
        }
    }

    public static int countBlockItems(Player player, Item item) {
        return OInventoryHelper.countItem(player, item);
    }

    private boolean consumeBlockItem(Player player, Item item) {
        return OInventoryHelper.consumeItem(player, item, 1);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipConsumer, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipConsumer, flag);
        tooltipConsumer.accept(Component.translatable("purplestick.tooltip.limit", getEffectiveLimit()));
        tooltipConsumer.accept(Component.translatable("purplestick.tooltip.mode", getMode(stack).getName()));
        tooltipConsumer.accept(Component.translatable("purplestick.tooltip.lock", getLock(stack).getName()));
        tooltipConsumer.accept(Component.translatable("purplestick.tooltip.open_screen_hint"));
    }
}
