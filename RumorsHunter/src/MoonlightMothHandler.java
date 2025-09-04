import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.methods.map.Tile;

public class MoonlightMothHandler implements HuntingHandler {
    
    private static final Tile HUNTING_AREA = new Tile(1568, 9441, 0);
    private static final int HUNTING_RADIUS = 10;
    private static final Tile BANK_TILE = new Tile(1543, 3040, 0); // Hunter Guild bank
    
    // Required items for moth catching
    private static final String[] REQUIRED_ITEMS = {};
    
    @Override
    public boolean checkRequirements() {
        Logger.log("Checking moonlight moth hunting requirements...");
        
        int hunterLevel = Skills.getRealLevel(Skill.HUNTER);
        if (hunterLevel < 15) {
            Logger.log("Hunter level too low for moonlight moths. Required: 15, Current: " + hunterLevel);
            return false;
        }
        
        Logger.log("Hunter level requirement met: " + hunterLevel + "/15");
        return true;
    }
    
    @Override
    public boolean prepareEquipment() {
        Logger.log("Preparing equipment for moonlight moth hunting...");
        Logger.log("Equipment ready: No special equipment required");
        return true;
    }
    
    @Override
    public boolean walkToHuntingArea() {
        Logger.log("Walking to moonlight moth hunting area...");
        
        while (Players.getLocal().getTile().distance(HUNTING_AREA) > HUNTING_RADIUS) {
            if (Walking.shouldWalk(8)) {
                Logger.log("Walking to moonlight moth area: " + HUNTING_AREA);
                if (Walking.walk(HUNTING_AREA)) {
                    Sleep.sleep(1500, 2200);
                } else {
                    Logger.log("Walk failed, retrying...");
                    Sleep.sleep(1000, 1500);
                }
            } else {
                Sleep.sleep(600, 1000);
            }
        }
        
        Logger.log("Successfully reached moonlight moth hunting area");
        return true;
    }
    
    @Override
    public boolean hunt() {
        Logger.log("Hunting moonlight moths...");
        
        // Check if we already have the moonlight moth wing
        if (Inventory.contains("Moonlight moth wing")) {
            Logger.log("Moonlight moth wing obtained! Going to guild guide...");
            return false; // Signal to change state
        }
        
        // Look for moonlight moths in the area
        NPC moonlightMoth = NPCs.closest(npc -> {
            String name = npc.getName();
            return name != null && name.toLowerCase().contains("moonlight moth");
        });
        
        if (moonlightMoth == null) {
            // Try broader search for moths
            moonlightMoth = NPCs.closest(npc -> {
                String name = npc.getName();
                return name != null && name.toLowerCase().contains("moth");
            });
        }
        
        if (moonlightMoth != null && moonlightMoth.exists()) {
            Logger.log("Found moonlight moth, attempting to catch...");
            
            if (moonlightMoth.interact("Catch")) {
                Logger.log("Attempting to catch moonlight moth...");
                Sleep.sleep(2000, 4000);
                
                // Wait for catch animation to complete
                int waitTime = 0;
                while (Players.getLocal().isAnimating() && waitTime < 10000) {
                    Sleep.sleep(500);
                    waitTime += 500;
                }
                
                // Check if we got the wing after catching
                if (Inventory.contains("Moonlight moth wing")) {
                    Logger.log("SUCCESS! Moonlight moth wing obtained!");
                    return false; // Signal to change state
                }
                
                Logger.log("Catch attempt completed");
                return true;
            } else {
                Logger.log("Failed to interact with moonlight moth");
            }
        } else {
            Logger.log("No moonlight moths found in area, waiting...");
            Sleep.sleep(3000, 5000);
        }
        
        return false;
    }
    
    @Override
    public boolean bankLoot() {
        Logger.log("Banking moonlight moth loot...");
        
        // Walk to bank using smooth walking
        while (Players.getLocal().getTile().distance(BANK_TILE) > 5) {
            if (Walking.shouldWalk(8)) {
                Logger.log("Walking to bank: " + BANK_TILE);
                if (Walking.walk(BANK_TILE)) {
                    Sleep.sleep(1500, 2200);
                } else {
                    Logger.log("Walk to bank failed, retrying...");
                    Sleep.sleep(1000, 1500);
                }
            } else {
                Sleep.sleep(600, 1000);
            }
        }
        
        // Open bank
        GameObject bankBooth = GameObjects.closest("Bank booth");
        if (bankBooth == null) {
            bankBooth = GameObjects.closest("Bank");
        }
        
        if (bankBooth != null && bankBooth.interact("Bank")) {
            Logger.log("Opening bank...");
            Sleep.sleep(2000, 3000);
            
            if (Bank.isOpen()) {
                Logger.log("Bank opened successfully");
                
                // Deposit all items except food/potions
                Bank.depositAllExcept("Lobster", "Shark", "Food");
                Logger.log("Deposited all items");
                Sleep.sleep(1000, 2000);
                
                Bank.close();
                Logger.log("Banking completed successfully");
                return true;
            } else {
                Logger.log("Failed to open bank");
            }
        } else {
            Logger.log("Could not find bank booth");
        }
        
        return false;
    }
    
    @Override
    public int getRequiredLevel() {
        return 15;
    }
    
    @Override
    public String[] getRequiredItems() {
        return REQUIRED_ITEMS;
    }
    
    @Override
    public int[] getHuntingAreaLocation() {
        return new int[]{HUNTING_AREA.getX(), HUNTING_AREA.getY(), HUNTING_AREA.getZ()};
    }
    
    @Override
    public int[] getBankLocation() {
        return new int[]{BANK_TILE.getX(), BANK_TILE.getY(), BANK_TILE.getZ()};
    }
    
    public boolean goToGuide() {
        Logger.log("Going to Guild Hunter Wolf (Master) with moonlight moth wing...");
        
        if (Inventory.contains("Moonlight moth wing")) {
            Logger.log("Moonlight moth wing confirmed in inventory");
            Logger.log("Calling GuildUtils.goToBasementAndTalkToWolf()...");
            boolean result = GuildUtils.goToBasementAndTalkToWolf();
            Logger.log("GuildUtils.goToBasementAndTalkToWolf() returned: " + result);
            return result;
        } else {
            Logger.log("ERROR: No moonlight moth wing found! Cannot proceed to guide.");
            return false;
        }
    }
}