package me.releasedsnow.com.hackathon.Ability;

import com.google.common.base.Predicates;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.*;
import com.projectkorra.projectkorra.ability.util.Collision;
import com.projectkorra.projectkorra.ability.util.ComboManager;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import me.releasedsnow.com.hackathon.ConfigManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.PointedDripstone;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class DripstoneDash extends EarthAbility implements AddonAbility, ComboAbility {

    private Location origin;
    private Vector direction;

    private int step = 0;

    private Spike currentSpike = null;
    private long lastGrowthTime = 0;

    private Spike lastSpike = null;
    private boolean finishedAllPillars = false;
    private long pillarsCompleteTime = 0;

    private List<Spike> allSpikes = new ArrayList<>();



    private List<Block> allSpikeBlocks =  new ArrayList<>();
    private List<Entity> hitEntities = new ArrayList<>();

    private final int maxSteps = ConfigManager.getConfig().getInt("Abilities.Earth.DripstoneDash.Range");
    private final long lingerDuration = ConfigManager.getConfig().getLong("Abilities.Earth.DripstoneDash.LingerDuration");
    private final double growthDelay = ConfigManager.getConfig().getDouble("Abilities.Earth.DripstoneDash.GrowthDelay");
    private final double damage = ConfigManager.getConfig().getDouble("Abilities.Earth.DripstoneDash.Damage");
    private final double knockup = ConfigManager.getConfig().getDouble("Abilities.Earth.DripstoneDash.Knockup");
    private final int trailBlocksRadius = ConfigManager.getConfig().getInt("Abilities.Earth.DripstoneDash.TrailBlockRadius");

    public DripstoneDash(Player player) {
        super(player);
        if (!bPlayer.canBendIgnoreBinds(this)) {
        remove();
            return;
        }
        Block source = getEarthSourceBlock(5);
        if (source == null) {
            return;
        }

        this.origin = source.getLocation();
        focusBlock(source);
        start();
    }

    @Override
    public void remove() {
        destroyAllPillars();
        bPlayer.addCooldown(this, getCooldown());
        super.remove();
    }


    @Override
    public void progress() {
        if (!player.isOnline() || player.isDead()) {
            remove();
            return;
        }

        long now = System.currentTimeMillis();

        if (currentSpike == null && step < maxSteps) {
            this.direction = player.getLocation().getDirection().setY(0).normalize();


            Location base;
            if (lastSpike == null) {
                Location rawLoc = origin.clone().add(direction.clone().multiply(step));
                Block topBlock = GeneralMethods.getTopBlock(rawLoc, 3, -3);

                if (topBlock == null) {
                    step++;
                    return;
                }
                base = topBlock.getLocation().add(0, 1, 0);
            }else {
                Location nextLoc = lastSpike.base.clone().add(direction.clone());
                Block nextBlock = GeneralMethods.getTopBlock(nextLoc, 3, -3);

                if (nextBlock == null || !isEarthbendable(nextBlock)) {
                    step ++;
                    return;
                }
                base = nextBlock.getLocation().add(0,1,0);
            }
            int height = step + 1;
            currentSpike = new Spike(base, 0, height);
            allSpikes.add(currentSpike);
            lastGrowthTime = now;
        }

        if (currentSpike != null && now - lastGrowthTime >= growthDelay) {
            lastGrowthTime = now;
            currentSpike.currentHeight++;
            createStalagmite(currentSpike.base, currentSpike.currentHeight, currentSpike.maxHeight);
            createEffects(currentSpike.base);

            checkCollision(currentSpike.base, currentSpike.currentHeight);

            if (currentSpike.currentHeight >= currentSpike.maxHeight) {
                currentSpike = null;
                step++;
            }
        }

        if (step >= maxSteps && currentSpike == null && !finishedAllPillars) {
            finishedAllPillars = true;
            pillarsCompleteTime = System.currentTimeMillis();
            return;
        }


        if (finishedAllPillars && System.currentTimeMillis() - pillarsCompleteTime >= lingerDuration) {
            destroyAllPillars();
            remove();
        }
    }


    private void destroyAllPillars() {
        for (Spike spike : allSpikes) {
            for (TempBlock tb : spike.tempBlocks) {
                Location location = tb.getLocation();
                Block block = location.getBlock();
                block.getWorld().spawnParticle(Particle.BLOCK_CRACK, location.add(0.5, 0.5, 0.5), 10, 0.2, 0.2, 0.2, 0.05, block.getBlockData());
                tb.revertBlock();
            }
        }
    }

    private void createEffects(Location location) {
        player.playSound(location, Sound.BLOCK_DEEPSLATE_BRICKS_BREAK, 2, 1);
    }

    private void createStalagmite(Location baseLocation, int currentHeight, int maxHeight) {

        Block spikeBlock = baseLocation.getBlock().getRelative(BlockFace.UP, currentHeight - 1);
        BlockData data = Bukkit.createBlockData(Material.POINTED_DRIPSTONE);
        allSpikeBlocks.add(spikeBlock);

        trailBlocks(baseLocation);
        if (data instanceof PointedDripstone dripstone) {
            dripstone.setVerticalDirection(BlockFace.UP);

            if (maxHeight == 1) {
                dripstone.setThickness(PointedDripstone.Thickness.TIP);
            } else if (currentHeight == 1) {
                dripstone.setThickness(PointedDripstone.Thickness.BASE);
            } else if (currentHeight == maxHeight - 1) {
                dripstone.setThickness(PointedDripstone.Thickness.FRUSTUM);
            } else if (currentHeight == maxHeight) {
                dripstone.setThickness(PointedDripstone.Thickness.TIP);
            } else {
                dripstone.setThickness(PointedDripstone.Thickness.MIDDLE);
            }
        }

        TempBlock tempBlock = new TempBlock(spikeBlock, data);
        player.getWorld().spawnParticle(Particle.REDSTONE, tempBlock.getLocation(), 2, .15, .15, .15, new Particle.DustOptions(Color.fromRGB(79, 53, 17), 1.5F));

        currentSpike.tempBlocks.add(tempBlock);
    }

    private void trailBlocks(Location baseLocation) {
        Material[] options = {
                Material.DRIPSTONE_BLOCK,
                Material.PACKED_MUD,
                Material.COARSE_DIRT,
                Material.GRANITE
        };

        Block baseBelow = baseLocation.clone().subtract(0,1,0).getBlock();
        if (isEarthbendable(baseBelow)) {
            for (Location surrounding : GeneralMethods.getCircle(baseBelow.getLocation(),trailBlocksRadius, 1, false, false, 0)) {
                if (!surrounding.getBlock().getRelative(BlockFace.UP).isPassable()) continue;
                if (Math.random() >= .8) continue;
                Random random = new Random();Material chosen = options[random.nextInt(options.length)];
                if (!TempBlock.isTempBlock(surrounding.getBlock())) {
                    TempBlock block = new TempBlock(surrounding.getBlock() , chosen);
                    currentSpike.tempBlocks.add(block);
                    surrounding.getWorld().spawnParticle(Particle.BLOCK_CRACK, surrounding, 10, .2, .2, .2, 0.05, block.getBlockData());
                }
            }
        }
    }
    private void focusBlock(Block sourceBlock) {
        switch (sourceBlock.getType()) {
            case SAND -> new TempBlock(sourceBlock, Material.SANDSTONE).setRevertTime(3000);
            case RED_SAND -> new TempBlock(sourceBlock, Material.RED_SANDSTONE).setRevertTime(3000);
            case STONE -> new TempBlock(sourceBlock, Material.COBBLESTONE).setRevertTime(3000);
            default -> new TempBlock(sourceBlock, Material.STONE).setRevertTime(3000);
        }
    }

    private static class Spike {
        Location base;
        int currentHeight;
        int maxHeight;

        List<TempBlock> tempBlocks = new ArrayList<>();
        Spike(Location base, int currentHeight, int maxHeight) {
            this.base = base;
            this.currentHeight = currentHeight;
            this.maxHeight = maxHeight;
        }
    }

    @Override
    public Object createNewComboInstance(Player player) {
        return new DripstoneDash(player);
    }

    @Override
    public ArrayList<ComboManager.AbilityInformation> getCombination() {
        ArrayList<ComboManager.AbilityInformation> combo = new ArrayList<>();
        combo.add(new ComboManager.AbilityInformation("EarthBlast", ClickType.LEFT_CLICK));
        combo.add(new ComboManager.AbilityInformation("EarthBlast", ClickType.LEFT_CLICK));
        combo.add(new ComboManager.AbilityInformation("Shockwave", ClickType.SHIFT_DOWN));
        combo.add(new ComboManager.AbilityInformation("Shockwave", ClickType.SHIFT_UP));

        return combo;
    }



    private void checkCollision(Location base, int height) {
        for (int i = 0; i < height; ++i) {
            Location location = base.clone().add(0.0, i, 0.0);
            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(location, 1.5)) {
                if (!hitEntities.contains(entity)) {
                    if (entity.equals(this.player) || !(entity instanceof LivingEntity)) continue;
                    Vector knockback = new Vector(0.0, knockup, 0.0);
                    entity.setVelocity(entity.getVelocity().multiply(knockback));
                    DamageHandler.damageEntity(entity, this.player, damage, this);
                    hitEntities.add(entity);
                }
            }
        }
    }



    @Override
    public boolean isDefault() {
        return false;
    }

    @Override
    public double getCollisionRadius() {
        return 2;
    }

    @Override public boolean isCollidable() {
        return true;
    }
    @Override public boolean isSneakAbility() { return true; }
    @Override public boolean isHarmlessAbility() { return false; }
    @Override public long getCooldown() { return 4000; }
    @Override public String getName() { return "DripstoneDash"; }
    @Override public Location getLocation() { return null; }
    @Override public void load() {}
    @Override public void stop() {}
    @Override public String getAuthor() { return "ReleasedSnow"; }
    @Override public String getVersion() { return "1.0.1"; }
}
