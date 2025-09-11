import org.dreambot.api.utilities.Logger;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Manages task tracking and step management for the UI
 */
public class TaskManager {
    
    private String currentStep = "Idle - Waiting for commands";
    private final List<String> upcomingSteps = Collections.synchronizedList(new ArrayList<>());
    private long stepStartTime = System.currentTimeMillis();
    
    /**
     * Set the current step being executed
     */
    public void setCurrentStep(String step) {
        currentStep = step;
        stepStartTime = System.currentTimeMillis();
        Logger.log("Python->Java: Current step: " + step);
    }
    
    /**
     * Get the current step
     */
    public String getCurrentStep() {
        return currentStep;
    }
    
    /**
     * Get the time when the current step started
     */
    public long getStepStartTime() {
        return stepStartTime;
    }
    
    /**
     * Add an upcoming step to the queue
     */
    public void addUpcomingStep(String step) {
        synchronized (upcomingSteps) {
            upcomingSteps.add(step);
        }
        Logger.log("Python->Java: Added upcoming step: " + step);
    }
    
    /**
     * Remove and return the next upcoming step
     */
    public String getNextStep() {
        synchronized (upcomingSteps) {
            if (!upcomingSteps.isEmpty()) {
                return upcomingSteps.remove(0);
            }
        }
        return null;
    }
    
    /**
     * Get a copy of all upcoming steps
     */
    public List<String> getUpcomingSteps() {
        synchronized (upcomingSteps) {
            return new ArrayList<>(upcomingSteps);
        }
    }
    
    /**
     * Clear all upcoming steps
     */
    public void clearUpcomingSteps() {
        synchronized (upcomingSteps) {
            upcomingSteps.clear();
        }
        Logger.log("Python->Java: Cleared all upcoming steps");
    }
    
    /**
     * Get the number of upcoming steps
     */
    public int getUpcomingStepsCount() {
        synchronized (upcomingSteps) {
            return upcomingSteps.size();
        }
    }
    
    /**
     * Check if there are any upcoming steps
     */
    public boolean hasUpcomingSteps() {
        synchronized (upcomingSteps) {
            return !upcomingSteps.isEmpty();
        }
    }
    
    /**
     * Get runtime in milliseconds since the current step started
     */
    public long getCurrentStepRuntime() {
        return System.currentTimeMillis() - stepStartTime;
    }
    
    /**
     * Get formatted runtime string for current step
     */
    public String getCurrentStepRuntimeFormatted() {
        long elapsed = getCurrentStepRuntime() / 1000;
        return String.format("%02d:%02d", elapsed / 60, elapsed % 60);
    }
}
