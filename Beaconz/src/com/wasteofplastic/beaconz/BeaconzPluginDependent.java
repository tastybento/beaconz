package com.wasteofplastic.beaconz;

import org.bukkit.generator.BlockPopulator;

import java.util.logging.Logger;

/**
 * Base class for classes that depend on a {@link org.bukkit.plugin.Plugin}.
 *
 * Delegates the often-used methods to the plugin so subclasses don't have to refer to the field when
 * logging, getting the register, etc.
 *
 * Sadly this cannot be used universally as some classes (BeaconMap, BeaconPopulator) need to extend existing
 * classes and Java does not (yet?) support multiple inheritance.
 */
public abstract class BeaconzPluginDependent {
    private final Beaconz beaconzPlugin;

    public final Beaconz getBeaconzPlugin() {
        return beaconzPlugin;
    }

    public BeaconzPluginDependent(Beaconz beaconzPlugin) {
        this.beaconzPlugin = beaconzPlugin;
    }

    public final Register getRegister() {
        return this.beaconzPlugin.getRegister();
    }

    public final Logger getLogger() {
        return this.beaconzPlugin.getLogger();
    }

    public final BlockPopulator getBlockPopulator() {
        return this.beaconzPlugin.getBp();
    }

    public final Scorecard getScorecard() {
        return this.beaconzPlugin.getScorecard();
    }
}
