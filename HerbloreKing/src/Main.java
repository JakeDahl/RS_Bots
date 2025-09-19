import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.widget.Widgets;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;

import java.util.Random;
import java.awt.*;

@ScriptManifest(name = "Prayer Potion Maker", description = "Makes prayer potions from grimy ranarr weeds, ranarr weeds, vials of water, and snape grass",
        author = "HerbloreKing", version = 1.0, category = Category.HERBLORE, image = "")

public class Main extends AbstractScript {
    
    private final Random random = new Random();
    
    // Tracking variables
    private int prayerPotionsMade = 0;
    private int grimyRanarrCleaned = 0;
    private int unfPotionsMade = 0;
    private long startTime;
    
    private enum State {
        BANKING,
        CHECKING_GRIMY_RANARR,
        WITHDRAWING_GRIMY_RANARR,
        CLEANING_GRIMY_RANARR,
        CHECKING_CLEAN_RANARR,
        WITHDRAWING_RANARR_AND_VIALS,
        MAKING_UNF_POTIONS,
        CHECKING_SNAPEGRASS,
        WITHDRAWING_SNAPEGRASS,
        MAKING_PRAYER_POTIONS,
        IDLE
    }
    
    @Override
    public void onStart() {
        Logger.log("Prayer Potion Maker started!");
        Logger.log("This bot will make prayer potions following this hierarchy:");
        Logger.log("1. Check for grimy ranarr -> clean them");
        Logger.log("2. If no grimy but clean ranarr exists -> make ranarr potion (unf)");
        Logger.log("3. Check for snape grass -> complete prayer potions");
        
        startTime = System.currentTimeMillis();
        Logger.log("Ready to start making prayer potions!");
    }
    
    private State getState() {
        // Check inventory state to determine next action
        boolean hasGrimyRanarr = Inventory.contains("Grimy ranarr weed");
        boolean hasCleanRanarr = Inventory.contains("Ranarr weed");
        boolean hasVialsOfWater = Inventory.contains("Vial of water");
        boolean hasRanarrPotionUnf = Inventory.contains("Ranarr potion (unf)");
        boolean hasSnapegrass = Inventory.contains("Snape grass");
        boolean hasPrayerPotions = Inventory.contains("Prayer potion(3)") || Inventory.contains("Prayer potion(4)");
        
        // If we have completed prayer potions, bank them
        if (hasPrayerPotions) {
            return State.BANKING;
        }
        
        // If we have ranarr potion (unf) and snapegrass, make prayer potions
        if (hasRanarrPotionUnf && hasSnapegrass) {
            return State.MAKING_PRAYER_POTIONS;
        }
        
        // If we have ranarr potion (unf) but no snape grass, always go to bank first to deposit and check for snape grass
        if (hasRanarrPotionUnf && !hasSnapegrass) {
            return State.BANKING; // Deposit unf potions first, then check for snape grass
        }
        
        // If we have clean ranarr but no vials (after cleaning), go to bank to deposit
        if (hasCleanRanarr && !hasVialsOfWater && !hasRanarrPotionUnf) {
            return State.BANKING;
        }
        
        // If we have clean ranarr and vials of water, make unf potions
        if (hasCleanRanarr && hasVialsOfWater) {
            return State.MAKING_UNF_POTIONS;
        }
        
        // If we have grimy ranarr, clean them (but only if bank is closed)
        if (hasGrimyRanarr) {
            if (Bank.isOpen()) {
                return State.BANKING; // Close bank first
            } else {
                return State.CLEANING_GRIMY_RANARR;
            }
        }
        
        // If inventory is empty or only has partial materials, check what we need
        if (Inventory.isEmpty() || (!hasGrimyRanarr && !hasCleanRanarr && !hasVialsOfWater && !hasRanarrPotionUnf)) {
            // Need to bank to check for materials
            if (!Bank.isOpen()) {
                return State.BANKING;
            } else {
                // First priority: check for grimy ranarr
                return State.CHECKING_GRIMY_RANARR;
            }
        }
        
        // Default case - if bank is not open and we need to do banking operations, open bank
        if (!Bank.isOpen()) {
            return State.BANKING;
        }
        
        return State.IDLE;
    }
    
    private void handleBanking() {
        // If bank is open and we have grimy ranarr (need to close to clean), just close it
        if (Bank.isOpen() && Inventory.contains("Grimy ranarr weed")) {
            Logger.log("Closing bank to clean grimy ranarr...");
            Bank.close();
            Sleep.sleepUntil(() -> !Bank.isOpen(), randomSleep(2000, 3000));
            return;
        }
        
        NPC banker = NPCs.closest("Banker");
        
        if (banker != null) {
            Logger.log("Opening bank with Banker...");
            if (banker.interact("Bank")) {
                Sleep.sleepUntil(() -> Bank.isOpen(), randomSleep(3000, 5000));
                
                // Deposit any completed prayer potions first
                if (Inventory.contains("Prayer potion(3)") || Inventory.contains("Prayer potion(4)")) {
                    Logger.log("Depositing completed prayer potions...");
                    Bank.depositAll("Prayer potion(3)");
                    Bank.depositAll("Prayer potion(4)");
                    Sleep.sleep(randomSleep(1000, 1500));
                }
                
                // Deposit ranarr potion (unf) before making more
                if (Inventory.contains("Ranarr potion (unf)")) {
                    Logger.log("Depositing ranarr potion (unf)...");
                    Bank.depositAll("Ranarr potion (unf)");
                    Sleep.sleep(randomSleep(1000, 1500));
                }
                
                // Deposit clean ranarr after cleaning
                if (Inventory.contains("Ranarr weed") && !Inventory.contains("Vial of water")) {
                    Logger.log("Depositing clean ranarr weed...");
                    Bank.depositAll("Ranarr weed");
                    Sleep.sleep(randomSleep(1000, 1500));
                }
            }
        } else {
            Logger.log("No Banker found nearby!");
        }
    }
    
    private void checkGrimyRanarr() {
        if (Bank.isOpen()) {
            Logger.log("Checking bank for grimy ranarr weed...");
            if (Bank.contains("Grimy ranarr weed")) {
                int grimyCount = Bank.count("Grimy ranarr weed");
                Logger.log("Found " + grimyCount + " grimy ranarr weed in bank");
                // Proceed to withdraw grimy ranarr
            } else {
                Logger.log("No grimy ranarr weed found, checking for clean ranarr...");
                // Move to next state - checking clean ranarr
            }
        }
    }
    
    private void withdrawGrimyRanarr() {
        if (Bank.isOpen() && Bank.contains("Grimy ranarr weed")) {
            Logger.log("Withdrawing 28 grimy ranarr weed...");
            if (Bank.withdraw("Grimy ranarr weed", 28)) {
                Sleep.sleepUntil(() -> Inventory.contains("Grimy ranarr weed"), randomSleep(2000, 3000));
                Logger.log("Withdrew grimy ranarr weed, closing bank...");
                Bank.close();
                Sleep.sleepUntil(() -> !Bank.isOpen(), randomSleep(2000, 3000));
            }
        }
    }
    
    private void cleanGrimyRanarr() {
        if (Inventory.contains("Grimy ranarr weed")) {
            Logger.log("Starting to clean grimy ranarr weed...");
            int initialGrimyCount = Inventory.count("Grimy ranarr weed");
            
            // Click grimy ranarr weed once to start cleaning
            if (Inventory.interact("Grimy ranarr weed", "Clean")) {
                Logger.log("Clicked grimy ranarr to start cleaning. Waiting 30 seconds or until all are cleaned...");
                
                // Wait 30 seconds or until no more grimy ranarr remains
                Sleep.sleepUntil(() -> !Inventory.contains("Grimy ranarr weed"), 33000);
                
                // Count how many we cleaned
                int finalGrimyCount = Inventory.count("Grimy ranarr weed");
                int cleanedCount = initialGrimyCount - finalGrimyCount;
                if (cleanedCount > 0) {
                    grimyRanarrCleaned += cleanedCount;
                    Logger.log("Cleaned " + cleanedCount + " grimy ranarr weed (Total: " + grimyRanarrCleaned + ")");
                }
                
                if (!Inventory.contains("Grimy ranarr weed")) {
                    Logger.log("All grimy ranarr cleaned! Going to bank to deposit...");
                } else {
                    Logger.log("Still have " + finalGrimyCount + " grimy ranarr after 30 seconds, continuing...");
                }
            } else {
                Logger.log("Failed to click grimy ranarr weed");
            }
        }
    }
    
    private void checkCleanRanarr() {
        if (Bank.isOpen()) {
            Logger.log("Checking bank for clean ranarr weed...");
            if (Bank.contains("Ranarr weed")) {
                int ranarrCount = Bank.count("Ranarr weed");
                Logger.log("Found " + ranarrCount + " clean ranarr weed in bank");
                if (Bank.contains("Vial of water")) {
                    int vialCount = Bank.count("Vial of water");
                    Logger.log("Found " + vialCount + " vials of water in bank");
                    // Proceed to withdraw materials for unf potions
                } else {
                    Logger.log("No vials of water found! Cannot make ranarr potion (unf)");
                }
            } else {
                Logger.log("No clean ranarr weed found in bank");
            }
        }
    }
    
    private void withdrawRanarrAndVials() {
        if (Bank.isOpen() && Bank.contains("Ranarr weed") && Bank.contains("Vial of water")) {
            Logger.log("Withdrawing 14 ranarr weed and 14 vials of water...");
            
            // Withdraw ranarr weed first
            if (Bank.withdraw("Ranarr weed", 14)) {
                Sleep.sleepUntil(() -> Inventory.contains("Ranarr weed"), randomSleep(1500, 2500));
            }
            
            // Then withdraw vials of water
            if (Bank.withdraw("Vial of water", 14)) {
                Sleep.sleepUntil(() -> Inventory.contains("Vial of water"), randomSleep(1500, 2500));
                Logger.log("Withdrew materials for ranarr potion (unf), closing bank...");
                Bank.close();
                Sleep.sleepUntil(() -> !Bank.isOpen(), randomSleep(2000, 3000));
            }
        }
    }
    
    private void makeUnfPotions() {
        if (Inventory.contains("Ranarr weed") && Inventory.contains("Vial of water")) {
            Logger.log("Making ranarr potion (unf)...");
            int initialUnfCount = Inventory.count("Ranarr potion (unf)");
            
            if (Inventory.interact("Ranarr weed", "Use")) {
                Sleep.sleep(randomSleep(500, 1000));
                if (Inventory.interact("Vial of water", "Use")) {
                    // Wait for the herblore interface to appear
                    Sleep.sleepUntil(() -> Widgets.isVisible(270, 13), randomSleep(2000, 3000));
                    
                    // Click the widget to make all unf potions
                    if (Widgets.isVisible(270, 13)) {
                        Logger.log("Clicking widget to make unf potions...");
                        if (Widgets.get(270, 13) != null && Widgets.get(270, 13).interact()) {
                            Sleep.sleepUntil(() -> 
                                Inventory.count("Ranarr potion (unf)") > initialUnfCount,
                                randomSleep(5000, 8000)
                            );
                        }
                    }
                    
                    int madeCount = Inventory.count("Ranarr potion (unf)") - initialUnfCount;
                    if (madeCount > 0) {
                        unfPotionsMade += madeCount;
                        Logger.log("Made " + madeCount + " ranarr potion (unf) (Total: " + unfPotionsMade + ")");
                    }
                    
                    // Wait 45 seconds or until no more materials remain
                    Logger.log("Waiting 45 seconds or until materials run out...");
                    Sleep.sleepUntil(() -> 
                        !Inventory.contains("Ranarr weed") || !Inventory.contains("Vial of water"),
                        45000
                    );
                    
                    if (!Inventory.contains("Ranarr weed") || !Inventory.contains("Vial of water")) {
                        Logger.log("Materials depleted, proceeding to banking...");
                    } else {
                        Logger.log("45 seconds elapsed, proceeding to banking...");
                    }
                }
            }
        }
    }
    
    private void checkSnapegrass() {
        if (Bank.isOpen()) {
            Logger.log("Checking bank for snape grass...");
            if (Bank.contains("Snape grass")) {
                int snapegrassCount = Bank.count("Snape grass");
                Logger.log("Found " + snapegrassCount + " snape grass in bank");
                // Proceed to withdraw snape grass
            } else {
                Logger.log("No snape grass found in bank! Cannot complete prayer potions");
            }
        }
    }
    
    private void withdrawSnapegrass() {
        if (Bank.isOpen() && Bank.contains("Snape grass")) {
            Logger.log("Withdrawing 14 snape grass...");
            if (Bank.withdraw("Snape grass", 14)) {
                Sleep.sleepUntil(() -> Inventory.contains("Snape grass"), randomSleep(2000, 3000));
                Logger.log("Withdrew snape grass, closing bank...");
                Bank.close();
                Sleep.sleepUntil(() -> !Bank.isOpen(), randomSleep(2000, 3000));
            }
        }
    }
    
    private void makePrayerPotions() {
        if (Inventory.contains("Ranarr potion (unf)") && Inventory.contains("Snape grass")) {
            Logger.log("Making prayer potions...");
            int initialPrayerCount = Inventory.count("Prayer potion(3)") + Inventory.count("Prayer potion(4)");
            
            if (Inventory.interact("Snape grass", "Use")) {
                Sleep.sleep(randomSleep(500, 1000));
                if (Inventory.interact("Ranarr potion (unf)", "Use")) {
                    // Wait for the herblore interface to appear
                    Sleep.sleepUntil(() -> Widgets.isVisible(270, 13), randomSleep(2000, 3000));
                    
                    // Click the widget to make all prayer potions
                    if (Widgets.isVisible(270, 13)) {
                        Logger.log("Clicking widget to make prayer potions...");
                        if (Widgets.get(270, 13) != null && Widgets.get(270, 13).interact()) {
                            Sleep.sleepUntil(() -> 
                                (Inventory.count("Prayer potion(3)") + Inventory.count("Prayer potion(4)")) > initialPrayerCount,
                                randomSleep(5000, 8000)
                            );
                        }
                    }
                    
                    int madeCount = (Inventory.count("Prayer potion(3)") + Inventory.count("Prayer potion(4)")) - initialPrayerCount;
                    if (madeCount > 0) {
                        prayerPotionsMade += madeCount;
                        Logger.log("Made " + madeCount + " prayer potions! (Total: " + prayerPotionsMade + ")");
                    }
                    
                    // Wait 45 seconds or until no more materials remain
                    Logger.log("Waiting 45 seconds or until materials run out...");
                    Sleep.sleepUntil(() -> 
                        !Inventory.contains("Ranarr potion (unf)") || !Inventory.contains("Snape grass"),
                        45000
                    );
                    
                    if (!Inventory.contains("Ranarr potion (unf)") || !Inventory.contains("Snape grass")) {
                        Logger.log("Materials depleted, proceeding to banking...");
                    } else {
                        Logger.log("45 seconds elapsed, proceeding to banking...");
                    }
                }
            }
        }
    }
    
    private int randomSleep(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }
    
    @Override
    public int onLoop() {
        State currentState = getState();
        
        switch (currentState) {
            case BANKING:
                handleBanking();
                break;
                
            case CHECKING_GRIMY_RANARR:
                checkGrimyRanarr();
                if (Bank.contains("Grimy ranarr weed")) {
                    withdrawGrimyRanarr();
                } else {
                    // No grimy ranarr, check for clean ranarr
                    checkCleanRanarr();
                    if (Bank.contains("Ranarr weed") && Bank.contains("Vial of water")) {
                        withdrawRanarrAndVials();
                    } else {
                        // No clean ranarr or vials, check for existing unf potions
                        if (Bank.contains("Ranarr potion (unf)")) {
                            Logger.log("Found ranarr potion (unf) in bank, checking for snape grass...");
                            checkSnapegrass();
                            if (Bank.contains("Snape grass")) {
                                // Withdraw 14 unf potions first
                                Logger.log("Withdrawing 14 ranarr potion (unf)...");
                                if (Bank.withdraw("Ranarr potion (unf)", 14)) {
                                    Sleep.sleepUntil(() -> Inventory.contains("Ranarr potion (unf)"), randomSleep(2000, 3000));
                                    withdrawSnapegrass();
                                }
                            } else {
                                Logger.log("No snape grass found in bank! Cannot complete prayer potions");
                            }
                        } else {
                            Logger.log("No materials found to continue making prayer potions");
                        }
                    }
                }
                break;
                
            case WITHDRAWING_GRIMY_RANARR:
                withdrawGrimyRanarr();
                break;
                
            case CLEANING_GRIMY_RANARR:
                cleanGrimyRanarr();
                break;
                
            case CHECKING_CLEAN_RANARR:
                checkCleanRanarr();
                break;
                
            case WITHDRAWING_RANARR_AND_VIALS:
                withdrawRanarrAndVials();
                break;
                
            case MAKING_UNF_POTIONS:
                makeUnfPotions();
                break;
                
            case CHECKING_SNAPEGRASS:
                checkSnapegrass();
                if (Bank.contains("Snape grass")) {
                    withdrawSnapegrass();
                }
                break;
                
            case WITHDRAWING_SNAPEGRASS:
                withdrawSnapegrass();
                break;
                
            case MAKING_PRAYER_POTIONS:
                makePrayerPotions();
                break;
                
            case IDLE:
                Logger.log("Waiting...");
                Sleep.sleep(randomSleep(1000, 2000));
                break;
        }
        
        return randomSleep(200, 500);
    }
    
    @Override
    public void onExit() {
        Logger.log("Prayer Potion Maker stopped!");
        
        // Calculate runtime
        long timeElapsed = System.currentTimeMillis() - startTime;
        long seconds = timeElapsed / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        String timeStr = String.format("%02d:%02d:%02d", hours % 24, minutes % 60, seconds % 60);
        Logger.log("Total runtime: " + timeStr);
        
        // Calculate rates
        double hoursElapsed = timeElapsed / (1000.0 * 60.0 * 60.0);
        int prayerPotionsPerHour = hoursElapsed > 0 ? (int) (prayerPotionsMade / hoursElapsed) : 0;
        
        Logger.log("Final Statistics:");
        Logger.log("Prayer potions made: " + prayerPotionsMade);
        Logger.log("Grimy ranarr cleaned: " + grimyRanarrCleaned);
        Logger.log("Ranarr potion (unf) made: " + unfPotionsMade);
        Logger.log("Prayer potions per hour: " + prayerPotionsPerHour);
    }
    
    @Override
    public void onPaint(Graphics2D graphics) {
        // Set up graphics properties
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Background panel
        graphics.setColor(new Color(0, 0, 0, 150));
        graphics.fillRoundRect(10, 10, 320, 140, 10, 10);
        
        // Border
        graphics.setColor(Color.WHITE);
        graphics.setStroke(new BasicStroke(2));
        graphics.drawRoundRect(10, 10, 320, 140, 10, 10);
        
        // Title
        graphics.setColor(Color.CYAN);
        graphics.setFont(new Font("Arial", Font.BOLD, 16));
        graphics.drawString("Prayer Potion Maker", 20, 30);
        
        // Calculate time and rates
        long timeElapsed = System.currentTimeMillis() - startTime;
        double hoursElapsed = timeElapsed / (1000.0 * 60.0 * 60.0);
        int prayerPotionsPerHour = hoursElapsed > 0 ? (int) (prayerPotionsMade / hoursElapsed) : 0;
        
        // Format time
        long seconds = timeElapsed / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        String timeStr = String.format("%02d:%02d:%02d", hours % 24, minutes % 60, seconds % 60);
        
        // Current state
        State currentState = getState();
        
        // Draw stats
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Arial", Font.PLAIN, 12));
        
        int yOffset = 50;
        graphics.drawString("Runtime: " + timeStr, 20, yOffset);
        yOffset += 15;
        graphics.drawString("State: " + currentState.toString(), 20, yOffset);
        yOffset += 15;
        graphics.drawString("Prayer Potions Made: " + prayerPotionsMade, 20, yOffset);
        yOffset += 15;
        graphics.drawString("Prayer Potions/Hour: " + prayerPotionsPerHour, 20, yOffset);
        yOffset += 15;
        graphics.drawString("Grimy Ranarr Cleaned: " + grimyRanarrCleaned, 20, yOffset);
        yOffset += 15;
        graphics.drawString("Ranarr Potion (unf) Made: " + unfPotionsMade, 20, yOffset);
        yOffset += 15;
        
        // Inventory status
        graphics.setColor(Color.YELLOW);
        String inventoryStatus = "Inventory: ";
        if (Inventory.contains("Grimy ranarr weed")) {
            inventoryStatus += "Grimy Ranarr (" + Inventory.count("Grimy ranarr weed") + ") ";
        }
        if (Inventory.contains("Ranarr weed")) {
            inventoryStatus += "Ranarr (" + Inventory.count("Ranarr weed") + ") ";
        }
        if (Inventory.contains("Vial of water")) {
            inventoryStatus += "Vials (" + Inventory.count("Vial of water") + ") ";
        }
        if (Inventory.contains("Ranarr potion (unf)")) {
            inventoryStatus += "Unf (" + Inventory.count("Ranarr potion (unf)") + ") ";
        }
        if (Inventory.contains("Snape grass")) {
            inventoryStatus += "Snape grass (" + Inventory.count("Snape grass") + ") ";
        }
        if (Inventory.contains("Prayer potion(3)") || Inventory.contains("Prayer potion(4)")) {
            int prayerPots = Inventory.count("Prayer potion(3)") + Inventory.count("Prayer potion(4)");
            inventoryStatus += "Prayer Pots (" + prayerPots + ") ";
        }
        graphics.drawString(inventoryStatus, 20, yOffset);
    }
}
