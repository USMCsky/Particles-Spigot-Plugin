package com;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Bisected;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Ambient particle plugin.
 *
 * This class does three separate visual jobs:
 * 1. Spawn drifting leaf particles around nearby tree leaves.
 * 2. Spawn colorful sparkle/twinkle particles around nearby flowers.
 * 3. Spawn a tiny dust puff at the feet of moving living entities.
 *
 * Most of the "look and feel" is controlled by the constants near the top of the class.
 * If you want to tune the effect later, start there first before changing the logic below.
 */
public final class Main extends JavaPlugin {

    // Block data used by FALLING_DUST so tree ambience looks like leaf debris instead of generic dust.
    private static final BlockData LEAF_DUST = Material.OAK_LEAVES.createBlockData();

    // Minimum horizontal movement speed (squared) required before ground-step dust is emitted.
    // Raising this value means players/mobs must move faster before the footstep cloud appears.
    private static final double MOVE_THRESHOLD_SQUARED = 0.0036D;

    // How far around each entity we search for leaf blocks to use as tree particle sources.
    private static final int TREE_HORIZONTAL_RADIUS = 6;
    private static final int TREE_VERTICAL_MIN = 1;
    private static final int TREE_VERTICAL_MAX = 5;

    // How far around each entity we search for flower blocks to use as flower particle sources.
    private static final int FLOWER_HORIZONTAL_RADIUS = 8;
    private static final int FLOWER_VERTICAL_MIN = -1;
    private static final int FLOWER_VERTICAL_MAX = 3;

    // Per-sample chance that a leaf ambience particle actually spawns.
    // Lower = calmer forests. Higher = more constantly active leaf drift.
    private static final double TREE_SAMPLE_CHANCE = 0.14D;

    // Per-extra-sample chance for flower ambience after the guaranteed first sample pass.
    // Lower = fewer flower effects overall. Higher = denser flower sparkle fields.
    private static final double FLOWER_SAMPLE_CHANCE = 0.42D;

    // Fallback color used when a flower type is allowed but does not have a custom dust color.
    private static final Particle.DustOptions DEFAULT_FLOWER_DUST = new Particle.DustOptions(Color.fromRGB(255, 170, 210), 0.75F);

    // Extra plant blocks that are flower-like for ambience purposes but may not be covered
    // by the built-in Tag.FLOWERS check the way we want.
    private static final Set<Material> EXTRA_FLOWERS = EnumSet.of(
            Material.PINK_PETALS,
            Material.WILDFLOWERS,
            Material.OPEN_EYEBLOSSOM,
            Material.CLOSED_EYEBLOSSOM
    );

    // Color palette for individual flower block types.
    // This is the main place to edit if you want different colors per flower.
    private static final Map<Material, Particle.DustOptions> FLOWER_DUST = createFlowerDustMap();

    @Override
    public void onEnable() {
        getLogger().info("Enabled ambient particles for leaves, flowers, and movement.");

        // Run every 4 ticks. This is frequent enough to look alive, but not every single tick.
        // Lower the period for a more intense effect, or raise it for a gentler / cheaper one.
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (World world : Bukkit.getWorlds()) {
                for (LivingEntity entity : world.getLivingEntities()) {
                    if (entity.isDead()) {
                        continue;
                    }

                    Location base = entity.getLocation();

                    // Each living entity becomes a "viewer anchor" for ambience.
                    // We search around that entity and spawn effects near nearby plants/leaves.
                    emitTreeAmbience(world, base, random);
                    emitFlowerAmbience(world, base, random);

                    if (isMovingOnGround(entity)) {
                        // Tiny movement dust at the feet so walking through nature feels grounded.
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

    // Finds nearby leaves and occasionally emits drifting leaf dust from a random subset of them.
    private void emitTreeAmbience(World world, Location origin, ThreadLocalRandom random) {
        List<Block> nearbyLeaves = findNearbyBlocks(origin, Tag.LEAVES, TREE_HORIZONTAL_RADIUS, TREE_VERTICAL_MIN, TREE_VERTICAL_MAX);
        if (nearbyLeaves.isEmpty()) {
            return;
        }

        // More nearby leaves allows more sample attempts, but the cap prevents this from exploding
        // in dense forests or giant custom trees.
        int samples = Math.min(2 + (nearbyLeaves.size() / 18), 5);
        for (int i = 0; i < samples; i++) {
            if (random.nextDouble() >= TREE_SAMPLE_CHANCE) {
                continue;
            }

            // Pick a random leaf block, then jitter the particle spawn point so the effect does not
            // stack in the exact center of the same block every time.
            Block leaf = nearbyLeaves.get(random.nextInt(nearbyLeaves.size()));
            Location source = leaf.getLocation().add(
                    0.5 + random.nextDouble(-0.3, 0.3),
                    0.7 + random.nextDouble(0.0, 0.5),
                    0.5 + random.nextDouble(-0.3, 0.3)
            );
            spawnTreeParticles(world, source);
        }
    }

    // Only emit footstep dust when the entity is actually on the ground and moving enough to matter.
    private boolean isMovingOnGround(LivingEntity entity) {
        if (!entity.isOnGround()) {
            return false;
        }
        Vector velocity = entity.getVelocity();
        double horizontalSpeedSquared = (velocity.getX() * velocity.getX()) + (velocity.getZ() * velocity.getZ());
        return horizontalSpeedSquared > MOVE_THRESHOLD_SQUARED;
    }

    // Finds nearby flowers and emits ambience from a small random subset of them.
    private void emitFlowerAmbience(World world, Location origin, ThreadLocalRandom random) {
        List<Block> nearbyFlowers = findNearbyFlowers(origin);
        if (nearbyFlowers.isEmpty()) {
            return;
        }

        // One flower sample is effectively guaranteed when flowers are nearby.
        // Additional samples become possible in denser flower beds, up to the cap.
        int samples = Math.min(1 + (nearbyFlowers.size() / 10), 4);
        for (int i = 0; i < samples; i++) {
            if (i > 0 && random.nextDouble() >= FLOWER_SAMPLE_CHANCE) {
                continue;
            }

            Block flower = nearbyFlowers.get(random.nextInt(nearbyFlowers.size()));
            spawnFlowerParticles(world, flower, random);
        }
    }

    // Builds the full flower effect at one flower:
    // - main colored dust bloom
    // - a subtle magical end-rod accent
    // - optional glow "twinkle"
    // - optional extra dust "shimmer"
    //
    // If you want to tone the flower effect down, this is the main method to edit.
    private void spawnFlowerParticles(World world, Block flower, ThreadLocalRandom random) {
        Block sourceBlock = normalizeFlowerBlock(flower);

        // Tall flowers need a higher visual origin so the particles appear around the blossom,
        // not halfway down the stem.
        double baseHeight = isTallFlower(sourceBlock) ? 0.7D : 0.45D;
        Particle.DustOptions dust = FLOWER_DUST.getOrDefault(flower.getType(), DEFAULT_FLOWER_DUST);

        // Main source point for the flower effect. The random offsets keep each spawn from looking
        // mechanically identical.
        Location source = sourceBlock.getLocation().add(
                0.5 + random.nextDouble(-0.35, 0.35),
                baseHeight + random.nextDouble(0.1, 0.7),
                0.5 + random.nextDouble(-0.35, 0.35)
        );

        // The base colored puff. Increase the count or spread for a fuller flower aura.
        // Decrease them for a softer, more delicate look.
        world.spawnParticle(Particle.DUST, source, 3, 0.16, 0.2, 0.16, 0.0, dust);

        // Subtle magical highlight. This helps the flower ambience sparkle without dominating it.
        world.spawnParticle(Particle.END_ROD, source, 1, 0.08, 0.12, 0.08, 0.005);

        // Twinkle effect.
        // The 0.55D value is the chance that the glow twinkle appears during this flower spawn.
        // This is the single easiest number to lower if the flower effect feels too busy.
        //
        // Examples:
        // 0.55D = frequent twinkles
        // 0.30D = moderate twinkles
        // 0.15D = rare twinkles
        if (random.nextDouble() < 0.30D) {
            Location twinkle = source.clone().add(
                    random.nextDouble(-0.22, 0.22),
                    random.nextDouble(0.1, 0.38),
                    random.nextDouble(-0.22, 0.22)
            );

            // count = 1 means a single glow speck.
            // The three spread values (0.06, 0.08, 0.06) control how loosely the glow is scattered
            // around the chosen twinkle point.
            world.spawnParticle(Particle.GLOW, twinkle, 1, 0.06, 0.08, 0.06, 0.0);
        }

        // Secondary shimmer. This is quieter than the glow twinkle because it reuses the flower color.
        // Lower 0.45D if you want fewer extra sparkle moments overall.
        if (random.nextDouble() < 0.30D) {
            Location shimmer = source.clone().add(
                    random.nextDouble(-0.2, 0.2),
                    random.nextDouble(0.08, 0.3),
                    random.nextDouble(-0.2, 0.2)
            );
            world.spawnParticle(Particle.DUST, shimmer, 1, 0.0, 0.0, 0.0, 0.0, dust);
        }
    }

    // Tree ambience is intentionally simple: just a little falling leaf dust.
    // Increase the count or spread if you want more dramatic forest motion.
    private void spawnTreeParticles(World world, Location source) {
        world.spawnParticle(Particle.FALLING_DUST, source, 2, 0.45, 0.18, 0.45, 0.0, LEAF_DUST);
    }

    // Searches around the entity for valid flower blocks.
    // We skip the top half of tall flowers so each double-height flower only counts once.
    private List<Block> findNearbyFlowers(Location origin) {
        List<Block> matches = new ArrayList<>();
        Block base = origin.getBlock();
        for (int y = FLOWER_VERTICAL_MIN; y <= FLOWER_VERTICAL_MAX; y++) {
            for (int x = -FLOWER_HORIZONTAL_RADIUS; x <= FLOWER_HORIZONTAL_RADIUS; x++) {
                for (int z = -FLOWER_HORIZONTAL_RADIUS; z <= FLOWER_HORIZONTAL_RADIUS; z++) {
                    Block block = base.getRelative(x, y, z);
                    if (isFlowerBlock(block.getType()) && !isTallFlowerTop(block)) {
                        matches.add(block);
                    }
                }
            }
        }
        return matches;
    }

    // Central check for whether a block should participate in flower ambience.
    private boolean isFlowerBlock(Material type) {
        return FLOWER_DUST.containsKey(type) || EXTRA_FLOWERS.contains(type) || Tag.FLOWERS.isTagged(type);
    }

    // If the chosen block is the top half of a tall flower, move down to the bottom half so our
    // origin math and color lookup stay consistent.
    private Block normalizeFlowerBlock(Block block) {
        if (isTallFlowerTop(block)) {
            return block.getRelative(BlockFace.DOWN);
        }
        return block;
    }

    // Tall flowers use Bisected block data, which lets us identify double-height flowers.
    private boolean isTallFlower(Block block) {
        return block.getBlockData() instanceof Bisected;
    }

    // Returns true only for the upper half of a tall flower.
    private boolean isTallFlowerTop(Block block) {
        if (!(block.getBlockData() instanceof Bisected bisected)) {
            return false;
        }
        return bisected.getHalf() == Bisected.Half.TOP;
    }

    // Generic nearby-block finder used for tag-based searches like leaves.
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

    // Defines the flower color palette. This is the easiest place to edit if you want the ambient
    // color around specific flowers to be warmer, cooler, brighter, etc.
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
        map.put(Material.PINK_PETALS, dust(255, 176, 216));
        map.put(Material.WILDFLOWERS, dust(246, 216, 102));
        map.put(Material.OPEN_EYEBLOSSOM, dust(255, 171, 74));
        map.put(Material.CLOSED_EYEBLOSSOM, dust(214, 160, 84));
        return map;
    }

    // Small helper for creating colored dust definitions.
    // The size is fixed at 1.0F here; lower values look finer, higher values look chunkier.
    private static Particle.DustOptions dust(int red, int green, int blue) {
        return new Particle.DustOptions(Color.fromRGB(red, green, blue), 1.0F);
    }
}
