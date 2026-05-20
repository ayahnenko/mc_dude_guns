package dude.guns;

import dude.guns.network.ShotgunRecoilPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@NullMarked
public class ShotgunItem extends Item {
    private static final double BLOCK_TRACE_STEP_EPSILON = 0.01;

    public ShotgunItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player user, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.PASS;
        }

        if (!consumeShell(user)) {
            level.playSound(
                    null,
                    user.getX(),
                    user.getY(),
                    user.getZ(),
                    ModSounds.SHOTGUN_EMPTY,
                    SoundSource.PLAYERS,
                    0.8f,
                    1.0f
            );

            if (user instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.literal("No shotgun shells!"));
            }

            return InteractionResult.FAIL;
        }

        if (level instanceof ServerLevel serverLevel) {
            fireShotgun(serverLevel, user);
            serverLevel.playSound(
                    null,
                    user.getX(),
                    user.getY(),
                    user.getZ(),
                    ModSounds.SHOTGUN_FIRE,
                    SoundSource.PLAYERS,
                    1.0f,
                    1.0f
            );

            if (user instanceof ServerPlayer serverPlayer) {
                ServerPlayNetworking.send(serverPlayer, new ShotgunRecoilPayload());
                damageShotgun(serverLevel, serverPlayer, hand);
            }
        }

        user.getCooldowns().addCooldown(user.getItemInHand(hand), ModConfig.get().shotgun.cooldownTicks);

        return InteractionResult.SUCCESS;
    }

    private void fireShotgun(ServerLevel level, Player user) {
        Vec3 start = user.getEyePosition();
        Vec3 baseDirection = user.getLookAngle().normalize();
        Vec3 muzzleStart = getMuzzlePosition(user);

        spawnMuzzleParticles(level, muzzleStart, baseDirection);
        TemporaryMuzzleLights.flash(level, user, muzzleStart);

        Map<LivingEntity, Float> damageByTarget = new HashMap<>();
        Map<LivingEntity, Vec3> hitPositionByTarget = new HashMap<>();
        int pelletHits = 0;

        ModConfig.Shotgun config = ModConfig.get().shotgun;

        for (int i = 0; i < config.pelletCount; i++) {
            Vec3 direction = randomDirectionInCone(baseDirection);
            PelletTrace trace = tracePellet(level, user, start, direction);

            if (trace.hits().isEmpty()) {
                trace.blockImpact().ifPresent(position -> spawnBlockImpactParticles(level, position));
                continue;
            }

            for (int hitIndex = 0; hitIndex < trace.hits().size(); hitIndex++) {
                PelletHit hit = trace.hits().get(hitIndex);

                spawnEntityHitParticles(level, hit.position());

                float damage = calculateDamage(hit.distance());

                // После каждого пробития pellet теряет часть энергии.
                damage *= (float) Math.pow(config.pierceDamageMultiplier, hitIndex);

                damageByTarget.merge(hit.target(), damage, Float::sum);
                hitPositionByTarget.putIfAbsent(hit.target(), hit.position());
                pelletHits++;
            }

            trace.blockImpact().ifPresent(position -> spawnBlockImpactParticles(level, position));
        }

        for (Map.Entry<LivingEntity, Float> entry : damageByTarget.entrySet()) {
            LivingEntity target = entry.getKey();
            float totalDamage = entry.getValue();

            boolean damaged = target.hurtServer(
                    level,
                    level.damageSources().playerAttack(user),
                    entry.getValue()
            );

            if (!damaged) {
                continue;
            }

            applyShotgunKnockback(user, target, totalDamage);

            Vec3 hitPosition = hitPositionByTarget.getOrDefault(
                    target,
                    target.position()
            );

            level.playSound(
                    null,
                    hitPosition.x,
                    hitPosition.y,
                    hitPosition.z,
                    SoundEvents.GENERIC_HURT,
                    SoundSource.PLAYERS,
                    0.8f,
                    1.2f
            );
        }

        Guns.LOGGER.info(
                "Bang! Pellet hits: {}, targets hit: {}",
                pelletHits,
                damageByTarget.size()
        );
    }

    private boolean consumeShell(Player user) {
        // В creative не тратим патроны.
        if (user.getAbilities().instabuild) {
            return true;
        }

        Inventory inventory = user.getInventory();

        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);

            if (stack.is(ModItems.SHOTGUN_SHELL)) {
                stack.shrink(1);
                return true;
            }
        }

        return false;
    }

    private PelletTrace tracePellet(ServerLevel level, Player user, Vec3 start, Vec3 direction) {
        ModConfig.Shotgun config = ModConfig.get().shotgun;
        Vec3 maxEnd = start.add(direction.scale(config.range));

        double maxDistanceSquared = config.range * config.range;
        double visualDistance = config.range;
        Optional<Vec3> blockImpact = Optional.empty();
        Vec3 blockTraceStart = start;

        // Сначала проверяем блоки, чтобы дробь не летела сквозь стены.
        // Хрупкие блоки пробиваются и не останавливают дробину.
        while (true) {
            BlockHitResult colliderHit = level.clip(new ClipContext(
                    blockTraceStart,
                    maxEnd,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    user
            ));

            BlockHitResult outlineHit = level.clip(new ClipContext(
                    blockTraceStart,
                    maxEnd,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    user
            ));

            if (isHitBeforeCollider(start, outlineHit, colliderHit)
                    && breakFragileBlock(level, user, outlineHit.getBlockPos())) {
                blockTraceStart = outlineHit.getLocation().add(direction.scale(BLOCK_TRACE_STEP_EPSILON));
                continue;
            }

            if (colliderHit.getType() == HitResult.Type.MISS) {
                break;
            }

            if (breakFragileBlock(level, user, colliderHit.getBlockPos())) {
                blockTraceStart = colliderHit.getLocation().add(direction.scale(BLOCK_TRACE_STEP_EPSILON));
                continue;
            }

            maxDistanceSquared = start.distanceToSqr(colliderHit.getLocation());
            visualDistance = Math.sqrt(maxDistanceSquared);
            blockImpact = Optional.of(colliderHit.getLocation());
            break;
        }

        AABB searchBox = user.getBoundingBox()
                .expandTowards(direction.scale(config.range))
                .inflate(1.0);

        List<PelletHit> hits = new ArrayList<>();

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, searchBox, entity ->
                entity != user
                        && entity.isAlive()
                        && entity.isPickable()
        )) {
            AABB hitBox = entity.getBoundingBox().inflate(entity.getPickRadius() + 0.25);
            Optional<Vec3> hitPosition = hitBox.clip(start, maxEnd);

            if (hitPosition.isEmpty()) {
                continue;
            }

            Vec3 currentHitPosition = hitPosition.get();
            double distanceSquared = start.distanceToSqr(currentHitPosition);

            // Если за блоком — не засчитываем.
            if (distanceSquared > maxDistanceSquared) {
                continue;
            }

            hits.add(new PelletHit(
                    entity,
                    currentHitPosition,
                    Math.sqrt(distanceSquared)
            ));
        }

        hits.sort(Comparator.comparingDouble(PelletHit::distance));

        if (hits.size() > config.maxTargetsPerPellet) {
            hits = hits.subList(0, config.maxTargetsPerPellet);
        }

        return new PelletTrace(hits, blockImpact, visualDistance);
    }

    private boolean isHitBeforeCollider(Vec3 start, BlockHitResult outlineHit, BlockHitResult colliderHit) {
        if (outlineHit.getType() == HitResult.Type.MISS) {
            return false;
        }

        if (colliderHit.getType() == HitResult.Type.MISS) {
            return true;
        }

        return start.distanceToSqr(outlineHit.getLocation()) <= start.distanceToSqr(colliderHit.getLocation());
    }

    private boolean breakFragileBlock(ServerLevel level, Player user, BlockPos pos) {
        ModConfig.Shotgun config = ModConfig.get().shotgun;

        if (!config.breakFragileBlocks) {
            return false;
        }

        BlockState state = level.getBlockState(pos);

        if (!isFragileShotgunBlock(state)) {
            return false;
        }

        return level.destroyBlock(pos, config.dropBrokenFragileBlocks, user, 512);
    }

    private boolean isFragileShotgunBlock(BlockState state) {
        Block block = state.getBlock();

        return block == Blocks.GLASS
                || block == Blocks.GLASS_PANE
                || block instanceof StainedGlassBlock
                || block instanceof StainedGlassPaneBlock
                || block instanceof TintedGlassBlock
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.CROPS)
                || state.is(BlockTags.CLIMBABLE)
                || state.is(BlockTags.CAVE_VINES)
                || state.is(BlockTags.CANDLES)
                || state.is(BlockTags.FLOWER_POTS)
                || state.is(BlockTags.CORALS)
                || state.is(BlockTags.REPLACEABLE_BY_TREES);
    }

    private float calculateDamage(double distance) {
        ModConfig.Shotgun config = ModConfig.get().shotgun;
        double distanceRatio = Math.clamp(distance / config.range, 0.0, 1.0);

        double multiplier = 1.0 - distanceRatio;
        multiplier = Math.max(multiplier, config.minDamageMultiplier);

        return (float) (config.maxDamagePerPellet * multiplier);
    }

    private Vec3 getMuzzlePosition(Player user) {
        Vec3 eye = user.getEyePosition();
        Vec3 forward = user.getLookAngle().normalize();

        return eye
                .add(forward.scale(0.8))  // чуть впереди игрока
                .add(0.0, -0.45, 0.0);    // ниже глаз, примерно уровень рук/оружия
    }

    private void spawnMuzzleParticles(ServerLevel level, Vec3 muzzleStart, Vec3 direction) {
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

    private Vec3 randomDirectionInCone(Vec3 forward) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        double maxAngleRadians = Math.toRadians(ModConfig.get().shotgun.spreadDegrees);

        // sqrt нужен, чтобы распределение по площади конуса было ровнее,
        // а не кучковалось в центре.
        double radius = Math.tan(maxAngleRadians) * Math.sqrt(random.nextDouble());
        double angle = random.nextDouble() * Math.PI * 2.0;

        Vec3 worldUp = new Vec3(0.0, 1.0, 0.0);
        Vec3 right = forward.cross(worldUp);

        // Если игрок смотрит почти строго вверх/вниз, cross может стать почти нулём.
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

    private void damageShotgun(ServerLevel level, ServerPlayer player, InteractionHand hand) {
        if (player.getAbilities().instabuild) {
            return;
        }

        ItemStack stack = player.getItemInHand(hand);

        EquipmentSlot slot = hand == InteractionHand.MAIN_HAND
                ? EquipmentSlot.MAINHAND
                : EquipmentSlot.OFFHAND;

        stack.hurtAndBreak(
                1,
                level,
                player,
                item -> player.onEquippedItemBroken(item, slot)
        );
    }

    private void spawnEntityHitParticles(ServerLevel level, Vec3 position) {
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

    private void spawnBlockImpactParticles(ServerLevel level, Vec3 position) {
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

    private void applyShotgunKnockback(Player user, LivingEntity target, float totalDamage) {
        ModConfig.Shotgun config = ModConfig.get().shotgun;
        double strength = totalDamage * config.knockbackPerDamage;
        strength = Math.clamp(strength, config.minKnockback, config.maxKnockback);

        target.knockback(
                strength,
                user.getX() - target.getX(),
                user.getZ() - target.getZ()
        );
    }

    private record PelletTrace(List<PelletHit> hits, Optional<Vec3> blockImpact, double visualDistance) {
    }

    private record PelletHit(LivingEntity target, Vec3 position, double distance) {
    }
}
