import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.input.Mouse;
import java.awt.*;

@ScriptManifest(name = "DreamBot Named Pipe Shim", description = "Named pipe receiver for Python-to-Java method calls",
        author = "DreamBotShim", version = 1.0, category = Category.UTILITY, image = "")

/**
 * DreamBot Script - Named Pipe Receiver for dynamic Java method calls from Python
 * Coordinated refactored version using separate component classes
 */
public class Main extends AbstractScript {
    
    // Component instances
    private TaskManager taskManager;
    private DreamBotAPIWrapper apiWrapper;
    private UIRenderer uiRenderer;
    
    @Override
    public void onStart() {
        Logger.log("DreamBot Named Pipe Shim started!");
        Logger.log("Initializing components...");
        
        // Initialize component instances
        taskManager = new TaskManager();
        apiWrapper = new DreamBotAPIWrapper(taskManager);
        
        // Initialize the pipe manager with API wrapper
        if (!PipeManager.initialize(apiWrapper)) {
            Logger.log("ERROR: Failed to initialize pipe manager!");
            PipeManager.stop();
            stop(); // Call the AbstractScript stop method
            return;
        }
        
        // Initialize UI renderer after PipeManager is ready
        uiRenderer = new UIRenderer(taskManager);
        
        Logger.log("All components initialized successfully");
        Logger.log("Python scripts can now send method calls to: " + PipeManager.getPipeName());
        Logger.log("Available methods: helloWorld, greet, calculate, walkToLocation, clickObject, etc.");
    }
    
    @Override
    public int onLoop() {
        // The named pipe receiver runs in its own background thread
        // This loop just keeps the script alive and can be used for additional logic
        
        // Handle mouse clicks for the skip button
        handleMouseClicks();
        
        // You can add any periodic tasks here
        // For now, just sleep and let the named pipe receiver handle calls
        return 100; // Shorter sleep for more responsive UI
    }
    
    @Override
    public void onExit() {
        Logger.log("DreamBot Named Pipe Shim stopping...");
        Logger.log("Shutting down pipe manager...");
        
        PipeManager.stop(); // Stop the pipe manager
        
        Logger.log("Pipe manager stopped");
        Logger.log("DreamBot Named Pipe Shim exited");
    }
    
    @Override
    public void onPaint(Graphics2D graphics) {
        if (uiRenderer != null) {
            uiRenderer.render(graphics);
        }
    }
    
    // Handle mouse clicks for skip button
    private void handleMouseClicks() {
        if (uiRenderer != null && apiWrapper != null) {
            Rectangle skipButtonRect = uiRenderer.getSkipButtonRect();
            // Check if mouse is held down and positioned over the skip button
            if (Mouse.isMouseHeldDown() && skipButtonRect.contains(Mouse.getPosition())) {
                Logger.log("Skip button clicked - requesting skip");
                apiWrapper.requestSkip();
                // Small delay to prevent multiple rapid clicks
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    
}
