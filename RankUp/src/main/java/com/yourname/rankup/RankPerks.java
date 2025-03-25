package com.yourserver.rankup;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RankPerks extends JavaPlugin implements Listener, TabExecutor, TabCompleter {
    private FileConfiguration config;
    private FileConfiguration lang;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        loadLang();
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("rankperks").setExecutor(this);
        getCommand("rankperks").setTabCompleter(this);
    }

    private void loadLang() {
        File langFile = new File(getDataFolder(), "lang.yml");
        if (!langFile.exists()) {
            saveResource("lang.yml", false);
        }
        lang = YamlConfiguration.loadConfiguration(langFile);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();

        String rank = getPlayerRank(player);
        double damageBoost = config.getDouble("ranks." + rank + ".damageBoost", 0);

        if (damageBoost > 0) {
            event.setDamage(event.getDamage() * (1 + damageBoost / 100));
        }
    }

    @EventHandler
    public void onPlayerKill(PlayerDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player)) return;
        Player killer = event.getEntity().getKiller();
        Player victim = event.getEntity();

        String victimRank = getPlayerRank(victim);
        if (config.contains("rankup." + victimRank)) {
            String newRank = config.getString("rankup." + victimRank);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + killer.getName() + " parent set " + newRank);
            killer.sendMessage(lang.getString("rankup.message").replace("{rank}", newRank).replace("{player}", victim.getName()));
        }
    }

    private String getPlayerRank(Player player) {
        for (String rank : config.getConfigurationSection("ranks").getKeys(false)) {
            if (player.hasPermission("rank." + rank)) {
                return rank;
            }
        }
        return "default"; // Fallback rank
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(lang.getString("commands.player_only"));
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            openRankGUI(player);
            return true;
        }
        return false;
    }

    private void openRankGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, lang.getString("gui.title"));

        for (String rank : config.getConfigurationSection("ranks").getKeys(false)) {
            ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§e" + rank);
            List<String> lore = new ArrayList<>();
            lore.add(lang.getString("gui.damage_boost").replace("{boost}", String.valueOf(config.getDouble("ranks." + rank + ".damageBoost", 0))));
            meta.setLore(lore);
            item.setItemMeta(meta);
            gui.addItem(item);
        }
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(lang.getString("gui.title"))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals(lang.getString("gui.title"))) {
            event.getPlayer().sendMessage(lang.getString("gui.closed"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
