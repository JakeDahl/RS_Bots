import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;

/**
 * Named Pipe Shim for dynamic Python method calls
 * Creates a named pipe to communicate with Python scripts
 */
public class NamedPipeShim {
    
    private static final String PIPE_NAME = "/tmp/dreambot_shim_pipe";
    private static final String PYTHON_SCRIPT_PATH = "python_handler.py";
    private static final Gson gson = new Gson();
    private static Process pythonProcess;
    private static PrintWriter pipeWriter;
    
    /**
     * Initialize the named pipe and start the Python handler
     */
    public static boolean initialize() {
        try {
            // Create named pipe if it doesn't exist
            createNamedPipe();
            
            // Start Python handler process
            startPythonHandler();
            
            // Open pipe for writing
            pipeWriter = new PrintWriter(new FileWriter(PIPE_NAME), true);
            
            System.out.println("NamedPipeShim initialized successfully");
            return true;
            
        } catch (Exception e) {
            System.err.println("Failed to initialize NamedPipeShim: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Create the named pipe using mkfifo command
     */
    private static void createNamedPipe() throws IOException, InterruptedException {
        if (!Files.exists(Paths.get(PIPE_NAME))) {
            Process mkfifoProcess = Runtime.getRuntime().exec("mkfifo " + PIPE_NAME);
            int exitCode = mkfifoProcess.waitFor();
            if (exitCode != 0) {
                throw new IOException("Failed to create named pipe with mkfifo");
            }
            System.out.println("Created named pipe: " + PIPE_NAME);
        } else {
            System.out.println("Named pipe already exists: " + PIPE_NAME);
        }
    }
    
    /**
     * Start the Python handler process
     */
    private static void startPythonHandler() throws IOException {
        String pythonScriptFullPath = System.getProperty("user.dir") + "/DreambotShim/src/" + PYTHON_SCRIPT_PATH;
        ProcessBuilder pb = new ProcessBuilder("python3", pythonScriptFullPath);
        pb.directory(new File(System.getProperty("user.dir") + "/DreambotShim/src/"));
        pythonProcess = pb.start();
        
        System.out.println("Started Python handler process");
    }
    
    /**
     * Call a Python method with no arguments
     */
    public static String callMethod(String methodName) {
        return callMethod(methodName, new Object[0]);
    }
    
    /**
     * Call a Python method with arguments
     */
    public static String callMethod(String methodName, Object... args) {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("method", methodName);
            
            JsonArray argsArray = new JsonArray();
            for (Object arg : args) {
                if (arg instanceof String) {
                    argsArray.add(new JsonPrimitive((String) arg));
                } else if (arg instanceof Number) {
                    argsArray.add(new JsonPrimitive((Number) arg));
                } else if (arg instanceof Boolean) {
                    argsArray.add(new JsonPrimitive((Boolean) arg));
                } else {
                    argsArray.add(new JsonPrimitive(arg.toString()));
                }
            }
            request.add("args", argsArray);
            
            String jsonRequest = gson.toJson(request);
            
            // Write to named pipe
            if (pipeWriter != null) {
                pipeWriter.println(jsonRequest);
                pipeWriter.flush();
                
                System.out.println("Sent request: " + jsonRequest);
                return "Request sent successfully";
            } else {
                return "Error: Pipe writer not initialized";
            }
            
        } catch (Exception e) {
            System.err.println("Error calling method " + methodName + ": " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * Call a Python method and wait for response (if response mechanism is implemented)
     */
    public static String callMethodWithResponse(String methodName, Object... args) {
        // For now, this is the same as callMethod
        // Can be extended to implement a response mechanism using a second pipe
        return callMethod(methodName, args);
    }
    
    /**
     * Cleanup resources
     */
    public static void cleanup() {
        try {
            if (pipeWriter != null) {
                pipeWriter.close();
            }
            
            if (pythonProcess != null && pythonProcess.isAlive()) {
                pythonProcess.destroyForcibly();
                pythonProcess.waitFor(5, TimeUnit.SECONDS);
            }
            
            // Remove named pipe
            Files.deleteIfExists(Paths.get(PIPE_NAME));
            
            System.out.println("NamedPipeShim cleanup completed");
            
        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }
    
    /**
     * Example usage and testing
     */
    public static void main(String[] args) {
        // Initialize the shim
        if (!initialize()) {
            System.err.println("Failed to initialize, exiting...");
            return;
        }
        
        // Wait a moment for Python process to start
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Example method calls
        System.out.println("=== Testing Named Pipe Shim ===");
        
        // Test simple method call
        callMethod("hello_world");
        
        // Test method with string argument
        callMethod("greet", "Alice");
        
        // Test method with multiple arguments
        callMethod("calculate", 10, 5, "add");
        
        // Test method with mixed argument types
        callMethod("process_data", "test_file.txt", 100, true);
        
        // Wait for processing
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Cleanup
        cleanup();
    }
}
