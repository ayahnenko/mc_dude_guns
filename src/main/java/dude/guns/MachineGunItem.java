package dude.guns;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.TintedGlassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NullMarked;

import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@NullMarked
public class MachineGunItem extends Item {
    private static final double BLOCK_TRACE_STEP_EPSILON = 0.01;

    public MachineGunItem(Properties properties) {
        super(properties);
    }

    public static void fireFromPacket(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        InteractionHand hand = getMachineGunHand(player);

        if (hand == null) {
            return;
        }

        ItemStack stack = player.getItemInHand(hand);
        ModConfig.MachineGun config = ModConfig.get().machineGun;

        if (WeaponCooldowns.isCoolingDown(player, stack)) {
            return;
        }

        if (!WeaponAmmo.consumeAmmo(player, config.usesAmmo, ModItems.MACHINE_GUN_ROUND)) {
            WeaponCooldowns.startEmpty(player, stack, config.emptyCooldownTicks);
            return;
        }

        fire(level, player);
        WeaponDurability.hurtItem(level, player, stack, hand, 1);
        WeaponCooldowns.start(player, stack, config.cooldownTicks);
    }

    private static void fire(ServerLevel level, ServerPlayer player) {
        ModConfig.MachineGun config = ModConfig.get().machineGun;
        Vec3 start = player.getEyePosition();
        Vec3 baseDirection = player.getLookAngle().normalize();
        Vec3 direction = randomDirectionInCone(baseDirection, config.spreadDegrees);
        Vec3 end = start.add(direction.scale(config.range));
        Vec3 muzzleStart = getMuzzlePosition(player);

        double maxDistanceSquared = config.range * config.range;
        Optional<Vec3> blockImpact = Optional.empty();
        Vec3 blockTraceStart = start;

        spawnMuzzleParticles(level, muzzleStart, baseDirection);
        TemporaryMuzzleLights.flash(level, player, muzzleStart);

        while (true) {
            BlockHitResult colliderHit = level.clip(new ClipContext(
                    blockTraceStart,
                    end,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player
            ));

            BlockHitResult outlineHit = level.clip(new ClipContext(
                    blockTraceStart,
                    end,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    player
            ));

            if (isHitBeforeCollider(start, outlineHit, colliderHit)
                    && breakFragileBlock(level, player, outlineHit.getBlockPos())) {
                blockTraceStart = outlineHit.getLocation().add(direction.scale(BLOCK_TRACE_STEP_EPSILON));
                continue;
            }

            if (colliderHit.getType() == HitResult.Type.MISS) {
                break;
            }

            if (breakFragileBlock(level, player, colliderHit.getBlockPos())) {
                blockTraceStart = colliderHit.getLocation().add(direction.scale(BLOCK_TRACE_STEP_EPSILON));
                continue;
            }

            maxDistanceSquared = start.distanceToSqr(colliderHit.getLocation());
            blockImpact = Optional.of(colliderHit.getLocation());
            break;
        }

        Optional<MachineGunHit> hit = findEntityHit(level, player, start, end, maxDistanceSquared);

        if (hit.isPresent()) {
            MachineGunHit machineGunHit = hit.get();
            spawnEntityHitParticles(level, machineGunHit.position());

            machineGunHit.target().hurtServer(
                    level,
                    level.damageSources().playerAttack(player),
                    config.damage
            );
        } else {
            blockImpact.ifPresent(position -> spawnBlockImpactParticles(level, position));
        }

        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.FIREWORK_ROCKET_BLAST,
                SoundSource.PLAYERS,
                0.35f,
                1.65f
        );
    }

    private static boolean isHitBeforeCollider(Vec3 start, BlockHitResult outlineHit, BlockHitResult colliderHit) {
        if (outlineHit.getType() == HitResult.Type.MISS) {
            return false;
        }

        if (colliderHit.getType() == HitResult.Type.MISS) {
            return true;
        }

        return start.distanceToSqr(outlineHit.getLocation()) <= start.distanceToSqr(colliderHit.getLocation());
    }

    private static boolean breakFragileBlock(ServerLevel level, ServerPlayer user, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (!isFragileMachineGunBlock(state)) {
            return false;
        }

        return level.destroyBlock(pos, false, user, 512);
    }

    private static boolean isFragileMachineGunBlock(BlockState state) {
        Block block = state.getBlock();

        return block == Blocks.GLASS
                || block == Blocks.GLASS_PANE
                || block == Blocks.SHORT_GRASS
                || block == Blocks.TALL_GRASS
                || block == Blocks.FERN
                || block == Blocks.LARGE_FERN
                || block == Blocks.DEAD_BUSH
                || block == Blocks.BUSH
                || block == Blocks.SHORT_DRY_GRASS
                || block == Blocks.TALL_DRY_GRASS
                || block == Blocks.LEAF_LITTER
                || block == Blocks.FIREFLY_BUSH
                || block instanceof StainedGlassBlock
                || block instanceof StainedGlassPaneBlock
                || block instanceof TintedGlassBlock
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.CROPS)
                || state.is(BlockTags.CLIMBABLE)
                || state.is(BlockTags.CAVE_VINES)
                || state.is(BlockTags.CORALS)
                || state.is(BlockTags.REPLACEABLE_BY_TREES);
    }

    private static Vec3 getMuzzlePosition(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 forward = player.getLookAngle().normalize();

        return eye
                .add(forward.scale(0.8))
                .add(0.0, -0.45, 0.0);
    }

    private static void spawnMuzzleParticles(ServerLevel level, Vec3 muzzleStart, Vec3 direction) {
        ModConfig.MuzzleParticles config = ModConfig.get().muzzleParticles;
        Vec3 front = muzzleStart.add(direction.scale(0.25));

        level.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                front.x,
                front.y,
                front.z,
                config.sparkCount,
                config.sparkSpread,
                config.sparkSpread,
                config.sparkSpread,
                config.sparkSpeed
        );

        level.sendParticles(
                ParticleTypes.SMOKE,
                muzzleStart.x,
                muzzleStart.y,
                muzzleStart.z,
                config.smokeCount,
                config.smokeSpreadX,
                config.smokeSpreadY,
                config.smokeSpreadZ,
                config.smokeSpeed
        );
    }

    private static Vec3 randomDirectionInCone(Vec3 forward, double spreadDegrees) {
        if (spreadDegrees <= 0.0) {
            return forward;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        double maxAngleRadians = Math.toRadians(spreadDegrees);
        double radius = Math.tan(maxAngleRadians) * Math.sqrt(random.nextDouble());
        double angle = random.nextDouble() * Math.PI * 2.0;

        Vec3 worldUp = new Vec3(0.0, 1.0, 0.0);
        Vec3 right = forward.cross(worldUp);

        if (right.lengthSqr() < 1.0E-6) {
            right = new Vec3(1.0, 0.0, 0.0);
        } else {
            right = right.normalize();
        }

        Vec3 up = right.cross(forward).normalize();
        Vec3 offset = right.scale(Math.cos(angle) * radius)
                .add(up.scale(Math.sin(angle) * radius));

        return forward.add(offset).normalize();
    }

    private static void spawnEntityHitParticles(ServerLevel level, Vec3 position) {
        level.sendParticles(
                ParticleTypes.DAMAGE_INDICATOR,
                position.x,
                position.y,
                position.z,
                1,
                0.05,
                0.05,
                0.05,
                0.02
        );
    }

    private static void spawnBlockImpactParticles(ServerLevel level, Vec3 position) {
        level.sendParticles(
                ParticleTypes.POOF,
                position.x,
                position.y,
                position.z,
                1,
                0.04,
                0.04,
                0.04,
                0.01
        );
    }

    private static Optional<MachineGunHit> findEntityHit(
            ServerLevel level,
            ServerPlayer player,
            Vec3 start,
            Vec3 end,
            double maxDistanceSquared
    ) {
        Vec3 direction = end.subtract(start).normalize();
        AABB searchBox = player.getBoundingBox()
                .expandTowards(direction.scale(ModConfig.get().machineGun.range))
                .inflate(1.0);

        return level.getEntitiesOfClass(LivingEntity.class, searchBox, entity ->
                        entity != player
                                && entity.isAlive()
                                && entity.isPickable()
                )
                .stream()
                .map(entity -> {
                    AABB hitBox = entity.getBoundingBox().inflate(entity.getPickRadius() + 0.25);
                    Optional<Vec3> hitPosition = hitBox.clip(start, end);

                    if (hitPosition.isEmpty()) {
                        return null;
                    }

                    Vec3 position = hitPosition.get();
                    double distanceSquared = start.distanceToSqr(position);

                    if (distanceSquared > maxDistanceSquared) {
                        return null;
                    }

                    return new MachineGunHit(entity, position, distanceSquared);
                })
                .filter(hit -> hit != null)
                .min(Comparator.comparingDouble(MachineGunHit::distanceSquared));
    }

    private static InteractionHand getMachineGunHand(ServerPlayer player) {
        if (player.getMainHandItem().is(ModItems.MACHINE_GUN)) {
            return InteractionHand.MAIN_HAND;
        }

        if (player.getOffhandItem().is(ModItems.MACHINE_GUN)) {
            return InteractionHand.OFF_HAND;
        }

        return null;
    }

    private record MachineGunHit(LivingEntity target, Vec3 position, double distanceSquared) {
    }
}
