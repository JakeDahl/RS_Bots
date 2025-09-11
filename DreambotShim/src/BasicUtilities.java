import org.dreambot.api.utilities.Logger;

/**
 * Basic utility methods that don't require DreamBot API integration
 */
public class BasicUtilities {
    
    /**
     * Simple hello world method
     */
    public String helloWorld() {
        String message = "Hello World from Java!";
        System.out.println(message);
        return message;
    }
    
    /**
     * Greet a person by name
     */
    public String greet(String name) {
        String message = "Hello, " + name + " from Java!";
        System.out.println(message);
        return message;
    }
    
    /**
     * Perform basic calculations
     */
    public double calculate(double a, double b, String operation) {
        double result = 0;
        
        switch (operation.toLowerCase()) {
            case "add":
                result = a + b;
                break;
            case "subtract":
                result = a - b;
                break;
            case "multiply":
                result = a * b;
                break;
            case "divide":
                if (b != 0) {
                    result = a / b;
                } else {
                    System.err.println("Error: Division by zero");
                    return Double.NaN;
                }
                break;
            default:
                System.err.println("Unknown operation: " + operation);
                return Double.NaN;
        }
        
        String message = "Calculation: " + a + " " + operation + " " + b + " = " + result;
        System.out.println(message);
        return result;
    }
    
    /**
     * Process data with mixed argument types
     */
    public String processData(String filename, int maxLines, boolean verbose) {
        String message = "Processing file: " + filename + ", max_lines: " + maxLines + ", verbose: " + verbose;
        System.out.println(message);
        
        if (verbose) {
            System.out.println("  - Opening file: " + filename);
            System.out.println("  - Reading up to " + maxLines + " lines");
            System.out.println("  - Processing complete");
        }
        
        return "Processed " + filename + " successfully from Java";
    }
    
    /**
     * Example DreamBot action handler
     */
    public String runDreambotAction(String action, String... params) {
        System.out.println("DreamBot Action: " + action);
        System.out.println("Parameters: " + String.join(", ", params));
        
        switch (action.toLowerCase()) {
            case "walk":
                String x = params.length > 0 ? params[0] : "0";
                String y = params.length > 1 ? params[1] : "0";
                return "Walking to coordinates (" + x + ", " + y + ") via Java";
                
            case "interact":
                String objectName = params.length > 0 ? params[0] : "unknown";
                String actionType = params.length > 1 ? params[1] : "click";
                return "Interacting with " + objectName + " using " + actionType + " via Java";
                
            case "check_inventory":
                return "Checking inventory contents via Java...";
                
            default:
                return "Unknown DreamBot action: " + action;
        }
    }
    
    /**
     * Log a message with specified level
     */
    public String logMessage(String level, String message) {
        String timestamp = java.time.LocalDateTime.now().toString();
        String logEntry = "[" + timestamp + "] " + level.toUpperCase() + ": " + message;
        System.out.println(logEntry);
        
        return "Logged message at level " + level + " via Java";
    }
}
