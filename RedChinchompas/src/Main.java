import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.Player;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@ScriptManifest(name = "Red Chinchompa Hunter", description = "Hunts red chinchompas with box traps in X pattern",
        author = "RedChinchompaBot", version = 1.0, category = Category.HUNTING, image = "")

public class Main extends AbstractScript {

    private final Random random = new Random();
    
    // Red chinchompa hunting area (updated coordinates)
    private final Area RED_CHINCHOMPA_AREA = new Area(2551, 2913, 2562, 2922);
    private final Tile HUNTING_CENTER = new Tile(2557, 2918, 0); // Center point for X pattern
    
    // X pattern - all traps touching at corners on adjacent tiles
    private final Tile[] X_PATTERN_4_TRAPS = {
        new Tile(2556, 2919, 0), // Top left
        new Tile(2557, 2918, 0), // Top right (adjacent diagonally)
        new Tile(2556, 2917, 0), // Bottom left (adjacent diagonally)  
        new Tile(2557, 2916, 0)  // Bottom right (adjacent diagonally)
    };
    
    private final Tile[] X_PATTERN_5_TRAPS = {
        new Tile(2556, 2919, 0), // Top left
        new Tile(2557, 2918, 0), // Top right (adjacent diagonally)
        new Tile(2556, 2918, 0), // Center (touching both)
        new Tile(2556, 2917, 0), // Bottom left (adjacent diagonally)
        new Tile(2557, 2916, 0)  // Bottom right (adjacent diagonally)
    };
    
    private List<Tile> trapLocations = new ArrayList<>();
    private int maxTraps = 4; // Default to 4 traps
    
    private enum State {
        CHECKING_REQUIREMENTS,
        WALKING_TO_AREA,
        SETTING_TRAPS,
        MONITORING_TRAPS,
        COLLECTING_CATCH
    }
    
    private State currentState = State.CHECKING_REQUIREMENTS;

    @Override
    public void onStart() {
        Logger.log("Red Chinchompa Hunter started!");
        
        // Determine trap count based on hunter level
        int hunterLevel = Skills.getRealLevel(Skill.HUNTER);
        if (hunterLevel >= 80) {
            maxTraps = 5;
            trapLocations = List.of(X_PATTERN_5_TRAPS);
            Logger.log("Using 5 traps (Hunter level: " + hunterLevel + ")");
        } else if (hunterLevel >= 60) {
            maxTraps = 4;
            trapLocations = List.of(X_PATTERN_4_TRAPS);
            Logger.log("Using 4 traps (Hunter level: " + hunterLevel + ")");
        } else {
            Logger.log("Hunter level too low! Need at least level 60. Current: " + hunterLevel);
            stop();
        }
        
        // Check for existing traps in the area
        checkExistingTraps();
    }
    
    private void checkExistingTraps() {
        Logger.log("Checking for existing traps in the area...");
        int existingTraps = 0;
        
        // Look for any existing box traps in the hunting area
        for (GameObject trap : GameObjects.all(obj -> 
            obj != null && 
            obj.getName().contains("Box trap") &&
            RED_CHINCHOMPA_AREA.contains(obj.getTile())
        )) {
            existingTraps++;
            Logger.log("Found existing trap at: " + trap.getTile() + " - " + trap.getName());
        }
        
        // Look for shaking boxes (traps with catches)
        for (GameObject trap : GameObjects.all(obj -> 
            obj != null && 
            obj.getName().contains("Shaking box") &&
            RED_CHINCHOMPA_AREA.contains(obj.getTile())
        )) {
            existingTraps++;
            Logger.log("Found existing shaking box at: " + trap.getTile() + " - ready to collect!");
        }
        
        Logger.log("Total existing traps found: " + existingTraps);
        
        if (existingTraps > 0) {
            Logger.log("Existing traps detected - will incorporate them into hunting routine");
            // Start with monitoring state instead of setting traps
            currentState = State.WALKING_TO_AREA;
        }
    }

    @Override
    public int onLoop() {
        switch (currentState) {
            case CHECKING_REQUIREMENTS:
                if (checkRequirements()) {
                    currentState = State.WALKING_TO_AREA;
                } else {
                    Logger.log("Requirements not met, stopping bot");
                    stop();
                }
                break;
                
            case WALKING_TO_AREA:
                if (walkToHuntingArea()) {
                    currentState = State.SETTING_TRAPS;
                }
                break;
                
            case SETTING_TRAPS:
                if (setTraps()) {
                    currentState = State.MONITORING_TRAPS;
                }
                break;
                
            case MONITORING_TRAPS:
                monitorAndCollectTraps();
                break;
                
            case COLLECTING_CATCH:
                collectCatch();
                currentState = State.SETTING_TRAPS; // Reset traps after collection
                break;
        }
        
        return randomSleep(100, 300);
    }
    
    private boolean checkRequirements() {
        int hunterLevel = Skills.getRealLevel(Skill.HUNTER);
        
        if (hunterLevel < 60) {
            Logger.log("Hunter level too low! Need at least 60. Current: " + hunterLevel);
            return false;
        }
        
        // Count existing traps in the area
        int existingTrapsCount = countAllExistingTraps();
        int boxTrapCount = Inventory.count("Box trap");
        int totalTraps = existingTrapsCount + boxTrapCount;
        
        if (totalTraps < maxTraps) {
            Logger.log("Not enough total traps! Need " + maxTraps + ", have " + existingTrapsCount + " existing + " + boxTrapCount + " in inventory = " + totalTraps);
            return false;
        }
        
        Logger.log("Requirements met - Hunter level: " + hunterLevel + ", Existing traps: " + existingTrapsCount + ", Inventory traps: " + boxTrapCount + ", Total: " + totalTraps);
        return true;
    }
    
    private int countAllExistingTraps() {
        int count = 0;
        
        // Count all types of existing traps in the hunting area
        for (GameObject trap : GameObjects.all(obj -> 
            obj != null && 
            RED_CHINCHOMPA_AREA.contains(obj.getTile()) &&
            (obj.getName().contains("Box trap") || obj.getName().contains("Shaking box"))
        )) {
            count++;
        }
        
        return count;
    }
    
    private boolean walkToHuntingArea() {
        if (RED_CHINCHOMPA_AREA.contains(Players.getLocal())) {
            Logger.log("Already in red chinchompa area");
            return true;
        }
        
        Logger.log("Walking to red chinchompa hunting area...");
        if (Walking.walk(HUNTING_CENTER)) {
            Sleep.sleepUntil(() -> RED_CHINCHOMPA_AREA.contains(Players.getLocal()), 5000);
            return RED_CHINCHOMPA_AREA.contains(Players.getLocal());
        }
        
        return false;
    }
    
    private boolean setTraps() {
        Logger.log("Setting up " + maxTraps + " box traps in X pattern...");
        
        int trapsSet = 0;
        for (Tile trapLocation : trapLocations) {
            if (trapsSet >= maxTraps) break;
            
            // Check if trap already exists at this location
            GameObject existingTrap = GameObjects.closest(obj -> 
                obj != null && 
                obj.getName().contains("Box trap") && 
                obj.getTile().equals(trapLocation)
            );
            
            if (existingTrap != null) {
                Logger.log("Trap already exists at " + trapLocation);
                trapsSet++;
                continue;
            }
            
            // Check if we have box traps in inventory
            if (!Inventory.contains("Box trap")) {
                Logger.log("No more box traps in inventory");
                break;
            }
            
            // Walk to trap location
            if (!Players.getLocal().getTile().equals(trapLocation)) {
                Walking.walk(trapLocation);
                Sleep.sleepUntil(() -> Players.getLocal().getTile().distance(trapLocation) <= 1, 3000);
            }
            
            // Lay the box trap
            if (Inventory.interact("Box trap", "Lay")) {
                Logger.log("Laying box trap at " + trapLocation);
                Sleep.sleepUntil(() -> !Inventory.contains("Box trap") || 
                    GameObjects.closest(obj -> 
                        obj != null && 
                        obj.getName().contains("Box trap") && 
                        obj.getTile().distance(trapLocation) <= 1
                    ) != null, 3000);
                trapsSet++;
                Sleep.sleep(randomSleep(500, 1000)); // Small delay between trap placements
            } else {
                Logger.log("Failed to lay box trap at " + trapLocation);
            }
        }
        
        Logger.log("Set " + trapsSet + " traps out of " + maxTraps + " planned");
        return trapsSet > 0;
    }
    
    private void monitorAndCollectTraps() {
        // Check each trap location for catches or failures
        for (Tile trapLocation : trapLocations) {
            GameObject trap = GameObjects.closest(obj -> 
                obj != null && 
                obj.getTile().distance(trapLocation) <= 2 && 
                (obj.getName().contains("Box trap") || obj.getName().contains("Shaking box"))
            );
            
            if (trap == null) {
                continue; // No trap at this location
            }
            
            String trapName = trap.getName();
            
            // Check for successful catch - shaking box means red chinchompa caught
            if (trapName.contains("Shaking box")) {
                Logger.log("Found red chinchompa catch at " + trapLocation + " - " + trapName);
                
                // Walk to the trap
                if (Players.getLocal().getTile().distance(trapLocation) > 1) {
                    Walking.walk(trapLocation);
                    Sleep.sleepUntil(() -> Players.getLocal().getTile().distance(trapLocation) <= 1, 2000);
                }
                
                // Check the shaking box to collect the red chinchompa
                if (trap.interact("Check")) {
                    Logger.log("Checking shaking box to collect red chinchompa");
                    Sleep.sleepUntil(() -> 
                        GameObjects.closest(obj -> 
                            obj != null && 
                            obj.getTile().distance(trapLocation) <= 1 && 
                            obj.getName().contains("Shaking box")
                        ) == null, 3000);
                    Sleep.sleep(randomSleep(500, 1000));
                }
            }
            
            // Check for any box trap with "Take" option (failed, fallen, or empty traps)
            else if (trapName.contains("Box trap")) {
                // Check if this trap has a "Take" option available
                String[] actions = trap.getActions();
                boolean canTake = false;
                for (String action : actions) {
                    if (action != null && action.equals("Take")) {
                        canTake = true;
                        break;
                    }
                }
                
                if (canTake) {
                    Logger.log("Found box trap with Take option at " + trapLocation);
                    
                    // Walk to the trap
                    if (Players.getLocal().getTile().distance(trapLocation) > 1) {
                        Walking.walk(trapLocation);
                        Sleep.sleepUntil(() -> Players.getLocal().getTile().distance(trapLocation) <= 1, 2000);
                    }
                    
                    // Take the trap
                    if (trap.interact("Take")) {
                        Logger.log("Taking box trap");
                        Sleep.sleepUntil(() -> Inventory.contains("Box trap"), 3000);
                        Sleep.sleep(randomSleep(500, 1000));
                    }
                }
            }
            
            // Check for fallen/failed traps (backup check)
            else if (trapName.contains("Fallen box") || trapName.contains("Failed")) {
                Logger.log("Found failed trap at " + trapLocation);
                
                // Walk to the trap
                if (Players.getLocal().getTile().distance(trapLocation) > 1) {
                    Walking.walk(trapLocation);
                    Sleep.sleepUntil(() -> Players.getLocal().getTile().distance(trapLocation) <= 1, 2000);
                }
                
                // Pick up the failed trap
                if (trap.interact("Take")) {
                    Logger.log("Picking up failed trap");
                    Sleep.sleepUntil(() -> Inventory.contains("Box trap"), 3000);
                    Sleep.sleep(randomSleep(500, 1000));
                }
            }
        }
        
        // Check if we need to reset traps
        int activeTraps = countActiveTraps();
        if (activeTraps < maxTraps && Inventory.contains("Box trap")) {
            Logger.log("Need to reset traps. Active: " + activeTraps + ", Max: " + maxTraps);
            currentState = State.SETTING_TRAPS;
        }
        
        // Check if inventory is getting full
        if (Inventory.fullSlotCount() >= 26) {
            Logger.log("Inventory nearly full, may need banking");
            // Could add banking logic here if needed
        }
    }
    
    private void collectCatch() {
        // This method can be expanded for specific catch collection logic
        Logger.log("Collecting catches...");
    }
    
    private int countActiveTraps() {
        int count = 0;
        for (Tile trapLocation : trapLocations) {
            GameObject trap = GameObjects.closest(obj -> 
                obj != null && 
                obj.getTile().distance(trapLocation) <= 2 && 
                obj.getName().contains("Box trap") &&
                !obj.getName().contains("Fallen")
            );
            if (trap != null) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void onExit() {
        Logger.log("Red Chinchompa Hunter stopped!");
    }
    
    private int randomSleep(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }
}
