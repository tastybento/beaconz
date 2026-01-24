package com.wasteofplastic.beaconz;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.wasteofplastic.beaconz.config.Lang;

/**
 * Global test setup that initializes Lang static strings before any test class runs.
 * This is critical because enums like GameMode and GameScoreGoal reference these strings,
 * and enums are initialized once per JVM. If they are loaded before Lang strings are set,
 * they will have null values forever.
 *
 * To use this, add to your test class:
 * @ExtendWith(GlobalTestSetup.class)
 */
public class GlobalTestSetup implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

    private static boolean started = false;

    @Override
    public void beforeAll(ExtensionContext context) {
        if (!started) {
            started = true;
            // Initialize Lang strings used by enums BEFORE any enum class is loaded
            Lang.scoreGameModeMiniGame = "Minigame";
            Lang.scoreStrategy = "Strategy";
            Lang.scoreGoalArea = "Area";
            Lang.scoreGoalBeacons = "Beacons";
            Lang.scoreGoalTime = "Time";
            Lang.scoreGoalTriangles = "Triangles";
            Lang.scoreGoalLinks = "Links";

            // Register a hook to be called when the root test context is about to be closed
            context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL).put("global-test-setup", this);
        }
    }

    @Override
    public void close() {
        // Cleanup if needed when tests complete
    }
}

