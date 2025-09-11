import org.dreambot.api.utilities.Logger;
import org.dreambot.api.methods.interactive.GameObjects;

/**
 * Handles game object interactions
 */
public class GameObjectInteractor {
    
    private final TaskManager taskManager;
    
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
                taskManager.setCurrentStep("Idle - Waiting for commands");
                return "Clicked object: " + objectName + " - SUCCESS";
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
}
