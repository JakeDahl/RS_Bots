import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.combat.Combat;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.tabs.Tabs;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;

import java.awt.*;
import java.text.NumberFormat;
import java.util.Random;

@ScriptManifest(name = "Spike Jumper", description = "Rapidly clicks between two tiles to jump over spikes repeatedly",
        author = "SpikeBot", version = 1.0, category = Category.MISC, image = "")

public class Main extends AbstractScript {

    private final Random random = new Random();
    
    // Define the two spike tiles to jump between
    private final Tile TILE_A = new Tile(2801, 9568, 3);  // First tile
    private final Tile TILE_B = new Tile(2798, 9568, 3);  // Second tile
    
    // Reference point for safety check (center of the two tiles)
    private final Tile REFERENCE_POINT = new Tile(2799, 9568, 3);
    
    // Maximum distance allowed from reference point before stopping bot
    private final int MAX_DISTANCE_FROM_AREA = 20;
    
    // Track current target tile
    private Tile currentTarget = TILE_A;
    
    // Distance threshold to consider we've reached a tile
    private final int DISTANCE_THRESHOLD = 1;
    
    // XP Tracking variables
    private int startingAgilityXP = 0;
    private long startTime;
    private final NumberFormat numberFormat = NumberFormat.getInstance();

    @Override
    public int onLoop() {
        try {
            // SAFETY CHECK: Stop bot if too far from designated area
            Tile playerTile = Players.getLocal().getTile();
            if (playerTile != null) {
                int distanceFromArea = (int) playerTile.distance(REFERENCE_POINT);
                if (distanceFromArea > MAX_DISTANCE_FROM_AREA) {
                    Logger.log("SAFETY STOP: Player is " + distanceFromArea + " tiles away from training area (max allowed: " + MAX_DISTANCE_FROM_AREA + ")");
                    Logger.log("Bot stopped for safety. Current position: " + playerTile + ", Reference point: " + REFERENCE_POINT);
                    stop();
                    return -1; // Stop the script
                }
            }
            
            // Priority: Check health and eat lobster if needed
            if (Combat.getHealthPercent() < 50 && Inventory.contains("Lobster")) {
                Logger.log("Health below 50%, switching to inventory to eat Lobster...");
                
                // Switch to inventory tab for eating
                if (!Tabs.isOpen(Tab.INVENTORY)) {
                    Tabs.open(Tab.INVENTORY);
                    Sleep.sleep(100, 200);
                }
                
                if (Inventory.interact("Lobster", "Eat")) {
                    Sleep.sleep(1000, 1500); // Wait for eating animation
                    
                    // Switch back to skills tab after eating
                    Logger.log("Finished eating, switching back to Skills tab...");
                    if (!Tabs.isOpen(Tab.SKILLS)) {
                        Tabs.open(Tab.SKILLS);
                        Sleep.sleep(100, 200);
                    }
                    
                    return randomSleep(100, 200);
                }
            }
            
            // Ensure we're on the Skills tab (unless we just ate)
            if (!Tabs.isOpen(Tab.SKILLS)) {
                Logger.log("Opening Skills tab...");
                Tabs.open(Tab.SKILLS);
                Sleep.sleep(100, 200);
            }
            
            if (playerTile == null) {
                Logger.log("Could not get player tile, waiting...");
                return randomSleep(75, 225); // Reduced by 25%
            }
            
            // Check if we're close to our current target
            if (isPlayerNearTile(playerTile, currentTarget)) {
                // Switch to the opposite tile
                currentTarget = (currentTarget.equals(TILE_A)) ? TILE_B : TILE_A;
                Logger.log("Switching target to: " + currentTarget);
            }
            
            // Walk to the current target tile
            if (Walking.walk(currentTarget)) {
                Logger.log("Walking to tile: " + currentTarget);
                // Reduced sleep by 25%
                Sleep.sleep(37, 112);
            } else {
                Logger.log("Failed to walk to tile: " + currentTarget);
            }
            
            // Very short delay to maintain rapid clicking - reduced by 25%
            return randomSleep(37, 75);
            
        } catch (Exception e) {
            Logger.log("Error in spike jumping: " + e.getMessage());
            return randomSleep(375, 750); // Also reduced by 25%
        }
    }
    
    /**
     * Check if player is near the target tile (within threshold distance)
     */
    private boolean isPlayerNearTile(Tile playerTile, Tile targetTile) {
        int distance = (int) playerTile.distance(targetTile);
        return distance <= DISTANCE_THRESHOLD;
    }

    @Override
    public void onStart() {
        Logger.log("Spike Jumper Bot started!");
        Logger.log("Will jump between tiles: " + TILE_A + " and " + TILE_B);
        Logger.log("Safety zone: " + MAX_DISTANCE_FROM_AREA + " tiles from reference point " + REFERENCE_POINT);
        Logger.log("Health monitoring: Will eat Lobster if health < 50%");
        Logger.log("Tab management: Stays on Skills tab, switches to Inventory only when eating");
        Logger.log("Optimized for 25% faster clicking and direct tile interaction");
        
        // Initialize XP tracking
        startingAgilityXP = Skills.getExperience(Skill.AGILITY);
        startTime = System.currentTimeMillis();
        Logger.log("Starting Agility XP: " + numberFormat.format(startingAgilityXP));
        
        // Determine initial target based on player position
        Tile playerTile = Players.getLocal().getTile();
        if (playerTile != null) {
            // Start with the tile that's further away
            double distanceToA = playerTile.distance(TILE_A);
            double distanceToB = playerTile.distance(TILE_B);
            
            currentTarget = (distanceToA > distanceToB) ? TILE_A : TILE_B;
            Logger.log("Starting position: " + playerTile);
            Logger.log("Initial target: " + currentTarget);
        }
        
        // Check for lobsters in inventory
        int lobsterCount = Inventory.count("Lobster");
        Logger.log("Lobsters in inventory: " + lobsterCount);
        
        // Ensure we start on the Skills tab
        if (!Tabs.isOpen(Tab.SKILLS)) {
            Logger.log("Opening Skills tab on startup...");
            Tabs.open(Tab.SKILLS);
        }
    }

    @Override
    public void onExit() {
        Logger.log("Spike Jumper Bot stopped!");
    }
    
    /**
     * Generates random sleep duration
     */
    private int randomSleep(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }
    
    @Override
    public void onPaint(Graphics2D graphics) {
        // Set up graphics properties
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Background panel
        graphics.setColor(new Color(0, 0, 0, 150));
        graphics.fillRoundRect(10, 10, 300, 120, 10, 10);
        
        // Border
        graphics.setColor(Color.WHITE);
        graphics.setStroke(new BasicStroke(2));
        graphics.drawRoundRect(10, 10, 300, 120, 10, 10);
        
        // Title
        graphics.setColor(Color.YELLOW);
        graphics.setFont(new Font("Arial", Font.BOLD, 16));
        graphics.drawString("Spike Jumper - Agility Tracker", 20, 30);
        
        // Calculate XP data
        int currentAgilityXP = Skills.getExperience(Skill.AGILITY);
        int xpGained = currentAgilityXP - startingAgilityXP;
        int currentLevel = Skills.getRealLevel(Skill.AGILITY);
        int xpToNextLevel = Skills.getExperienceToLevel(Skill.AGILITY);
        
        // Calculate time and XP per hour
        long timeElapsed = System.currentTimeMillis() - startTime;
        double hoursElapsed = timeElapsed / (1000.0 * 60.0 * 60.0);
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
        graphics.drawString("Current Level: " + currentLevel, 20, yOffset);
        yOffset += 15;
        graphics.drawString("Current XP: " + numberFormat.format(currentAgilityXP), 20, yOffset);
        yOffset += 15;
        graphics.drawString("XP Gained: " + numberFormat.format(xpGained), 20, yOffset);
        yOffset += 15;
        graphics.drawString("XP/Hour: " + numberFormat.format(xpPerHour), 20, yOffset);
        yOffset += 15;
        graphics.drawString("XP to Next Level: " + numberFormat.format(xpToNextLevel), 20, yOffset);
    }
}
