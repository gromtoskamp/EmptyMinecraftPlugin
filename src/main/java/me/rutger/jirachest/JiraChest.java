package me.rutger.jirachest;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;

import net.md_5.bungee.api.chat.*;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.block.Chest;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//todo jira reload command
public class JiraChest extends JavaPlugin implements Listener, TabCompleter {
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

        // if no chests are set in config.yml, skip populating chestCfg & triggering holograms
        if (config.getConfigurationSection("chests") == null) {
            Bukkit.broadcastMessage("JiraChest: No chests are set!");
            return;
        }

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
    public void leave(InventoryCloseEvent event) throws IOException {
        Inventory inventory = event.getInventory();

        // Check if jira chest
        if (!matchChest( world.getBlockAt( inventory.getLocation() ))){
            return;
        }

        // Get chest contents as itemstacks
        ItemStack[] chestContent = inventory.getStorageContents();
        // Iterate through itemstacks & filter on written books
        for (ItemStack stack : chestContent){
            if (stack != null && stack.getType() == Material.WRITTEN_BOOK){
                // Get bookmeta
                BookMeta issue = (BookMeta) stack.getItemMeta();
                // Get title
                debug( String.valueOf( issue.getTitle() ) );

                String regex = "(^HEET-\\d+)";

                Pattern pattern = Pattern.compile(regex);
                Matcher match = pattern.matcher( issue.getTitle() );

                if (match.find( )) {
                    debug("KEY: " + match.group(0) );

                    String key = match.group(0);
                    String transition = matchChestLookup( world.getBlockAt( inventory.getLocation() ) ).toString();

                    JiraRequest jiraRequest = new JiraRequest();

                    jiraRequest.transition(key, transition);
                }

            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) throws IOException {
        Player player = event.getPlayer();

        Action action = event.getAction();
        Block target = event.getClickedBlock();


        if (target.getType().toString().contains("BUTTON")){
            if (action != Action.RIGHT_CLICK_BLOCK){
                event.setCancelled(true);
                player.sendMessage("DON'T BREAK THE "+ ChatColor.GOLD + "FUCKING" + ChatColor.RESET + " BUTTON!");
            }

            syncJira(player);
        }
        if (target.getType() == Material.CHEST && matchChest( world.getBlockAt( target.getLocation() ))) {
            if (action == Action.LEFT_CLICK_BLOCK){
                event.setCancelled(true);
                player.sendMessage("DON'T BREAK THE "+ ChatColor.GOLD + "FUCKING" + ChatColor.RESET + " CHEST!");
            }
        }
    }


    private void syncJira(Player player) throws IOException {
        player.sendMessage("LET'S SYNC JIRA!");

        emptyChests();

        JiraRequest jiraRequest = new JiraRequest();

        // Parse request response to json object
        JSONObject json = new JSONObject( jiraRequest.getAllIssues() );

        // Iterate over issues
        json.getJSONArray("issues").forEach( keyStr -> {
            syncIssue(player, jiraRequest, keyStr);
        });
    }

    private void syncIssue(Player player, JiraRequest jiraRequest, Object keyStr) {

        // Create JSONObjects from issue
        JSONObject issue = new JSONObject(keyStr.toString());
        JSONObject fields = issue.getJSONObject("fields");
        JSONObject creator = fields.getJSONObject("creator");
        JSONObject status = fields.getJSONObject("status");

        // Get strings from issue json
        String key = issue.get("key").toString();
        String summary = fields.get("summary").toString();
        String description = "";
        String title = key + " " + summary;

        // Trim title after 32 chars, this is a Minecraft limit
        title = title.substring(0, Math.min(title.length(), 32));

        // Filter out "null" from json, this ends up as a string in the json response from Jira for some reason..
        if ( fields.get("description").toString().equalsIgnoreCase("NULL") == false ) {
            // Set desciption
            description = fields.get("description").toString();
        }

        // Define page
        ComponentBuilder page = new ComponentBuilder();

        // Add title to page in Bold
        TextComponent bookTitle = new TextComponent(TextComponent.fromLegacyText("§l"+key + " " + summary + "§r"));
        page.append(bookTitle);

        // Define clickable link to Jira page
        final TextComponent issuelink = new TextComponent();
        final TextComponent link = new TextComponent(TextComponent.fromLegacyText("\n\n » View on Jira\n\n"));
        link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, jiraRequest.getUri("/browse/"+key)));
        link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("Click to visit Jira!")));
        link.setUnderlined(true);
        link.setColor( net.md_5.bungee.api.ChatColor.BLUE );

        issuelink.addExtra(link);
        page.append(issuelink);

        page.create();

        // Define description
        TextComponent bookDescription = new TextComponent(TextComponent.fromLegacyText(description));
        page.append(bookDescription);

        // Build new written book
        ItemStack writtenBook = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookMeta = (BookMeta) writtenBook.getItemMeta();

        // Add title
        bookMeta.setTitle( title );

        // Add Jira reporter as author
        bookMeta.setAuthor( creator.get("displayName").toString() );

        // Add page to book
        bookMeta.spigot().addPage( page.create() );

        // Finish up book
        writtenBook.setItemMeta(bookMeta);

        String lane = getLane(status.get("id").toString());
        // Put book in correct chest
        putInChest(writtenBook, lane);

        // DEBUG: output lane
//        debug(lane);
    }

    private String getLane(String statusId) {
        // Get all available lanes from config.yml
        List lanes = getLanes();
        // Add lane > location to map if stored in config.yml
        for (Object lane: lanes) {
            String laneKey = String.valueOf(lane);
            String laneId = config.get("laneIds."+laneKey+".id").toString();

            if (laneId.equalsIgnoreCase(statusId)) {
                return laneKey;
            }
        }

        return "";
    }

    // Put book in correct chest, according to lane
    public void putInChest(ItemStack book, String lane) {
        Location chestLocation;
        try {
            chestLocation = getChestLocation(lane);
        } catch (RuntimeException e) {
            debug(e.getMessage());
            return;
        }

        Chest chest = (Chest) chestLocation.getBlock().getState();
        chest.getInventory().addItem(book);
    }

    // Clear jira chests
    public void emptyChests() {
        for (Object lane : getLanes()) {
            Location chestLocation = getChestLocation(lane.toString());
            Chest chest = (Chest) chestLocation.getBlock().getState();

            chest.getInventory().clear();
        }
    }

    public Location getChestLocation(String lane) {
        for (Object location : chestsCfg.keySet()) {
            if (location.toString().equalsIgnoreCase(lane)) {
                Location holoLocation = (Location) chestsCfg.get(lane);
                return new Location(world, holoLocation.getX(), holoLocation.getY()-2, holoLocation.getZ());
            }
        }

        throw new RuntimeException("Chest " + lane + " not found");
    }

    // Match event chest to chest in chestCfg
    public boolean matchChest(Block eventBlock){
        // Iterate through chestCfg map
        for (Object c : chestsCfg.values()){
            // Compare eventBlock against chests from chestCfg
            if (world.getBlockAt( (Location)c ).getRelative(0, -2, 0).equals(eventBlock)){
                return true;
            }
        }

        return false;
    }

    // Lookup event chest in chestCfg
    public Object matchChestLookup(Block eventBlock){
        // Iterate through chestCfg map
        for (Object key : chestsCfg.keySet()) {

            if (world.getBlockAt( (Location) chestsCfg.get(key) ).getRelative(0, -2, 0).equals(eventBlock)){
//                debug( key.toString() );
//                debug( config.get("laneIds."+key+".transition").toString() );
                return config.get("laneIds."+key+".transition").toString();
            }
        }

        return null;
    }

    // Build chestCfg map
    public void reloadChestCfg(){
        // Get all available lanes from config.yml
        List<Object> lanes = getLanes();
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

    private List<Object> getLanes() {
        return (List) config.get("lanes");
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

        if (config.contains("syncButton") == true){
            // Set hologram for sync button
            Double X = (Double) config.get("syncButton.x");
            Double Y = (Double) config.get("syncButton.holoY");
            Double Z = (Double) config.get("syncButton.z");

            Location syncButton = new Location( world, (Double)X, (Double)Y, (Double)Z );

            Hologram hologram = HologramsAPI.createHologram(plugin, syncButton);
            TextLine textLine = hologram.appendTextLine("Sync Jira");
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

    public void setSync(Player player){
        Block target = player.getTargetBlockExact(16);
        Location syncButton = null;
        // Check if block player is looking at is a button
        if (target.getType().toString().contains("BUTTON")){
            syncButton = target.getLocation();
        } else {
            // Check if any of the attached blocks is a button
            if( target.getRelative(BlockFace.NORTH).toString().contains("BUTTON")) syncButton = target.getRelative(BlockFace.NORTH).getLocation();
            if( target.getRelative(BlockFace.SOUTH).toString().contains("BUTTON")) syncButton = target.getRelative(BlockFace.SOUTH).getLocation();
            if( target.getRelative(BlockFace.EAST).toString().contains("BUTTON")) syncButton = target.getRelative(BlockFace.EAST).getLocation();
            if( target.getRelative(BlockFace.WEST).toString().contains("BUTTON")) syncButton = target.getRelative(BlockFace.WEST).getLocation();
            if( target.getRelative(BlockFace.UP).toString().contains("BUTTON")) syncButton = target.getRelative(BlockFace.UP).getLocation();
            if( target.getRelative(BlockFace.DOWN).toString().contains("BUTTON")) syncButton = target.getRelative(BlockFace.DOWN).getLocation();
        }

        if (syncButton != null){
            // Center sync button location and save location to config.yml
            Double X = syncButton.getX() + 0.5;
            Double Y = syncButton.getY();
            Double Z = syncButton.getZ() + 0.5;

            config.set("syncButton.x", X);
            config.set("syncButton.y", Y);
            config.set("syncButton.z", Z);

            // Generate & save holo
            double holoY = target.getLocation().getY() + 1;
            config.set("syncButton.holoY", holoY );
            // Save config file
            saveConfig();

            // Update chestsCfg
            reloadChestCfg();
            // Clear holograms
            clearHolos();
            // Display holograms with updated values
            setHolos();
        } else {
            player.sendMessage("Couldn't find a button..");
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
        // Autocomplete for "setchest"
        if (command.getName().equalsIgnoreCase("setchest") && args.length == 1) {
            if (sender instanceof Player) {
                // Get all available lanes from config.yml
                List lanes = getLanes();
                return lanes;
            }
        }
        // autocomplete for listchests
        if (command.getName().equalsIgnoreCase("listchests") && args.length == 1) {
            if (sender instanceof Player) {
                // Get all stored chests so only stored chests will be shown
                ArrayList<String> chests = new ArrayList<String>(listChests());
                // Add "all"
                chests.add("all");
                return chests;
            }
        }
        return null;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // Set chest as a jira chest (check if sender is a player)
        if (label.equalsIgnoreCase("setchest") && args.length > 0 && sender instanceof Player){
            // Get list of valid args from config -> lanes
            List lanes = getLanes();
            if (lanes.contains(args[0].toLowerCase())){
                setChest(args[0].toLowerCase(), (Player) sender);
                return true;
            } else {
                return false;
            }

        }

        // Set a button to sync with Jira (check if sender is a player)
        if (label.equalsIgnoreCase("setsync") && sender instanceof Player) {
            setSync((Player) sender);
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

        if (label.equalsIgnoreCase("clearchests")){
            emptyChests();
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

        // DEBUG: general testing function
        if (label.equalsIgnoreCase("test")){

            JiraRequest jiraRequest = new JiraRequest();

            try {
                // Get specific issue json
                debug( jiraRequest.getIssue("HEET-1") );
                debug (jiraRequest.getTransitions("HEET-1"));

                // Get all open issues json
//                debug( jiraRequest.getAllIssues());

                // Transition issue to another lane
                // Get lanes - id's from config
//                HashMap<String, String> laneIds = new HashMap<String, String>();
//                for (String lane : config.getConfigurationSection("laneIds").getKeys(false)){
//                    String laneID = config.get("laneIds."+lane).toString();
//                    laneIds.put(lane, laneID );
//                }
//                debug( jiraRequest.transition("HEET-1", laneIds.get("backlog")));

            } catch (IOException e) {
                e.printStackTrace();
            }




        }
        return true;
    }
}
