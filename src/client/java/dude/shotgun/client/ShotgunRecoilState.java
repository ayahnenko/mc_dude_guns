package dude.shotgun.client;

public class ShotgunRecoilState {
    private static final int MAX_RECOIL_TICKS = 8;

    private static int recoilTicks = 0;

    public static void start() {
        recoilTicks = MAX_RECOIL_TICKS;
    }

    public static void tick() {
        if (recoilTicks > 0) {
            recoilTicks--;
        }
    }

    public static boolean isActive() {
        return recoilTicks > 0;
    }

    public static float getProgress() {
        if (recoilTicks <= 0) {
            return 0.0f;
        }

        return recoilTicks / (float) MAX_RECOIL_TICKS;
    }
}