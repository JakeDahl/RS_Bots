import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.interactive.GameObjects;

/**
 * Manages all banking operations and queries
 */
public class BankingManager {
    
    private TaskManager taskManager;
    
    public BankingManager(TaskManager taskManager) {
        this.taskManager = taskManager;
    }
    
    /**
     * Check if bank is actually open using DreamBot API
     */
    public boolean bankIsOpen() {
        try {
            boolean isOpen = Bank.isOpen();
            Logger.log("Python->Java: Bank is open: " + isOpen);
            return isOpen;
        } catch (Exception e) {
            Logger.log("Python->Java: Error checking bank status: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Open bank using DreamBot API - Enhanced version with skip support
     */
    public String openBank() {
        try {
            taskManager.setCurrentStep("Opening bank");
            Logger.log("Python->Java: Opening bank...");
            
            if (Bank.isOpen()) {
                Logger.log("Python->Java: Bank already open");
                taskManager.setCurrentStep("Idle - Waiting for commands");
                return "Bank already open - SUCCESS";
            }
            
            // Try to find and interact with bank booth first
            var bank = GameObjects.closest("Bank booth");
            if (bank == null) {
                // Fallback to bank chest
                bank = GameObjects.closest("Bank chest");
            }
            
            if (bank != null) {
                Logger.log("Python->Java: Found bank object, attempting to interact...");
                if (bank.interact("Bank")) {
                    Logger.log("Python->Java: Bank interaction initiated, waiting for bank to open...");
                    // Wait for bank to open with timeout
                    boolean opened = Sleep.sleepUntil(() -> Bank.isOpen(), 5000);
                    
                    if (opened && Bank.isOpen()) {
                        Logger.log("Python->Java: Bank opened successfully");
                        taskManager.setCurrentStep("Idle - Waiting for commands");
                        return "Bank opened - SUCCESS";
                    } else {
                        Logger.log("Python->Java: Bank open timed out");
                        taskManager.setCurrentStep("Idle - Waiting for commands");
                        return "Bank opened - TIMEOUT";
                    }
                } else {
                    Logger.log("Python->Java: Failed to interact with bank");
                    taskManager.setCurrentStep("Idle - Waiting for commands");
                    return "Bank opened - FAILED (interaction failed)";
                }
            } else {
                Logger.log("Python->Java: No bank booth or chest found");
                taskManager.setCurrentStep("Idle - Waiting for commands");
                return "Bank opened - FAILED (no bank found)";
            }
        } catch (Exception e) {
            Logger.log("Python->Java: Exception in openBank: " + e.getMessage());
            e.printStackTrace();
            taskManager.setCurrentStep("Idle - Waiting for commands");
            return "Bank opened - ERROR: " + e.getMessage();
        }
    }
    
    /**
     * Close bank using DreamBot API
     */
    public String closeBank() {
        try {
            Logger.log("Python->Java: Closing bank...");
            
            if (!Bank.isOpen()) {
                Logger.log("Python->Java: Bank already closed");
                return "Bank already closed - SUCCESS";
            }
            
            Logger.log("Python->Java: Attempting to close bank...");
            if (Bank.close()) {
                Logger.log("Python->Java: Bank close initiated, waiting for confirmation...");
                // Wait for bank to close with timeout
                boolean closed = Sleep.sleepUntil(() -> !Bank.isOpen(), 3000);
                if (closed) {
                    Logger.log("Python->Java: Bank closed successfully");
                    return "Bank closed - SUCCESS";
                } else {
                    Logger.log("Python->Java: Bank close timed out");
                    return "Bank closed - TIMEOUT";
                }
            } else {
                Logger.log("Python->Java: Failed to close bank");
                return "Bank closed - FAILED";
            }
        } catch (Exception e) {
            Logger.log("Python->Java: Exception in closeBank: " + e.getMessage());
            e.printStackTrace();
            return "Bank closed - ERROR: " + e.getMessage();
        }
    }
    
    /**
     * Deposit all items except specified items
     */
    public String depositAllExcept(String... itemsToKeep) {
        try {
            Logger.log("Python->Java: Depositing all except: " + String.join(", ", itemsToKeep));
            
            if (!Bank.isOpen()) {
                Logger.log("Python->Java: Bank not open, cannot deposit");
                return "Deposit failed - Bank not open";
            }
            
            // Count initial inventory
            int initialCount = org.dreambot.api.methods.container.impl.Inventory.fullSlotCount();
            Logger.log("Python->Java: Initial inventory count: " + initialCount);
            
            Logger.log("Python->Java: Attempting to deposit all except specified items...");
            if (Bank.depositAllExcept(itemsToKeep)) {
                Logger.log("Python->Java: Deposit command sent, waiting for completion...");
                // Wait for deposit to complete - check that only items to keep remain
                boolean deposited = Sleep.sleepUntil(() -> {
                    int expectedCount = 0;
                    for (String item : itemsToKeep) {
                        if (org.dreambot.api.methods.container.impl.Inventory.contains(item)) {
                            expectedCount++;
                        }
                    }
                    int currentCount = org.dreambot.api.methods.container.impl.Inventory.fullSlotCount();
                    Logger.log("Python->Java: Current inventory: " + currentCount + ", expected: " + expectedCount);
                    return currentCount == expectedCount;
                }, 5000);
                
                if (deposited) {
                    Logger.log("Python->Java: Deposit completed successfully");
                    return "Deposit all except - SUCCESS";
                } else {
                    Logger.log("Python->Java: Deposit timed out");
                    return "Deposit all except - TIMEOUT";
                }
            } else {
                Logger.log("Python->Java: Deposit command failed");
                return "Deposit all except - FAILED";
            }
        } catch (Exception e) {
            Logger.log("Python->Java: Exception in depositAllExcept: " + e.getMessage());
            e.printStackTrace();
            return "Deposit all except - ERROR: " + e.getMessage();
        }
    }
    
    /**
     * Deposit specific item with count
     */
    public String depositItem(String itemName, int count) {
        try {
            Logger.log("Python->Java: Depositing " + count + " " + itemName);
            
            if (!Bank.isOpen()) {
                Logger.log("Python->Java: Bank not open, cannot deposit");
                return "Deposit failed - Bank not open";
            }
            
            int initialCount = org.dreambot.api.methods.container.impl.Inventory.count(itemName);
            Logger.log("Python->Java: Initial count of " + itemName + ": " + initialCount);
            
            if (initialCount == 0) {
                Logger.log("Python->Java: Item not found in inventory");
                return "Deposit item - Item not found in inventory";
            }
            
            int amountToDeposit = count == -1 ? initialCount : Math.min(count, initialCount);
            Logger.log("Python->Java: Attempting to deposit " + amountToDeposit + " " + itemName);
            
            if (Bank.deposit(itemName, amountToDeposit)) {
                Logger.log("Python->Java: Deposit command sent, waiting for completion...");
                // Wait for deposit to complete
                boolean deposited = Sleep.sleepUntil(() -> {
                    int currentCount = org.dreambot.api.methods.container.impl.Inventory.count(itemName);
                    int expectedCount = initialCount - amountToDeposit;
                    Logger.log("Python->Java: Current count: " + currentCount + ", expected: " + expectedCount);
                    return currentCount == expectedCount;
                }, 5000);
                
                if (deposited) {
                    Logger.log("Python->Java: Deposit completed successfully");
                    return "Deposit item - SUCCESS (deposited " + amountToDeposit + ")";
                } else {
                    Logger.log("Python->Java: Deposit timed out");
                    return "Deposit item - TIMEOUT";
                }
            } else {
                Logger.log("Python->Java: Deposit command failed");
                return "Deposit item - FAILED";
            }
        } catch (Exception e) {
            Logger.log("Python->Java: Exception in depositItem: " + e.getMessage());
            e.printStackTrace();
            return "Deposit item - ERROR: " + e.getMessage();
        }
    }
    
    /**
     * Withdraw specific item with count
     */
    public String withdrawItem(String itemName, int count) {
        try {
            Logger.log("Python->Java: Withdrawing " + count + " " + itemName);
            
            if (!Bank.isOpen()) {
                Logger.log("Python->Java: Bank not open, cannot withdraw");
                return "Withdraw failed - Bank not open";
            }
            
            if (!Bank.contains(itemName)) {
                Logger.log("Python->Java: Item not found in bank");
                return "Withdraw item - Item not found in bank";
            }
            
            int initialCount = org.dreambot.api.methods.container.impl.Inventory.count(itemName);
            int bankCount = Bank.count(itemName);
            Logger.log("Python->Java: Initial inventory count: " + initialCount + ", bank count: " + bankCount);
            
            Logger.log("Python->Java: Attempting to withdraw " + count + " " + itemName);
            if (Bank.withdraw(itemName, count)) {
                Logger.log("Python->Java: Withdraw command sent, waiting for completion...");
                // Wait for withdrawal to complete
                boolean withdrawn = Sleep.sleepUntil(() -> {
                    int currentCount = org.dreambot.api.methods.container.impl.Inventory.count(itemName);
                    Logger.log("Python->Java: Current inventory count: " + currentCount + ", initial: " + initialCount);
                    return currentCount > initialCount;
                }, 5000);
                
                if (withdrawn) {
                    int withdrawnAmount = org.dreambot.api.methods.container.impl.Inventory.count(itemName) - initialCount;
                    Logger.log("Python->Java: Withdraw completed successfully, withdrew " + withdrawnAmount);
                    return "Withdraw item - SUCCESS (withdrew " + withdrawnAmount + ")";
                } else {
                    Logger.log("Python->Java: Withdraw timed out");
                    return "Withdraw item - TIMEOUT";
                }
            } else {
                Logger.log("Python->Java: Withdraw command failed");
                return "Withdraw item - FAILED";
            }
        } catch (Exception e) {
            Logger.log("Python->Java: Exception in withdrawItem: " + e.getMessage());
            e.printStackTrace();
            return "Withdraw item - ERROR: " + e.getMessage();
        }
    }
    
    /**
     * Check if bank contains specific item
     */
    public boolean bankContains(String itemName) {
        try {
            if (!Bank.isOpen()) {
                Logger.log("Python->Java: Bank not open, cannot check contents");
                return false;
            }
            
            boolean contains = Bank.contains(itemName);
            Logger.log("Python->Java: Bank contains " + itemName + ": " + contains);
            return contains;
        } catch (Exception e) {
            Logger.log("Python->Java: Exception in bankContains: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get bank item count
     */
    public int getBankItemCount(String itemName) {
        try {
            if (!Bank.isOpen()) {
                Logger.log("Python->Java: Bank not open, cannot get item count");
                return -1;
            }
            
            int count = Bank.count(itemName);
            Logger.log("Python->Java: Bank count of " + itemName + ": " + count);
            return count;
        } catch (Exception e) {
            Logger.log("Python->Java: Exception in getBankItemCount: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }
}
