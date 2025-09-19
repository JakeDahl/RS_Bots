import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Facade wrapper class for DreamBot API methods that can be called from Python.
 * Delegates method calls to appropriate handler classes for better organization.
 */
public class DreamBotAPIWrapper {
    
    private static final AtomicBoolean skipRequested = new AtomicBoolean(false);
    private TaskManager taskManager;
    
    // Handler classes
    private BasicUtilities basicUtilities;
    private PlayerStateManager playerStateManager;
    private MovementHandler movementHandler;
    private BankingManager bankingManager;
    private InventoryManager inventoryManager;
    private DialogueHandler dialogueHandler;
    private GameObjectInteractor gameObjectInteractor;
    private GroundItemHandler groundItemHandler;
    
    public DreamBotAPIWrapper(TaskManager taskManager) {
        this.taskManager = taskManager;
        
        // Initialize handler classes
        this.basicUtilities = new BasicUtilities();
        this.playerStateManager = new PlayerStateManager();
        this.movementHandler = new MovementHandler(taskManager);
        this.bankingManager = new BankingManager(taskManager);
        this.inventoryManager = new InventoryManager(taskManager);
        this.dialogueHandler = new DialogueHandler(taskManager, skipRequested);
        this.gameObjectInteractor = new GameObjectInteractor(taskManager);
        this.groundItemHandler = new GroundItemHandler(taskManager);
    }
    
    // ===== Basic Utility Methods (delegated to BasicUtilities) =====
    
    /**
     * Simple hello world method
     */
    public String helloWorld() {
        return basicUtilities.helloWorld();
    }
    
    /**
     * Greet a person by name
     */
    public String greet(String name) {
        return basicUtilities.greet(name);
    }
    
    /**
     * Perform basic calculations
     */
    public double calculate(double a, double b, String operation) {
        return basicUtilities.calculate(a, b, operation);
    }
    
    /**
     * Process data with mixed argument types
     */
    public String processData(String filename, int maxLines, boolean verbose) {
        return basicUtilities.processData(filename, maxLines, verbose);
    }
    
    /**
     * Example DreamBot action handler
     */
    public String runDreambotAction(String action, String... params) {
        return basicUtilities.runDreambotAction(action, params);
    }
    
    /**
     * Log a message with specified level
     */
    public String logMessage(String level, String message) {
        return basicUtilities.logMessage(level, message);
    }
    
    // ===== Player State Methods (delegated to PlayerStateManager) =====
    
    /**
     * Get player's current location as formatted string
     */
    public String getPlayerLocation() {
        return playerStateManager.getPlayerLocation();
    }
    
    /**
     * Get player's current X coordinate
     */
    public int getPlayerX() {
        return playerStateManager.getPlayerX();
    }
    
    /**
     * Get player's current Y coordinate
     */
    public int getPlayerY() {
        return playerStateManager.getPlayerY();
    }
    
    /**
     * Check if player is moving
     */
    public boolean isPlayerMoving() {
        return playerStateManager.isPlayerMoving();
    }
    
    /**
     * Get player's skill level
     */
    public int getSkillLevel(String skillName) {
        return playerStateManager.getSkillLevel(skillName);
    }
    
    /**
     * Check if player is animating (doing an action)
     */
    public boolean isPlayerAnimating() {
        return playerStateManager.isPlayerAnimating();
    }
    
    // ===== Movement Methods (delegated to MovementHandler) =====
    
    /**
     * DreamBot-specific walking method - Enhanced with unlimited attempts until arrival and skip support
     * Supports 3D coordinates with z (plane) parameter
     */
    public String walkToLocation(int x, int y, int z) {
        return movementHandler.walkToLocation(x, y, z);
    }
    
    /**
     * Request to skip the current operation
     */
    public String requestSkip() {
        return movementHandler.requestSkip();
    }
    
    // ===== Banking Methods (delegated to BankingManager) =====
    
    /**
     * Check if bank is actually open using DreamBot API
     */
    public boolean bankIsOpen() {
        return bankingManager.bankIsOpen();
    }
    
    /**
     * Open bank using DreamBot API - Enhanced version with skip support
     */
    public String openBank() {
        return bankingManager.openBank();
    }
    
    /**
     * Close bank using DreamBot API
     */
    public String closeBank() {
        return bankingManager.closeBank();
    }
    
    /**
     * Deposit all items except specified items
     */
    public String depositAllExcept(String... itemsToKeep) {
        return bankingManager.depositAllExcept(itemsToKeep);
    }
    
    /**
     * Deposit specific item with count
     */
    public String depositItem(String itemName, int count) {
        return bankingManager.depositItem(itemName, count);
    }
    
    /**
     * Withdraw specific item with count
     */
    public String withdrawItem(String itemName, int count) {
        return bankingManager.withdrawItem(itemName, count);
    }
    
    /**
     * Check if bank contains specific item
     */
    public boolean bankContains(String itemName) {
        return bankingManager.bankContains(itemName);
    }
    
    /**
     * Get bank item count
     */
    public int getBankItemCount(String itemName) {
        return bankingManager.getBankItemCount(itemName);
    }
    
    // ===== Inventory Methods (delegated to InventoryManager) =====
    
    /**
     * Get real inventory count from DreamBot API
     */
    public int getInventoryCount() {
        return inventoryManager.getInventoryCount();
    }
    
    /**
     * Use one item on another item in inventory (item combination/crafting)
     */
    public String useItemOnItem(String primaryItem, String secondaryItem, boolean useItemIds) {
        return inventoryManager.useItemOnItem(primaryItem, secondaryItem, useItemIds);
    }
    
    /**
     * Perform a custom action on an item, or use an item on a game object
     * Examples: "Eat" on "Lobster", use "Bread" on "Oven", "Drop" an item, etc.
     */
    public String performItemAction(String action, String item, String target, boolean useItemIds, String targetType) {
        return inventoryManager.performItemAction(action, item, target, useItemIds, targetType);
    }
    
    /**
     * Check if inventory contains a specific item and return count
     * Returns -1 if item not found, 0+ for actual count
     */
    public int checkInventoryForItem(String itemName, boolean useItemId) {
        return inventoryManager.checkInventoryForItem(itemName, useItemId);
    }
    
    /**
     * Check if inventory contains a specific item (boolean result)
     * Simple true/false check without count
     */
    public boolean inventoryContainsItem(String itemName, boolean useItemId) {
        return inventoryManager.inventoryContainsItem(itemName, useItemId);
    }
    
    // ===== Dialogue Methods (delegated to DialogueHandler) =====
    
    /**
     * Handle NPC dialogue interactions, waiting for all dialogue to complete.
     * Based on Tutorial Island DialogueHandler pattern.
     */
    public String handleNPCDialogue(String npcName, int maxWaitTime) {
        return dialogueHandler.handleNPCDialogue(npcName, maxWaitTime);
    }
    
    // ===== Game Object Methods (delegated to GameObjectInteractor) =====
    
    /**
     * DreamBot-specific object interaction
     */
    public String clickObject(String objectName) {
        return gameObjectInteractor.clickObject(objectName);
    }
    
    /**
     * List all nearby game objects within range
     */
    public String listNearbyGameObjects() {
        return gameObjectInteractor.listNearbyGameObjects();
    }
    
    /**
     * Get game objects suitable for a specific action (e.g., "go up", "climb down", "enter")
     */
    public String getGameObjectsForAction(String action) {
        return gameObjectInteractor.getGameObjectsForAction(action);
    }
    
    /**
     * Search for game objects by name or partial name match
     */
    public String searchGameObjects(String searchTerm) {
        return gameObjectInteractor.searchGameObjects(searchTerm);
    }
    
    /**
     * Get detailed information about a specific game object
     */
    public String getObjectDetails(String objectName) {
        return gameObjectInteractor.getObjectDetails(objectName);
    }
    
    // ===== Ground Item Methods (delegated to GroundItemHandler) =====
    
    /**
     * Pick up a ground item by name
     */
    public String pickupGroundItem(String itemName) {
        return groundItemHandler.pickupGroundItem(itemName);
    }
    
    /**
     * Pick up a ground item by ID
     */
    public String pickupGroundItemById(int itemId) {
        return groundItemHandler.pickupGroundItemById(itemId);
    }
    
    /**
     * Get information about nearby ground items
     */
    public String getNearbyGroundItems() {
        return groundItemHandler.getNearbyGroundItems();
    }
    
    /**
     * Check if a specific ground item exists nearby
     */
    public boolean groundItemExists(String itemName) {
        return groundItemHandler.groundItemExists(itemName);
    }
    
    /**
     * Get the distance to the closest ground item by name
     */
    public double getDistanceToGroundItem(String itemName) {
        return groundItemHandler.getDistanceToGroundItem(itemName);
    }
    
    // ===== Task Management Methods =====
    
    /**
     * Add an upcoming step to the task queue
     */
    public String addUpcomingStep(String step) {
        try {
            taskManager.addUpcomingStep(step);
            return "Added upcoming step: " + step;
        } catch (Exception e) {
            return "Error adding upcoming step: " + e.getMessage();
        }
    }
    
    /**
     * Clear all upcoming steps
     */
    public String clearUpcomingSteps() {
        try {
            taskManager.clearUpcomingSteps();
            return "Cleared all upcoming steps";
        } catch (Exception e) {
            return "Error clearing upcoming steps: " + e.getMessage();
        }
    }
    
    /**
     * Get the number of upcoming steps
     */
    public int getUpcomingStepsCount() {
        try {
            return taskManager.getUpcomingStepsCount();
        } catch (Exception e) {
            return -1;
        }
    }
    
    /**
     * Check if there are any upcoming steps
     */
    public boolean hasUpcomingSteps() {
        try {
            return taskManager.hasUpcomingSteps();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get the next upcoming step without removing it
     */
    public String peekNextStep() {
        try {
            if (taskManager.hasUpcomingSteps()) {
                String nextStep = taskManager.getUpcomingSteps().get(0);
                return nextStep;
            } else {
                return "No upcoming steps";
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * Remove and return the next upcoming step
     */
    public String getNextStep() {
        try {
            String nextStep = taskManager.getNextStep();
            if (nextStep != null) {
                return nextStep;
            } else {
                return "No next step available";
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
