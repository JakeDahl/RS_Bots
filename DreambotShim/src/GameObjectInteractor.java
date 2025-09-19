import org.dreambot.api.utilities.Logger;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.wrappers.interactive.GameObject;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Handles game object interactions and object discovery
 */
public class GameObjectInteractor {
    
    private final TaskManager taskManager;
    
    // Common action-to-object mappings for intelligent object discovery
    private static final Map<String, String[]> ACTION_OBJECT_MAP = new HashMap<>();
    
    static {
        // Movement actions
        ACTION_OBJECT_MAP.put("go up", new String[]{"Ladder", "Stairs", "Staircase", "Steps", "Rope"});
        ACTION_OBJECT_MAP.put("go down", new String[]{"Ladder", "Stairs", "Staircase", "Steps", "Trapdoor", "Hole"});
        ACTION_OBJECT_MAP.put("climb up", new String[]{"Ladder", "Stairs", "Staircase", "Steps", "Rope", "Tree"});
        ACTION_OBJECT_MAP.put("climb down", new String[]{"Ladder", "Stairs", "Staircase", "Steps", "Trapdoor", "Hole"});
        
        // Doors and entrances
        ACTION_OBJECT_MAP.put("enter", new String[]{"Door", "Gate", "Portal", "Entrance", "Doorway"});
        ACTION_OBJECT_MAP.put("exit", new String[]{"Door", "Gate", "Portal", "Exit", "Doorway"});
        ACTION_OBJECT_MAP.put("open", new String[]{"Door", "Gate", "Chest", "Box", "Container"});
        
        // Interactive objects
        ACTION_OBJECT_MAP.put("mine", new String[]{"Rock", "Ore", "Mining site", "Tin ore", "Iron ore", "Coal"});
        ACTION_OBJECT_MAP.put("chop", new String[]{"Tree", "Willow", "Oak", "Yew", "Magic tree"});
        ACTION_OBJECT_MAP.put("fish", new String[]{"Fishing spot", "Net", "Rod"});
        ACTION_OBJECT_MAP.put("cook", new String[]{"Fire", "Range", "Cooking range", "Oven"});
        
        // Banking and trading
        ACTION_OBJECT_MAP.put("bank", new String[]{"Bank", "Bank booth", "Bank chest", "Banker"});
        ACTION_OBJECT_MAP.put("shop", new String[]{"Shop", "Store", "Counter", "Merchant"});
        
        // Altar and prayer
        ACTION_OBJECT_MAP.put("pray", new String[]{"Altar", "Prayer altar", "Shrine"});
        ACTION_OBJECT_MAP.put("recharge", new String[]{"Altar", "Prayer altar", "Shrine", "Obelisk"});
    }
    
    private final Random random = new Random();
    
    public GameObjectInteractor(TaskManager taskManager) {
        this.taskManager = taskManager;
    }
    
    /**
     * DreamBot-specific object interaction
     */
    public String clickObject(String objectName) {
        try {
            Logger.log("Python->Java: Clicking object: " + objectName);
            taskManager.setCurrentStep("Clicking object: " + objectName);
            
            var gameObject = GameObjects.closest(objectName);
            if (gameObject != null && gameObject.interact()) {
                Logger.log("Python->Java: Successfully clicked object: " + objectName);
                
                // Add delay after successful interaction (1000-2000ms)
                int delayMs = 1000 + random.nextInt(1001); // 1000 + 0-1000 = 1000-2000ms
                Logger.log("Python->Java: Adding post-interaction delay of " + delayMs + "ms");
                taskManager.setCurrentStep("Waiting after interaction (" + delayMs + "ms)");
                
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Logger.log("Python->Java: Delay interrupted: " + ie.getMessage());
                    Thread.currentThread().interrupt(); // Restore interrupted status
                }
                
                taskManager.setCurrentStep("Idle - Waiting for commands");
                return "Clicked object: " + objectName + " - SUCCESS (delayed " + delayMs + "ms)";
            } else {
                Logger.log("Python->Java: Failed to click object: " + objectName + " (not found or interaction failed)");
                taskManager.setCurrentStep("Idle - Waiting for commands");
                return "Clicked object: " + objectName + " - FAILED (not found or interaction failed)";
            }
        } catch (Exception e) {
            Logger.log("Python->Java: Exception in clickObject: " + e.getMessage());
            e.printStackTrace();
            taskManager.setCurrentStep("Idle - Waiting for commands");
            return "Clicked object: " + objectName + " - ERROR: " + e.getMessage();
        }
    }
    
    /**
     * List all nearby game objects within a reasonable range
     * Returns a formatted string with object names, IDs, and distances
     */
    public String listNearbyGameObjects() {
        try {
            Logger.log("Python->Java: Listing nearby game objects");
            taskManager.setCurrentStep("Scanning nearby objects");
            
            List<GameObject> allObjects = GameObjects.all();
            List<String> objectInfo = new ArrayList<>();
            
            if (allObjects.isEmpty()) {
                taskManager.setCurrentStep("Idle - Waiting for commands");
                return "No game objects found nearby";
            }
            
            // Sort objects by distance and limit to reasonable range
            allObjects.stream()
                .filter(obj -> obj != null && obj.getTile() != null)
                .filter(obj -> obj.distance() <= 10) // Only objects within 10 tiles
                .sorted((a, b) -> Double.compare(a.distance(), b.distance()))
                .limit(20) // Limit to first 20 objects to avoid spam
                .forEach(obj -> {
                    String name = obj.getName();
                    if (name == null || name.equals("null")) {
                        name = "Unknown";
                    }
                    objectInfo.add(String.format("%s (ID: %d, Distance: %.1f)", 
                        name, obj.getID(), obj.distance()));
                });
            
            taskManager.setCurrentStep("Idle - Waiting for commands");
            
            if (objectInfo.isEmpty()) {
                return "No named game objects found within range";
            }
            
            return "Nearby Game Objects:\n" + String.join("\n", objectInfo);
            
        } catch (Exception e) {
            Logger.log("Python->Java: Exception in listNearbyGameObjects: " + e.getMessage());
            e.printStackTrace();
            taskManager.setCurrentStep("Idle - Waiting for commands");
            return "Error listing objects: " + e.getMessage();
        }
    }
    
    /**
     * Get game objects suitable for a specific action (e.g., "go up", "climb down", "enter")
     * Uses the ACTION_OBJECT_MAP to suggest relevant objects
     */
    public String getGameObjectsForAction(String action) {
        try {
            Logger.log("Python->Java: Finding objects for action: " + action);
            taskManager.setCurrentStep("Searching objects for action: " + action);
            
            String actionLower = action.toLowerCase().trim();
            String[] expectedObjects = ACTION_OBJECT_MAP.get(actionLower);
            
            List<String> foundObjects = new ArrayList<>();
            List<GameObject> allObjects = GameObjects.all();
            
            if (expectedObjects != null) {
                // Search for expected object types
                for (String expectedName : expectedObjects) {
                    List<GameObject> matchingObjects = allObjects.stream()
                        .filter(obj -> obj != null && obj.getName() != null)
                        .filter(obj -> obj.getName().toLowerCase().contains(expectedName.toLowerCase()))
                        .filter(obj -> obj.distance() <= 15)
                        .sorted((a, b) -> Double.compare(a.distance(), b.distance()))
                        .limit(5)
                        .toList();
                    
                    for (GameObject obj : matchingObjects) {
                        foundObjects.add(String.format("%s (Distance: %.1f)", obj.getName(), obj.distance()));
                    }
                }
            }
            
            // Also do a general search based on the action keyword
            List<GameObject> keywordObjects = allObjects.stream()
                .filter(obj -> obj != null && obj.getName() != null)
                .filter(obj -> obj.getName().toLowerCase().contains(actionLower) || 
                              actionLower.contains(obj.getName().toLowerCase()))
                .filter(obj -> obj.distance() <= 15)
                .sorted((a, b) -> Double.compare(a.distance(), b.distance()))
                .limit(5)
                .toList();
            
            for (GameObject obj : keywordObjects) {
                String objInfo = String.format("%s (Distance: %.1f)", obj.getName(), obj.distance());
                if (!foundObjects.contains(objInfo)) {
                    foundObjects.add(objInfo);
                }
            }
            
            taskManager.setCurrentStep("Idle - Waiting for commands");
            
            if (foundObjects.isEmpty()) {
                String suggestion = expectedObjects != null ? 
                    "Expected objects: " + String.join(", ", expectedObjects) : 
                    "No specific objects mapped for this action";
                return String.format("No objects found for action '%s'. %s", action, suggestion);
            }
            
            return String.format("Objects for action '%s':\n%s", action, String.join("\n", foundObjects));
            
        } catch (Exception e) {
            Logger.log("Python->Java: Exception in getGameObjectsForAction: " + e.getMessage());
            e.printStackTrace();
            taskManager.setCurrentStep("Idle - Waiting for commands");
            return "Error finding objects for action: " + e.getMessage();
        }
    }
    
    /**
     * Search for game objects by name or partial name match
     */
    public String searchGameObjects(String searchTerm) {
        try {
            Logger.log("Python->Java: Searching for objects: " + searchTerm);
            taskManager.setCurrentStep("Searching for: " + searchTerm);
            
            String searchLower = searchTerm.toLowerCase().trim();
            List<GameObject> allObjects = GameObjects.all();
            
            List<String> matchingObjects = new ArrayList<>();
            
            allObjects.stream()
                .filter(obj -> obj != null && obj.getName() != null)
                .filter(obj -> obj.getName().toLowerCase().contains(searchLower))
                .filter(obj -> obj.distance() <= 15)
                .sorted((a, b) -> Double.compare(a.distance(), b.distance()))
                .limit(10)
                .forEach(obj -> {
                    matchingObjects.add(String.format("%s (ID: %d, Distance: %.1f)", 
                        obj.getName(), obj.getID(), obj.distance()));
                });
            
            taskManager.setCurrentStep("Idle - Waiting for commands");
            
            if (matchingObjects.isEmpty()) {
                return String.format("No objects found matching '%s'", searchTerm);
            }
            
            return String.format("Objects matching '%s':\n%s", searchTerm, String.join("\n", matchingObjects));
            
        } catch (Exception e) {
            Logger.log("Python->Java: Exception in searchGameObjects: " + e.getMessage());
            e.printStackTrace();
            taskManager.setCurrentStep("Idle - Waiting for commands");
            return "Error searching objects: " + e.getMessage();
        }
    }
    
    /**
     * Get detailed information about a specific game object
     */
    public String getObjectDetails(String objectName) {
        try {
            Logger.log("Python->Java: Getting details for object: " + objectName);
            taskManager.setCurrentStep("Analyzing object: " + objectName);
            
            GameObject gameObject = GameObjects.closest(objectName);
            
            if (gameObject == null) {
                taskManager.setCurrentStep("Idle - Waiting for commands");
                return String.format("Object '%s' not found", objectName);
            }
            
            StringBuilder details = new StringBuilder();
            details.append(String.format("Object Details for '%s':\n", objectName));
            details.append(String.format("- Name: %s\n", gameObject.getName()));
            details.append(String.format("- ID: %d\n", gameObject.getID()));
            details.append(String.format("- Distance: %.1f tiles\n", gameObject.distance()));
            details.append(String.format("- Position: (%d, %d, %d)\n", 
                gameObject.getTile().getX(), gameObject.getTile().getY(), gameObject.getTile().getZ()));
            details.append(String.format("- Can Interact: %s\n", gameObject.canReach() ? "Yes" : "No"));
            
            // Try to get available actions
            try {
                var actions = gameObject.getActions();
                if (actions != null && actions.length > 0) {
                    details.append("- Available Actions: ");
                    List<String> actionList = new ArrayList<>();
                    for (String action : actions) {
                        if (action != null && !action.isEmpty()) {
                            actionList.add(action);
                        }
                    }
                    details.append(String.join(", ", actionList));
                    details.append("\n");
                }
            } catch (Exception ex) {
                details.append("- Available Actions: Unable to retrieve\n");
            }
            
            taskManager.setCurrentStep("Idle - Waiting for commands");
            return details.toString();
            
        } catch (Exception e) {
            Logger.log("Python->Java: Exception in getObjectDetails: " + e.getMessage());
            e.printStackTrace();
            taskManager.setCurrentStep("Idle - Waiting for commands");
            return "Error getting object details: " + e.getMessage();
        }
    }
}
