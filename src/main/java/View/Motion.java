package View;

import javafx.util.Duration;

/**
 * Shared animation timing for the game screens.
 *
 * Every animation (dice roll, piece steps, hover effects) takes
 * its durations from here, so automated tests can speed them all
 * up without changing how the game looks for players.
 *
 * MVC Component: View
 * Iteration: 1
 */
final class Motion {

    /** Time a piece takes to hop onto the next square. */
    static final double STEP_MILLIS = 170;

    private static volatile double speed = 1.0;

    private Motion() {
    }

    /**
     * Sets the playback speed of all animations.
     *
     * @param newSpeed 1.0 for normal speed, higher is faster
     */
    static void setSpeed(double newSpeed) {
        if (newSpeed <= 0) {
            throw new IllegalArgumentException(
                    "Animation speed must be positive");
        }
        speed = newSpeed;
    }

    /** @return the current playback speed (1.0 is normal) */
    static double speed() {
        return speed;
    }

    /**
     * Converts a normal-speed duration to the current speed.
     *
     * @param millis duration at normal speed
     * @return the duration to use now
     */
    static Duration ms(double millis) {
        return Duration.millis(millis / speed);
    }
}
