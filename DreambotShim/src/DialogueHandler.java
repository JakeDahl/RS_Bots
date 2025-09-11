import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.methods.dialogues.Dialogues;
import java.util.concurrent.atomic.AtomicBoolean;

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
            
            Logger.log("Python->Java: Starting NPC dialogue handling" + (npcName.isEmpty() ? "" : " for NPC: " + npcName));
            taskManager.setCurrentStep("Handling NPC dialogue" + (npcName.isEmpty() ? "" : " with " + npcName));

            // If NPC name is provided, try to interact with the NPC first
            if (!npcName.isEmpty()) {
                Logger.log("Python->Java: Looking for NPC: " + npcName);
                NPC npc = NPCs.closest(npcName);
                if (npc != null) {
                    Logger.log("Python->Java: Found NPC " + npcName + ", attempting to interact...");
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
                        
                        // Wait for dialogue to appear
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
