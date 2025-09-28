import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.widget.Widgets;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.interactive.Player;

import java.util.Random;
import java.awt.*;

@ScriptManifest(name = "Oak Larder Builder", description = "Builds and removes Oak Larders for Construction training",
        author = "ConstructionKing", version = 1.0, category = Category.CONSTRUCTION, image = "")

public class Main extends AbstractScript {
    
    private final Random random = new Random();
    
    // Tracking variables
    private int lardersBuilt = 0;
    private int lardersRemoved = 0;
    private int oakPlanksUsed = 0;
    private long startTime;
    private long butlerWaitStartTime = 0;
    private int startingXP = 0;
    private int currentXP = 0;
    
    private enum State {
        BUILD_LARDER,
        SELECTING_LARDER,
        WAITING_ANIMATION,
        REMOVE_LARDER,
        CONFIRMING_REMOVAL,
        CHECK_MATERIALS,
        NEED_RESTOCK,
        WAITING_FOR_BUTLER_ENGAGEMENT,
        TALK_TO_BUTLER,
        CONTINUING_FEE_DIALOGUE,
        CONTINUING_THANK_YOU_DIALOGUE,
        SELECTING_RESTOCK_OPTION,
        WAITING_FOR_BUTLER_RETURN,
        RECEIVING_PLANKS,
        DISMISSING_BUTLER_DIALOGUE,
        IDLE
    }
    
    @Override
    public void onStart() {
        Logger.log("Oak Larder Builder started!");
        Logger.log("This bot will build and remove Oak Larders for Construction training:");
        Logger.log("1. Build on Larder space");
        Logger.log("2. Select Oak larder from menu (widget 458,5,2)");
        Logger.log("3. Wait for construction animation to complete");
        Logger.log("4. Remove the completed Larder");
        Logger.log("5. Repeat until < 8 Oak planks remain");
        
        startTime = System.currentTimeMillis();
        Logger.log("Ready to start building Oak Larders!");
        
        // Check initial oak plank count and XP
        int oakPlankCount = Inventory.count("Oak plank");
        Logger.log("Starting with " + oakPlankCount + " Oak planks");
        
        // Track starting XP using DreamBot Skills API
        startingXP = Skills.getExperience(Skill.CONSTRUCTION);
        currentXP = startingXP;
        Logger.log("Starting Construction XP: " + startingXP);
    }
    
    private State getState() {
        int oakPlankCount = Inventory.count("Oak plank");
        
        // Check if there's a dialogue open FIRST - but handle special cases
        if (Dialogues.inDialogue()) {
            String npcDialogue = Dialogues.getNPCDialogue();
            
            // Check for level-up dialogue first - these should always be skipped immediately
            if (npcDialogue != null && (npcDialogue.contains("Congratulations") || 
                npcDialogue.contains("level up") || npcDialogue.contains("Level up") ||
                npcDialogue.contains("levelled up") || npcDialogue.contains("leveled up") ||
                npcDialogue.contains("You have achieved") || npcDialogue.contains("gained a level") ||
                npcDialogue.contains("Your construction level is now"))) {
                Logger.log("Level-up dialogue detected - skipping immediately");
                return State.DISMISSING_BUTLER_DIALOGUE; // Reuse this state to skip dialogue
            }
            
            // If this is an NPC dialogue, check for specific Butler dialogue to skip
            if (npcDialogue != null) {
                // Check for Butler "Your goods, sir" dialogue - skip processing and proceed to construction
                if (npcDialogue.contains("Your goods, sir")) {
                    Logger.log("Butler says 'Your goods, sir' - skipping dialogue and proceeding to larder construction");
                    // Don't process this dialogue, let construction continue with dialogue open
                    // This keeps the Butler in place while we build larders
                    // Fall through to construction state checks below
                } else if (npcDialogue.contains("My fee is")) {
                    Logger.log("Butler dialogue 'My fee is' detected - continuing dialogue first");
                    return State.CONTINUING_FEE_DIALOGUE;
                } else if (npcDialogue.contains("Thank you, sir.")) {
                    Logger.log("Butler dialogue 'Thank you, sir.' detected - continuing dialogue first");
                    return State.CONTINUING_THANK_YOU_DIALOGUE;
                } else if (oakPlankCount >= 8) {
                    // If we have enough planks (>= 8), only handle removal confirmations
                    // Check if this might be a removal confirmation by looking for "Yes" option
                    String[] options = Dialogues.getOptions();
                    if (options != null && options.length > 0) {
                        // Look for "Yes" option which indicates removal confirmation
                        for (String option : options) {
                            if (option.toLowerCase().contains("yes")) {
                                return State.CONFIRMING_REMOVAL;
                            }
                        }
                    }
                    // If not a removal confirmation and we have >= 8 planks, 
                    // ignore the dialogue and proceed with construction cycle
                    Logger.log("Have sufficient planks (" + oakPlankCount + "), ignoring Butler dialogue and proceeding with construction");
                } else {
                    // If we need restock (< 8), handle Butler dialogue
                    String[] options = Dialogues.getOptions();
                    if (options != null && options.length > 0) {
                        // Butler dialogue with options - select first one
                        return State.SELECTING_RESTOCK_OPTION;
                    } else {
                        // Butler return dialogue (no options)
                        return State.RECEIVING_PLANKS;
                    }
                }
            } else {
                // This is NOT an NPC dialogue (interface dialogue) - check for level-up first
                // Check if this might be a level-up interface by looking at dialogue text
                if (Dialogues.getNPCDialogue() == null) {
                    // Since getCurrentDialogue() doesn't exist, we'll skip interface dialogue level-up detection
                    // and handle interface dialogues normally
                }
                
                // This is NOT an NPC dialogue (interface dialogue) - continue through it
                Logger.log("Non-NPC dialogue detected - continuing through interface dialogue");
                String[] options = Dialogues.getOptions();
                if (options != null && options.length > 0) {
                    // Look for "Yes" option which indicates removal confirmation
                    for (String option : options) {
                        if (option.toLowerCase().contains("yes")) {
                            return State.CONFIRMING_REMOVAL;
                        }
                    }
                    // If not a removal confirmation, still process it normally
                    return State.CONFIRMING_REMOVAL; // Process any interface dialogue
                } else {
                    // Interface dialogue without options - click continue
                    return State.CONFIRMING_REMOVAL; // Process any interface dialogue
                }
            }
        }
        
        // Now check if we need to restock (< 8 oak planks) - only if no dialogue is open
        if (oakPlankCount < 8) {
            // Check if Butler is nearby to start restock process
            NPC butler = NPCs.closest("Butler");
            if (butler != null && butler.exists()) {
                // Talk to butler immediately when larder is built and planks < 8
                Logger.log("Larder built and < 8 planks remaining - talking to butler immediately");
                butlerWaitStartTime = 0; // Reset wait timer
                return State.TALK_TO_BUTLER;
            } else {
                Logger.log("Need restock but Butler not found nearby!");
                butlerWaitStartTime = 0; // Reset wait timer
                return State.IDLE;
            }
        } else {
            // Reset butler wait timer when we have enough planks
            butlerWaitStartTime = 0;
        }
        
        // Check if player is currently animating (building/removing)
        Player localPlayer = Players.getLocal();
        if (localPlayer != null && localPlayer.getAnimation() != -1) {
            return State.WAITING_ANIMATION;
        }
        
        // Check if construction interface is open (widget 458,5,2 for Oak larder)
        if (Widgets.isVisible(458, 5, 2)) {
            return State.SELECTING_LARDER;
        }
        
        // Check for completed Larder to remove
        GameObject larder = GameObjects.closest("Larder");
        if (larder != null && larder.exists()) {
            return State.REMOVE_LARDER;
        }
        
        // Check for Larder space to build on
        GameObject larderSpace = GameObjects.closest("Larder space");
        if (larderSpace != null && larderSpace.exists()) {
            return State.BUILD_LARDER;
        }
        
        return State.IDLE;
    }
    
    private void buildLarder() {
        GameObject larderSpace = GameObjects.closest("Larder space");
        if (larderSpace != null && larderSpace.exists()) {
            Logger.log("Building on Larder space...");
            if (larderSpace.interact("Build")) {
                // Wait for the construction interface to appear
                Sleep.sleepUntil(() -> Widgets.isVisible(458, 5, 2), randomSleep(3000, 5000));
                
                if (Widgets.isVisible(458, 5, 2)) {
                    Logger.log("Construction interface opened successfully");
                } else {
                    Logger.log("Construction interface did not open, retrying...");
                }
            } else {
                Logger.log("Failed to click on Larder space");
            }
        } else {
            Logger.log("No Larder space found nearby!");
        }
    }
    
    private void selectOakLarder() {
        if (Widgets.isVisible(458, 5, 2)) {
            Logger.log("Selecting Oak larder from construction menu...");
            if (Widgets.get(458, 5, 2) != null && Widgets.get(458, 5, 2).interact()) {
                // Wait for animation to start or interface to close
                Sleep.sleepUntil(() -> 
                    !Widgets.isVisible(458, 5, 2) || Players.getLocal().getAnimation() != -1,
                    randomSleep(2000, 3000)
                );
                
                if (Players.getLocal().getAnimation() != -1) {
                    Logger.log("Construction animation started");
                    int preBuildPlanks = Inventory.count("Oak plank");
                    // Wait a moment for plank consumption
                    Sleep.sleep(randomSleep(1000, 1500));
                    int postBuildPlanks = Inventory.count("Oak plank");
                    int planksUsed = preBuildPlanks - postBuildPlanks;
                    if (planksUsed > 0) {
                        oakPlanksUsed += planksUsed;
                        lardersBuilt++;
                        Logger.log("Used " + planksUsed + " Oak planks. Total used: " + oakPlanksUsed);
                        Logger.log("Larders built: " + lardersBuilt);
                    }
                } else {
                    Logger.log("Animation did not start, something may have gone wrong");
                }
            } else {
                Logger.log("Failed to click Oak larder widget");
            }
        }
    }
    
    private void waitForAnimation() {
        Player localPlayer = Players.getLocal();
        if (localPlayer != null && localPlayer.getAnimation() != -1) {
            Logger.log("Waiting for construction/removal animation to complete...");
            // Wait up to 10 seconds for animation to finish
            Sleep.sleepUntil(() -> Players.getLocal().getAnimation() == -1, 10000);
            
            if (Players.getLocal().getAnimation() == -1) {
                Logger.log("Animation completed");
            } else {
                Logger.log("Animation timeout reached, continuing...");
            }
            
            // Additional small delay after animation completes
            Sleep.sleep(randomSleep(500, 1000));
        }
    }
    
    private void removeLarder() {
        GameObject larder = GameObjects.closest("Larder");
        if (larder != null && larder.exists()) {
            Logger.log("Removing completed Larder...");
            if (larder.interact("Remove")) {
                // Wait for confirmation dialogue to appear
                Sleep.sleepUntil(() -> Dialogues.inDialogue(), randomSleep(2000, 3000));
                
                if (Dialogues.inDialogue()) {
                    Logger.log("Removal dialogue appeared, will confirm in next cycle");
                } else {
                    Logger.log("Removal dialogue did not appear");
                }
            } else {
                Logger.log("Failed to click Remove on Larder");
            }
        } else {
            Logger.log("No Larder found to remove!");
        }
    }
    
    private void confirmRemoval() {
        if (Dialogues.inDialogue()) {
            Logger.log("Confirming removal by selecting 'Yes'...");
            if (Dialogues.clickContinue() || Dialogues.chooseOption("Yes")) {
                // Wait for dialogue to close and removal animation to start
                Sleep.sleepUntil(() -> !Dialogues.inDialogue(), randomSleep(1000, 2000));
                
                if (!Dialogues.inDialogue()) {
                    Logger.log("Confirmation successful, waiting for removal animation...");
                    // Wait for removal animation to start
                    Sleep.sleepUntil(() -> Players.getLocal().getAnimation() != -1, randomSleep(2000, 3000));
                    
                    if (Players.getLocal().getAnimation() != -1) {
                        Logger.log("Removal animation started");
                        lardersRemoved++;
                        Logger.log("Larders removed: " + lardersRemoved);
                    }
                }
            } else {
                Logger.log("Failed to confirm removal");
            }
        }
    }
    
    private void checkMaterials() {
        int oakPlankCount = Inventory.count("Oak plank");
        Logger.log("Current Oak plank count: " + oakPlankCount);
        
        if (oakPlankCount < 8) {
            Logger.log("Less than 8 Oak planks remaining - restock needed!");
        } else {
            Logger.log("Sufficient materials, continuing construction cycle...");
        }
    }
    
    private void needRestock() {
        int oakPlankCount = Inventory.count("Oak plank");
        Logger.log("RESTOCK REQUIRED: Only " + oakPlankCount + " Oak planks remaining!");
        Logger.log("Starting restock routine with Butler...");
        
        // The state machine will handle talking to butler
        // This method is just for logging the restock need
    }
    
    private void waitForButlerEngagement() {
        long waitTime = System.currentTimeMillis() - butlerWaitStartTime;
        Logger.log("Waiting for butler to engage automatically... (" + (waitTime / 1000) + "/5 seconds)");
        
        // Check if butler has engaged (dialogue opened)
        if (Dialogues.inDialogue()) {
            Logger.log("Butler engaged automatically - dialogue opened!");
            butlerWaitStartTime = 0; // Reset timer
            return;
        }
        
        // Sleep briefly while waiting
        Sleep.sleep(randomSleep(200, 400));
    }
    
    private void talkToButler() {
        NPC butler = NPCs.closest("Butler");
        if (butler != null && butler.exists()) {
            Logger.log("Talking to Butler for restock...");
            if (butler.interact("Talk-to")) {
                // Wait for dialogue to appear
                Sleep.sleepUntil(() -> Dialogues.inDialogue(), randomSleep(2000, 3000));
                
                if (Dialogues.inDialogue()) {
                    Logger.log("Butler dialogue opened, will select first option in next cycle");
                } else {
                    Logger.log("Butler dialogue did not appear");
                }
            } else {
                Logger.log("Failed to talk to Butler");
            }
        } else {
            Logger.log("No Butler found to talk to!");
        }
    }
    
    private void continueFeeDialogue() {
        if (Dialogues.inDialogue()) {
            Logger.log("Continuing 'My fee is' dialogue with clickContinue...");
            if (Dialogues.clickContinue()) {
                Logger.log("Successfully continued fee dialogue, waiting for options to appear...");
                // Wait for options to appear
                Sleep.sleepUntil(() -> {
                    String[] options = Dialogues.getOptions();
                    return options != null && options.length > 0;
                }, randomSleep(2000, 3000));
            } else {
                Logger.log("Failed to continue fee dialogue");
            }
        }
    }
    
    private void continueThankYouDialogue() {
        if (Dialogues.inDialogue()) {
            Logger.log("Continuing 'Thank you, sir.' dialogue with clickContinue...");
            if (Dialogues.clickContinue()) {
                Logger.log("Successfully continued 'Thank you, sir.' dialogue, waiting for options to appear...");
                // Wait for options to appear
                Sleep.sleepUntil(() -> {
                    String[] options = Dialogues.getOptions();
                    return options != null && options.length > 0;
                }, randomSleep(2000, 3000));
            } else {
                Logger.log("Failed to continue 'Thank you, sir.' dialogue");
            }
        }
    }
    
    private void selectRestockOption() {
        if (Dialogues.inDialogue()) {
            Logger.log("Selecting first option for oak plank restock...");
            String[] options = Dialogues.getOptions();
            if (options != null && options.length > 0) {
                Logger.log("Available options: " + String.join(", ", options));
                // Select the first option
                if (Dialogues.chooseOption(1)) {
                    Logger.log("Selected first option, waiting for Butler to return...");
                    // Wait for dialogue to close
                    Sleep.sleepUntil(() -> !Dialogues.inDialogue(), randomSleep(1000, 2000));
                } else {
                    Logger.log("Failed to select first option");
                }
            } else {
                Logger.log("No options available in dialogue");
            }
        }
    }
    
    private void waitForButlerReturn() {
        Logger.log("Waiting for Butler to return with oak planks...");
        // Wait for Butler to return and open dialogue (up to 30 seconds)
        Sleep.sleepUntil(() -> Dialogues.inDialogue(), 30000);
        
        if (Dialogues.inDialogue()) {
            Logger.log("Butler returned with dialogue");
        } else {
            Logger.log("Butler did not return within 30 seconds");
        }
    }
    
    private void receivePlanks() {
        if (Dialogues.inDialogue()) {
            Logger.log("Receiving oak planks from Butler...");
            int prePlankCount = Inventory.count("Oak plank");
            
            // DO NOT CLOSE THE DIALOGUE - keep it open so Butler stays put
            // Just wait a moment for planks to be received
            Sleep.sleep(randomSleep(1000, 1500));
            
            int postPlankCount = Inventory.count("Oak plank");
            int planksReceived = postPlankCount - prePlankCount;
            
            if (planksReceived > 0) {
                Logger.log("Successfully received " + planksReceived + " Oak planks from Butler!");
                Logger.log("Current Oak plank count: " + postPlankCount);
                Logger.log("KEEPING DIALOGUE OPEN - Butler will stay in place");
                
                // Add 5-second wait after butler returns with planks
                Logger.log("Waiting 5 seconds after butler returned with planks...");
                Sleep.sleep(5000);
                Logger.log("5-second wait complete - resuming construction cycle with dialogue open...");
            } else {
                // If no planks received yet, might need to wait a bit more
                Logger.log("Waiting for plank delivery... Current count: " + postPlankCount);
                Sleep.sleep(randomSleep(500, 1000));
                
                // Check again
                postPlankCount = Inventory.count("Oak plank");
                planksReceived = postPlankCount - prePlankCount;
                if (planksReceived > 0) {
                    Logger.log("Successfully received " + planksReceived + " Oak planks from Butler!");
                    Logger.log("Current Oak plank count: " + postPlankCount);
                    Logger.log("KEEPING DIALOGUE OPEN - Butler will stay in place");
                    
                    // Add 5-second wait after butler returns with planks
                    Logger.log("Waiting 5 seconds after butler returned with planks...");
                    Sleep.sleep(5000);
                    Logger.log("5-second wait complete - resuming construction cycle with dialogue open...");
                }
            }
        }
    }
    
    private void dismissButlerDialogue() {
        if (Dialogues.inDialogue()) {
            // Check if this is a level-up dialogue first
            String npcDialogue = Dialogues.getNPCDialogue();
            
            boolean isLevelUp = false;
            if (npcDialogue != null && (npcDialogue.contains("Congratulations") || 
                npcDialogue.contains("level up") || npcDialogue.contains("Level up") ||
                npcDialogue.contains("levelled up") || npcDialogue.contains("leveled up") ||
                npcDialogue.contains("You have achieved") || npcDialogue.contains("gained a level") ||
                npcDialogue.contains("Your construction level is now"))) {
                isLevelUp = true;
                Logger.log("Level-up dialogue detected - dismissing level-up notification...");
            } else {
                Logger.log("Have enough oak planks (>= 8), dismissing Butler dialogue...");
            }
            
            String[] options = Dialogues.getOptions();
            if (options != null && options.length > 0) {
                if (isLevelUp) {
                    Logger.log("Dismissing level-up dialogue with options by selecting first option...");
                } else {
                    Logger.log("Dismissing Butler dialogue with options by selecting first option...");
                }
                if (Dialogues.chooseOption(1)) {
                    Sleep.sleepUntil(() -> !Dialogues.inDialogue(), randomSleep(2000, 3000));
                    if (!Dialogues.inDialogue()) {
                        if (isLevelUp) {
                            Logger.log("Level-up dialogue successfully dismissed");
                        } else {
                            Logger.log("Butler dialogue successfully dismissed");
                        }
                    } else {
                        Logger.log("Dialogue still open after option selection");
                    }
                }
            } else {
                if (isLevelUp) {
                    Logger.log("Dismissing level-up dialogue with clickContinue...");
                } else {
                    Logger.log("Dismissing Butler dialogue with clickContinue...");
                }
                if (Dialogues.clickContinue()) {
                    Sleep.sleepUntil(() -> !Dialogues.inDialogue(), randomSleep(2000, 3000));
                    if (!Dialogues.inDialogue()) {
                        if (isLevelUp) {
                            Logger.log("Level-up dialogue successfully dismissed");
                        } else {
                            Logger.log("Butler dialogue successfully dismissed");
                        }
                    } else {
                        Logger.log("Dialogue still open after clickContinue");
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
            case BUILD_LARDER:
                buildLarder();
                break;
                
            case SELECTING_LARDER:
                selectOakLarder();
                break;
                
            case WAITING_ANIMATION:
                waitForAnimation();
                break;
                
            case REMOVE_LARDER:
                removeLarder();
                break;
                
            case CONFIRMING_REMOVAL:
                confirmRemoval();
                break;
                
            case CHECK_MATERIALS:
                checkMaterials();
                break;
                
            case NEED_RESTOCK:
                needRestock();
                break;
                
            case WAITING_FOR_BUTLER_ENGAGEMENT:
                waitForButlerEngagement();
                break;
                
            case TALK_TO_BUTLER:
                talkToButler();
                break;
                
            case CONTINUING_FEE_DIALOGUE:
                continueFeeDialogue();
                break;
                
            case CONTINUING_THANK_YOU_DIALOGUE:
                continueThankYouDialogue();
                break;
                
            case SELECTING_RESTOCK_OPTION:
                selectRestockOption();
                break;
                
            case WAITING_FOR_BUTLER_RETURN:
                waitForButlerReturn();
                break;
                
            case RECEIVING_PLANKS:
                receivePlanks();
                break;
                
            case DISMISSING_BUTLER_DIALOGUE:
                dismissButlerDialogue();
                break;
                
            case IDLE:
                Logger.log("Waiting for next action...");
                Sleep.sleep(randomSleep(1000, 2000));
                break;
        }
        
        return randomSleep(200, 500);
    }
    
    @Override
    public void onExit() {
        Logger.log("Oak Larder Builder stopped!");
        
        // Calculate runtime
        long timeElapsed = System.currentTimeMillis() - startTime;
        long seconds = timeElapsed / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        String timeStr = String.format("%02d:%02d:%02d", hours % 24, minutes % 60, seconds % 60);
        Logger.log("Total runtime: " + timeStr);
        
        // Calculate rates
        double hoursElapsed = timeElapsed / (1000.0 * 60.0 * 60.0);
        int xpGained = currentXP - startingXP;
        int xpPerHour = hoursElapsed > 0 ? (int) (xpGained / hoursElapsed) : 0;
        
        Logger.log("Final Statistics:");
        Logger.log("Larders built: " + lardersBuilt);
        Logger.log("Larders removed: " + lardersRemoved);
        Logger.log("Oak planks used: " + oakPlanksUsed);
        Logger.log("XP gained: " + xpGained);
        Logger.log("XP per hour: " + xpPerHour);
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
        graphics.setColor(Color.ORANGE);
        graphics.setFont(new Font("Arial", Font.BOLD, 16));
        graphics.drawString("Oak Larder Builder", 20, 30);
        
        // Calculate time and XP rates
        long timeElapsed = System.currentTimeMillis() - startTime;
        double hoursElapsed = timeElapsed / (1000.0 * 60.0 * 60.0);
        
        // Update current XP using DreamBot Skills API
        currentXP = Skills.getExperience(Skill.CONSTRUCTION);
        int xpGained = currentXP - startingXP;
        int xpPerHour = hoursElapsed > 0 ? (int) (xpGained / hoursElapsed) : 0;
        
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
        graphics.drawString("XP Gained: " + xpGained, 20, yOffset);
        yOffset += 15;
        graphics.drawString("XP/Hour: " + xpPerHour, 20, yOffset);
        yOffset += 15;
        graphics.drawString("Larders Built: " + lardersBuilt, 20, yOffset);
        yOffset += 15;
        
        // Current materials
        int oakPlankCount = Inventory.count("Oak plank");
        graphics.setColor(oakPlankCount < 8 ? Color.RED : Color.YELLOW);
        graphics.drawString("Oak Planks: " + oakPlankCount + (oakPlankCount < 8 ? " (RESTOCK NEEDED!)" : ""), 20, yOffset);
        
        // Status indicator
        yOffset += 20;
        graphics.setColor(currentState == State.NEED_RESTOCK ? Color.RED : Color.GREEN);
        String statusText = currentState == State.NEED_RESTOCK ? "STOPPED - RESTOCK NEEDED" : "RUNNING";
        graphics.drawString("Status: " + statusText, 20, yOffset);
    }
}
