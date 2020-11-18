package me.rutger.block_data_test;


import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public final class Block_data_test extends JavaPlugin implements Listener, TabCompleter {
    // Set config(.yml)
    FileConfiguration config = getConfig();

    // Define some global vars
    public static Plugin plugin;

    // Define world
    public static World world;

    // Define dynamic chest map
    public HashMap chestsCfg;

    @Override
    public void onEnable() {
        getLogger().info("DEBUG: onEnable");

        // Save config.yml
        this.saveDefaultConfig();

        // Plugin startup logic
        plugin = this;

        // Register events, i.e. onInventoryClick
        registerEvents(this, this);

        if (!Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays")) {
            getLogger().severe("*** HolographicDisplays is not installed or not enabled. ***");
            getLogger().severe("*** This plugin will be disabled. ***");
            this.setEnabled(false);
            return;
        }

        // Get world
        world = Bukkit.getWorlds().get(0);

        chestsCfg = new HashMap<String, Block>();

        getLogger().info( "DEBUG chests from config.yml:" + String.valueOf( config.getConfigurationSection("chests").getKeys(false) ));

        // Build chestsCfg map
        reloadChestCfg();
        // Clear existing holograms for reloading purposes
        clearHolos();
        // Set & display all holograms
        setHolos();

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

        // Remove all holograms
        for (Hologram holo : HologramsAPI.getHolograms(plugin)) {
            holo.delete();
        }

        // Stop memory leaks
        plugin = null;
    }

    // Register events for plugin
    public static void registerEvents(org.bukkit.plugin.Plugin plugin, Listener... listeners) {
        for (Listener listener : listeners) {
            Bukkit.getServer().getPluginManager().registerEvents(listener, plugin);
        }
    }

    // Debug function
    public void debug (String msg){
        Bukkit.broadcastMessage( msg );
    }

    @EventHandler
    public void leave(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();

        // Check if jira chest
        if (matchChest( world.getBlockAt( inventory.getLocation() ))){

            // Get chest contents as itemstacks
            ItemStack[] chestContent = inventory.getStorageContents();
            // Iterate through itemstacks & filter on written books
            for (ItemStack stack : chestContent){
                if (stack != null && stack.getType() == Material.WRITTEN_BOOK){
                    // Get bookmeta
                    BookMeta issue = (BookMeta) stack.getItemMeta();
                    // Get title
                    debug( String.valueOf( issue.getTitle() ) );
                }
            }
        }
    }


        // Match event chest to chest in chestCfg
    public boolean matchChest(Block eventBlock){
        Boolean match = false;
        // Iterate through chestCfg map
        for (Object c : chestsCfg.values()){
            // Compare eventBlock against chests from chestCfg
            if (world.getBlockAt( (Location)c ).getRelative(0, -2, 0).equals(eventBlock)){
                match = true;
            }
        }
        return match;
    }

    // Build chestCfg map
    public void reloadChestCfg(){
        // Get all available lanes from config.yml
        List lanes = (List) config.get("lanes");
        // Add lane > location to map if stored in config.yml
        for (Object lane: lanes) {
            String laneKey = String.valueOf(lane);

            if ( config.getConfigurationSection("chests").getKeys(false).contains( laneKey)){
                Double X = (Double) config.get("chests."+laneKey+".x");
                Double Y = (Double) config.get("chests."+laneKey+".y");
                Double Z = (Double) config.get("chests."+laneKey+".z");

                Location loc = new Location( world, (Double)X, (Double)Y, (Double)Z );

                chestsCfg.put(laneKey, loc);
            }
        }
    }

    public void clearHolos(){
        for (Hologram holo : HologramsAPI.getHolograms(plugin)) {
            holo.delete();
        }
    }
    public void setHolos(){
        // Set holograms for all set chests
        for (Object c : chestsCfg.keySet() ){
            // Get chest as a block
            Location chest = (Location) chestsCfg.get(c);
            // Modify Y for holo location
            double holoY = chest.getY() + 2;

            Location holoLoc = chest;
            holoLoc.setY(holoY);

            // Add hologram
            Hologram hologram = HologramsAPI.createHologram(plugin, holoLoc);
            TextLine textLine = hologram.appendTextLine(c.toString().toUpperCase());
        }
    }

    // Function to set a chest as a jira lane
    public boolean setChest(String lane, Player player){
        // Get block player is looking at as "target"
        Block target = player.getTargetBlockExact(16);

        // If target is a chest
        if (target.getType() == Material.CHEST){

            // Center chest location and save chest location to config.yml
            Double X = target.getLocation().getX() + 0.5;
            Double Y = target.getLocation().getY();
            Double Z = target.getLocation().getZ() + 0.5;

            config.set("chests." + lane + ".x", X);
            config.set("chests." + lane + ".y", Y);
            config.set("chests." + lane + ".z", Z);

            // Generate & save holo
            double holoY = target.getLocation().getY() + 2;
            config.set("chests." + lane + ".holoY", holoY );

            // Save config file
            saveConfig();

            // Update chestsCfg
            reloadChestCfg();
            // Clear holograms
            clearHolos();
            // Display holograms with updated values
            setHolos();

            return true;
        } else {
            // If block is not a chest, return error message to player
            debug("Target block is not a chest!");
            return false;
        }
    }

    // Return List of set chests
    public List<String> listChests(){
        if (getConfig().contains( "chests")) {
            List<String> chests = new ArrayList<String>(config.getConfigurationSection("chests").getKeys(false));
            return chests;
        }
        return null;
    }

    //Tab completer for commands
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("setchest") && args.length == 1) {
            if (sender instanceof Player) {
                // Get all available lanes from config.yml
                List lanes = (List) config.get("lanes");
                return lanes;
            }
        }
        if (command.getName().equalsIgnoreCase("listchests") && args.length == 1) {
            if (sender instanceof Player) {
                // Get all stored chests
                ArrayList<String> chests = new ArrayList<String>(listChests());
                // Add "all"
                chests.add("all");
                return chests;
            }
        }
        return null;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Set chest as a jira chest
        if (label.equalsIgnoreCase("setchest") && args.length > 0){
            // Get list of valid args from config -> lanes
            List lanes = (List) config.get("lanes");
            if (lanes.contains(args[0].toLowerCase())){
                setChest(args[0].toLowerCase(), (Player) sender);
                return true;
            } else {
                return false;
            }

        }

        // Quick & dirty command to list all chests
        if (label.equalsIgnoreCase("listchests")){

            // Get all stored chests from listChests() -> config.yml
            ArrayList<String> chests = new ArrayList<String>(listChests());

            // If chests isn't empty
            if (!chests.isEmpty()){
                // If first arg is "all", output all chests
                if (args.length > 0) {

                    if (args[0].equalsIgnoreCase("all")) {
                        // Return all chests
                        debug("All chests currently set: " + String.valueOf(listChests()));

                    // Else, check if first arg is set as a chest
                    } else {
                        if (chests.contains(args[0].toLowerCase())) {

                            for (String c : chests) {
                                if (c.equalsIgnoreCase(args[0])) {
                                    debug(c);

                                    Location chest = (Location) chestsCfg.get(c);
                                    Double X = chest.getX();
                                    Double Y = chest.getY();
                                    Double Z = chest.getZ();
                                    debug( "Chest at " + X.intValue() + " " + Y.intValue() + " " + Z.intValue() );
                                }
                            }
                        } else {
                            debug("Chest " + args[0] + " isn't set..");
                        }
                    }
                } else {
                    // Return all chests
                    debug("All chests currently set: " + String.valueOf(listChests()));
                }
            } else {
                // If no chests are set, return error to player
                debug("No chests are set!");
            }

        }

        // List all set holograms to player
        if (label.equalsIgnoreCase("getholos")){
            debug( String.valueOf( HologramsAPI.getHolograms(plugin) ));
        }

        // Clear all set holograms
        if (label.equalsIgnoreCase("clearholos")) {
            for (Hologram holo : HologramsAPI.getHolograms(plugin)) {
                holo.delete();
            }
        }

        // DEBUG: test function to set a holo
        if (label.equalsIgnoreCase("testholo")){
            Player player = (Player) sender;

            Location location = player.getLocation();

            location.setY(6);

            Hologram hologram = HologramsAPI.createHologram(plugin, location);

            TextLine textLine = hologram.appendTextLine("TEST");
        }
        // DEBUG: general testing function
        if (label.equalsIgnoreCase("test")){
            debug("test?");
        }
        return true;
    }
}
