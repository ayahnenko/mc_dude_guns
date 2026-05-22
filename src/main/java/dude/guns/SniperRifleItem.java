package dude.guns;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NullMarked;

import java.util.Comparator;
import java.util.Optional;

@NullMarked
public class SniperRifleItem extends Item {
    public SniperRifleItem(Properties properties) {
        super(properties);
    }

    public static void fireFromPacket(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        InteractionHand hand = getSniperHand(player);

        if (hand == null) {
            return;
        }

        ItemStack stack = player.getItemInHand(hand);

        ModConfig.SniperRifle config = ModConfig.get().sniperRifle;

        if (WeaponCooldowns.isCoolingDown(player, stack)) {
            return;
        }

        if (!WeaponAmmo.consumeAmmo(player, config.usesAmmo, ModItems.SNIPER_ROUND)) {
            WeaponCooldowns.startEmpty(player, stack, config.emptyCooldownTicks);

            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    ModSounds.SHOTGUN_EMPTY,
                    SoundSource.PLAYERS,
                    0.8f,
                    1.0f
            );
            return;
        }

        fire(level, player);
        WeaponDurability.hurtItem(level, player, stack, hand, 1);
        WeaponCooldowns.start(player, stack, config.cooldownTicks);
    }

    private static void fire(ServerLevel level, ServerPlayer player) {
        ModConfig.SniperRifle config = ModConfig.get().sniperRifle;
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 end = start.add(direction.scale(config.range));

        BlockHitResult blockHit = level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));

        double maxDistanceSquared = config.range * config.range;
        Vec3 impactPosition = end;

        if (blockHit.getType() != HitResult.Type.MISS) {
            maxDistanceSquared = start.distanceToSqr(blockHit.getLocation());
            impactPosition = blockHit.getLocation();
        }

        Optional<SniperHit> hit = findEntityHit(level, player, start, end, maxDistanceSquared);

        if (hit.isPresent()) {
            SniperHit sniperHit = hit.get();
            impactPosition = sniperHit.position();
            spawnEntityHitParticles(level, impactPosition);

            sniperHit.target().hurtServer(
                    level,
                    level.damageSources().playerAttack(player),
                    config.damage
            );
        } else if (blockHit.getType() != HitResult.Type.MISS) {
            spawnBlockImpactParticles(level, impactPosition);
        }

        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS,
                0.45f,
                1.7f
        );
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

    private static Optional<SniperHit> findEntityHit(
            ServerLevel level,
            ServerPlayer player,
            Vec3 start,
            Vec3 end,
            double maxDistanceSquared
    ) {
        Vec3 direction = end.subtract(start).normalize();
        AABB searchBox = player.getBoundingBox()
                .expandTowards(direction.scale(ModConfig.get().sniperRifle.range))
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

                    return new SniperHit(entity, position, distanceSquared);
                })
                .filter(hit -> hit != null)
                .min(Comparator.comparingDouble(SniperHit::distanceSquared));
    }

    private static InteractionHand getSniperHand(ServerPlayer player) {
        if (player.getMainHandItem().is(ModItems.SNIPER_RIFLE)) {
            return InteractionHand.MAIN_HAND;
        }

        if (player.getOffhandItem().is(ModItems.SNIPER_RIFLE)) {
            return InteractionHand.OFF_HAND;
        }

        return null;
    }

    private record SniperHit(LivingEntity target, Vec3 position, double distanceSquared) {
    }
}
