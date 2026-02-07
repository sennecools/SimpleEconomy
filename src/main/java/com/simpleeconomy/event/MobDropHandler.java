package com.simpleeconomy.event;

import com.simpleeconomy.config.ModConfig;
import com.simpleeconomy.data.TransactionLog;
import com.simpleeconomy.economy.EconomyManager;
import com.simpleeconomy.util.SoundHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinBrute;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.Random;

public class MobDropHandler {

    private static final Random random = new Random();

    // Drop chances (percentage)
    private static final double BASE_DROP_CHANCE = 0.5; // 50% chance to drop coins

    @SubscribeEvent
    public void onMobDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Entity source = event.getSource().getEntity();

        // Only if killed by a player
        if (!(source instanceof ServerPlayer player)) {
            return;
        }

        // PvP kill reward
        if (entity instanceof ServerPlayer victim) {
            handlePvpKill(player, victim);
            return;
        }

        // Calculate coin drop
        double coins = calculateCoinDrop(entity);
        if (coins <= 0) {
            return;
        }

        // Random chance to drop
        if (random.nextDouble() > BASE_DROP_CHANCE) {
            // Check for guaranteed drops (bosses always drop)
            if (!isGuaranteedDrop(entity)) {
                return;
            }
        }

        // Give coins
        EconomyManager.addBalance(player, coins);

        // Log transaction (only for significant amounts to avoid spam)
        if (coins >= 5) {
            TransactionLog log = TransactionLog.get(player.getServer());
            log.addTransaction(player.getUUID(), TransactionLog.Transaction.mobDrop(coins, getEntityName(entity)));
        }

        // Play sound and show message for significant drops
        if (coins >= 10) {
            SoundHelper.playMobDropSound(player);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("+" + EconomyManager.formatBalance(coins) + " " + ModConfig.getCurrencyName())
                .withStyle(s -> s.withColor(0xFFD700)));
        }
    }

    private double calculateCoinDrop(LivingEntity entity) {
        // Bosses - big rewards
        if (entity instanceof EnderDragon) {
            return 500 + random.nextInt(500); // 500-1000
        }
        if (entity instanceof WitherBoss) {
            return 250 + random.nextInt(250); // 250-500
        }
        if (entity instanceof ElderGuardian) {
            return 50 + random.nextInt(50); // 50-100
        }
        if (entity instanceof Warden) {
            return 100 + random.nextInt(100); // 100-200
        }

        // Strong hostile mobs
        if (entity instanceof Ravager) {
            return 20 + random.nextInt(20); // 20-40
        }
        if (entity instanceof Evoker) {
            return 15 + random.nextInt(10); // 15-25
        }
        if (entity instanceof Vindicator) {
            return 8 + random.nextInt(7); // 8-15
        }
        if (entity instanceof Witch) {
            return 5 + random.nextInt(5); // 5-10
        }
        if (entity instanceof PiglinBrute) {
            return 10 + random.nextInt(10); // 10-20
        }
        if (entity instanceof Piglin) {
            return 3 + random.nextInt(3); // 3-6
        }
        if (entity instanceof Ghast) {
            return 8 + random.nextInt(7); // 8-15
        }
        if (entity instanceof Blaze) {
            return 5 + random.nextInt(5); // 5-10
        }
        if (entity instanceof MagmaCube magmaCube) {
            return magmaCube.getSize() * 2; // Size-based
        }
        if (entity instanceof Slime slime) {
            return slime.getSize(); // Size-based
        }

        // Regular hostile mobs
        if (entity instanceof Creeper) {
            return 3 + random.nextInt(3); // 3-6
        }
        if (entity instanceof Skeleton) {
            return 2 + random.nextInt(3); // 2-5
        }
        if (entity instanceof Zombie) {
            return 2 + random.nextInt(2); // 2-4
        }
        if (entity instanceof Spider) {
            return 2 + random.nextInt(2); // 2-4
        }
        if (entity instanceof EnderMan) {
            return 5 + random.nextInt(5); // 5-10
        }
        if (entity instanceof Guardian) {
            return 5 + random.nextInt(5); // 5-10
        }
        if (entity instanceof Phantom) {
            return 3 + random.nextInt(3); // 3-6
        }
        if (entity instanceof Drowned) {
            return 2 + random.nextInt(3); // 2-5
        }
        if (entity instanceof Husk) {
            return 2 + random.nextInt(3); // 2-5
        }
        if (entity instanceof Stray) {
            return 2 + random.nextInt(3); // 2-5
        }
        if (entity instanceof Silverfish) {
            return 1; // 1
        }
        if (entity instanceof Endermite) {
            return 1; // 1
        }
        if (entity instanceof CaveSpider) {
            return 3 + random.nextInt(2); // 3-5
        }
        if (entity instanceof Shulker) {
            return 10 + random.nextInt(10); // 10-20
        }

        // Nether mobs
        if (entity instanceof WitherSkeleton) {
            return 5 + random.nextInt(5); // 5-10
        }
        if (entity instanceof Hoglin) {
            return 5 + random.nextInt(5); // 5-10
        }
        if (entity instanceof Zoglin) {
            return 5 + random.nextInt(5); // 5-10
        }

        // Passive mobs - small rewards (for farming)
        if (entity instanceof Cow || entity instanceof Pig || entity instanceof Sheep || entity instanceof Chicken) {
            return random.nextDouble() < 0.3 ? 1 : 0; // 30% chance for 1 coin
        }

        // Default for any other hostile mob
        if (entity instanceof Monster) {
            return 1 + random.nextInt(3); // 1-4
        }

        return 0;
    }

    private boolean isGuaranteedDrop(LivingEntity entity) {
        return entity instanceof EnderDragon ||
               entity instanceof WitherBoss ||
               entity instanceof ElderGuardian ||
               entity instanceof Warden ||
               entity instanceof Ravager;
    }

    private String getEntityName(LivingEntity entity) {
        return entity.getType().getDescription().getString();
    }

    private void handlePvpKill(ServerPlayer killer, ServerPlayer victim) {
        double percent = ModConfig.getKillRewardPercent();
        if (percent <= 0) return;

        double victimBalance = EconomyManager.getBalance(victim);
        if (victimBalance <= 0) return;

        double bounty = Math.floor(victimBalance * percent * 100) / 100.0; // round down to 2 decimals
        if (bounty < 0.01) return;

        EconomyManager.removeBalance(victim, bounty);
        EconomyManager.addBalance(killer, bounty);

        // Log for both
        TransactionLog log = TransactionLog.get(killer.getServer());
        log.addTransaction(killer.getUUID(), TransactionLog.Transaction.pvpKill(bounty, victim.getUUID(), victim.getName().getString()));
        log.addTransaction(victim.getUUID(), TransactionLog.Transaction.pvpDeath(bounty, killer.getUUID(), killer.getName().getString()));

        String amount = EconomyManager.formatBalance(bounty);
        String currency = ModConfig.getCurrencyName();

        killer.sendSystemMessage(net.minecraft.network.chat.Component.literal("[PVP] ").withStyle(s -> s.withColor(0xFF5555).withBold(true))
            .append(net.minecraft.network.chat.Component.literal("+" + amount + " " + currency + " for killing " + victim.getName().getString())
                .withStyle(s -> s.withColor(0x55FF55).withBold(false))));

        victim.sendSystemMessage(net.minecraft.network.chat.Component.literal("[PVP] ").withStyle(s -> s.withColor(0xFF5555).withBold(true))
            .append(net.minecraft.network.chat.Component.literal("-" + amount + " " + currency + " lost to " + killer.getName().getString())
                .withStyle(s -> s.withColor(0xFF5555).withBold(false))));

        SoundHelper.playMobDropSound(killer);
    }
}
