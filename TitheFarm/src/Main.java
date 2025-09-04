import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.input.Keyboard;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;

import java.util.Random;
import java.awt.*;
import java.text.NumberFormat;

@ScriptManifest(name = "Tithe Farm Bot", description = "Plants Golovanova seeds, waters, harvests, and deposits at Tithe Farm",
        author = "TitheFarmBot", version = 1.0, category = Category.FARMING, image = "")

public class Main extends AbstractScript {
    
    // Tithe Farm area (updated coordinates provided)
    private final Area TITHE_FARM_AREA = new Area(new Tile(13408, 6614), new Tile(13434, 6643));
    
    // House area with seed table
    private final Tile HOUSE_CENTER = new Tile(1799, 3502);
    private final Area HOUSE_AREA = new Area(new Tile(1796, 3504), new Tile(1803, 3500));
    
    private final Random random = new Random();
    
    // XP Tracking variables
    private int startingXP;
    private int startingLevel;
    private long startTime;
    private int plantsHarvested = 0;
    private int seedsPlanted = 0;
    private final NumberFormat numberFormat = NumberFormat.getInstance();
    
    private enum State {
        GETTING_SEEDS,
        ENTERING_FARM,
        CLEARING_DEAD_PLANTS,
        PLANTING_SEEDS,
        WATERING_PLANTS,
        HARVESTING_PLANTS,
        DEPOSITING_HARVEST,
        REFILLING_WATERING_CANS,
        WAITING_FOR_GROWTH,
        IDLE
    }
    
    @Override
    public void onStart() {
        Logger.log("Tithe Farm Bot started!");
        Logger.log("Target: Tithe Farm minigame");
        Logger.log("Seeds: Golovanova seeds");
        Logger.log("Will plant, water, harvest, and deposit automatically");
        
        // Initialize XP tracking
        startingXP = Skill.FARMING.getExperience();
        startingLevel = Skill.FARMING.getLevel();
        startTime = System.currentTimeMillis();
        Logger.log("Starting Farming XP: " + numberFormat.format(startingXP));
        Logger.log("Starting Farming Level: " + startingLevel);
        
        // Check if player has required items
        if (!hasRequiredItems()) {
            Logger.log("WARNING: Make sure you have Golovanova seeds and watering cans!");
        } else {
            Logger.log("Required items detected - ready to farm!");
        }
    }
    
    private State getState() {
        // Check if we need seeds first
        if (!hasSeeds()) {
            return State.GETTING_SEEDS;
        }
        
        // Check if we have seeds but not in farming area (need to enter farm)
        if (hasSeeds() && HOUSE_AREA.contains(Players.getLocal()) && !TITHE_FARM_AREA.contains(Players.getLocal())) {
            return State.ENTERING_FARM;
        }
        
        // Check for dead plants that need clearing (high priority)
        if (hasDeadPlants()) {
            return State.CLEARING_DEAD_PLANTS;
        }
        
        // Check if we have items to deposit
        if (hasHarvestedItems()) {
            return State.DEPOSITING_HARVEST;
        }
        
        // Check if all watering cans are empty
        if (allWateringCansEmpty()) {
            return State.REFILLING_WATERING_CANS;
        }
        
        // Check for ready-to-harvest plants
        if (hasReadyPlants()) {
            return State.HARVESTING_PLANTS;
        }
        
        // Check for plants that need watering
        if (hasPlantsThatNeedWatering()) {
            return State.WATERING_PLANTS;
        }
        
        // Check if we have seeds and empty patches
        if (hasSeeds() && hasEmptyPatches()) {
            return State.PLANTING_SEEDS;
        }
        
        // If nothing to do, wait for growth
        return State.WAITING_FOR_GROWTH;
    }
    
    private boolean hasRequiredItems() {
        return Inventory.contains("Golovanova seed") && 
               (Inventory.contains("Watering can") || Inventory.contains("Watering can(8)") || 
                Inventory.contains("Watering can(7)") || Inventory.contains("Watering can(6)") ||
                Inventory.contains("Watering can(5)") || Inventory.contains("Watering can(4)") ||
                Inventory.contains("Watering can(3)") || Inventory.contains("Watering can(2)") ||
                Inventory.contains("Watering can(1)"));
    }
    
    private boolean hasSeeds() {
        return Inventory.contains("Golovanova seed");
    }
    
    private boolean hasHarvestedItems() {
        // Check for common Tithe Farm harvest items - only deposit if we have at least 20
        return Inventory.count("Golovanova fruit") >= 20;
    }
    
    private boolean allWateringCansEmpty() {
        // Check if all watering cans are empty
        return Inventory.contains("Watering can") && 
               !Inventory.contains("Watering can(8)") && !Inventory.contains("Watering can(7)") &&
               !Inventory.contains("Watering can(6)") && !Inventory.contains("Watering can(5)") &&
               !Inventory.contains("Watering can(4)") && !Inventory.contains("Watering can(3)") &&
               !Inventory.contains("Watering can(2)") && !Inventory.contains("Watering can(1)");
    }
    
    private boolean hasEmptyPatches() {
        GameObject emptyPatch = GameObjects.closest("Tithe patch");
        return emptyPatch != null && emptyPatch.exists();
    }
    
    private boolean hasPlantsThatNeedWatering() {
        // Look for planted patches that might need watering
        GameObject plantedPatch = GameObjects.closest(gameObject -> 
            gameObject != null && gameObject.getName().contains("Golovanova") && 
            gameObject.hasAction("Water"));
        return plantedPatch != null;
    }
    
    private boolean hasReadyPlants() {
        // Look for fully grown plants ready to harvest
        GameObject readyPlant = GameObjects.closest(gameObject -> 
            gameObject != null && gameObject.getName().contains("Golovanova") && 
            gameObject.hasAction("Harvest"));
        return readyPlant != null;
    }
    
    private boolean hasDeadPlants() {
        // Look for dead plants that need to be cleared
        GameObject deadPlant = GameObjects.closest(gameObject -> 
            gameObject != null && gameObject.getName().contains("Golovanova") && 
            gameObject.hasAction("Clear"));
        return deadPlant != null;
    }
    
    private void plantSeeds() {
        if (!hasSeeds()) {
            Logger.log("No seeds available for planting");
            return;
        }
        
        GameObject emptyPatch = GameObjects.closest("Tithe patch");
        if (emptyPatch != null && emptyPatch.exists()) {
            Logger.log("Planting Golovanova seed on empty patch...");
            if (Inventory.interact("Golovanova seed", "Use")) {
                Sleep.sleep(randomSleep(600, 1000));
                if (emptyPatch.interact("Use")) {
                    Sleep.sleepUntil(() -> !emptyPatch.exists() || 
                                   Inventory.count("Golovanova seed") < Inventory.count("Golovanova seed"), 
                                   randomSleep(6000, 10000)); // Increased sleep by 2x for more leniency
                    seedsPlanted++;
                    Logger.log("Successfully planted seed! (Total: " + seedsPlanted + ")");
                }
            }
        } else {
            Logger.log("No empty patches found for planting");
        }
    }
    
    private void waterPlants() {
        GameObject plantToWater = GameObjects.closest(gameObject -> 
            gameObject != null && gameObject.getName().contains("Golovanova") && 
            gameObject.hasAction("Water"));
            
        if (plantToWater != null && plantToWater.exists()) {
            Logger.log("Watering plant...");
            if (plantToWater.interact("Water")) {
                Sleep.sleepUntil(() -> !plantToWater.hasAction("Water"), 
                               randomSleep(2000, 3000)); // Reduced timeout - condition should detect completion quickly
                Logger.log("Successfully watered plant!");
            }
        } else {
            Logger.log("No plants need watering at the moment");
        }
    }
    
    private void harvestPlants() {
        GameObject readyPlant = GameObjects.closest(gameObject -> 
            gameObject != null && gameObject.getName().contains("Golovanova") && 
            gameObject.hasAction("Harvest"));
            
        if (readyPlant != null && readyPlant.exists()) {
            Logger.log("Harvesting ready plant using direct Harvest action...");
            // Ensure we're not using any inventory item - direct plant interaction only
            if (readyPlant.hasAction("Harvest") && readyPlant.interact("Harvest")) {
                Sleep.sleepUntil(() -> !readyPlant.exists() || 
                               Inventory.contains("Golovanova fruit"), 
                               randomSleep(6000, 10000)); // Increased sleep by 2x for more leniency
                plantsHarvested++;
                Logger.log("Successfully harvested plant! (Total: " + plantsHarvested + ")");
            } else {
                Logger.log("Could not harvest plant - Harvest action not available");
            }
        } else {
            Logger.log("No plants ready for harvest");
        }
    }
    
    private void depositHarvest() {
        GameObject sack = GameObjects.closest("Sack");
        if (sack != null && sack.exists()) {
            Logger.log("Depositing harvest into sack...");
            if (sack.interact("Deposit")) {
                Sleep.sleepUntil(() -> !hasHarvestedItems(), 
                               randomSleep(2000, 4000));
                Logger.log("Successfully deposited harvest!");
            }
        } else {
            Logger.log("Could not find sack for depositing");
        }
    }
    
    private void refillWateringCans() {
        GameObject waterBarrel = GameObjects.closest("Water barrel");
        if (waterBarrel != null && waterBarrel.exists()) {
            Logger.log("Refilling all watering cans at water barrel...");
            
            // Keep refilling until all watering cans are filled
            while (Inventory.contains("Watering can")) {
                Logger.log("Refilling empty watering can...");
                if (Inventory.interact("Watering can", "Use")) {
                    Sleep.sleep(randomSleep(600, 1000));
                    if (waterBarrel.interact("Use")) {
                        Sleep.sleepUntil(() -> !Inventory.contains("Watering can") || 
                                       Inventory.contains("Watering can(8)"), 
                                       randomSleep(2000, 3000));
                        Logger.log("Refilled one watering can!");
                        Sleep.sleep(randomSleep(300, 600)); // Small delay between refills
                    } else {
                        Logger.log("Could not interact with water barrel");
                        break;
                    }
                } else {
                    Logger.log("Could not interact with empty watering can");
                    break;
                }
            }
            
            if (!allWateringCansEmpty()) {
                Logger.log("Successfully refilled all watering cans!");
            }
        } else {
            Logger.log("Could not find water barrel for refilling");
        }
    }
    
    private void getSeeds() {
        Logger.log("Need to get seeds from seed table...");
        
        // Check if we're in the house area
        if (!HOUSE_AREA.contains(Players.getLocal())) {
            Logger.log("Walking to house area to get seeds...");
            if (Walking.walk(HOUSE_CENTER)) {
                Sleep.sleepUntil(() -> HOUSE_AREA.contains(Players.getLocal()) || 
                               !Players.getLocal().isMoving(), 
                               randomSleep(8000, 12000));
            }
            return;
        }
        
        // Look for seed table
        GameObject seedTable = GameObjects.closest("Seed table");
        if (seedTable != null && seedTable.exists()) {
            Logger.log("Found seed table, searching for seeds...");
            if (seedTable.interact("Search")) {
                Sleep.sleepUntil(() -> Dialogues.inDialogue(), randomSleep(3000, 5000));
                
                // Handle dialogue - select Golovanova seed option
                if (Dialogues.inDialogue()) {
                    Logger.log("In dialogue, selecting Golovanova seed (level 34) option...");
                    if (Dialogues.areOptionsAvailable()) {
                        if (Dialogues.chooseOption("Golovanova seed (level 34)")) {
                            Sleep.sleepUntil(() -> Dialogues.canEnterInput() || !Dialogues.inDialogue(), randomSleep(2000, 3000));
                            
                            // Enter amount (10000)
                            if (Dialogues.canEnterInput()) {
                                Logger.log("Entering seed amount: 10000");
                                Keyboard.type("10000", true);
                                Sleep.sleepUntil(() -> !Dialogues.inDialogue() || hasSeeds(), randomSleep(3000, 5000));
                                Logger.log("Successfully obtained seeds!");
                            }
                        } else {
                            Logger.log("Could not find 'Golovanova seed (level 34)' option");
                        }
                    } else if (Dialogues.canContinue()) {
                        Dialogues.continueDialogue();
                    }
                }
            }
        } else {
            Logger.log("Could not find seed table in house area");
        }
    }
    
    private void enterFarm() {
        Logger.log("Need to enter farming area through farm door...");
        
        // Look for farm door
        GameObject farmDoor = GameObjects.closest("Farm door");
        if (farmDoor != null && farmDoor.exists()) {
            Logger.log("Found farm door, opening it...");
            if (farmDoor.interact("Open")) {
                Sleep.sleepUntil(() -> Dialogues.inDialogue(), randomSleep(3000, 5000));
                
                // Handle dialogue - select second option to enter farming area
                if (Dialogues.inDialogue()) {
                    Logger.log("In dialogue, selecting second option to enter farming area...");
                    if (Dialogues.areOptionsAvailable()) {
                        if (Dialogues.chooseOption(2)) { // Select second option (index 2)
                            Sleep.sleepUntil(() -> TITHE_FARM_AREA.contains(Players.getLocal()) || !Dialogues.inDialogue(), 
                                           randomSleep(5000, 8000));
                            Logger.log("Successfully entered farming area!");
                        } else {
                            Logger.log("Could not select second option to enter farming area");
                        }
                    } else if (Dialogues.canContinue()) {
                        Dialogues.continueDialogue();
                    }
                }
            }
        } else {
            Logger.log("Could not find farm door");
        }
    }
    
    private void clearDeadPlants() {
        GameObject deadPlant = GameObjects.closest(gameObject -> 
            gameObject != null && gameObject.getName().contains("Golovanova") && 
            gameObject.hasAction("Clear"));
            
        if (deadPlant != null && deadPlant.exists()) {
            Logger.log("Clearing dead plant...");
            if (deadPlant.interact("Clear")) {
                Sleep.sleepUntil(() -> !deadPlant.exists(), 
                               randomSleep(2000, 3000));
                Logger.log("Successfully cleared dead plant!");
            }
        } else {
            Logger.log("No dead plants found to clear");
        }
    }
    
    private void waitForGrowth() {
        Logger.log("Waiting for plants to grow or other activities...");
        Sleep.sleep(randomSleep(3000, 5000));
    }
    
    private int randomSleep(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }
    
    @Override
    public int onLoop() {
        // Check if we're in the Tithe Farm area or House area, if not, just wait
        if (!TITHE_FARM_AREA.contains(Players.getLocal()) && !HOUSE_AREA.contains(Players.getLocal())) {
            Logger.log("Not in Tithe Farm area or House area. Please manually navigate to the Tithe Farm.");
            return randomSleep(5000, 8000); // Wait longer before checking again
        }
        
        State currentState = getState();
        Logger.log("Current state: " + currentState);
        
        switch (currentState) {
            case GETTING_SEEDS:
                getSeeds();
                break;
                
            case ENTERING_FARM:
                enterFarm();
                break;
                
            case CLEARING_DEAD_PLANTS:
                clearDeadPlants();
                break;
                
            case PLANTING_SEEDS:
                plantSeeds();
                break;
                
            case WATERING_PLANTS:
                waterPlants();
                break;
                
            case HARVESTING_PLANTS:
                harvestPlants();
                break;
                
            case DEPOSITING_HARVEST:
                depositHarvest();
                break;
                
            case REFILLING_WATERING_CANS:
                refillWateringCans();
                break;
                
            case WAITING_FOR_GROWTH:
                waitForGrowth();
                break;
                
            case IDLE:
                Logger.log("Idle state - checking for activities...");
                Sleep.sleep(randomSleep(2000, 3000));
                break;
        }
        
        return randomSleep(600, 1200);
    }
    
    @Override
    public void onExit() {
        Logger.log("Tithe Farm Bot stopped!");
        Logger.log("Seeds planted: " + seedsPlanted);
        Logger.log("Plants harvested: " + plantsHarvested);
        
        // Final XP summary
        int finalXP = Skill.FARMING.getExperience();
        int xpGained = finalXP - startingXP;
        Logger.log("Starting Farming XP: " + numberFormat.format(startingXP));
        Logger.log("Final Farming XP: " + numberFormat.format(finalXP));
        Logger.log("Total XP Gained: " + numberFormat.format(xpGained));
        
        if (Skill.FARMING.getLevel() > 0) {
            Logger.log("Current Farming level: " + Skill.FARMING.getLevel());
        }
    }
    
    @Override
    public void onPaint(Graphics2D graphics) {
        // Set up graphics properties
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Background panel
        graphics.setColor(new Color(0, 0, 0, 150));
        graphics.fillRoundRect(10, 10, 280, 140, 10, 10);
        
        // Border
        graphics.setColor(Color.WHITE);
        graphics.setStroke(new BasicStroke(2));
        graphics.drawRoundRect(10, 10, 280, 140, 10, 10);
        
        // Title
        graphics.setColor(Color.GREEN);
        graphics.setFont(new Font("Arial", Font.BOLD, 16));
        String title = "Tithe Farm Bot";
        graphics.drawString(title, 20, 30);
        
        // Calculate XP data
        int currentFarmingXP = Skill.FARMING.getExperience();
        int xpGained = currentFarmingXP - startingXP;
        int currentLevel = Skill.FARMING.getLevel();
        int xpToNextLevel = Skill.FARMING.getExperienceToLevel();
        
        // Calculate time and rates
        long timeElapsed = System.currentTimeMillis() - startTime;
        double hoursElapsed = timeElapsed / (1000.0 * 60.0 * 60.0);
        int xpPerHour = hoursElapsed > 0 ? (int) (xpGained / hoursElapsed) : 0;
        int plantsPerHour = hoursElapsed > 0 ? (int) (plantsHarvested / hoursElapsed) : 0;
        
        // Format time
        long seconds = timeElapsed / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        String timeStr = String.format("%02d:%02d:%02d", hours % 24, minutes % 60, seconds % 60);
        
        // Draw stats
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Arial", Font.PLAIN, 12));
        
        int yOffset = 50;
        graphics.drawString("Runtime: " + timeStr, 20, yOffset);
        yOffset += 15;
        graphics.drawString("Current Level: " + currentLevel + " (Started: " + startingLevel + ")", 20, yOffset);
        yOffset += 15;
        graphics.drawString("XP Gained: " + numberFormat.format(xpGained), 20, yOffset);
        yOffset += 15;
        graphics.drawString("XP/Hour: " + numberFormat.format(xpPerHour), 20, yOffset);
        yOffset += 15;
        graphics.drawString("XP to Next Level: " + numberFormat.format(xpToNextLevel), 20, yOffset);
        yOffset += 15;
        graphics.drawString("Seeds Planted: " + numberFormat.format(seedsPlanted), 20, yOffset);
        yOffset += 15;
        graphics.drawString("Plants Harvested: " + numberFormat.format(plantsHarvested), 20, yOffset);
        yOffset += 15;
        graphics.drawString("Plants/Hour: " + numberFormat.format(plantsPerHour), 20, yOffset);
    }
}
