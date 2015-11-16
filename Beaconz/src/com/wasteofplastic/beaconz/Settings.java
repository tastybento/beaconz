package com.wasteofplastic.beaconz;

import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class Settings {
    public static String worldName;
    public static Double distribution;
    public static int xCenter;
    public static int zCenter;
    public static int size;
    public static boolean randomSpawn;
    public static long seedAdjustment;
    public static long hackCoolDown;
    public static List<String> overHackEffects;
    public static TreeMap<Integer,ItemStack> teamGoodies = new TreeMap<Integer,ItemStack>();
    public static TreeMap<Integer,ItemStack> enemyGoodies = new TreeMap<Integer,ItemStack>();
    public static HashMap<Integer, List<PotionEffect>> enemyFieldEffects;
    public static HashMap<Integer, List<PotionEffect>> friendlyFieldEffects;
}
