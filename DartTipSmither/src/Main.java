import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.widget.Widgets;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.widgets.WidgetChild;

import java.awt.*;
import java.text.NumberFormat;
import java.util.Random;

@ScriptManifest(name = "Dart Tip Smither", description = "Smiths dart tips at West Varrock anvil with auto bar progression",
        author = "DartTipSmither", version = 2.0, category = Category.SMITHING, image = "")

public class Main extends AbstractScript {
    
    // West Varrock locations
    private final Tile BANK_TILE = new Tile(3185, 3436); // West Varrock bank
    private final Area BANK_AREA = new Area(new Tile(3180, 3445), new Tile(3190, 3430));
    private final Tile ANVIL_TILE = new Tile(3188, 3425); // West Varrock anvil
    private final Area ANVIL_AREA = new Area(new Tile(3186, 3427), new Tile(3190, 3423));
    
    private final Random random = new Random();
    
    // Bar progression system
    private static class BarType {
        final String barName;
        final String dartTipName;
        final int levelRequired;
        final int widgetParent;
        final int widgetChild;
        
        BarType(String barName, String dartTipName, int levelRequired, int widgetParent, int widgetChild) {
            this.barName = barName;
            this.dartTipName = dartTipName;
            this.levelRequired = levelRequired;
            this.widgetParent = widgetParent;
            this.widgetChild = widgetChild;
        }
    }
    
    // Define all available bar types in order of level requirement
    private static final BarType[] BAR_TYPES = {
        new BarType("Bronze bar", "Bronze dart tip", 4, 29, 1),
        new BarType("Iron bar", "Iron dart tip", 19, 29, 1), 
        new BarType("Steel bar", "Steel dart tip", 34, 29, 1),
        new BarType("Mithril bar", "Mithril dart tip", 54, 29, 1),
        new BarType("Adamant bar", "Adamant dart tip", 74, 29, 1),
        new BarType("Rune bar", "Rune dart tip", 89, 29, 1)
    };
    
    private BarType currentBarType;
    
    // Tracking variables
    private int barsUsed = 0;
    private int dartTipsMade = 0;
    private long startTime;
    private int startingXP;
    private int startingLevel;
    private final NumberFormat numberFormat = NumberFormat.getInstance();
    
    // Smithing interface widget IDs
    private static final int SMITHING_INTERFACE = 312;
    private static final int DART_TIPS_PARENT = 29;
    private static final int DART_TIPS_CHILD = 1;
    
    private enum State {
        BANKING,
        WALKING_TO_ANVIL, 
        WALKING_TO_BANK,
        SMITHING,
        WAITING_FOR_SMITHING,
        IDLE
    }
    
    @Override
    public void onStart() {
        Logger.log("Dart Tip Smither started!");
        Logger.log("Location: West Varrock bank and anvil");
        
        // Determine the best bar type for current level
        currentBarType = getBestBarType();
        if (currentBarType == null) {
            Logger.log("ERROR: No suitable bar type found for current smithing level!");
            Logger.log("Current level: " + Skill.SMITHING.getLevel());
            Logger.log("Minimum level required: 4 (Bronze dart tips)");
            stop();
            return;
        }
        
        Logger.log("Target: " + currentBarType.dartTipName + "s");
        Logger.log("Required level: " + currentBarType.levelRequired);
        Logger.log("Current level: " + Skill.SMITHING.getLevel());
        
        // Check for hammer
        if (!hasHammer()) {
            Logger.log("ERROR: Hammer not found in inventory or equipment!");
            stop();
            return;
        }
        
        // Initialize tracking
        startTime = System.currentTimeMillis();
        startingXP = Skill.SMITHING.getExperience();
        startingLevel = Skill.SMITHING.getLevel();
        
        Logger.log("Requirements met:");
        Logger.log("- Smithing level: " + Skill.SMITHING.getLevel());
        Logger.log("- Hammer: Found");
        Logger.log("- Bar type: " + currentBarType.barName + " -> " + currentBarType.dartTipName);
        Logger.log("Starting Smithing XP: " + numberFormat.format(startingXP));
        Logger.log("Ready to start smithing!");
    }
    
    private BarType getBestBarType() {
        int currentLevel = Skill.SMITHING.getLevel();
        BarType bestBarType = null;
        
        // Find the highest level bar type that we can use
        for (BarType barType : BAR_TYPES) {
            if (currentLevel >= barType.levelRequired) {
                bestBarType = barType;
            } else {
                break; // Since array is sorted by level, no point checking higher levels
            }
        }
        
        return bestBarType;
    }
    
    private void checkForLevelUp() {
        BarType newBestBarType = getBestBarType();
        if (newBestBarType != null && newBestBarType != currentBarType) {
            Logger.log("LEVEL UP DETECTED! Switching from " + currentBarType.barName + " to " + newBestBarType.barName);
            currentBarType = newBestBarType;
            Logger.log("Now targeting: " + currentBarType.dartTipName + "s");
        }
    }
    
    private boolean hasHammer() {
        return Inventory.contains("Hammer") || 
               Inventory.contains("Imcando hammer") ||
               // Add other hammer variants as needed
               Players.getLocal().getEquipment() != null;
    }
    
    private State getState() {
        // Check for level up and update bar type if needed
        checkForLevelUp();
        
        // Only bank if we have dart tips AND no bars (finished smithing)
        if (Inventory.contains(currentBarType.dartTipName) && !Inventory.contains(currentBarType.barName)) {
            if (BANK_AREA.contains(Players.getLocal()) && !Bank.isOpen()) {
                return State.BANKING;
            } else if (BANK_AREA.contains(Players.getLocal()) && Bank.isOpen()) {
                return State.BANKING;
            } else {
                return State.WALKING_TO_BANK;
            }
        }
        
        // If we don't have bars and no dart tips, go get bars
        if (!Inventory.contains(currentBarType.barName) && !Inventory.contains(currentBarType.dartTipName)) {
            if (BANK_AREA.contains(Players.getLocal()) && !Bank.isOpen()) {
                return State.BANKING;
            } else if (BANK_AREA.contains(Players.getLocal()) && Bank.isOpen()) {
                return State.BANKING;
            } else {
                return State.WALKING_TO_BANK;
            }
        }
        
        // If we have bars, continue smithing
        if (Inventory.contains(currentBarType.barName)) {
            if (ANVIL_AREA.contains(Players.getLocal())) {
                // Check if smithing interface is open
                if (Widgets.isVisible(SMITHING_INTERFACE)) {
                    return State.SMITHING;
                } else if (Players.getLocal().isAnimating()) {
                    return State.WAITING_FOR_SMITHING;
                } else {
                    return State.SMITHING;
                }
            } else {
                return State.WALKING_TO_ANVIL;
            }
        }
        
        return State.IDLE;
    }
    
    private void handleBanking() {
        if (!Bank.isOpen()) {
            Logger.log("Opening bank...");
            if (Bank.open()) {
                Sleep.sleepUntil(Bank::isOpen, randomSleep(3000, 5000));
            }
            return;
        }
        
        // Deposit dart tips if we have them
        if (Inventory.contains(currentBarType.dartTipName)) {
            Logger.log("Depositing " + currentBarType.dartTipName + "s...");
            int dartTipCount = Inventory.count(currentBarType.dartTipName);
            if (Bank.deposit(currentBarType.dartTipName, dartTipCount)) {
                dartTipsMade += dartTipCount;
                Sleep.sleepUntil(() -> !Inventory.contains(currentBarType.dartTipName), randomSleep(2000, 3000));
                Logger.log("Deposited " + dartTipCount + " " + currentBarType.dartTipName + "s! Total made: " + dartTipsMade);
            }
            return;
        }
        
        // Withdraw bars if we don't have them
        if (!Inventory.contains(currentBarType.barName)) {
            if (!Bank.contains(currentBarType.barName)) {
                Logger.log("ERROR: No " + currentBarType.barName + "s in bank!");
                stop();
                return;
            }
            
            Logger.log("Withdrawing 27 " + currentBarType.barName + "s...");
            if (Bank.withdraw(currentBarType.barName, 27)) {
                Sleep.sleepUntil(() -> Inventory.contains(currentBarType.barName), randomSleep(2000, 3000));
                Logger.log("Withdrawn " + currentBarType.barName + "s: " + Inventory.count(currentBarType.barName));
            }
        }
        
        // Close bank when done
        if (Inventory.contains(currentBarType.barName) && !Inventory.contains(currentBarType.dartTipName)) {
            Logger.log("Closing bank...");
            Bank.close();
            Sleep.sleepUntil(() -> !Bank.isOpen(), randomSleep(1000, 2000));
        }
    }
    
    private void walkToAnvil() {
        Logger.log("Walking to anvil...");
        if (Walking.walk(ANVIL_TILE)) {
            Sleep.sleepUntil(() -> ANVIL_AREA.contains(Players.getLocal()) || 
                           !Players.getLocal().isMoving(), 
                           randomSleep(8000, 12000));
        }
    }
    
    private void walkToBank() {
        Logger.log("Walking to bank...");
        if (Walking.walk(BANK_TILE)) {
            Sleep.sleepUntil(() -> BANK_AREA.contains(Players.getLocal()) || 
                           !Players.getLocal().isMoving(), 
                           randomSleep(8000, 12000));
        }
    }
    
    private void handleSmithing() {
        GameObject anvil = GameObjects.closest("Anvil");
        if (anvil == null) {
            Logger.log("No anvil found nearby");
            return;
        }
        
        // If smithing interface is not open, use bar on anvil
        if (!Widgets.isVisible(SMITHING_INTERFACE)) {
            Logger.log("Using " + currentBarType.barName + " on anvil...");
            if (Inventory.interact(currentBarType.barName, "Use")) {
                Sleep.sleep(randomSleep(600, 1000));
                if (anvil.interact("Use")) {
                    Sleep.sleepUntil(() -> Widgets.isVisible(SMITHING_INTERFACE), 
                                   randomSleep(3000, 5000));
                }
            }
        } else {
            Logger.log("Smithing interface is open, looking for dart tips option...");
            
            // Use the current bar type's widget information
            WidgetChild dartTipsWidget = Widgets.getWidgetChild(SMITHING_INTERFACE, currentBarType.widgetParent, currentBarType.widgetChild);
            if (dartTipsWidget != null && dartTipsWidget.isVisible()) {
                Logger.log("Found dart tips widget, attempting to click...");

                boolean clicked = false;

                // Method 1: Try specific "Smith set" action
                if (dartTipsWidget.interact("Smith set")) {
                    Logger.log("Clicked dart tips with 'Smith set' action");
                    clicked = true;
                }
                // Method 2: Try generic interact
                else if (dartTipsWidget.interact()) {
                    Logger.log("Clicked dart tips with generic interact");
                    clicked = true;
                }

                if (clicked) {
                    int barCount = Inventory.count(currentBarType.barName);
                    barsUsed += barCount;
                    Sleep.sleepUntil(() -> Players.getLocal().isAnimating() ||
                                    !Widgets.isVisible(SMITHING_INTERFACE),
                            randomSleep(3000, 6000));
                    Logger.log("Started smithing " + barCount + " " + currentBarType.barName + "s into " + currentBarType.dartTipName + "s...");
                } else {
                    Logger.log("Failed to click dart tips widget");
                }
            }
        }
    }
    
    private void waitForSmithing() {
        // Wait while player is animating and we still have bars
        if (Players.getLocal().isAnimating() && Inventory.contains(currentBarType.barName)) {
            Logger.log("Smithing in progress... Bars remaining: " + Inventory.count(currentBarType.barName));
            Sleep.sleep(randomSleep(4000, 6000)); // Doubled wait time for dart tips
        } else if (!Inventory.contains(currentBarType.barName)) {
            Logger.log("All " + currentBarType.barName.toLowerCase() + "s processed into dart tips!");
        }
    }
    
    private int randomSleep(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }
    
    @Override
    public int onLoop() {
        State currentState = getState();
        Logger.log("Current state: " + currentState);
        
        switch (currentState) {
            case BANKING:
                handleBanking();
                break;
                
            case WALKING_TO_ANVIL:
                walkToAnvil();
                break;
                
            case WALKING_TO_BANK:
                walkToBank();
                break;
                
            case SMITHING:
                handleSmithing();
                break;
                
            case WAITING_FOR_SMITHING:
                waitForSmithing();
                break;
                
            case IDLE:
                Logger.log("Idle state - checking conditions...");
                Sleep.sleep(randomSleep(2000, 3000));
                break;
        }
        
        return randomSleep(600, 1200);
    }
    
    @Override
    public void onExit() {
        Logger.log("Dart Tip Smither stopped!");
        Logger.log("Bars used: " + barsUsed);
        Logger.log("Dart tips made: " + dartTipsMade);
        
        // Calculate runtime
        long timeElapsed = System.currentTimeMillis() - startTime;
        long seconds = timeElapsed / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        String timeStr = String.format("%02d:%02d:%02d", hours % 24, minutes % 60, seconds % 60);
        Logger.log("Total runtime: " + timeStr);
        
        // Calculate rates
        double hoursElapsed = timeElapsed / (1000.0 * 60.0 * 60.0);
        int dartTipsPerHour = hoursElapsed > 0 ? (int) (dartTipsMade / hoursElapsed) : 0;
        Logger.log("Dart tips per hour: " + dartTipsPerHour);
        
        // Final XP summary
        int finalXP = Skill.SMITHING.getExperience();
        int xpGained = finalXP - startingXP;
        int xpPerHour = hoursElapsed > 0 ? (int) (xpGained / hoursElapsed) : 0;
        Logger.log("Starting Smithing XP: " + numberFormat.format(startingXP));
        Logger.log("Final Smithing XP: " + numberFormat.format(finalXP));
        Logger.log("Total XP Gained: " + numberFormat.format(xpGained));
        Logger.log("XP per hour: " + numberFormat.format(xpPerHour));
        Logger.log("Current Smithing level: " + Skill.SMITHING.getLevel());
    }
    
    @Override
    public void onPaint(Graphics2D graphics) {
        // Set up graphics properties
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Background panel
        graphics.setColor(new Color(0, 0, 0, 150));
        graphics.fillRoundRect(10, 10, 320, 150, 10, 10);
        
        // Border
        graphics.setColor(Color.WHITE);
        graphics.setStroke(new BasicStroke(2));
        graphics.drawRoundRect(10, 10, 320, 150, 10, 10);
        
        // Title
        graphics.setColor(Color.ORANGE);
        graphics.setFont(new Font("Arial", Font.BOLD, 16));
        graphics.drawString("Dart Tip Smither", 20, 30);
        
        // Calculate XP data
        int currentSmithingXP = Skill.SMITHING.getExperience();
        int xpGained = currentSmithingXP - startingXP;
        int currentLevel = Skill.SMITHING.getLevel();
        int xpToNextLevel = Skill.SMITHING.getExperienceToLevel();
        
        // Calculate time and rates
        long timeElapsed = System.currentTimeMillis() - startTime;
        double hoursElapsed = timeElapsed / (1000.0 * 60.0 * 60.0);
        int dartTipsPerHour = hoursElapsed > 0 ? (int) (dartTipsMade / hoursElapsed) : 0;
        int barsPerHour = hoursElapsed > 0 ? (int) (barsUsed / hoursElapsed) : 0;
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
        
        // Show current state
        State currentState = getState();
        graphics.setColor(Color.CYAN);
        graphics.drawString("State: " + currentState, 20, yOffset + 15);
    }
}
