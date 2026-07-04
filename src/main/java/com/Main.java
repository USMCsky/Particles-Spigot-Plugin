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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class Main extends JavaPlugin {

    private static final BlockData LEAF_DUST = Material.OAK_LEAVES.createBlockData();
    private static final double MOVE_THRESHOLD_SQUARED = 0.0036D;
    private static final int TREE_HORIZONTAL_RADIUS = 6;
    private static final int TREE_VERTICAL_MIN = 1;
    private static final int TREE_VERTICAL_MAX = 5;
    private static final int FLOWER_HORIZONTAL_RADIUS = 5;
    private static final int FLOWER_VERTICAL_MIN = -1;
    private static final int FLOWER_VERTICAL_MAX = 2;
    private static final double TREE_SAMPLE_CHANCE = 0.14D;
    private static final double FLOWER_SAMPLE_CHANCE = 0.26D;
    private static final Particle.DustOptions DEFAULT_FLOWER_DUST = new Particle.DustOptions(Color.fromRGB(255, 170, 210), 0.75F);
    private static final Map<Material, Particle.DustOptions> FLOWER_DUST = createFlowerDustMap();

    @Override
    public void onEnable() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (World world : Bukkit.getWorlds()) {
                for (LivingEntity entity : world.getLivingEntities()) {
                    if (entity.isDead()) {
                        continue;
                    }

                    Location base = entity.getLocation();
                    emitTreeAmbience(world, base, random);
                    emitFlowerAmbience(world, base, random);

                    if (isMovingOnGround(entity)) {
                        Location feet = base.clone().add(0.0, 0.1, 0.0);
                        world.spawnParticle(Particle.CLOUD, feet, 1, 0.12, 0.03, 0.12, 0.005);
                    }
                }
            }
        }, 0L, 4L);
    }

    @Override
    public void onDisable() {
        // No shutdown action needed.
    }

    private void emitTreeAmbience(World world, Location origin, ThreadLocalRandom random) {
        List<Block> nearbyLeaves = findNearbyBlocks(origin, Tag.LEAVES, TREE_HORIZONTAL_RADIUS, TREE_VERTICAL_MIN, TREE_VERTICAL_MAX);
        if (nearbyLeaves.isEmpty()) {
            return;
        }

        int samples = Math.min(2 + (nearbyLeaves.size() / 18), 5);
        for (int i = 0; i < samples; i++) {
            if (random.nextDouble() >= TREE_SAMPLE_CHANCE) {
                continue;
            }

            Block leaf = nearbyLeaves.get(random.nextInt(nearbyLeaves.size()));
            Location source = leaf.getLocation().add(
                    0.5 + random.nextDouble(-0.3, 0.3),
                    0.7 + random.nextDouble(0.0, 0.5),
                    0.5 + random.nextDouble(-0.3, 0.3)
            );
            spawnTreeParticles(world, source);
        }
    }

    private boolean isMovingOnGround(LivingEntity entity) {
        if (!entity.isOnGround()) {
            return false;
        }
        Vector velocity = entity.getVelocity();
        double horizontalSpeedSquared = (velocity.getX() * velocity.getX()) + (velocity.getZ() * velocity.getZ());
        return horizontalSpeedSquared > MOVE_THRESHOLD_SQUARED;
    }

    private void emitFlowerAmbience(World world, Location origin, ThreadLocalRandom random) {
        List<Block> nearbyFlowers = findNearbyBlocks(origin, Tag.FLOWERS, FLOWER_HORIZONTAL_RADIUS, FLOWER_VERTICAL_MIN, FLOWER_VERTICAL_MAX);
        if (nearbyFlowers.isEmpty()) {
            return;
        }

        int samples = Math.min(2 + (nearbyFlowers.size() / 10), 7);
        for (int i = 0; i < samples; i++) {
            if (random.nextDouble() >= FLOWER_SAMPLE_CHANCE) {
                continue;
            }

            Block flower = nearbyFlowers.get(random.nextInt(nearbyFlowers.size()));
            spawnFlowerParticles(world, flower, random);
        }
    }

    private void spawnFlowerParticles(World world, Block flower, ThreadLocalRandom random) {
        Particle.DustOptions dust = FLOWER_DUST.getOrDefault(flower.getType(), DEFAULT_FLOWER_DUST);
        Location source = flower.getLocation().add(
                0.5 + random.nextDouble(-0.35, 0.35),
                0.45 + random.nextDouble(0.0, 0.55),
                0.5 + random.nextDouble(-0.35, 0.35)
        );
        world.spawnParticle(Particle.DUST, source, 1, 0.02, 0.03, 0.02, 0.0, dust);
        world.spawnParticle(Particle.GLOW, source, 1, 0.04, 0.06, 0.04, 0.0);

        if (random.nextDouble() < 0.35D) {
            Location twinkle = source.clone().add(
                    random.nextDouble(-0.18, 0.18),
                    random.nextDouble(0.12, 0.35),
                    random.nextDouble(-0.18, 0.18)
            );
            world.spawnParticle(Particle.DUST, twinkle, 1, 0.0, 0.0, 0.0, 0.0, dust);
        }
    }

    private void spawnTreeParticles(World world, Location source) {
        world.spawnParticle(Particle.FALLING_DUST, source, 2, 0.45, 0.18, 0.45, 0.0, LEAF_DUST);
    }

    private List<Block> findNearbyBlocks(Location origin, Tag<Material> tag, int horizontalRadius, int minY, int maxY) {
        List<Block> matches = new ArrayList<>();
        Block base = origin.getBlock();
        for (int y = minY; y <= maxY; y++) {
            for (int x = -horizontalRadius; x <= horizontalRadius; x++) {
                for (int z = -horizontalRadius; z <= horizontalRadius; z++) {
                    Block block = base.getRelative(x, y, z);
                    if (tag.isTagged(block.getType())) {
                        matches.add(block);
                    }
                }
            }
        }
        return matches;
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
