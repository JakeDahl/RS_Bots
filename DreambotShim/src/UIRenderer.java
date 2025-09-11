import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import java.awt.*;
import java.util.List;

/**
 * Handles UI rendering for the DreamBot script onPaint method
 */
public class UIRenderer {
    
    private static final Rectangle skipButtonRect = new Rectangle(250, 135, 50, 20);
    private TaskManager taskManager;
    private long scriptStartTime;
    
    public UIRenderer(TaskManager taskManager) {
        this.taskManager = taskManager;
        this.scriptStartTime = System.currentTimeMillis();
    }
    
    /**
     * Render the enhanced UI similar to the original onPaint method
     */
    public void render(Graphics2D graphics) {
        // Set up graphics properties for better rendering
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        // Enhanced main background panel with gradient effect
        int panelWidth = 320;
        int panelHeight = 160;
        
        // Gradient background
        GradientPaint gradient = new GradientPaint(10, 10, new Color(0, 0, 0, 180), 
                                                   10, panelHeight, new Color(20, 20, 40, 160));
        graphics.setPaint(gradient);
        graphics.fillRoundRect(10, 10, panelWidth, panelHeight, 15, 15);
        
        // Enhanced border with glow effect
        graphics.setColor(new Color(100, 200, 255, 200));
        graphics.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.drawRoundRect(10, 10, panelWidth, panelHeight, 15, 15);
        
        // Inner border for depth
        graphics.setColor(new Color(255, 255, 255, 100));
        graphics.setStroke(new BasicStroke(1));
        graphics.drawRoundRect(12, 12, panelWidth-4, panelHeight-4, 12, 12);
        
        // Task Management Section with enhanced styling
        int yOffset = 35;
        graphics.setColor(new Color(255, 200, 0));
        graphics.setFont(new Font("Arial", Font.BOLD, 13));
        graphics.drawString("Task Management:", 20, yOffset);
        
        // Current task with progress bar background
        yOffset += 20;
        String timeStr = taskManager.getCurrentStepRuntimeFormatted();
        String currentStepText = taskManager.getCurrentStep() + " (" + timeStr + ")";
        if (currentStepText.length() > 45) {
            currentStepText = currentStepText.substring(0, 42) + "...";
        }
        
        // Current task background
        graphics.setColor(new Color(50, 50, 100, 100));
        graphics.fillRoundRect(18, yOffset-12, panelWidth-36, 16, 6, 6);
        
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("Arial", Font.PLAIN, 11));
        graphics.drawString("Current: " + currentStepText, 22, yOffset);
        
        // Upcoming steps with improved formatting
        yOffset += 22;
        graphics.setColor(new Color(200, 200, 200));
        graphics.setFont(new Font("Arial", Font.BOLD, 10));
        graphics.drawString("Upcoming Steps:", 20, yOffset);
        
        graphics.setFont(new Font("Arial", Font.PLAIN, 10));
        List<String> upcomingSteps = taskManager.getUpcomingSteps();
        int maxSteps = Math.min(upcomingSteps.size(), 3);
        for (int i = 0; i < maxSteps; i++) {
            yOffset += 14;
            String step = upcomingSteps.get(i);
            if (step.length() > 42) {
                step = step.substring(0, 39) + "...";
            }
            
            // Step number background
            graphics.setColor(new Color(100, 100, 150, 80));
            graphics.fillOval(22, yOffset-10, 12, 12);
            
            graphics.setColor(new Color(150, 150, 255));
            graphics.drawString(String.valueOf(i + 1), 26, yOffset-2);
            
            graphics.setColor(new Color(220, 220, 220));
            graphics.drawString(step, 40, yOffset);
        }
        
        if (upcomingSteps.isEmpty()) {
            yOffset += 14;
            graphics.setColor(new Color(150, 150, 150));
            graphics.drawString("No upcoming steps", 25, yOffset);
        }
        
        // Enhanced skip button with better styling
        graphics.setColor(new Color(220, 60, 60, 200));
        graphics.fillRoundRect(skipButtonRect.x, skipButtonRect.y, skipButtonRect.width, skipButtonRect.height, 8, 8);
        
        // Skip button border
        graphics.setColor(new Color(255, 100, 100));
        graphics.setStroke(new BasicStroke(2));
        graphics.drawRoundRect(skipButtonRect.x, skipButtonRect.y, skipButtonRect.width, skipButtonRect.height, 8, 8);
        
        // Skip button text with shadow
        graphics.setColor(new Color(0, 0, 0, 150));
        graphics.setFont(new Font("Arial", Font.BOLD, 10));
        graphics.drawString("SKIP", skipButtonRect.x + 13, skipButtonRect.y + 14); // Shadow
        
        graphics.setColor(Color.WHITE);
        graphics.drawString("SKIP", skipButtonRect.x + 12, skipButtonRect.y + 13); // Main text
    }
    
    /**
     * Get the skip button rectangle for mouse interaction handling
     */
    public Rectangle getSkipButtonRect() {
        return skipButtonRect;
    }
    
    /**
     * Reset the script start time (call this when the script starts)
     */
    public void resetStartTime() {
        this.scriptStartTime = System.currentTimeMillis();
    }
}
