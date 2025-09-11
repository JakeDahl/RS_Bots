import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.lang.reflect.Method;

/**
 * Manages named pipe communication for receiving method calls from Python
 */
public class PipeManager {
    
    private static final String PIPE_NAME = "/tmp/dreambot_shim_pipe";
    private static final String RESPONSE_PIPE_NAME = "/tmp/dreambot_shim_response_pipe";
    private static final Gson gson = new Gson();
    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static Thread listenerThread;
    private static DreamBotAPIWrapper apiWrapper;
    
    /**
     * Initialize the named pipe receiver and start listening
     */
    public static boolean initialize(DreamBotAPIWrapper wrapper) {
        try {
            apiWrapper = wrapper;
            
            // Create named pipe if it doesn't exist
            createNamedPipe();
            
            // Start listening thread
            startListening();
            
            System.out.println("PipeManager initialized successfully");
            return true;
            
        } catch (Exception e) {
            System.err.println("Failed to initialize PipeManager: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Create the named pipe using mkfifo command
     */
    private static void createNamedPipe() throws IOException, InterruptedException {
        // Create request pipe
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
        
        // Create response pipe
        if (!Files.exists(Paths.get(RESPONSE_PIPE_NAME))) {
            Process mkfifoProcess = Runtime.getRuntime().exec("mkfifo " + RESPONSE_PIPE_NAME);
            int exitCode = mkfifoProcess.waitFor();
            if (exitCode != 0) {
                throw new IOException("Failed to create response pipe with mkfifo");
            }
            System.out.println("Created response pipe: " + RESPONSE_PIPE_NAME);
        } else {
            System.out.println("Response pipe already exists: " + RESPONSE_PIPE_NAME);
        }
    }
    
    /**
     * Start the listening thread
     */
    private static void startListening() {
        running.set(true);
        listenerThread = new Thread(() -> {
            System.out.println("Started listening for method calls on " + PIPE_NAME);
            
            while (running.get()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(PIPE_NAME))) {
                    String line;
                    while ((line = reader.readLine()) != null && running.get()) {
                        if (!line.trim().isEmpty()) {
                            processMethodCall(line.trim());
                        }
                    }
                } catch (IOException e) {
                    if (running.get()) {
                        System.err.println("Error reading from pipe: " + e.getMessage());
                        try {
                            Thread.sleep(1000); // Wait before retrying
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
            
            System.out.println("Stopped listening for method calls");
        });
        
        listenerThread.setDaemon(true);
        listenerThread.start();
    }
    
    /**
     * Process a method call request from Python
     */
    private static void processMethodCall(String jsonRequest) {
        try {
            System.out.println("Received request: " + jsonRequest);
            
            JsonObject request = gson.fromJson(jsonRequest, JsonObject.class);
            String methodName = request.get("method").getAsString();
            JsonArray argsArray = request.getAsJsonArray("args");
            
            // Get request ID if present
            String requestId = request.has("id") ? request.get("id").getAsString() : null;
            
            // Convert JSON args to Object array
            Object[] args = new Object[argsArray.size()];
            for (int i = 0; i < argsArray.size(); i++) {
                JsonElement element = argsArray.get(i);
                if (element.isJsonPrimitive()) {
                    if (element.getAsJsonPrimitive().isString()) {
                        args[i] = element.getAsString();
                    } else if (element.getAsJsonPrimitive().isNumber()) {
                        // Try to determine if it's an integer or double
                        String numStr = element.getAsString();
                        if (numStr.contains(".")) {
                            args[i] = element.getAsDouble();
                        } else {
                            args[i] = element.getAsInt();
                        }
                    } else if (element.getAsJsonPrimitive().isBoolean()) {
                        args[i] = element.getAsBoolean();
                    }
                } else {
                    args[i] = element.toString();
                }
            }
            
            // Call the method
            Object result = callMethod(methodName, args);
            System.out.println("Method " + methodName + " result: " + result);
            
            // Send response back through response pipe
            sendResponse(requestId, methodName, result, null);
            
        } catch (Exception e) {
            System.err.println("Error processing method call: " + e.getMessage());
            e.printStackTrace();
            // Send error response
            sendResponse(null, "unknown", null, e.getMessage());
        }
    }
    
    /**
     * Dynamically call a method by name using reflection
     */
    private static Object callMethod(String methodName, Object... args) {
        try {
            // Get method parameter types
            Class<?>[] paramTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i].getClass();
                // Handle primitive type boxing
                if (paramTypes[i] == Integer.class) paramTypes[i] = int.class;
                else if (paramTypes[i] == Double.class) paramTypes[i] = double.class;
                else if (paramTypes[i] == Boolean.class) paramTypes[i] = boolean.class;
                else if (paramTypes[i] == Float.class) paramTypes[i] = float.class;
                else if (paramTypes[i] == Long.class) paramTypes[i] = long.class;
            }
            
            // Try to find the method in DreamBotAPIWrapper class first
            Method method = null;
            try {
                method = DreamBotAPIWrapper.class.getDeclaredMethod(methodName, paramTypes);
            } catch (NoSuchMethodException e) {
                // Try with different parameter types (e.g., int vs Integer)
                Method[] methods = DreamBotAPIWrapper.class.getDeclaredMethods();
                for (Method m : methods) {
                    if (m.getName().equals(methodName) && m.getParameterCount() == args.length) {
                        method = m;
                        break;
                    }
                }
            }
            
            if (method != null) {
                method.setAccessible(true);
                return method.invoke(apiWrapper, args);
            } else {
                return "Error: Method '" + methodName + "' not found";
            }
            
        } catch (Exception e) {
            return "Error executing method '" + methodName + "': " + e.getMessage();
        }
    }
    
    /**
     * Send response back to Python through response pipe
     */
    private static void sendResponse(String requestId, String methodName, Object result, String error) {
        try {
            JsonObject response = new JsonObject();
            if (requestId != null) {
                response.addProperty("id", requestId);
            }
            response.addProperty("method", methodName);
            
            if (error != null) {
                response.addProperty("error", error);
            } else {
                // Convert result to appropriate JSON type
                if (result instanceof Boolean) {
                    response.addProperty("result", (Boolean) result);
                } else if (result instanceof Number) {
                    response.addProperty("result", (Number) result);
                } else {
                    response.addProperty("result", result.toString());
                }
            }
            
            String responseJson = gson.toJson(response);
            System.out.println("Sending response: " + responseJson);
            
            // Write to response pipe
            try (PrintWriter writer = new PrintWriter(new FileWriter(RESPONSE_PIPE_NAME))) {
                writer.println(responseJson);
                writer.flush();
            }
            
        } catch (Exception e) {
            System.err.println("Error sending response: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Stop the named pipe receiver
     */
    public static void stop() {
        running.set(false);
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
        
        try {
            // Remove both pipes
            Files.deleteIfExists(Paths.get(PIPE_NAME));
            Files.deleteIfExists(Paths.get(RESPONSE_PIPE_NAME));
            System.out.println("Named pipe receiver stopped and cleaned up");
        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }
    
    /**
     * Check if the pipe manager is running
     */
    public static boolean isRunning() {
        return running.get();
    }
    
    /**
     * Get the pipe name
     */
    public static String getPipeName() {
        return PIPE_NAME;
    }
    
    /**
     * Get the response pipe name
     */
    public static String getResponsePipeName() {
        return RESPONSE_PIPE_NAME;
    }
}
