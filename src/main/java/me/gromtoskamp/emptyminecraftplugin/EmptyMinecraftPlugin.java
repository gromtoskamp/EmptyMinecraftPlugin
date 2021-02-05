package me.gromtoskamp.emptyminecraftplugin;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class EmptyMinecraftPlugin extends JavaPlugin implements Listener, TabCompleter {

    // Define some global vars
    public static Plugin plugin;

    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;

        // Register events, i.e. onInventoryClick
        registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Stop memory leaks
        plugin = null;
    }

    // Register events for plugin
    public static void registerEvents(org.bukkit.plugin.Plugin plugin, Listener... listeners) {
        for (Listener listener : listeners) {
            Bukkit.getServer().getPluginManager().registerEvents(listener, plugin);
        }
    }
}
