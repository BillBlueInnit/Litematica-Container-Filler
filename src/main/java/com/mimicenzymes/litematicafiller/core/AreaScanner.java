package com.mimicenzymes.litematicafiller.core;

import com.mimicenzymes.litematicafiller.config.Configs;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AreaScanner {
    private static final Map<BlockPos, Long> ATTEMPT_COOLDOWNS = new HashMap<>();

    private static class PendingTask {
        final BlockPos pos;
        final Map<Integer, ItemStack> required;
        final double distSq;

        PendingTask(BlockPos pos, Map<Integer, ItemStack> required, double distSq) {
            this.pos = pos;
            this.required = required;
            this.distSq = distSq;
        }
    }

    public static void executeScan(Minecraft mc, boolean isSilentPrinter) {
        if (mc.player == null || mc.level == null) return;

        var schematicWorld = SchematicWorldHandler.getSchematicWorld();
        if (schematicWorld == null) {
            if (!isSilentPrinter) mc.gui.setOverlayMessage(Component.translatable("litematica_container_filler.message.no_schematic_world"), true);
            return;
        }

        BlockPos center = mc.player.blockPosition();
        int r = Configs.FILL_RADIUS.getIntegerValue();
        boolean syncLayer = Configs.SYNC_LITE_LAYER.getBooleanValue();
        long now = System.currentTimeMillis();

        int maxTasks = isSilentPrinter ? 15 : 40;

        List<PendingTask> pendingTasks = new ArrayList<>();
        Set<BlockPos> processedPositions = new HashSet<>();

        double reach = mc.player.blockInteractionRange();
        double reachSq = (reach + 0.5) * (reach + 0.5);
        Vec3 eyePos = mc.player.getEyePosition();

        if (r == 0) {
            for (BlockPos rawPos : com.mimicenzymes.litematicafiller.render.HighlightScanner.getHighlights().keySet()) {
                BlockState state = schematicWorld.getBlockState(rawPos);
                if (state == null || state.isAir() || !state.hasBlockEntity()) continue;

                BlockPos[] halves = LitematicaContainerReader.getDoubleContainerHalves(schematicWorld, rawPos, state);
                BlockPos taskPos = halves != null ? halves[0] : rawPos;

                if (!processedPositions.add(taskPos)) continue;

                if (eyePos.distanceToSqr(Vec3.atCenterOf(taskPos)) > reachSq) continue;

                if (isSilentPrinter && ATTEMPT_COOLDOWNS.containsKey(taskPos) && now - ATTEMPT_COOLDOWNS.get(taskPos) < 5000) {
                    continue;
                }

                Map<Integer, ItemStack> required = LitematicaContainerReader.getRequiredItems(taskPos, mc.level.registryAccess());
                boolean isCrafter = state.getBlock() instanceof net.minecraft.world.level.block.CrafterBlock;
                boolean needsLocking = isCrafter && LitematicaContainerReader.doesCrafterNeedLocking(taskPos, mc);
                boolean hasItems = required != null && !required.isEmpty() && !RealContainerCache.isSatisfied(taskPos, required);

                if (!hasItems && !needsLocking) continue;

                pendingTasks.add(new PendingTask(taskPos, required == null ? new HashMap<>() : required, taskPos.distSqr(center)));
            }
        } else {
            for (int x = -r; x <= r; x++) {
                for (int y = -r; y <= r; y++) {
                    for (int z = -r; z <= r; z++) {
                        BlockPos rawPos = center.offset(x, y, z);

                        if (syncLayer && !fi.dy.masa.litematica.data.DataManager.getRenderLayerRange().isPositionWithinRange(rawPos)) continue;

                        BlockState state = schematicWorld.getBlockState(rawPos);
                        if (state.isAir() || !state.hasBlockEntity()) continue;

                        BlockPos[] halves = LitematicaContainerReader.getDoubleContainerHalves(schematicWorld, rawPos, state);
                        BlockPos taskPos = halves != null ? halves[0] : rawPos;

                        if (!processedPositions.add(taskPos)) continue;

                        if (eyePos.distanceToSqr(Vec3.atCenterOf(taskPos)) > reachSq) continue;

                        if (isSilentPrinter && ATTEMPT_COOLDOWNS.containsKey(taskPos) && now - ATTEMPT_COOLDOWNS.get(taskPos) < 5000) {
                            continue;
                        }

                        Map<Integer, ItemStack> required = LitematicaContainerReader.getRequiredItems(taskPos, mc.level.registryAccess());

                        boolean isCrafter = state.getBlock() instanceof net.minecraft.world.level.block.CrafterBlock;
                        boolean needsLocking = isCrafter && LitematicaContainerReader.doesCrafterNeedLocking(taskPos, mc);
                        boolean hasItems = required != null && !required.isEmpty() && !RealContainerCache.isSatisfied(taskPos, required);

                        if (!hasItems && !needsLocking) continue;

                        pendingTasks.add(new PendingTask(taskPos, required == null ? new HashMap<>() : required, taskPos.distSqr(center)));
                    }
                }
            }
        }

        pendingTasks.sort(Comparator.comparingDouble(t -> t.distSq));

        int count = 0;
        for (PendingTask task : pendingTasks) {
            AutoFillerStateMachine.getInstance().addTask(task.pos, task.required);
            ATTEMPT_COOLDOWNS.put(task.pos, now);
            count++;
            if (count >= maxTasks) break;
        }

        if (!isSilentPrinter) {
            if (count > 0) {
                mc.gui.setOverlayMessage(Component.translatable("litematica_container_filler.message.scan_start", count), true);
            } else {
                mc.gui.setOverlayMessage(Component.translatable("litematica_container_filler.message.no_requirements"), true);
            }
        }
    }
}