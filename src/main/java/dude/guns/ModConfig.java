package dude.guns;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_RESOURCE_PATH = "/guns.json";

    private static ModConfig INSTANCE = new ModConfig();

    public Shotgun shotgun = new Shotgun();
    public SniperRifle sniperRifle = new SniperRifle();
    public MuzzleParticles muzzleParticles = new MuzzleParticles();
    public MuzzleLight muzzleLight = new MuzzleLight();

    private ModConfig() {
    }

    public static void initialize() {
        load();
    }

    public static ModConfig get() {
        return INSTANCE;
    }

    private static void load() {
        InputStream stream = ModConfig.class.getResourceAsStream(CONFIG_RESOURCE_PATH);

        if (stream == null) {
            Guns.LOGGER.warn("Bundled config {} was not found, using defaults", CONFIG_RESOURCE_PATH);
            INSTANCE = new ModConfig();
            return;
        }

        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            ModConfig config = GSON.fromJson(reader, ModConfig.class);
            INSTANCE = config == null ? new ModConfig() : config;
        } catch (IOException | RuntimeException exception) {
            Guns.LOGGER.warn("Failed to load bundled config {}, using defaults", CONFIG_RESOURCE_PATH, exception);
            INSTANCE = new ModConfig();
        }

        INSTANCE.sanitize();
    }

    private void sanitize() {
        if (shotgun == null) {
            shotgun = new Shotgun();
        }

        if (muzzleParticles == null) {
            muzzleParticles = new MuzzleParticles();
        }

        if (sniperRifle == null) {
            sniperRifle = new SniperRifle();
        }

        if (muzzleLight == null) {
            muzzleLight = new MuzzleLight();
        }

        shotgun.sanitize();
        sniperRifle.sanitize();
        muzzleParticles.sanitize();
        muzzleLight.sanitize();
    }

    public static final class Shotgun {
        public int durability = 600;
        public boolean usesAmmo = true;
        public int shellStackSize = 64;
        public int pelletCount = 12;
        public double range = 16.0;
        public double spreadDegrees = 12.0;
        public float maxDamagePerPellet = 4.0f;
        public float minDamageMultiplier = 0.15f;
        public int cooldownTicks = 32;
        public int emptyCooldownTicks = 20;
        public double knockbackPerDamage = 0.04;
        public double minKnockback = 0.25;
        public double maxKnockback = 1.2;
        public int maxTargetsPerPellet = 3;
        public float pierceDamageMultiplier = 0.55f;
        public boolean breakFragileBlocks = true;
        public boolean dropBrokenFragileBlocks = false;

        private void sanitize() {
            durability = Math.clamp(durability, 1, 4096);
            shellStackSize = Math.clamp(shellStackSize, 1, 99);
            pelletCount = Math.clamp(pelletCount, 1, 128);
            range = Math.clamp(range, 1.0, 128.0);
            spreadDegrees = Math.clamp(spreadDegrees, 0.0, 89.0);
            maxDamagePerPellet = Math.clamp(maxDamagePerPellet, 0.0f, 100.0f);
            minDamageMultiplier = Math.clamp(minDamageMultiplier, 0.0f, 1.0f);
            cooldownTicks = Math.clamp(cooldownTicks, 0, 20 * 60);
            emptyCooldownTicks = Math.clamp(emptyCooldownTicks, 0, 20 * 60);
            knockbackPerDamage = Math.clamp(knockbackPerDamage, 0.0, 10.0);
            minKnockback = Math.clamp(minKnockback, 0.0, 10.0);
            maxKnockback = Math.clamp(maxKnockback, minKnockback, 10.0);
            maxTargetsPerPellet = Math.clamp(maxTargetsPerPellet, 1, 32);
            pierceDamageMultiplier = Math.clamp(pierceDamageMultiplier, 0.0f, 1.0f);
        }
    }

    public static final class MuzzleParticles {
        public int sparkCount = 4;
        public double sparkSpread = 0.08;
        public double sparkSpeed = 0.03;
        public int smokeCount = 2;
        public double smokeSpreadX = 0.12;
        public double smokeSpreadY = 0.08;
        public double smokeSpreadZ = 0.12;
        public double smokeSpeed = 0.01;

        private void sanitize() {
            sparkCount = Math.clamp(sparkCount, 0, 64);
            sparkSpread = Math.clamp(sparkSpread, 0.0, 2.0);
            sparkSpeed = Math.clamp(sparkSpeed, 0.0, 2.0);
            smokeCount = Math.clamp(smokeCount, 0, 64);
            smokeSpreadX = Math.clamp(smokeSpreadX, 0.0, 2.0);
            smokeSpreadY = Math.clamp(smokeSpreadY, 0.0, 2.0);
            smokeSpreadZ = Math.clamp(smokeSpreadZ, 0.0, 2.0);
            smokeSpeed = Math.clamp(smokeSpeed, 0.0, 2.0);
        }
    }

    public static final class SniperRifle {
        public int durability = 256;
        public boolean usesAmmo = true;
        public int roundStackSize = 32;
        public float zoomFovMultiplier = 0.25f;
        public float zoomStep = 0.25f;
        public double range = 64.0;
        public float damage = 18.0f;
        public int cooldownTicks = 30;
        public int emptyCooldownTicks = 20;

        private void sanitize() {
            durability = Math.clamp(durability, 1, 4096);
            roundStackSize = Math.clamp(roundStackSize, 1, 99);
            zoomFovMultiplier = Math.clamp(zoomFovMultiplier, 0.05f, 1.0f);
            zoomStep = Math.clamp(zoomStep, 0.01f, 1.0f);
            range = Math.clamp(range, 1.0, 256.0);
            damage = Math.clamp(damage, 0.0f, 100.0f);
            cooldownTicks = Math.clamp(cooldownTicks, 0, 20 * 60);
            emptyCooldownTicks = Math.clamp(emptyCooldownTicks, 0, 20 * 60);
        }
    }

    public static final class MuzzleLight {
        public int lightLevel = 12;
        public int lifetimeTicks = 2;

        private void sanitize() {
            lightLevel = Math.clamp(lightLevel, 0, 15);
            lifetimeTicks = Math.clamp(lifetimeTicks, 0, 20 * 5);
        }
    }
}
