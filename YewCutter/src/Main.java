import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.item.GroundItems;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.world.Worlds;
import org.dreambot.api.methods.worldhopper.WorldHopper;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.items.GroundItem;
import org.dreambot.api.wrappers.world.World;

import java.util.Random;
import java.awt.*;
import java.text.NumberFormat;

@ScriptManifest(name = "Yew Cutter", description = "Cuts yew trees in Edgeville with Dragon axe and banks logs (Level 75+)",
        author = "YewCutter", version = 1.0, category = Category.WOODCUTTING, image = "")

public class Main extends AbstractScript {
    
    // Edgeville yew trees area
    private final Tile YEW_TREE_TILE = new Tile(3087, 3468); // Edgeville yew trees
    private final Area YEW_AREA = YEW_TREE_TILE.getArea(10);
    
    // Edgeville bank
    private final Tile EDGEVILLE_BANK_TILE = new Tile(3094, 3492);
    private final Area EDGEVILLE_BANK_AREA = EDGEVILLE_BANK_TILE.getArea(5);
    
    private final Random random = new Random();
    
    // XP Tracking variables
    private int startingXP;
    private int startingLevel;
    private long startTime;
    private int logsCount = 0;
    private int nestCount = 0;
    private final NumberFormat numberFormat = NumberFormat.getInstance();
    
    private enum State {
        WALKING_TO_TREES,
        CUTTING_YEWS,
        WALKING_TO_BANK,
        BANKING,
        CHECKING_AXE,
        GETTING_AXE,
        FLEEING_COMBAT,
        WORLD_HOPPING,
        IDLE
    }
    
    @Override
    public void onStart() {
        Logger.log("Yew Cutter started!");
        Logger.log("Target: Yew trees in Edgeville");
        Logger.log("Banking at: Edgeville Bank");
        Logger.log("Will pick up Bird nests automatically");
        Logger.log("Will flee to bank and hop worlds if attacked by aggressors");
        
        // Check level requirement
        if (Skill.WOODCUTTING.getLevel() < 75) {
            Logger.log("ERROR: Woodcutting level 75+ required to cut Yew trees!");
            Logger.log("Current level: " + Skill.WOODCUTTING.getLevel());
            stop();
            return;
        }
        
        // Initialize XP tracking
        startingXP = Skill.WOODCUTTING.getExperience();
        startingLevel = Skill.WOODCUTTING.getLevel();
        startTime = System.currentTimeMillis();
        Logger.log("Starting Woodcutting XP: " + numberFormat.format(startingXP));
        Logger.log("Starting Woodcutting Level: " + startingLevel);
        
        // Check if player has a Dragon axe
        if (!hasRequiredAxe()) {
            Logger.log("WARNING: Dragon axe not found in inventory or equipment!");
            Logger.log("Bot will attempt to get Dragon axe from bank automatically.");
        } else {
            Logger.log("Dragon axe detected - ready to cut yews!");
        }
    }
    
    private State getState() {
        // First check for combat - highest priority
        if (hasAggressors()) {
            if (isNearBank()) {
                return State.WORLD_HOPPING; // If at bank and have aggressors, hop worlds
            } else {
                return State.FLEEING_COMBAT; // Run to bank
            }
        }
        
        // Check if we need to verify axe
        if (!hasRequiredAxe()) {
            if (Bank.isOpen()) {
                return State.GETTING_AXE;
            } else if (isNearBank()) {
                return State.GETTING_AXE;
            } else {
                return State.CHECKING_AXE;
            }
        }
        
        // If inventory is full, go bank
        if (Inventory.isFull()) {
            if (Bank.isOpen()) {
                return State.BANKING;
            } else if (isNearBank()) {
                return State.BANKING;
            } else {
                return State.WALKING_TO_BANK;
            }
        } else {
            // Inventory has space, go cut trees
            if (YEW_AREA.contains(Players.getLocal())) {
                if (isPlayerCutting()) {
                    return State.IDLE; // Wait while cutting
                } else {
                    return State.CUTTING_YEWS;
                }
            } else {
                return State.WALKING_TO_TREES;
            }
        }
    }
    
    private boolean hasRequiredAxe() {
        // Check inventory for Dragon axe
        if (Inventory.contains("Dragon axe")) {
            return true;
        }
        
        // For now, we'll assume if not in inventory, we need to get it
        // In a real implementation, you could check equipment here
        return false;
    }
    
    private boolean isPlayerCutting() {
        // Check if player is currently performing woodcutting animation
        return Players.getLocal().isAnimating() && Players.getLocal().getAnimation() != -1;
    }
    
    private boolean isNearBank() {
        return EDGEVILLE_BANK_AREA.contains(Players.getLocal());
    }
    
    private void walkToYewTrees() {
        if (!YEW_AREA.contains(Players.getLocal())) {
            Logger.log("Walking to yew trees in Edgeville...");
            Tile randomizedTile = YEW_TREE_TILE.getRandomized(3);
            if (Walking.walk(randomizedTile)) {
                Sleep.sleepUntil(() -> YEW_AREA.contains(Players.getLocal()) || 
                               !Players.getLocal().isMoving(), 
                               randomSleep(5000, 8000));
            }
        }
    }
    
    private void cutYewTree() {
        // Check for bird nests first (high priority)
        if (checkForBirdNest()) {
            return; // If we found and picked up a bird nest, exit early
        }
        
        if (Inventory.isFull()) {
            Logger.log("Inventory full, cannot cut more trees");
            return;
        }
        
        if (isPlayerCutting()) {
            Logger.log("Already cutting a tree, waiting...");
            return;
        }
        
        GameObject yewTree = GameObjects.closest("Yew tree");
        if (yewTree != null && yewTree.exists()) {
            Logger.log("Cutting yew tree...");
            if (yewTree.interact("Chop down")) {
                Sleep.sleepUntil(() -> isPlayerCutting() || 
                               Inventory.count("Yew logs") > getCurrentYewLogCount(), 
                               randomSleep(3000, 5000));
            }
        } else {
            Logger.log("No yew trees found nearby, waiting for respawn...");
            Sleep.sleep(randomSleep(2000, 4000));
        }
    }
    
    private boolean checkForBirdNest() {
        // Look for bird nests on the ground nearby
        GroundItem birdNest = GroundItems.closest("Bird nest");
        if (birdNest != null && birdNest.exists()) {
            // Check if inventory has space (need to account for Dragon axe)
            if (Inventory.isFull()) {
                Logger.log("Inventory full, cannot pick up Bird nest - will bank first");
                return false;
            }
            
            Logger.log("Found Bird nest on ground! Picking it up...");
            if (birdNest.interact("Take")) {
                Sleep.sleepUntil(() -> !birdNest.exists() || Inventory.contains("Bird nest"), 
                               randomSleep(2000, 3000));
                
                if (Inventory.contains("Bird nest")) {
                    nestCount++;
                    Logger.log("Successfully picked up Bird nest! (Total: " + nestCount + ")");
                    return true;
                } else {
                    Logger.log("Failed to pick up Bird nest");
                }
            } else {
                Logger.log("Could not interact with Bird nest");
            }
        }
        return false;
    }
    
    private boolean hasAggressors() {
        // Check if any NPCs are targeting/interacting with the player
        java.util.List<NPC> aggressors = NPCs.getInteractingWith(Players.getLocal().getIndex());
        return !aggressors.isEmpty();
    }
    
    private void fleeFromCombat() {
        Logger.log("Aggressors detected! Fleeing to bank...");
        if (!isNearBank()) {
            walkToBank();
        }
    }
    
    private void hopToRandomMemberWorld() {
        Logger.log("Hopping to a random member world...");
        java.util.List<World> memberWorlds = Worlds.members();
        
        if (!memberWorlds.isEmpty()) {
            // Filter out current world and get a random one
            World currentWorld = Worlds.getCurrent();
            if (currentWorld != null) {
                memberWorlds.removeIf(world -> world.getID() == currentWorld.getID());
            }
            
            if (!memberWorlds.isEmpty()) {
                World randomWorld = Worlds.getRandomWorld(memberWorlds);
                if (randomWorld != null) {
                    Logger.log("Attempting to hop to world " + randomWorld.getID());
                    if (WorldHopper.hopWorld(randomWorld.getID(), false)) {
                        Logger.log("Successfully hopped to world " + randomWorld.getID());
                        Sleep.sleep(randomSleep(3000, 5000)); // Wait after hopping
                    } else {
                        Logger.log("Failed to hop to world " + randomWorld.getID());
                    }
                } else {
                    Logger.log("Could not get random member world");
                }
            } else {
                Logger.log("No other member worlds available");
            }
        } else {
            Logger.log("No member worlds found");
        }
    }
    
    private int getCurrentYewLogCount() {
        return Inventory.count("Yew logs");
    }
    
    private void walkToBank() {
        Logger.log("Walking to Edgeville bank...");
        Tile randomizedBankTile = EDGEVILLE_BANK_TILE.getRandomized(2);
        if (Walking.walk(randomizedBankTile)) {
            Sleep.sleepUntil(() -> EDGEVILLE_BANK_AREA.contains(Players.getLocal()) || 
                           !Players.getLocal().isMoving(), 
                           randomSleep(5000, 8000));
        }
    }
    
    private void handleBanking() {
        if (!Bank.isOpen()) {
            GameObject bank = GameObjects.closest("Bank booth");
            if (bank == null) {
                bank = GameObjects.closest("Bank chest");
            }
            
            if (bank != null && bank.interact("Bank")) {
                Sleep.sleepUntil(() -> Bank.isOpen(), randomSleep(3000, 5000));
            } else {
                Logger.log("Could not find bank booth or chest");
                return;
            }
        }
        
        if (Bank.isOpen()) {
            // Count logs before depositing for tracking
            int logsToDeposit = Inventory.count("Yew logs");
            
            Logger.log("Depositing all items except Dragon axe...");
            if (Bank.depositAllExcept("Dragon axe")) {
                Sleep.sleepUntil(() -> Inventory.fullSlotCount() == 1 && Inventory.contains("Dragon axe"), 
                               randomSleep(2000, 3000));
                Logger.log("Successfully deposited items, keeping Dragon axe");
                
                // Update logs count for tracking
                logsCount += logsToDeposit;
            } else {
                // Fallback: deposit yew logs specifically if depositAllExcept fails
                int logCount = Inventory.count("Yew logs");
                if (logCount > 0) {
                    Logger.log("Fallback: Depositing " + logCount + " yew logs...");
                    if (Bank.deposit("Yew logs", logCount)) {
                        Sleep.sleepUntil(() -> !Inventory.contains("Yew logs"), 
                                       randomSleep(2000, 3000));
                        Logger.log("Successfully deposited yew logs");
                        
                        // Update logs count for tracking
                        logsCount += logCount;
                    }
                }
                
                // Also deposit bird nests if any
                int nestCount = Inventory.count("Bird nest");
                if (nestCount > 0) {
                    Logger.log("Depositing " + nestCount + " bird nests...");
                    Bank.deposit("Bird nest", nestCount);
                    Sleep.sleepUntil(() -> !Inventory.contains("Bird nest"), 
                                   randomSleep(1000, 2000));
                }
            }
            
            // Close bank and return to cutting
            Bank.close();
            Sleep.sleepUntil(() -> !Bank.isOpen(), randomSleep(1000, 2000));
            Logger.log("Banking complete, returning to yew trees");
        }
    }
    
    private void checkAxeRequirement() {
        Logger.log("Checking for Dragon axe...");
        if (!hasRequiredAxe()) {
            Logger.log("Dragon axe not found in inventory! Walking to bank to get one...");
            if (!isNearBank()) {
                walkToBank();
            }
        }
    }
    
    private void handleGettingAxe() {
        if (!Bank.isOpen()) {
            GameObject bank = GameObjects.closest("Bank booth");
            if (bank == null) {
                bank = GameObjects.closest("Bank chest");
            }
            
            if (bank != null && bank.interact("Bank")) {
                Sleep.sleepUntil(() -> Bank.isOpen(), randomSleep(3000, 5000));
            } else {
                Logger.log("Could not find bank booth or chest");
                return;
            }
        }
        
        if (Bank.isOpen()) {
            Logger.log("Looking for Dragon axe in bank...");
            if (Bank.contains("Dragon axe")) {
                Logger.log("Found Dragon axe in bank! Withdrawing...");
                if (Bank.withdraw("Dragon axe", 1)) {
                    Sleep.sleepUntil(() -> Inventory.contains("Dragon axe"), randomSleep(2000, 3000));
                    if (Inventory.contains("Dragon axe")) {
                        Logger.log("Successfully withdrew Dragon axe!");
                        Bank.close();
                        Sleep.sleepUntil(() -> !Bank.isOpen(), randomSleep(1000, 2000));
                    } else {
                        Logger.log("Failed to withdraw Dragon axe");
                    }
                } else {
                    Logger.log("Could not withdraw Dragon axe from bank");
                }
            } else {
                Logger.log("ERROR: No Dragon axe found in bank!");
                Logger.log("Please ensure you have a Dragon axe in your bank to use this bot.");
                Sleep.sleep(randomSleep(5000, 10000)); // Wait longer before trying again
            }
        }
    }
    
    private int randomSleep(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }
    
    @Override
    public int onLoop() {
        State currentState = getState();
        Logger.log("Current state: " + currentState);
        
        // Always check for bird nests in the cutting area regardless of state (except combat states)
        if (YEW_AREA.contains(Players.getLocal()) && !Inventory.isFull() && 
            currentState != State.FLEEING_COMBAT && currentState != State.WORLD_HOPPING) {
            checkForBirdNest();
        }
        
        switch (currentState) {
            case WALKING_TO_TREES:
                walkToYewTrees();
                break;
                
            case CUTTING_YEWS:
                cutYewTree();
                break;
                
            case WALKING_TO_BANK:
                walkToBank();
                break;
                
            case BANKING:
                handleBanking();
                break;
                
            case CHECKING_AXE:
                checkAxeRequirement();
                Sleep.sleep(randomSleep(3000, 5000)); // Wait before checking again
                break;
                
            case GETTING_AXE:
                handleGettingAxe();
                break;
                
            case FLEEING_COMBAT:
                fleeFromCombat();
                break;
                
            case WORLD_HOPPING:
                hopToRandomMemberWorld();
                break;
                
            case IDLE:
                Logger.log("Waiting while cutting tree or for logs to appear...");
                // Wait for cutting animation to finish or inventory to change
                Sleep.sleepUntil(() -> !isPlayerCutting() || Inventory.isFull() || 
                               getCurrentYewLogCount() != Inventory.count("Yew logs"), 
                               randomSleep(1000, 3000));
                break;
        }
        
        return randomSleep(100, 500);
    }
    
    @Override
    public void onExit() {
        Logger.log("Yew Cutter stopped!");
        Logger.log("Final log count in inventory: " + Inventory.count("Yew logs"));
        Logger.log("Total yew logs cut: " + logsCount);
        Logger.log("Total bird nests collected: " + nestCount);
        
        // Final XP summary
        int finalXP = Skill.WOODCUTTING.getExperience();
        int xpGained = finalXP - startingXP;
        Logger.log("Starting Woodcutting XP: " + numberFormat.format(startingXP));
        Logger.log("Final Woodcutting XP: " + numberFormat.format(finalXP));
        Logger.log("Total XP Gained: " + numberFormat.format(xpGained));
        
        if (Skill.WOODCUTTING.getLevel() > 0) {
            Logger.log("Current Woodcutting level: " + Skill.WOODCUTTING.getLevel());
        }
    }
    
    @Override
    public void onPaint(Graphics2D graphics) {
        // Set up graphics properties
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Background panel
        graphics.setColor(new Color(0, 0, 0, 150));
        graphics.fillRoundRect(10, 10, 320, 155, 10, 10);
        
        // Border
        graphics.setColor(Color.WHITE);
        graphics.setStroke(new BasicStroke(2));
        graphics.drawRoundRect(10, 10, 320, 155, 10, 10);
        
        // Title
        graphics.setColor(Color.GREEN);
        graphics.setFont(new Font("Arial", Font.BOLD, 16));
        String title = "Yew Cutter - Edgeville (Anti-PK)";
        graphics.drawString(title, 20, 30);
        
        // Calculate XP data
        int currentWoodcuttingXP = Skill.WOODCUTTING.getExperience();
        int xpGained = currentWoodcuttingXP - startingXP;
        int currentLevel = Skill.WOODCUTTING.getLevel();
        int xpToNextLevel = Skill.WOODCUTTING.getExperienceToLevel();
        
        // Calculate time and rates
        long timeElapsed = System.currentTimeMillis() - startTime;
        double hoursElapsed = timeElapsed / (1000.0 * 60.0 * 60.0);
        int xpPerHour = hoursElapsed > 0 ? (int) (xpGained / hoursElapsed) : 0;
        int logsPerHour = hoursElapsed > 0 ? (int) (logsCount / hoursElapsed) : 0;
        
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
        graphics.drawString("Current XP: " + numberFormat.format(currentWoodcuttingXP), 20, yOffset);
        yOffset += 15;
        graphics.drawString("XP Gained: " + numberFormat.format(xpGained), 20, yOffset);
        yOffset += 15;
        graphics.drawString("XP/Hour: " + numberFormat.format(xpPerHour), 20, yOffset);
        yOffset += 15;
        graphics.drawString("XP to Next Level: " + numberFormat.format(xpToNextLevel), 20, yOffset);
        yOffset += 15;
        graphics.drawString("Yew Logs Cut: " + numberFormat.format(logsCount), 20, yOffset);
        yOffset += 15;
        graphics.drawString("Logs/Hour: " + numberFormat.format(logsPerHour), 20, yOffset);
        yOffset += 15;
        
        // Bird nest stats with different color
        graphics.setColor(Color.YELLOW);
        graphics.drawString("Bird Nests Found: " + numberFormat.format(nestCount), 20, yOffset);
    }
}
