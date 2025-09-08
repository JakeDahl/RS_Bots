import org.dreambot.api.methods.container.impl.Inventory;
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

@ScriptManifest(name = "Iron Power Miner", description = "Mines iron ore within 1 tile radius of starting position and drops when full",
        author = "IronPowerMiner", version = 1.0, category = Category.MINING, image = "")

public class Main extends AbstractScript {
    
    private Tile startingTile;
    private Area miningArea;
    private final Random random = new Random();
    
    // Tracking variables
    private int ironMined = 0;
    private long startTime;
    private int startingXP;
    private int startingLevel;
    private final NumberFormat numberFormat = NumberFormat.getInstance();
    
    private enum State {
        MINING_IRON,
        DROPPING_IRON,
        MOVING_TO_IRON,
        WAITING,
        IDLE
    }
    
    @Override
    public void onStart() {
        Logger.log("Iron Power Miner started!");
        
        // Capture the starting position
        startingTile = Players.getLocal().getTile();
        if (startingTile == null) {
            Logger.log("ERROR: Could not get starting position!");
            stop();
            return;
        }
        
        // Create mining area with 1 tile radius from starting position
        miningArea = startingTile.getArea(1);
        
        Logger.log("Starting position: " + startingTile.toString());
        Logger.log("Mining area: 1 tile radius from starting position");
        Logger.log("Will mine iron ore and drop when inventory is full");
        
        // Initialize tracking
        startTime = System.currentTimeMillis();
        startingXP = Skill.MINING.getExperience();
        startingLevel = Skill.MINING.getLevel();
        
        Logger.log("Starting Mining XP: " + numberFormat.format(startingXP));
        Logger.log("Starting Mining Level: " + startingLevel);
        Logger.log("Ready to start mining!");
    }
    
    private State getState() {
        // Only drop iron when inventory is completely full
        if (Inventory.isFull()) {
            return State.DROPPING_IRON;
        }
        
        // If inventory has space and iron rocks are available, mine
        GameObject ironRock = getIronRock();
        if (ironRock != null) {
            if (isPlayerMining()) {
                return State.IDLE; // Wait while mining
            } else if (miningArea.contains(Players.getLocal())) {
                return State.MINING_IRON;
            } else {
                return State.MOVING_TO_IRON;
            }
        } else {
            // No iron rocks available, wait for respawn
            return State.WAITING;
        }
    }
    
    private GameObject getIronRock() {
        // Find iron rocks within the mining area
        return GameObjects.closest(gameObject -> 
            gameObject != null && 
            gameObject.getName().equals("Iron rocks") && 
            miningArea.contains(gameObject.getTile())
        );
    }
    
    private boolean isPlayerMining() {
        // Check if player is currently performing mining animation
        return Players.getLocal().isAnimating() && Players.getLocal().getAnimation() != -1;
    }
    
    private void mineIron() {
        GameObject ironRock = getIronRock();
        if (ironRock != null && ironRock.exists()) {
            Logger.log("Mining iron ore...");
            int currentIronCount = Inventory.count("Iron ore");
            
            if (ironRock.interact("Mine")) {
                Sleep.sleepUntil(() -> 
                    isPlayerMining() || 
                    Inventory.count("Iron ore") > currentIronCount ||
                    Inventory.isFull(),
                    randomSleep(3000, 5000)
                );
                
                // Check if we got iron ore and count the difference
                int newIronCount = Inventory.count("Iron ore");
                if (newIronCount > currentIronCount) {
                    int ironGained = newIronCount - currentIronCount;
                    ironMined += ironGained;
                    Logger.log("Mined " + ironGained + " iron ore! Total mined: " + ironMined);
                }
            }
        } else {
            Logger.log("No iron rocks available in mining area");
        }
    }
    
    private void dropAllIron() {
        // Items to drop: iron ore and gems
        String[] itemsToDrop = {"Iron ore", "Uncut emerald", "Uncut ruby", "Uncut sapphire"};
        
        int totalItemsToDisplay = 0;
        for (String item : itemsToDrop) {
            totalItemsToDisplay += Inventory.count(item);
        }
        
        if (totalItemsToDisplay > 0) {
            Logger.log("Dropping all items (" + totalItemsToDisplay + " pieces: iron ore and gems)...");
            
            // Drop all specified items at maximum speed
            int maxAttempts = 50; // Safety limit to prevent infinite loops
            int attempts = 0;
            
            // Keep dropping until no target items remain
            boolean hasItems = true;
            while (hasItems && attempts < maxAttempts) {
                hasItems = false;
                
                for (String item : itemsToDrop) {
                    if (Inventory.contains(item)) {
                        hasItems = true;
                        if (Inventory.drop(item)) {
                            Sleep.sleep(randomSleep(3, 8)); // Lightning fast dropping
                            attempts = 0; // Reset attempts counter on successful drop
                        } else {
                            attempts++;
                            Sleep.sleep(15); // Minimal pause before retry
                            Logger.log("Failed to drop " + item + ", attempt " + attempts);
                        }
                        break; // Drop one item at a time for consistency
                    }
                }
            }
            
            // Final verification and cleanup
            int totalRemaining = 0;
            for (String item : itemsToDrop) {
                totalRemaining += Inventory.count(item);
            }
            
            if (totalRemaining == 0) {
                Logger.log("Successfully dropped all " + totalItemsToDisplay + " items - inventory cleared!");
            } else {
                Logger.log("WARNING: " + totalRemaining + " items still in inventory after dropping attempts");
                // Force drop any remaining items at maximum speed
                for (String item : itemsToDrop) {
                    while (Inventory.contains(item)) {
                        Inventory.drop(item);
                        Sleep.sleep(8); // Ultra-minimal delay for force dropping
                    }
                }
                
                int finalCount = 0;
                for (String item : itemsToDrop) {
                    finalCount += Inventory.count(item);
                }
                Logger.log("Force-dropped remaining items. Final count: " + finalCount);
            }
        }
    }
    
    private void moveToIronRock() {
        GameObject ironRock = getIronRock();
        if (ironRock != null) {
            Tile rockTile = ironRock.getTile();
            if (rockTile != null && !miningArea.contains(Players.getLocal())) {
                Logger.log("Moving closer to iron rock...");
                
                // Walk to a tile adjacent to the rock within our mining area
                Tile targetTile = getClosestValidTile(rockTile);
                if (targetTile != null) {
                    if (Walking.walk(targetTile)) {
                        Sleep.sleepUntil(() -> 
                            miningArea.contains(Players.getLocal()) || 
                            !Players.getLocal().isMoving(),
                            randomSleep(3000, 5000)
                        );
                    }
                }
            }
        }
    }
    
    private Tile getClosestValidTile(Tile rockTile) {
        // Find the closest tile to the rock that's within our mining area
        Tile[] areaTiles = miningArea.getTiles();
        Tile closestTile = null;
        double shortestDistance = Double.MAX_VALUE;
        
        for (Tile tile : areaTiles) {
            double distance = tile.distance(rockTile);
            if (distance < shortestDistance) {
                shortestDistance = distance;
                closestTile = tile;
            }
        }
        
        return closestTile;
    }
    
    private int randomSleep(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }
    
    @Override
    public int onLoop() {
        State currentState = getState();
        
        switch (currentState) {
            case MINING_IRON:
                mineIron();
                break;
                
            case DROPPING_IRON:
                dropAllIron();
                break;
                
            case MOVING_TO_IRON:
                moveToIronRock();
                break;
                
            case WAITING:
                Logger.log("No iron rocks available, waiting for respawn...");
                Sleep.sleep(randomSleep(2000, 4000));
                break;
                
            case IDLE:
                Logger.log("Mining in progress, waiting...");
                Sleep.sleepUntil(() -> 
                    !isPlayerMining() || 
                    Inventory.isFull() ||
                    Inventory.count("Iron ore") != Inventory.count("Iron ore"), 
                    randomSleep(1000, 2000)
                );
                break;
        }
        
        return randomSleep(100, 300);
    }
    
    @Override
    public void onExit() {
        Logger.log("Iron Power Miner stopped!");
        Logger.log("Starting position was: " + startingTile.toString());
        Logger.log("Total iron ore mined: " + ironMined);
        Logger.log("Final iron ore in inventory: " + Inventory.count("Iron ore"));
        
        // Calculate runtime
        long timeElapsed = System.currentTimeMillis() - startTime;
        long seconds = timeElapsed / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        String timeStr = String.format("%02d:%02d:%02d", hours % 24, minutes % 60, seconds % 60);
        Logger.log("Total runtime: " + timeStr);
        
        // Calculate rates
        double hoursElapsed = timeElapsed / (1000.0 * 60.0 * 60.0);
        int ironPerHour = hoursElapsed > 0 ? (int) (ironMined / hoursElapsed) : 0;
        Logger.log("Iron ore per hour: " + ironPerHour);
        
        // Final XP summary
        int finalXP = Skill.MINING.getExperience();
        int xpGained = finalXP - startingXP;
        int xpPerHour = hoursElapsed > 0 ? (int) (xpGained / hoursElapsed) : 0;
        Logger.log("Starting Mining XP: " + numberFormat.format(startingXP));
        Logger.log("Final Mining XP: " + numberFormat.format(finalXP));
        Logger.log("Total XP Gained: " + numberFormat.format(xpGained));
        Logger.log("XP per hour: " + numberFormat.format(xpPerHour));
        Logger.log("Current Mining level: " + Skill.MINING.getLevel());
    }
    
    @Override
    public void onPaint(Graphics2D graphics) {
        // Set up graphics properties
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Background panel (adjusted height after removing iron ore stats)
        graphics.setColor(new Color(0, 0, 0, 150));
        graphics.fillRoundRect(10, 10, 300, 110, 10, 10);
        
        // Border
        graphics.setColor(Color.WHITE);
        graphics.setStroke(new BasicStroke(2));
        graphics.drawRoundRect(10, 10, 300, 110, 10, 10);
        
        // Title
        graphics.setColor(Color.CYAN);
        graphics.setFont(new Font("Arial", Font.BOLD, 16));
        graphics.drawString("Iron Power Miner", 20, 30);
        
        // Calculate XP data
        int currentMiningXP = Skill.MINING.getExperience();
        int xpGained = currentMiningXP - startingXP;
        int currentLevel = Skill.MINING.getLevel();
        int xpToNextLevel = Skill.MINING.getExperienceToLevel();
        
        // Calculate time and rates
        long timeElapsed = System.currentTimeMillis() - startTime;
        double hoursElapsed = timeElapsed / (1000.0 * 60.0 * 60.0);
        int ironPerHour = hoursElapsed > 0 ? (int) (ironMined / hoursElapsed) : 0;
        int xpPerHour = hoursElapsed > 0 ? (int) (xpGained / hoursElapsed) : 0;
        
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
        
        // Show starting position info
        graphics.setColor(Color.YELLOW);
        graphics.drawString("Start Pos: " + startingTile.getX() + ", " + startingTile.getY(), 20, yOffset);
    }
}
