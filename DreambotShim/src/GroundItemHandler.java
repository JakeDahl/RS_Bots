import org.dreambot.api.methods.item.GroundItems;
import org.dreambot.api.wrappers.items.GroundItem;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.utilities.Timer;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.methods.interactive.Players;

import java.util.List;

/**
 * Handler for ground item related operations
 */
public class GroundItemHandler {
    
    private TaskManager taskManager;
    
    public GroundItemHandler(TaskManager taskManager) {
        this.taskManager = taskManager;
    }
    
    /**
     * Pick up a ground item by name
     */
    public String pickupGroundItem(String itemName) {
        try {
            // Find the closest ground item with the specified name
            GroundItem groundItem = GroundItems.closest(itemName);
            
            if (groundItem == null) {
                return "No ground item found with name: " + itemName;
            }
            
            if (!groundItem.exists()) {
                return "Ground item no longer exists: " + itemName;
            }
            
            Logger.log("Python->Java: Found ground item " + itemName + " at location: " + groundItem.getTile());
            
            // Check if we're within 1 tile of the ground item
            Tile playerTile = Players.getLocal().getTile();
            Tile itemTile = groundItem.getTile();
            int distance = (int) playerTile.distance(itemTile);
            
            Logger.log("Python->Java: Distance to ground item: " + distance + " tiles");
            
            // If we're not within 1 tile, walk closer
            if (distance > 1) {
                Logger.log("Python->Java: Walking to ground item location...");
                if (taskManager != null) {
                    taskManager.setCurrentStep("Walking to ground item: " + itemName);
                }
                
                if (Walking.walk(itemTile)) {
                    Logger.log("Python->Java: Walking command sent, waiting to get closer...");
                    
                    // Wait for walking to complete or get within 1 tile
                    boolean reachedItem = Sleep.sleepUntil(() -> {
                        if (Players.getLocal() == null) return true; // Safety check
                        return Players.getLocal().getTile().distance(itemTile) <= 1;
                    }, 10000); // 10 second timeout for walking
                    
                    if (reachedItem) {
                        Logger.log("Python->Java: Successfully moved within range of ground item");
                    } else {
                        Logger.log("Python->Java: Failed to reach ground item within timeout, attempting pickup anyway");
                    }
                } else {
                    Logger.log("Python->Java: Failed to initiate walking to ground item");
                    return "Failed to walk to ground item: " + itemName;
                }
            } else {
                Logger.log("Python->Java: Already within range of ground item");
            }
            
            // Refresh the ground item reference in case it changed
            groundItem = GroundItems.closest(itemName);
            if (groundItem == null || !groundItem.exists()) {
                return "Ground item disappeared while approaching: " + itemName;
            }
            
            if (taskManager != null) {
                taskManager.setCurrentStep("Picking up ground item: " + itemName);
            }
            
            // Try to pick up the item
            String[] actions = groundItem.getActions();
            String pickupAction = "Take"; // Default action for picking up items
            
            // Look for common pickup actions
            for (String action : actions) {
                if (action != null && (action.equalsIgnoreCase("Take") || 
                                     action.equalsIgnoreCase("Pick-up") || 
                                     action.equalsIgnoreCase("Pick up"))) {
                    pickupAction = action;
                    break;
                }
            }
            
            if (!groundItem.hasAction(pickupAction)) {
                return "Ground item does not have action '" + pickupAction + "'. Available actions: " + java.util.Arrays.toString(actions);
            }
            
            // Perform the pickup action
            if (groundItem.interact(pickupAction)) {
                // Wait a moment for the action to process
                Timer pickupTimer = new Timer(5000); // 5 seconds max wait
                while (groundItem.exists() && !pickupTimer.finished()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return "Interrupted while picking up item: " + itemName;
                    }
                }
                
                if (!groundItem.exists()) {
                    return "Successfully picked up ground item: " + itemName;
                } else {
                    return "Clicked on ground item but it still exists: " + itemName;
                }
            } else {
                return "Failed to interact with ground item: " + itemName;
            }
            
        } catch (Exception e) {
            return "Error picking up ground item '" + itemName + "': " + e.getMessage();
        }
    }
    
    /**
     * Pick up a ground item by ID
     */
    public String pickupGroundItemById(int itemId) {
        try {
            // Find the closest ground item with the specified ID
            GroundItem groundItem = GroundItems.closest(itemId);
            
            if (groundItem == null) {
                return "No ground item found with ID: " + itemId;
            }
            
            if (!groundItem.exists()) {
                return "Ground item no longer exists with ID: " + itemId;
            }
            
            Logger.log("Python->Java: Found ground item ID " + itemId + " (" + groundItem.getName() + ") at location: " + groundItem.getTile());
            
            // Check if we're within 1 tile of the ground item
            Tile playerTile = Players.getLocal().getTile();
            Tile itemTile = groundItem.getTile();
            int distance = (int) playerTile.distance(itemTile);
            
            Logger.log("Python->Java: Distance to ground item: " + distance + " tiles");
            
            // If we're not within 1 tile, walk closer
            if (distance > 1) {
                Logger.log("Python->Java: Walking to ground item location...");
                if (taskManager != null) {
                    taskManager.setCurrentStep("Walking to ground item ID: " + itemId);
                }
                
                if (Walking.walk(itemTile)) {
                    Logger.log("Python->Java: Walking command sent, waiting to get closer...");
                    
                    // Wait for walking to complete or get within 1 tile
                    boolean reachedItem = Sleep.sleepUntil(() -> {
                        if (Players.getLocal() == null) return true; // Safety check
                        return Players.getLocal().getTile().distance(itemTile) <= 1;
                    }, 10000); // 10 second timeout for walking
                    
                    if (reachedItem) {
                        Logger.log("Python->Java: Successfully moved within range of ground item");
                    } else {
                        Logger.log("Python->Java: Failed to reach ground item within timeout, attempting pickup anyway");
                    }
                } else {
                    Logger.log("Python->Java: Failed to initiate walking to ground item");
                    return "Failed to walk to ground item ID: " + itemId;
                }
            } else {
                Logger.log("Python->Java: Already within range of ground item");
            }
            
            // Refresh the ground item reference in case it changed
            groundItem = GroundItems.closest(itemId);
            if (groundItem == null || !groundItem.exists()) {
                return "Ground item disappeared while approaching ID: " + itemId;
            }
            
            if (taskManager != null) {
                taskManager.setCurrentStep("Picking up ground item ID: " + itemId);
            }
            
            // Try to pick up the item
            String[] actions = groundItem.getActions();
            String pickupAction = "Take"; // Default action for picking up items
            
            // Look for common pickup actions
            for (String action : actions) {
                if (action != null && (action.equalsIgnoreCase("Take") || 
                                     action.equalsIgnoreCase("Pick-up") || 
                                     action.equalsIgnoreCase("Pick up"))) {
                    pickupAction = action;
                    break;
                }
            }
            
            if (!groundItem.hasAction(pickupAction)) {
                return "Ground item does not have action '" + pickupAction + "'. Available actions: " + java.util.Arrays.toString(actions);
            }
            
            // Perform the pickup action
            if (groundItem.interact(pickupAction)) {
                // Wait a moment for the action to process
                Timer pickupTimer = new Timer(5000); // 5 seconds max wait
                while (groundItem.exists() && !pickupTimer.finished()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return "Interrupted while picking up item ID: " + itemId;
                    }
                }
                
                if (!groundItem.exists()) {
                    return "Successfully picked up ground item ID: " + itemId + " (" + groundItem.getName() + ")";
                } else {
                    return "Clicked on ground item but it still exists ID: " + itemId;
                }
            } else {
                return "Failed to interact with ground item ID: " + itemId;
            }
            
        } catch (Exception e) {
            return "Error picking up ground item ID '" + itemId + "': " + e.getMessage();
        }
    }
    
    /**
     * Get information about nearby ground items
     */
    public String getNearbyGroundItems() {
        try {
            List<GroundItem> allGroundItems = GroundItems.all();
            
            if (allGroundItems.isEmpty()) {
                return "No ground items found nearby";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("Found ").append(allGroundItems.size()).append(" ground items:\n");
            
            int count = 0;
            for (GroundItem item : allGroundItems) {
                if (count >= 10) { // Limit to first 10 items to avoid spam
                    result.append("... and ").append(allGroundItems.size() - count).append(" more items");
                    break;
                }
                
                result.append("- ").append(item.getName())
                      .append(" (ID: ").append(item.getId())
                      .append(", Amount: ").append(item.getAmount())
                      .append(", Distance: ").append(String.format("%.1f", Calculations.distance(Players.getLocal().getTile(), item.getTile())))
                      .append(")\n");
                count++;
            }
            
            return result.toString();
        } catch (Exception e) {
            return "Error getting nearby ground items: " + e.getMessage();
        }
    }
    
    /**
     * Check if a specific ground item exists nearby
     */
    public boolean groundItemExists(String itemName) {
        try {
            GroundItem item = GroundItems.closest(itemName);
            return item != null && item.exists();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get the distance to the closest ground item by name
     */
    public double getDistanceToGroundItem(String itemName) {
        try {
            GroundItem item = GroundItems.closest(itemName);
            if (item != null && item.exists()) {
                return Calculations.distance(Players.getLocal().getTile(), item.getTile());
            }
            return -1; // Item not found
        } catch (Exception e) {
            return -1;
        }
    }
}
