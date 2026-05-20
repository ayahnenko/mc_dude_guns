package dude.guns;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class TemporaryMuzzleLights {
    private static final int BLOCK_UPDATE_FLAGS = 3;

    private static final Map<ServerLevel, Map<BlockPos, Long>> ACTIVE_LIGHTS = new HashMap<>();

    private TemporaryMuzzleLights() {
    }

    public static void initialize() {
        ServerTickEvents.END_LEVEL_TICK.register(TemporaryMuzzleLights::tickLevel);
    }

    public static void flash(ServerLevel level, Player player, Vec3 muzzlePosition) {
        ModConfig.MuzzleLight config = ModConfig.get().muzzleLight;

        if (config.lightLevel <= 0 || config.lifetimeTicks <= 0) {
            return;
        }

        BlockPos pos = findLightPosition(level, player, muzzlePosition);

        if (pos == null) {
            return;
        }

        BlockState lightState = Blocks.LIGHT.defaultBlockState()
                .setValue(LightBlock.LEVEL, config.lightLevel);

        level.setBlock(pos, lightState, BLOCK_UPDATE_FLAGS);

        ACTIVE_LIGHTS
                .computeIfAbsent(level, _ -> new HashMap<>())
                .put(pos.immutable(), level.getGameTime() + config.lifetimeTicks);
    }

    private static BlockPos findLightPosition(ServerLevel level, Player player, Vec3 muzzlePosition) {
        BlockPos muzzleBlock = BlockPos.containing(muzzlePosition);

        if (canPlaceLight(level, muzzleBlock)) {
            return muzzleBlock;
        }

        BlockPos eyeBlock = BlockPos.containing(player.getEyePosition());

        if (canPlaceLight(level, eyeBlock)) {
            return eyeBlock;
        }

        BlockPos bodyBlock = player.blockPosition();

        if (canPlaceLight(level, bodyBlock)) {
            return bodyBlock;
        }

        return null;
    }

    private static boolean canPlaceLight(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || isManagedLight(level, pos);
    }

    private static boolean isManagedLight(ServerLevel level, BlockPos pos) {
        Map<BlockPos, Long> levelLights = ACTIVE_LIGHTS.get(level);
        return levelLights != null && levelLights.containsKey(pos);
    }

    private static void tickLevel(ServerLevel level) {
        Map<BlockPos, Long> levelLights = ACTIVE_LIGHTS.get(level);

        if (levelLights == null) {
            return;
        }

        long now = level.getGameTime();
        Iterator<Map.Entry<BlockPos, Long>> iterator = levelLights.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Long> entry = iterator.next();

            if (entry.getValue() > now) {
                continue;
            }

            BlockPos pos = entry.getKey();

            if (level.getBlockState(pos).getBlock() == Blocks.LIGHT) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), BLOCK_UPDATE_FLAGS);
            }

            iterator.remove();
        }

        if (levelLights.isEmpty()) {
            ACTIVE_LIGHTS.remove(level);
        }
    }
}
