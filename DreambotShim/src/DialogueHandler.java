import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.map.Tile;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles NPC dialogue interactions with TaskManager integration
 */
public class DialogueHandler {
    
    private final TaskManager taskManager;
    private final AtomicBoolean skipRequested;
    
    public DialogueHandler(TaskManager taskManager, AtomicBoolean skipRequested) {
        this.taskManager = taskManager;
        this.skipRequested = skipRequested;
    }
    
    /**
     * Handle NPC dialogue interactions, waiting for all dialogue to complete.
     * Based on Tutorial Island DialogueHandler pattern.
     */
    public String handleNPCDialogue(String npcName, int maxWaitTime) {
        try {
            // Check if player is available instead of using isLoggedIn()
            if (Players.getLocal() == null) {
                return createErrorResponse("Player is not available");
            }

            int interactionCount = 0;
            long startTime = System.currentTimeMillis();
            long maxWaitMillis = maxWaitTime * 1000L;
            boolean npcInteractionAttempted = false;
            
            // Loop detection variables
            String rootDialogueState = null;
            Set<String> seenDialogueStates = new HashSet<>();
            boolean rootDialogueFound = false;
            
            Logger.log("Python->Java: Starting NPC dialogue handling" + (npcName.isEmpty() ? "" : " for NPC: " + npcName));
            taskManager.setCurrentStep("Handling NPC dialogue" + (npcName.isEmpty() ? "" : " with " + npcName));

            // If NPC name is provided, try to interact with the NPC first
            if (!npcName.isEmpty()) {
                Logger.log("Python->Java: Looking for NPC: " + npcName);
                NPC npc = NPCs.closest(npcName);
                if (npc != null) {
                    Logger.log("Python->Java: Found NPC " + npcName + " at location: " + npc.getTile());
                    
                    // Check if we're within 1 tile of the NPC
                    Tile playerTile = Players.getLocal().getTile();
                    Tile npcTile = npc.getTile();
                    int distance = (int) playerTile.distance(npcTile);
                    
                    Logger.log("Python->Java: Distance to NPC: " + distance + " tiles");
                    
                    // If we're not within 1 tile, walk closer
                    if (distance > 1) {
                        Logger.log("Python->Java: Walking to NPC location...");
                        taskManager.setCurrentStep("Walking to NPC " + npcName);
                        
                        if (Walking.walk(npcTile)) {
                            Logger.log("Python->Java: Walking command sent, waiting to get closer...");
                            
                            // Wait for walking to complete or get within 1 tile
                            boolean reachedNPC = Sleep.sleepUntil(() -> {
                                if (Players.getLocal() == null) return true; // Safety check
                                return Players.getLocal().getTile().distance(npcTile) <= 1;
                            }, 10000); // 10 second timeout for walking
                            
                            if (reachedNPC) {
                                Logger.log("Python->Java: Successfully moved within range of NPC");
                            } else {
                                Logger.log("Python->Java: Failed to reach NPC within timeout, attempting interaction anyway");
                            }
                        } else {
                            Logger.log("Python->Java: Failed to initiate walking to NPC");
                            return createErrorResponse("Failed to walk to NPC: " + npcName);
                        }
                    } else {
                        Logger.log("Python->Java: Already within range of NPC");
                    }
                    
                    // Now attempt to interact with the NPC
                    Logger.log("Python->Java: Attempting to interact with NPC...");
                    taskManager.setCurrentStep("Talking to NPC " + npcName);
                    
                    if (npc.interact("Talk-to")) {
                        Logger.log("Python->Java: Successfully interacted with NPC, waiting for dialogue...");
                        npcInteractionAttempted = true;
                        // Wait a moment for dialogue to appear after interaction
                        Sleep.sleep(500, 800);
                    } else {
                        Logger.log("Python->Java: Failed to interact with NPC " + npcName);
                        return createErrorResponse("Failed to interact with NPC: " + npcName);
                    }
                } else {
                    Logger.log("Python->Java: NPC " + npcName + " not found");
                    return createErrorResponse("NPC not found: " + npcName);
                }
            }

            // Wait for dialogue to appear or timeout
            long lastDialogueCheck = startTime;
            boolean initialDialogueWaitPeriod = npcInteractionAttempted;
            long dialogueWaitTimeout = 3000; // 3 seconds to wait for dialogue after NPC interaction
            
            while (System.currentTimeMillis() - startTime < maxWaitMillis) {
                try {
                    // Check for skip request
                    if (skipRequested.get()) {
                        Logger.log("Python->Java: Skip requested during dialogue handling");
                        skipRequested.set(false);
                        taskManager.setCurrentStep("Idle - Waiting for commands");
                        return createSuccessResponse("Dialogue handling skipped", interactionCount, System.currentTimeMillis() - startTime);
                    }

                    if (Dialogues.inDialogue()) {
                        interactionCount++;
                        Logger.log("Python->Java: Dialogue detected, interaction #" + interactionCount);
                        
                        // Track dialogue state for loop detection
                        String currentDialogueState = getCurrentDialogueState();
                        
                        // Skip empty dialogue states
                        if (!currentDialogueState.isEmpty()) {
                            // Check if we've seen this dialogue state before (loop detection)
                            if (seenDialogueStates.contains(currentDialogueState)) {
                                Logger.log("Python->Java: Loop detected! Encountered previously seen dialogue state. Breaking dialogue loop.");
                                Logger.log("Python->Java: Repeated dialogue state: " + currentDialogueState.substring(0, Math.min(150, currentDialogueState.length())) + "...");
                                long totalWaitTime = System.currentTimeMillis() - startTime;
                                taskManager.setCurrentStep("Idle - Waiting for commands");
                                return createSuccessResponse("Dialogue loop detected and broken", interactionCount, totalWaitTime);
                            }
                            
                            // Set root dialogue state on first dialogue encounter
                            if (rootDialogueState == null) {
                                rootDialogueState = currentDialogueState;
                                rootDialogueFound = true;
                                Logger.log("Python->Java: Set root dialogue state: " + rootDialogueState.substring(0, Math.min(100, rootDialogueState.length())) + "...");
                            }
                            
                            // Add this new dialogue state to our seen states
                            seenDialogueStates.add(currentDialogueState);
                        }
                        
                        // Handle the dialogue based on Tutorial Island pattern
                        if (Dialogues.canContinue()) {
                            Logger.log("Python->Java: Continuing dialogue...");
                            if (Dialogues.continueDialogue()) {
                                Sleep.sleep(600, 1000); // Sleep similar to Tutorial Island handler
                                continue;
                            } else {
                                Logger.log("Python->Java: Failed to continue dialogue");
                                break;
                            }
                        } else if (Dialogues.areOptionsAvailable()) {
                            String[] options = Dialogues.getOptions();
                            if (options != null && options.length > 0) {
                                // Always select the first available option to keep dialogue flowing
                                String selectedOption = options[0];
                                Logger.log("Python->Java: Selecting option: " + selectedOption + " (interaction " + interactionCount + ")");
                                
                                if (Dialogues.chooseOption(selectedOption)) {
                                    Sleep.sleep(600, 1000);
                                    continue;
                                } else {
                                    Logger.log("Python->Java: Failed to select option, trying by index");
                                    if (Dialogues.chooseOption(1)) {
                                        Sleep.sleep(600, 1000);
                                        continue;
                                    }
                                }
                            } else {
                                Logger.log("Python->Java: No options available despite areOptionsAvailable() returning true");
                            }
                        } else {
                            // If we're still in dialogue but can't continue or select options
                            String npcDialogue = Dialogues.getNPCDialogue();
                            if (npcDialogue != null && !npcDialogue.trim().isEmpty()) {
                                Logger.log("Python->Java: In dialogue but no actions available. NPC text: " + npcDialogue);
                                Sleep.sleep(500, 800);
                            } else {
                                Logger.log("Python->Java: Empty dialogue detected - treating as complete");
                                // Empty dialogues are part of the game and should be treated as complete
                                break;
                            }
                        }
                    } else {
                        // No dialogue present, check if we had any interactions
                        if (interactionCount > 0) {
                            long totalWaitTime = System.currentTimeMillis() - startTime;
                            Logger.log("Python->Java: Dialogue handling completed. Total interactions: " + interactionCount + ", Time: " + totalWaitTime + "ms");
                            taskManager.setCurrentStep("Idle - Waiting for commands");
                            return createSuccessResponse("Dialogue completed successfully", interactionCount, totalWaitTime);
                        }
                        
                        // Check if we should stop waiting for dialogue
                        long currentTime = System.currentTimeMillis();
                        
                        // If no NPC interaction was attempted and no dialogue is present, return immediately
                        if (!npcInteractionAttempted) {
                            Logger.log("Python->Java: No NPC interaction attempted and no active dialogue found");
                            taskManager.setCurrentStep("Idle - Waiting for commands");
                            return createSuccessResponse("No active dialogue found", 0, currentTime - startTime);
                        }
                        
                        // If NPC interaction was attempted but no dialogue appeared after reasonable wait time
                        if (initialDialogueWaitPeriod && (currentTime - startTime) > dialogueWaitTimeout) {
                            Logger.log("Python->Java: NPC interaction attempted but no dialogue appeared after " + dialogueWaitTimeout + "ms");
                            taskManager.setCurrentStep("Idle - Waiting for commands");
                            return createSuccessResponse("No dialogue appeared after NPC interaction", 0, currentTime - startTime);
                        }
                        
                        // Continue waiting for dialogue to appear
                        Sleep.sleep(300, 500);
                    }
                } catch (Exception e) {
                    Logger.log("Python->Java: Error during dialogue handling: " + e.getMessage());
                    Sleep.sleep(500, 700);
                }
            }

            // Timeout reached
            taskManager.setCurrentStep("Idle - Waiting for commands");
            if (interactionCount > 0) {
                long totalWaitTime = System.currentTimeMillis() - startTime;
                return createSuccessResponse("Dialogue handling timed out but had interactions", interactionCount, totalWaitTime);
            } else {
                return createErrorResponse("No dialogue found within " + maxWaitTime + " seconds");
            }

        } catch (Exception e) {
            taskManager.setCurrentStep("Idle - Waiting for commands");
            return createErrorResponse("Failed to handle NPC dialogue: " + e.getMessage());
        }
    }

    // Helper methods for dialogue handling
    
    /**
     * Create a unique state identifier for the current dialogue
     * This combines NPC text and available options to create a unique dialogue "fingerprint"
     */
    private String getCurrentDialogueState() {
        StringBuilder stateBuilder = new StringBuilder();
        
        // Get NPC dialogue text
        String npcText = Dialogues.getNPCDialogue();
        if (npcText != null && !npcText.trim().isEmpty()) {
            stateBuilder.append("NPC:").append(npcText.trim());
        }
        
        // Get available options if any
        if (Dialogues.areOptionsAvailable()) {
            String[] options = Dialogues.getOptions();
            if (options != null && options.length > 0) {
                stateBuilder.append("|OPTIONS:");
                for (int i = 0; i < options.length; i++) {
                    if (i > 0) stateBuilder.append(",");
                    stateBuilder.append(options[i]);
                }
            }
        }
        
        // Add dialogue state flags to make the state more unique
        stateBuilder.append("|CAN_CONTINUE:").append(Dialogues.canContinue());
        stateBuilder.append("|HAS_OPTIONS:").append(Dialogues.areOptionsAvailable());
        
        return stateBuilder.toString();
    }
    
    private String createSuccessResponse(String message, int interactionCount, long totalWaitTime) {
        return "{"
            + "\"success\": true, "
            + "\"message\": \"" + message + "\", "
            + "\"interactionCount\": " + interactionCount + ", "
            + "\"totalWaitTime\": " + totalWaitTime
            + "}";
    }

    private String createErrorResponse(String error) {
        return "{"
            + "\"success\": false, "
            + "\"error\": \"" + error + "\""
            + "}";
    }
}
