package com;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class Main extends JavaPlugin {

    private static final BlockData LEAF_DUST = Material.OAK_LEAVES.createBlockData();
    private static final double MOVE_THRESHOLD_SQUARED = 0.0036D;
    private static final Particle.DustOptions DEFAULT_FLOWER_DUST = new Particle.DustOptions(Color.fromRGB(255, 170, 210), 1.0F);
    private static final Map<Material, Particle.DustOptions> FLOWER_DUST = createFlowerDustMap();

    @Override
    public void onEnable() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (World world : Bukkit.getWorlds()) {
                for (LivingEntity entity : world.getLivingEntities()) {
                    if (entity.isDead()) {
                        continue;
                    }

                    Location base = entity.getLocation();

                    if (isUnderLeaves(base)) {
                        Location leaves = base.clone().add(0.0, 2.2, 0.0);
                        world.spawnParticle(Particle.FALLING_DUST, leaves, 2, 0.6, 0.2, 0.6, 0.0, LEAF_DUST);
                    }

                    if (ThreadLocalRandom.current().nextDouble() < 0.35D) {
                        spawnFlowerParticles(world, base);
                    }

                    if (isMovingOnGround(entity)) {
                        Location feet = base.clone().add(0.0, 0.1, 0.0);
                        world.spawnParticle(Particle.CLOUD, feet, 3, 0.2, 0.05, 0.2, 0.01);
                    }
                }
            }
        }, 0L, 5L);
    }

    @Override
    public void onDisable() {
        // No shutdown action needed.
    }

    private boolean isUnderLeaves(Location location) {
        Block aboveHead = location.getBlock().getRelative(0, 2, 0);
        Block canopy = location.getBlock().getRelative(0, 3, 0);
        return Tag.LEAVES.isTagged(aboveHead.getType()) || Tag.LEAVES.isTagged(canopy.getType());
    }

    private boolean isMovingOnGround(LivingEntity entity) {
        if (!entity.isOnGround()) {
            return false;
        }
        Vector velocity = entity.getVelocity();
        double horizontalSpeedSquared = (velocity.getX() * velocity.getX()) + (velocity.getZ() * velocity.getZ());
        return horizontalSpeedSquared > MOVE_THRESHOLD_SQUARED;
    }

    private void spawnFlowerParticles(World world, Location location) {
        Block flower = findNearbyFlower(location);
        if (flower == null) {
            return;
        }

        Particle.DustOptions dust = FLOWER_DUST.getOrDefault(flower.getType(), DEFAULT_FLOWER_DUST);
        Location source = flower.getLocation().add(0.5, 0.7, 0.5);
        world.spawnParticle(Particle.DUST, source, 3, 0.2, 0.25, 0.2, 0.0, dust);
    }

    private Block findNearbyFlower(Location origin) {
        Block base = origin.getBlock();
        for (int y = -1; y <= 1; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    Block block = base.getRelative(x, y, z);
                    if (Tag.FLOWERS.isTagged(block.getType())) {
                        return block;
                    }
                }
            }
        }
        return null;
    }

    private static Map<Material, Particle.DustOptions> createFlowerDustMap() {
        Map<Material, Particle.DustOptions> map = new EnumMap<>(Material.class);
        map.put(Material.DANDELION, dust(255, 222, 65));
        map.put(Material.POPPY, dust(230, 48, 56));
        map.put(Material.BLUE_ORCHID, dust(102, 180, 255));
        map.put(Material.ALLIUM, dust(201, 133, 255));
        map.put(Material.AZURE_BLUET, dust(236, 240, 255));
        map.put(Material.RED_TULIP, dust(245, 64, 64));
        map.put(Material.ORANGE_TULIP, dust(255, 156, 42));
        map.put(Material.WHITE_TULIP, dust(245, 245, 245));
        map.put(Material.PINK_TULIP, dust(255, 156, 196));
        map.put(Material.OXEYE_DAISY, dust(245, 245, 220));
        map.put(Material.CORNFLOWER, dust(85, 120, 255));
        map.put(Material.LILY_OF_THE_VALLEY, dust(232, 255, 232));
        map.put(Material.SUNFLOWER, dust(255, 208, 70));
        map.put(Material.LILAC, dust(213, 165, 255));
        map.put(Material.ROSE_BUSH, dust(215, 42, 83));
        map.put(Material.PEONY, dust(255, 144, 181));
        map.put(Material.TORCHFLOWER, dust(255, 135, 30));
        map.put(Material.PITCHER_PLANT, dust(118, 204, 86));
        return map;
    }

    private static Particle.DustOptions dust(int red, int green, int blue) {
        return new Particle.DustOptions(Color.fromRGB(red, green, blue), 1.0F);
    }
}
