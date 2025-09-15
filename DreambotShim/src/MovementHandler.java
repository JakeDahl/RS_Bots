import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.walking.impl.Walking;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles movement and walking functionality
 */
public class MovementHandler {
    
    private static final AtomicBoolean skipRequested = new AtomicBoolean(false);
    private TaskManager taskManager;
    
    public MovementHandler(TaskManager taskManager) {
        this.taskManager = taskManager;
    }
    
    /**
     * DreamBot-specific walking method - Enhanced with unlimited attempts until arrival and skip support
     * Supports 3D coordinates with z (plane) parameter
     */
    public String walkToLocation(int x, int y, int z) {
        try {
            // Debug logging for parameter values
            Logger.log("DEBUG: walkToLocation received parameters - x: " + x + " (type: int), y: " + y + " (type: int), z: " + z + " (type: int)");
            
            taskManager.setCurrentStep("Walking to location (" + x + ", " + y + ", " + z + ")");
            
            Tile target = new Tile(x, y, z);
            Logger.log("DEBUG: Created Tile object with coordinates (" + target.getX() + ", " + target.getY() + ", " + target.getZ() + ")");
            Logger.log("Python->Java: Walking to location (" + x + ", " + y + ", " + z + ") - Will not return until arrived or skipped");

            // Check if already at the target (within 3 tiles AND on same plane)
            if (Players.getLocal() != null) {
                Tile currentTile = Players.getLocal().getTile();
                if (currentTile.distance(target) <= 3 && currentTile.getZ() == target.getZ()) {
                    taskManager.setCurrentStep("Idle - Waiting for commands");
                    return "Walking to (" + x + ", " + y + ", " + z + ") - Already at destination";
                }
            }
            
            // Walking loop - keep walking until we reach the destination (unlimited attempts) or skip is requested
            int attempts = 0;
            
            while (true) { // Unlimited attempts until we arrive or skip
                // Check for skip request
                if (skipRequested.get()) {
                    Logger.log("Python->Java: Skip requested, canceling walk");
                    skipRequested.set(false);
                    taskManager.setCurrentStep("Idle - Waiting for commands");
                    return "Walking to (" + x + ", " + y + ") - SKIPPED";
                }
                
                attempts++;
                
                if (Players.getLocal() == null) {
                    Logger.log("Python->Java: Player is null, waiting before retry...");
                    try {
                        Thread.sleep(2000); // Wait longer if player is null
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        taskManager.setCurrentStep("Idle - Waiting for commands");
                        return "Walking to (" + x + ", " + y + ") - INTERRUPTED";
                    }
                    continue; // Try again
                }
                
                Tile currentTile = Players.getLocal().getTile();
                double currentDistance = currentTile.distance(target);
                
                Logger.log("Python->Java: Distance to target: " + String.format("%.1f", currentDistance) + " tiles (attempt " + attempts + ")");
                taskManager.setCurrentStep("Walking to (" + x + ", " + y + ") - " + String.format("%.1f", currentDistance) + " tiles away");
                
                // Check if we're within 3 tiles of the target AND on same plane - SUCCESS EXIT
                if (currentDistance <= 3 && currentTile.getZ() == target.getZ()) {
                    Logger.log("Python->Java: Successfully arrived at destination after " + attempts + " attempts!");
                    taskManager.setCurrentStep("Idle - Waiting for commands");
                    return "Walking to (" + x + ", " + y + ", " + z + ") - SUCCESS (arrived after " + attempts + " attempts)";
                }
                
                // Attempt to walk to the target
                Logger.log("Python->Java: Attempting to walk to target (attempt " + attempts + ")");
                boolean walkInitiated = Walking.walk(target);
                
                if (walkInitiated) {
                    // Wait for the player to start moving, reach destination, or stop moving
                    Logger.log("Python->Java: Walk command sent, waiting for movement...");
                    boolean walkCompleted = Sleep.sleepUntil(() -> {
                        // Check for skip request during movement
                        if (skipRequested.get()) {
                            Logger.log("Python->Java: Skip requested during movement");
                            return true;
                        }
                        
                        if (Players.getLocal() == null) return false; // Don't exit if player becomes null
                        
                        Tile newTile = Players.getLocal().getTile();
                        double newDistance = newTile.distance(target);
                        
                        // Check if we've arrived (within 3 tiles AND on same plane)
                        if (newDistance <= 3 && newTile.getZ() == target.getZ()) {
                            Logger.log("Python->Java: Destination reached during movement!");
                            return true;
                        }
                        
                        // Check if player stopped moving (but haven't arrived yet)
                        if (!Players.getLocal().isMoving()) {
                            Logger.log("Python->Java: Player stopped moving, distance: " + String.format("%.1f", newDistance));
                            return true; // Exit sleep to try walking again
                        }
                        
                        return false; // Keep waiting
                    }, 10000); // Wait up to 10 seconds for each walk attempt
                    
                    // Check for skip after movement wait
                    if (skipRequested.get()) {
                        Logger.log("Python->Java: Skip requested, canceling walk");
                        skipRequested.set(false);
                        taskManager.setCurrentStep("Idle - Waiting for commands");
                        return "Walking to (" + x + ", " + y + ") - SKIPPED";
                    }
                    
                    // Check if we've arrived after this walk attempt
                    if (Players.getLocal() != null) {
                        Tile finalTile = Players.getLocal().getTile();
                        double finalDistance = finalTile.distance(target);
                        
                        if (finalDistance <= 3 && finalTile.getZ() == target.getZ()) {
                            Logger.log("Python->Java: Successfully arrived at destination!");
                            taskManager.setCurrentStep("Idle - Waiting for commands");
                            return "Walking to (" + x + ", " + y + ", " + z + ") - SUCCESS (arrived after " + attempts + " attempts)";
                        }
                        
                        // Log progress made
                        double progress = currentDistance - finalDistance;
                        if (progress > 0) {
                            Logger.log("Python->Java: Made progress: " + String.format("%.1f", progress) + " tiles closer");
                        } else {
                            Logger.log("Python->Java: No progress made, will retry...");
                        }
                    }
                } else {
                    Logger.log("Python->Java: Walk command failed, retrying...");
                }
                
                // Small delay between attempts to prevent spam
                try {
                    Thread.sleep(1500); // 1.5 second delay between attempts
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    taskManager.setCurrentStep("Idle - Waiting for commands");
                    return "Walking to (" + x + ", " + y + ") - INTERRUPTED";
                }
                
                // Log status every 10 attempts
                if (attempts % 10 == 0) {
                    if (Players.getLocal() != null) {
                        double currentDist = Players.getLocal().getTile().distance(target);
                        Logger.log("Python->Java: Status update - Attempt " + attempts + ", Distance: " + String.format("%.1f", currentDist) + " tiles");
                    }
                }
            }
            
        } catch (Exception e) {
            Logger.log("Python->Java: Exception in walkToLocation: " + e.getMessage());
            e.printStackTrace();
            taskManager.setCurrentStep("Idle - Waiting for commands");
            return "Walking to (" + x + ", " + y + ") - ERROR: " + e.getMessage();
        }
    }
    
    /**
     * Request to skip the current operation
     */
    public String requestSkip() {
        skipRequested.set(true);
        taskManager.setCurrentStep("Skipping current task...");
        Logger.log("Python->Java: Skip requested via method call");
        return "Skip requested";
    }
}
