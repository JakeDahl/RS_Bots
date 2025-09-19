import org.dreambot.api.utilities.Logger;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.interactive.GameObjects;

/**
 * Manages inventory operations and item interactions
 */
public class InventoryManager {
    
    private TaskManager taskManager;
    
    public InventoryManager(TaskManager taskManager) {
        this.taskManager = taskManager;
    }
    
    /**
     * Get real inventory count from DreamBot API
     */
    public int getInventoryCount() {
        try {
            int count = Inventory.fullSlotCount();
            Logger.log("Python->Java: Inventory count: " + count);
            return count;
        } catch (Exception e) {
            Logger.log("Python->Java: Error getting inventory count: " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * Check if inventory contains a specific item and return count
     * Returns -1 if item not found, 0+ for actual count
     */
    public int checkInventoryForItem(String itemName, boolean useItemId) {
        try {
            Logger.log("Python->Java: Checking inventory for item: " + itemName + " (useItemId: " + useItemId + ")");
            
            if (useItemId) {
                // Parse item as ID
                try {
                    int itemId = Integer.parseInt(itemName);
                    
                    if (Inventory.contains(itemId)) {
                        int count = Inventory.count(itemId);
                        Logger.log("Python->Java: Found " + count + " of item ID " + itemId + " in inventory");
                        return count;
                    } else {
                        Logger.log("Python->Java: Item ID " + itemId + " not found in inventory");
                        return -1;
                    }
                } catch (NumberFormatException e) {
                    Logger.log("Python->Java: Invalid item ID provided: " + itemName);
                    return -1;
                }
            } else {
                // Use item as name
                if (Inventory.contains(itemName)) {
                    int count = Inventory.count(itemName);
                    Logger.log("Python->Java: Found " + count + " of '" + itemName + "' in inventory");
                    return count;
                } else {
                    Logger.log("Python->Java: Item '" + itemName + "' not found in inventory");
                    return -1;
                }
            }
        } catch (Exception e) {
            Logger.log("Python->Java: Exception in checkInventoryForItem: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }
    
    /**
     * Check if inventory contains a specific item (boolean result)
     * Simple true/false check without count
     */
    public boolean inventoryContainsItem(String itemName, boolean useItemId) {
        try {
            Logger.log("Python->Java: Checking if inventory contains item: " + itemName + " (useItemId: " + useItemId + ")");
            
            if (useItemId) {
                // Parse item as ID
                try {
                    int itemId = Integer.parseInt(itemName);
                    boolean contains = Inventory.contains(itemId);
                    Logger.log("Python->Java: Inventory " + (contains ? "contains" : "does not contain") + " item ID " + itemId);
                    return contains;
                } catch (NumberFormatException e) {
                    Logger.log("Python->Java: Invalid item ID provided: " + itemName);
                    return false;
                }
            } else {
                // Use item as name
                boolean contains = Inventory.contains(itemName);
                Logger.log("Python->Java: Inventory " + (contains ? "contains" : "does not contain") + " '" + itemName + "'");
                return contains;
            }
        } catch (Exception e) {
            Logger.log("Python->Java: Exception in inventoryContainsItem: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Use one item on another item in inventory (item combination/crafting)
     */
    public String useItemOnItem(String primaryItem, String secondaryItem, boolean useItemIds) {
        try {
            Logger.log("Python->Java: Using " + primaryItem + " on " + secondaryItem + " (useItemIds: " + useItemIds + ")");
            
            if (useItemIds) {
                // Parse items as IDs
                try {
                    int primaryId = Integer.parseInt(primaryItem);
                    int secondaryId = Integer.parseInt(secondaryItem);
                    
                    // Check if both items exist in inventory
                    if (!Inventory.contains(primaryId)) {
                        Logger.log("Python->Java: Primary item with ID " + primaryId + " not found in inventory");
                        return "Use item on item - FAILED (primary item with ID " + primaryId + " not found)";
                    }
                    
                    if (!Inventory.contains(secondaryId)) {
                        Logger.log("Python->Java: Secondary item with ID " + secondaryId + " not found in inventory");
                        return "Use item on item - FAILED (secondary item with ID " + secondaryId + " not found)";
                    }
                    
                    Logger.log("Python->Java: Attempting to combine items by ID: " + primaryId + " on " + secondaryId);
                    boolean success = Inventory.combine(primaryId, secondaryId);
                    
                    if (success) {
                        Logger.log("Python->Java: Successfully used item " + primaryId + " on " + secondaryId);
                        return "Use item on item - SUCCESS (used ID " + primaryId + " on ID " + secondaryId + ")";
                    } else {
                        Logger.log("Python->Java: Failed to use item " + primaryId + " on " + secondaryId);
                        return "Use item on item - FAILED (combine operation failed)";
                    }
                } catch (NumberFormatException e) {
                    Logger.log("Python->Java: Invalid item IDs provided: " + primaryItem + ", " + secondaryItem);
                    return "Use item on item - ERROR: Invalid item IDs (must be numbers)";
                }
            } else {
                // Use items as names
                // Check if both items exist in inventory
                if (!Inventory.contains(primaryItem)) {
                    Logger.log("Python->Java: Primary item '" + primaryItem + "' not found in inventory");
                    return "Use item on item - FAILED (primary item '" + primaryItem + "' not found)";
                }
                
                if (!Inventory.contains(secondaryItem)) {
                    Logger.log("Python->Java: Secondary item '" + secondaryItem + "' not found in inventory");
                    return "Use item on item - FAILED (secondary item '" + secondaryItem + "' not found)";
                }
                
                Logger.log("Python->Java: Attempting to combine items by name: " + primaryItem + " on " + secondaryItem);
                boolean success = Inventory.combine(primaryItem, secondaryItem);
                
                if (success) {
                    Logger.log("Python->Java: Successfully used " + primaryItem + " on " + secondaryItem);
                    return "Use item on item - SUCCESS (used '" + primaryItem + "' on '" + secondaryItem + "')";
                } else {
                    Logger.log("Python->Java: Failed to use " + primaryItem + " on " + secondaryItem);
                    return "Use item on item - FAILED (combine operation failed)";
                }
            }
        } catch (Exception e) {
            Logger.log("Python->Java: Exception in useItemOnItem: " + e.getMessage());
            e.printStackTrace();
            return "Use item on item - ERROR: " + e.getMessage();
        }
    }
    
    /**
     * Perform a custom action on an item, or use an item on a game object
     * Examples: "Eat" on "Lobster", use "Bread" on "Oven", "Drop" an item, etc.
     */
    public String performItemAction(String action, String item, String target, boolean useItemIds, String targetType) {
        try {
            Logger.log("Python->Java: Performing action '" + action + "' on item '" + item + 
                      (target != null ? "' with target '" + target + "' (type: " + targetType + ")" : "'") + 
                      " (useItemIds: " + useItemIds + ")");
            
            taskManager.setCurrentStep("Performing " + action + " on " + item + 
                                     (target != null ? " with " + target : ""));
            
            // Check if item exists in inventory
            if (useItemIds) {
                try {
                    int itemId = Integer.parseInt(item);
                    if (!Inventory.contains(itemId)) {
                        Logger.log("Python->Java: Item with ID " + itemId + " not found in inventory");
                        taskManager.setCurrentStep("Idle - Waiting for commands");
                        return "Perform item action - FAILED (item with ID " + itemId + " not found in inventory)";
                    }
                } catch (NumberFormatException e) {
                    Logger.log("Python->Java: Invalid item ID: " + item);
                    taskManager.setCurrentStep("Idle - Waiting for commands");
                    return "Perform item action - ERROR: Invalid item ID (must be a number)";
                }
            } else {
                if (!Inventory.contains(item)) {
                    Logger.log("Python->Java: Item '" + item + "' not found in inventory");
                    taskManager.setCurrentStep("Idle - Waiting for commands");
                    return "Perform item action - FAILED (item '" + item + "' not found in inventory)";
                }
            }
            
            boolean success = false;
            String resultMessage = "";
            
            if (target == null || target.trim().isEmpty() || "null".equals(target)) {
                // Simple item action (e.g., "Eat", "Drop", "Drink", "Wield")
                Logger.log("Python->Java: Performing simple action '" + action + "' on item");
                
                if (useItemIds) {
                    int itemId = Integer.parseInt(item);
                    success = performSpecificItemAction(action, itemId, true);
                    resultMessage = success ? 
                        "Performed '" + action + "' on item ID " + itemId + " - SUCCESS" :
                        "Performed '" + action + "' on item ID " + itemId + " - FAILED";
                } else {
                    success = performSpecificItemAction(action, item, false);
                    resultMessage = success ? 
                        "Performed '" + action + "' on '" + item + "' - SUCCESS" :
                        "Performed '" + action + "' on '" + item + "' - FAILED";
                }
            } else {
                // Action with target
                if ("item".equals(targetType)) {
                    // Target is another inventory item - use item on item
                    Logger.log("Python->Java: Using item '" + item + "' on inventory item '" + target + "'");
                    
                    if (useItemIds) {
                        try {
                            int itemId = Integer.parseInt(item);
                            int targetId = Integer.parseInt(target);
                            success = Inventory.combine(itemId, targetId);
                            resultMessage = success ?
                                "Used item ID " + itemId + " on item ID " + targetId + " - SUCCESS" :
                                "Used item ID " + itemId + " on item ID " + targetId + " - FAILED";
                        } catch (NumberFormatException e) {
                            Logger.log("Python->Java: Invalid item or target ID for item action");
                            taskManager.setCurrentStep("Idle - Waiting for commands");
                            return "Perform item action - ERROR: Invalid item or target ID";
                        }
                    } else {
                        success = Inventory.combine(item, target);
                        resultMessage = success ?
                            "Used '" + item + "' on '" + target + "' - SUCCESS" :
                            "Used '" + item + "' on '" + target + "' - FAILED";
                    }
                } else {
                    // Target is a game object - use item on object
                    Logger.log("Python->Java: Using item '" + item + "' on game object '" + target + "'");
                    
                    var gameObject = GameObjects.closest(target);
                    if (gameObject == null) {
                        Logger.log("Python->Java: Game object '" + target + "' not found");
                        taskManager.setCurrentStep("Idle - Waiting for commands");
                        return "Perform item action - FAILED (game object '" + target + "' not found)";
                    }
                    
                    if (useItemIds) {
                        try {
                            int itemId = Integer.parseInt(item);
                            var inventoryItem = Inventory.get(itemId);
                            if (inventoryItem != null) {
                                success = inventoryItem.useOn(gameObject);
                                resultMessage = success ?
                                    "Used item ID " + itemId + " on '" + target + "' - SUCCESS" :
                                    "Used item ID " + itemId + " on '" + target + "' - FAILED";
                            }
                        } catch (NumberFormatException e) {
                            Logger.log("Python->Java: Invalid item ID for object interaction");
                            taskManager.setCurrentStep("Idle - Waiting for commands");
                            return "Perform item action - ERROR: Invalid item ID";
                        }
                    } else {
                        var inventoryItem = Inventory.get(item);
                        if (inventoryItem != null) {
                            success = inventoryItem.useOn(gameObject);
                            resultMessage = success ?
                                "Used '" + item + "' on '" + target + "' - SUCCESS" :
                                "Used '" + item + "' on '" + target + "' - FAILED";
                        }
                    }
                }
            }
            
            if (success) {
                Logger.log("Python->Java: " + resultMessage);
                taskManager.setCurrentStep("Idle - Waiting for commands");
                return "Perform item action - " + resultMessage;
            } else {
                Logger.log("Python->Java: " + resultMessage);
                taskManager.setCurrentStep("Idle - Waiting for commands");
                return "Perform item action - " + resultMessage;
            }
            
        } catch (Exception e) {
            Logger.log("Python->Java: Exception in performItemAction: " + e.getMessage());
            e.printStackTrace();
            taskManager.setCurrentStep("Idle - Waiting for commands");
            return "Perform item action - ERROR: " + e.getMessage();
        }
    }
    
    /**
     * Performs specific item actions using the interact() method for all actions
     * This method handles different action types but uses interact() for consistency
     */
    private boolean performSpecificItemAction(String action, Object itemIdentifier, boolean useItemId) {
        try {
            var inventoryItem = useItemId ? 
                Inventory.get((Integer) itemIdentifier) : 
                Inventory.get((String) itemIdentifier);
            
            if (inventoryItem == null) {
                Logger.log("Python->Java: Item not found in inventory: " + itemIdentifier);
                return false;
            }
            
            // Special handling for Drop action to use the dedicated Inventory.drop() method
            if ("Drop".equalsIgnoreCase(action)) {
                Logger.log("Python->Java: Using Inventory.drop() method for item: " + itemIdentifier);
                if (useItemId) {
                    return Inventory.drop((Integer) itemIdentifier);
                } else {
                    return Inventory.drop((String) itemIdentifier);
                }
            } else {
                // Use interact() method for all other actions (Eat, Drink, Wield, etc.)
                Logger.log("Python->Java: Using interact('" + action + "') method for item: " + itemIdentifier);
                return inventoryItem.interact(action);
            }
            
        } catch (Exception e) {
            Logger.log("Python->Java: Exception in performSpecificItemAction: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
