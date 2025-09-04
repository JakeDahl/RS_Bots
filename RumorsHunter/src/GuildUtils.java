import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.methods.map.Tile;

public class GuildUtils {
    
    private static final Tile BASEMENT_TILE = new Tile(1556, 9462, 0);
    private static final String GUILD_HUNTER_WOLF_NAME = "Guild Hunter Wolf";
    private static final String MASTER_NAME = "Master";
    
    public static boolean goToGuildBasement() {
        Logger.log("Navigating to Hunter Guild basement...");
        
        while (Players.getLocal().getTile().distance(BASEMENT_TILE) > 3) {
            if (Walking.shouldWalk(6)) {
                Logger.log("Walking to basement tile: " + BASEMENT_TILE);
                if (Walking.walk(BASEMENT_TILE)) {
                    // Wait for walking to start and complete
                    Sleep.sleep(1800, 2500);
                } else {
                    Logger.log("Walk failed, retrying...");
                    Sleep.sleep(1000, 1500);
                }
            } else {
                // Player is moving, just wait
                Sleep.sleep(600, 1200);
            }
        }
        
        Logger.log("Successfully reached Hunter Guild basement");
        return true;
    }
    
    public static boolean talkToGuildHunterWolf() {
        Logger.log("Looking for Guild Hunter Wolf (Master)...");
        
        // Look for Guild Hunter Wolf first
        NPC guildHunterWolf = NPCs.closest(GUILD_HUNTER_WOLF_NAME);
        if (guildHunterWolf != null && guildHunterWolf.exists()) {
            Logger.log("Found Guild Hunter Wolf, attempting to talk...");
            if (guildHunterWolf.interact("Talk-to")) {
                Sleep.sleep(2000, 3000);
                Logger.log("Successfully talked to Guild Hunter Wolf");
                return true;
            } else {
                Logger.log("Failed to interact with Guild Hunter Wolf");
            }
        }
        
        // If Guild Hunter Wolf not found, look for Master
        NPC master = NPCs.closest(MASTER_NAME);
        if (master != null && master.exists()) {
            Logger.log("Found Master, attempting to talk...");
            if (master.interact("Talk-to")) {
                Sleep.sleep(2000, 3000);
                Logger.log("Successfully talked to Master");
                return true;
            } else {
                Logger.log("Failed to interact with Master");
            }
        }
        
        // If neither found, try a broader search
        java.util.List<NPC> nearbyNPCs = NPCs.all(npc -> {
            String name = npc.getName();
            return name != null && (name.contains("Guild Hunter") || name.contains("Master") || name.contains("Wolf"));
        });
        
        if (!nearbyNPCs.isEmpty()) {
            Logger.log("Found " + nearbyNPCs.size() + " potential NPCs nearby:");
            for (NPC npc : nearbyNPCs) {
                Logger.log("- " + npc.getName());
            }
            
            // Try talking to the first potential NPC
            NPC target = nearbyNPCs.get(0);
            Logger.log("Attempting to talk to: " + target.getName());
            if (target.interact("Talk-to")) {
                Sleep.sleep(2000, 3000);
                Logger.log("Successfully talked to " + target.getName());
                return true;
            }
        }
        
        Logger.log("Could not find Guild Hunter Wolf or Master in the area");
        return false;
    }
    
    public static boolean goToBasementAndTalkToWolf() {
        Logger.log("Going to Hunter Guild basement and talking to Guild Hunter Wolf...");
        
        if (goToGuildBasement()) {
            Sleep.sleep(1000, 2000); // Brief pause after arriving
            return talkToGuildHunterWolf();
        } else {
            Logger.log("Failed to reach basement, cannot talk to Wolf");
            return false;
        }
    }
    
    public static Tile getBasementTile() {
        return BASEMENT_TILE;
    }
}