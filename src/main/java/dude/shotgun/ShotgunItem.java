package dude.shotgun;

import dude.shotgun.network.ShotgunRecoilPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
    private static final int PELLET_COUNT = 12;
    private static final double RANGE = 16.0;
    private static final double SPREAD_DEGREES = 7.0;

    // 8 pellets × 4 damage = 24 damage, то есть зомби вблизи должен умереть.
    private static final float MAX_DAMAGE_PER_PELLET = 4.0f;
    private static final float MIN_DAMAGE_MULTIPLIER = 0.15f;

    private static final int COOLDOWN_TICKS = 20;

    private static final double KNOCKBACK_PER_DAMAGE = 0.04;
    private static final double MIN_KNOCKBACK = 0.25;
    private static final double MAX_KNOCKBACK = 1.2;

    private static final int MAX_TARGETS_PER_PELLET = 3;
    private static final float PIERCE_DAMAGE_MULTIPLIER = 0.55f;

    public ShotgunItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player user, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.PASS;
        }

        if (!consumeShell(user)) {
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

        user.getCooldowns().addCooldown(user.getItemInHand(hand), COOLDOWN_TICKS);

        return InteractionResult.SUCCESS;
    }

    private void fireShotgun(ServerLevel level, Player user) {
        Vec3 start = user.getEyePosition();
        Vec3 baseDirection = user.getLookAngle().normalize();
        Vec3 muzzleStart = getMuzzlePosition(user);

        spawnMuzzleParticles(level, muzzleStart, baseDirection);

        Map<LivingEntity, Float> damageByTarget = new HashMap<>();
        Map<LivingEntity, Vec3> hitPositionByTarget = new HashMap<>();
        int pelletHits = 0;

        for (int i = 0; i < PELLET_COUNT; i++) {
            Vec3 direction = randomDirectionInCone(baseDirection);
            PelletTrace trace = tracePellet(level, user, start, direction);

            spawnPelletTracers(level, start, direction, trace.visualDistance());
            if (trace.hits().isEmpty()) {
                trace.blockImpact().ifPresent(position -> spawnBlockImpactParticles(level, position));
                continue;
            }

            for (int hitIndex = 0; hitIndex < trace.hits().size(); hitIndex++) {
                PelletHit hit = trace.hits().get(hitIndex);

                spawnEntityHitParticles(level, hit.position());

                float damage = calculateDamage(hit.distance());

                // После каждого пробития pellet теряет часть энергии.
                damage *= (float) Math.pow(PIERCE_DAMAGE_MULTIPLIER, hitIndex);

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

        Shotgun.LOGGER.info(
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
        Vec3 maxEnd = start.add(direction.scale(RANGE));

        // Сначала проверяем блоки, чтобы дробь не летела сквозь стены.
        BlockHitResult blockHit = level.clip(new ClipContext(
                start,
                maxEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                user
        ));

        double maxDistanceSquared = RANGE * RANGE;
        double visualDistance = RANGE;
        Optional<Vec3> blockImpact = Optional.empty();

        if (blockHit.getType() != HitResult.Type.MISS) {
            maxDistanceSquared = start.distanceToSqr(blockHit.getLocation());
            visualDistance = Math.sqrt(maxDistanceSquared);
            blockImpact = Optional.of(blockHit.getLocation());
        }

        AABB searchBox = user.getBoundingBox()
                .expandTowards(direction.scale(RANGE))
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

        if (hits.size() > MAX_TARGETS_PER_PELLET) {
            hits = hits.subList(0, MAX_TARGETS_PER_PELLET);
        }

        return new PelletTrace(hits, blockImpact, visualDistance);
    }

    private float calculateDamage(double distance) {
        double distanceRatio = Math.clamp(distance / RANGE, 0.0, 1.0);

        double multiplier = 1.0 - distanceRatio;
        multiplier = Math.max(multiplier, MIN_DAMAGE_MULTIPLIER);

        return (float) (MAX_DAMAGE_PER_PELLET * multiplier);
    }

    private Vec3 getMuzzlePosition(Player user) {
        Vec3 eye = user.getEyePosition();
        Vec3 forward = user.getLookAngle().normalize();

        return eye
                .add(forward.scale(0.8))  // чуть впереди игрока
                .add(0.0, -0.45, 0.0);    // ниже глаз, примерно уровень рук/оружия
    }

    private void spawnMuzzleParticles(ServerLevel level, Vec3 muzzleStart, Vec3 direction) {
        Vec3 front = muzzleStart.add(direction.scale(0.25));

        // Короткая яркая вспышка у ствола.
        level.sendParticles(
                ParticleTypes.FLAME,
                front.x,
                front.y,
                front.z,
                2,
                0.08,
                0.08,
                0.08,
                0.02
        );

        // Жирный короткий дымок.
        level.sendParticles(
                ParticleTypes.POOF,
                muzzleStart.x,
                muzzleStart.y,
                muzzleStart.z,
                2,
                0.12,
                0.08,
                0.12,
                0.01
        );
    }

    private void spawnPelletTracers(ServerLevel level, Vec3 start, Vec3 direction, double distanceLimit) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (double distance = 1.5; distance <= distanceLimit; distance += 2.5) {
            // Трассеры редкие, чтобы не было стены дыма.
            if (random.nextDouble() > 0.18) {
                continue;
            }

            Vec3 pos = start.add(direction.scale(distance));

            level.sendParticles(
                    random.nextBoolean() ? ParticleTypes.CRIT : ParticleTypes.FIREWORK,
                    pos.x,
                    pos.y,
                    pos.z,
                    1,
                    0.01,
                    0.01,
                    0.01,
                    0.0
            );
        }
    }

    private Vec3 randomDirectionInCone(Vec3 forward) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        double maxAngleRadians = Math.toRadians(ShotgunItem.SPREAD_DEGREES);

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
        double strength = totalDamage * KNOCKBACK_PER_DAMAGE;
        strength = Math.clamp(strength, MIN_KNOCKBACK, MAX_KNOCKBACK);

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