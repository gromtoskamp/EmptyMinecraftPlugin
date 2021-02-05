package me.heetmeisje.netherkippen;

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

public class NetherKippen extends JavaPlugin implements Listener, TabCompleter {

    public final String netherWorldName = "world_nether";

    public final String normalKippen = "https://www.dropbox.com/s/woqhfntnrcog1rj/DefaultKip.zip?dl=1";
    public final String netherKippen = "https://www.dropbox.com/s/qsxcdahgz0jh2l5/NetherKIP.zip?dl=1";

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

    @EventHandler
    public void onPlayerLogin(PlayerJoinEvent event) {
        if (!event.getPlayer().getWorld().getName().equals(netherWorldName)) return;
        setResourcePack(event);
    }

    @EventHandler
    public void onPlayerPortal(PlayerChangedWorldEvent event) {
       setResourcePack(event);
    }

    public void setResourcePack(PlayerEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        String worldName = world.getName();
        if (worldName.equals(netherWorldName)) {
            player.setResourcePack(netherKippen);
            return;
        }

        player.setResourcePack(normalKippen);
    }
}
