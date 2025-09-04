import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.trade.Trade;
import org.dreambot.api.wrappers.interactive.Player;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

@ScriptManifest(name = "Runecraft Bot", description = "Runs runes from bank to altars with configurable UI",
        author = "RunecraftBot", version = 1.0, category = Category.RUNECRAFTING, image = "")

public class Main extends AbstractScript {

    private final Random random = new Random();
    private JFrame gui;
    private JFrame controlPanel;
    private JComboBox<String> runeTypeDropdown;
    private JTextField targetNameField;
    private JSlider priceSlider;
    private JLabel priceLabel;
    private JButton startButton;
    
    private String selectedRuneType = "Air";
    private String targetName = "";
    private int runPrice = 1000;
    private boolean scriptStarted = false;
    private boolean guiClosed = false;
    
    private RuneHandler currentRuneHandler;
    
    private enum State {
        IDLE,
        BANKING,
        WALKING_TO_ALTAR,
        CRAFTING_RUNES,
        ADVERTISING,
        TRADING
    }
    
    private State currentState = State.IDLE;

    @Override
    public void onStart() {
        Logger.log("Runecraft Bot started!");
        
        createGUI();
        
        Logger.log("GUI created successfully");
    }
    
    private void createGUI() {
        SwingUtilities.invokeLater(() -> {
            // Close existing GUI if open
            if (gui != null) {
                gui.dispose();
            }
            
            gui = new JFrame("Runecraft Bot Configuration");
            gui.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            gui.setLayout(new BorderLayout());
            gui.setSize(400, 350);
            
            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            
            // Rune Type Dropdown
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.WEST;
            mainPanel.add(new JLabel("Rune Type:"), gbc);
            
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            runeTypeDropdown = new JComboBox<>(new String[]{"Air", "Fire"});
            runeTypeDropdown.addActionListener(e -> {
                selectedRuneType = (String) runeTypeDropdown.getSelectedItem();
                Logger.log("Selected rune type: " + selectedRuneType);
            });
            mainPanel.add(runeTypeDropdown, gbc);
            
            // Target Name Field
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.fill = GridBagConstraints.NONE;
            mainPanel.add(new JLabel("Target Name:"), gbc);
            
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            targetNameField = new JTextField(15);
            targetNameField.setToolTipText("Leave empty to advertise publicly");
            mainPanel.add(targetNameField, gbc);
            
            // Price Slider
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.fill = GridBagConstraints.NONE;
            mainPanel.add(new JLabel("Run Price:"), gbc);
            
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            priceSlider = new JSlider(100, 100000, 1000);
            priceSlider.setMajorTickSpacing(10000);
            priceSlider.setMinorTickSpacing(1000);
            priceSlider.setPaintTicks(true);
            priceSlider.setSnapToTicks(true);
            
            priceSlider.addChangeListener(e -> {
                runPrice = ((priceSlider.getValue() + 50) / 100) * 100; // Round to nearest 100
                priceLabel.setText("Price: " + runPrice + " gp");
            });
            mainPanel.add(priceSlider, gbc);
            
            // Price Label
            gbc.gridx = 1;
            gbc.gridy = 3;
            priceLabel = new JLabel("Price: " + runPrice + " gp");
            mainPanel.add(priceLabel, gbc);
            
            // Buttons Panel
            gbc.gridx = 0;
            gbc.gridy = 4;
            gbc.gridwidth = 2;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            
            JPanel buttonPanel = new JPanel(new FlowLayout());
            
            // Start/Reconfigure Button
            if (scriptStarted) {
                startButton = new JButton("Update Settings");
                startButton.addActionListener(e -> updateSettings());
            } else {
                startButton = new JButton("Start Bot");
                startButton.addActionListener(e -> startBot());
            }
            buttonPanel.add(startButton);
            
            // Stop Bot Button (if running)
            if (scriptStarted) {
                JButton stopButton = new JButton("Stop Bot");
                stopButton.addActionListener(e -> stopBot());
                buttonPanel.add(stopButton);
            }
            
            // Reopen GUI Button (always available)
            JButton reopenButton = new JButton("Reopen GUI");
            reopenButton.addActionListener(e -> {
                gui.dispose();
                createGUI();
            });
            buttonPanel.add(reopenButton);
            
            mainPanel.add(buttonPanel, gbc);
            
            // Instructions Panel
            JPanel instructionPanel = new JPanel();
            instructionPanel.setBorder(BorderFactory.createTitledBorder("Instructions"));
            JLabel instructionLabel = new JLabel("<html><center>Configure your settings and click Start Bot<br/>Use Update Settings button to change while running</center></html>");
            instructionLabel.setHorizontalAlignment(SwingConstants.CENTER);
            instructionPanel.add(instructionLabel);
            
            gui.add(mainPanel, BorderLayout.CENTER);
            gui.add(instructionPanel, BorderLayout.SOUTH);
            gui.setLocationRelativeTo(null);
            gui.setVisible(true);
        });
    }
    
    private void startBot() {
        targetName = targetNameField.getText().trim();
        
        Logger.log("Starting bot with configuration:");
        Logger.log("Rune Type: " + selectedRuneType);
        Logger.log("Target Name: " + (targetName.isEmpty() ? "Public advertising" : targetName));
        Logger.log("Run Price: " + runPrice + " gp");
        
        // Initialize appropriate rune handler
        switch (selectedRuneType) {
            case "Air":
                currentRuneHandler = new AirRuneHandler();
                break;
            case "Fire":
                currentRuneHandler = new FireRuneHandler();
                break;
            default:
                Logger.log("Unknown rune type: " + selectedRuneType);
                return;
        }
        
        scriptStarted = true;
        guiClosed = true;
        currentState = State.BANKING;
        
        // Close main GUI and create small control panel
        if (gui != null) {
            gui.dispose();
        }
        
        createControlPanel();
        
        Logger.log("Bot started successfully!");
        Logger.log("Use the control panel to reconfigure or stop the bot");
    }
    
    private void updateSettings() {
        targetName = targetNameField.getText().trim();
        
        Logger.log("Settings updated:");
        Logger.log("Rune Type: " + selectedRuneType);
        Logger.log("Target Name: " + (targetName.isEmpty() ? "Public advertising" : targetName));
        Logger.log("Run Price: " + runPrice + " gp");
        
        // Initialize appropriate rune handler if type changed
        switch (selectedRuneType) {
            case "Air":
                if (!(currentRuneHandler instanceof AirRuneHandler)) {
                    currentRuneHandler = new AirRuneHandler();
                    Logger.log("Switched to Air Rune Handler");
                }
                break;
            case "Fire":
                if (!(currentRuneHandler instanceof FireRuneHandler)) {
                    currentRuneHandler = new FireRuneHandler();
                    Logger.log("Switched to Fire Rune Handler");
                }
                break;
        }
        
        // Close GUI
        if (gui != null) {
            gui.dispose();
        }
        
        Logger.log("Settings updated successfully!");
    }
    
    private void stopBot() {
        Logger.log("Stopping bot...");
        scriptStarted = false;
        currentState = State.IDLE;
        
        // Close GUI
        if (gui != null) {
            gui.dispose();
        }
        
        Logger.log("Bot stopped - you can restart with new settings");
    }
    
    private void createControlPanel() {
        SwingUtilities.invokeLater(() -> {
            // Close existing control panel if open
            if (controlPanel != null) {
                controlPanel.dispose();
            }
            
            controlPanel = new JFrame("Runecraft Bot - Control Panel");
            controlPanel.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            controlPanel.setSize(300, 150);
            controlPanel.setAlwaysOnTop(true);
            
            JPanel panel = new JPanel(new GridLayout(4, 1, 5, 5));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            // Status label
            JLabel statusLabel = new JLabel("<html><center><b>Bot Running</b><br/>Type: " + selectedRuneType + 
                " | Price: " + runPrice + " gp</center></html>");
            statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(statusLabel);
            
            // Target label
            String targetText = targetName.isEmpty() ? "Public Advertising" : "Target: " + targetName;
            JLabel targetLabel = new JLabel("<html><center>" + targetText + "</center></html>");
            targetLabel.setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(targetLabel);
            
            // Buttons
            JPanel buttonPanel = new JPanel(new FlowLayout());
            
            JButton configButton = new JButton("Reconfigure");
            configButton.addActionListener(e -> {
                controlPanel.dispose();
                createGUI();
            });
            buttonPanel.add(configButton);
            
            JButton stopButton = new JButton("Stop Bot");
            stopButton.addActionListener(e -> {
                stopBot();
                controlPanel.dispose();
            });
            buttonPanel.add(stopButton);
            
            panel.add(buttonPanel);
            
            // Instructions
            JLabel instructionLabel = new JLabel("<html><center><small>Always on top for easy access</small></center></html>");
            instructionLabel.setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(instructionLabel);
            
            controlPanel.add(panel);
            controlPanel.setLocationRelativeTo(null);
            controlPanel.setVisible(true);
        });
    }
    
    public void reopenGUI() {
        Logger.log("Reopening configuration GUI...");
        SwingUtilities.invokeLater(() -> createGUI());
    }

    @Override
    public int onLoop() {
        if (!scriptStarted) {
            return randomSleep(1000, 2000);
        }
        
        switch (currentState) {
            case IDLE:
                Logger.log("Bot is idle...");
                return randomSleep(2000, 3000);
                
            case BANKING:
                // Check if we already have 28 essence and are near altar
                if (Inventory.count("Pure essence") == 28 && isNearAltar()) {
                    Logger.log("Already have 28 essence and at altar - skipping banking, going to craft");
                    currentState = State.CRAFTING_RUNES;
                } else if (Inventory.count("Pure essence") == 28) {
                    Logger.log("Already have 28 essence - walking to altar instead of banking");
                    currentState = State.WALKING_TO_ALTAR;
                } else {
                    Logger.log("Banking state - preparing for runecrafting...");
                    if (handleBanking()) {
                        currentState = State.WALKING_TO_ALTAR;
                    }
                }
                break;
                
            case WALKING_TO_ALTAR:
                Logger.log("Walking to " + selectedRuneType.toLowerCase() + " altar...");
                if (currentRuneHandler.walkToAltar()) {
                    currentState = State.CRAFTING_RUNES;
                }
                break;
                
            case CRAFTING_RUNES:
                Logger.log("Crafting " + selectedRuneType.toLowerCase() + " runes...");
                if (currentRuneHandler.craftRunes()) {
                    if (targetName.isEmpty()) {
                        currentState = State.ADVERTISING;
                    } else {
                        currentState = State.TRADING;
                    }
                }
                break;
                
            case ADVERTISING:
                Logger.log("Advertising runs at " + runPrice + " gp each...");
                handleAdvertising();
                break;
                
            case TRADING:
                Logger.log("Looking for target: " + targetName);
                handleTargetTrading();
                break;
        }
        
        return randomSleep(100, 500);
    }
    
    private boolean handleBanking() {
        Logger.log("Handling banking - preparing for " + selectedRuneType + " rune crafting");
        
        // Check if we need to bank
        if (!needsBanking()) {
            Logger.log("Already have supplies, skipping banking");
            return true;
        }
        
        // Walk to nearest bank if not already there
        if (!isNearBank()) {
            int[] bankLocation = currentRuneHandler.getBankLocation();
            if (Walking.walk(bankLocation[0], bankLocation[1])) {
                Logger.log("Walking to bank at (" + bankLocation[0] + ", " + bankLocation[1] + ")");
                Sleep.sleepUntil(() -> isNearBank() || !Players.getLocal().isMoving(), 10000);
            } else {
                Logger.log("Failed to walk to bank");
                return false;
            }
        }
        
        if (isNearBank()) {
            // Simulate banking actions
            Logger.log("Opening bank...");
            if (Bank.open()) {
                Sleep.sleepUntil(Bank::isOpen, 5000);
                
                if (Bank.isOpen()) {
                    Logger.log("Banking supplies for " + selectedRuneType + " runes");
                    
                    // Deposit all items first to make space
                    Logger.log("Depositing all items...");
                    Bank.depositAllItems();
                    Sleep.sleep(1000);
                    
                    // Withdraw 28 pure essence
                    Logger.log("Withdrawing 28 pure essence...");
                    if (Bank.contains("Pure essence")) {
                        if (Bank.withdraw("Pure essence", 28)) {
                            Logger.log("Successfully withdrew 28 pure essence");
                            Sleep.sleepUntil(() -> Inventory.count("Pure essence") == 28, 5000);
                            
                            if (Inventory.count("Pure essence") == 28) {
                                Logger.log("Confirmed: 28 pure essence in inventory");
                            } else {
                                Logger.log("Warning: Only " + Inventory.count("Pure essence") + " pure essence in inventory");
                            }
                        } else {
                            Logger.log("Failed to withdraw pure essence");
                        }
                    } else {
                        Logger.log("No pure essence found in bank!");
                        Bank.close();
                        return false;
                    }
                    
                    // Close bank
                    Bank.close();
                    Sleep.sleepUntil(() -> !Bank.isOpen(), 3000);
                    
                    Logger.log("Banking complete");
                    return true;
                } else {
                    Logger.log("Failed to open bank");
                    return false;
                }
            } else {
                Logger.log("Could not open bank");
                return false;
            }
        }
        
        Logger.log("Not near bank");
        return false;
    }
    
    private boolean needsBanking() {
        int essenceCount = Inventory.count("Pure essence");
        Logger.log("Checking if banking is needed... Current essence: " + essenceCount);
        
        // Need banking if we don't have 28 essence
        if (essenceCount < 28) {
            Logger.log("Need banking - only have " + essenceCount + "/28 essence");
            return true;
        }
        
        Logger.log("No banking needed - have full inventory of essence");
        return false;
    }
    
    private boolean isNearBank() {
        // Simple distance check to bank
        int[] bankLocation = currentRuneHandler.getBankLocation();
        int playerX = Players.getLocal().getX();
        int playerY = Players.getLocal().getY();
        
        return Math.abs(playerX - bankLocation[0]) <= 10 && Math.abs(playerY - bankLocation[1]) <= 10;
    }
    
    private boolean isNearAltar() {
        // Simple distance check to altar
        int[] altarLocation = currentRuneHandler.getAltarLocation();
        int playerX = Players.getLocal().getX();
        int playerY = Players.getLocal().getY();
        
        return Math.abs(playerX - altarLocation[0]) <= 15 && Math.abs(playerY - altarLocation[1]) <= 15;
    }
    
    private void handleAdvertising() {
        Logger.log("Advertising: " + selectedRuneType + " runs " + runPrice + " gp each!");
        
        // Simulate public chat advertising
        String adMessage = selectedRuneType + " runs " + runPrice + " gp each! PM me!";
        Logger.log("Public chat: " + adMessage);
        
        // Wait for potential customers
        Sleep.sleep(10000);
        
        // Reset to banking for next run
        Logger.log("Advertisement cycle complete, returning to bank");
        currentState = State.BANKING;
    }
    
    private void handleTargetTrading() {
        Logger.log("Looking for target player: " + targetName);
        
        // Check if trade is already open
        if (Trade.isOpen()) {
            handleActiveTrading();
            return;
        }
        
        // Look for target player
        Player targetPlayer = Players.closest(targetName);
        if (targetPlayer != null) {
            Logger.log("Found target player: " + targetName);
            
            // Initiate trade
            if (Trade.tradeWithPlayer(targetPlayer)) {
                Logger.log("Initiated trade with " + targetName);
                Sleep.sleepUntil(() -> Trade.isOpen(1), 10000);
                
                if (Trade.isOpen(1)) {
                    Logger.log("Trade window opened successfully");
                    handleActiveTrading();
                } else {
                    Logger.log("Trade window failed to open");
                    Sleep.sleep(3000);
                }
            } else {
                Logger.log("Failed to initiate trade with " + targetName);
                Sleep.sleep(2000);
            }
        } else {
            Logger.log("Target player " + targetName + " not found, continuing to search...");
            Sleep.sleep(5000);
        }
    }
    
    private void handleActiveTrading() {
        if (!Trade.isOpen()) {
            Logger.log("Trade window closed unexpectedly");
            currentState = State.BANKING;
            return;
        }
        
        String tradingWith = Trade.getTradingWith();
        Logger.log("Currently trading with: " + tradingWith);
        
        if (Trade.isOpen(1)) {
            // First trade screen - setup the trade
            handleFirstTradeScreen();
        } else if (Trade.isOpen(2)) {
            // Second trade screen - confirm the trade
            handleSecondTradeScreen();
        }
    }
    
    private void handleFirstTradeScreen() {
        Logger.log("On first trade screen - setting up trade");
        
        // Add our runes to trade
        String runeName = selectedRuneType + " rune";
        int runeCount = Inventory.count(runeName);
        
        if (runeCount > 0) {
            Logger.log("Adding " + runeCount + " " + runeName + "(s) to trade");
            if (Trade.addItem(runeName, runeCount)) {
                Logger.log("Successfully added runes to trade");
                
                // Wait a moment for them to add their gold
                Sleep.sleep(2000);
                
                // Check if they have the correct amount of gold
                int expectedGold = runeCount * runPrice;
                if (validateGoldAmount(expectedGold)) {
                    Logger.log("Gold amount validated (" + expectedGold + " gp), accepting first trade");
                    if (Trade.acceptTrade(1)) {
                        Logger.log("Accepted first trade screen");
                        Sleep.sleepUntil(() -> Trade.isOpen(2), 10000);
                    } else {
                        Logger.log("Failed to accept first trade");
                        Trade.declineTrade(1);
                    }
                } else {
                    Logger.log("Incorrect gold amount, declining trade");
                    Trade.declineTrade(1);
                    currentState = State.BANKING;
                }
            } else {
                Logger.log("Failed to add runes to trade");
                Trade.declineTrade(1);
                currentState = State.BANKING;
            }
        } else {
            Logger.log("No " + runeName + "s in inventory to trade");
            Trade.declineTrade(1);
            currentState = State.BANKING;
        }
    }
    
    private void handleSecondTradeScreen() {
        Logger.log("On second trade screen - confirming trade");
        
        // Final validation before accepting
        String runeName = selectedRuneType + " rune";
        int runeCount = 0;
        
        // Count our runes in trade window
        if (Trade.contains(true, runeName)) {
            // Get the actual count from our trade screen
            if (Trade.getItem(true, runeName) != null) {
                runeCount = Trade.getItem(true, runeName).getAmount();
            }
        }
        
        if (runeCount > 0) {
            int expectedGold = runeCount * runPrice;
            
            // Final gold validation
            if (validateGoldAmount(expectedGold)) {
                Logger.log("Final validation passed - completing trade");
                if (Trade.acceptTrade(2)) {
                    Logger.log("Trade completed successfully!");
                    
                    // Wait for trade to complete
                    Sleep.sleepUntil(() -> !Trade.isOpen(), 5000);
                    
                    Logger.log("Trade finished, returning to bank for next run");
                    currentState = State.BANKING;
                } else {
                    Logger.log("Failed to accept second trade");
                    Trade.declineTrade(2);
                    currentState = State.BANKING;
                }
            } else {
                Logger.log("Final gold validation failed, declining trade");
                Trade.declineTrade(2);
                currentState = State.BANKING;
            }
        } else {
            Logger.log("No runes found in trade, declining");
            Trade.declineTrade(2);
            currentState = State.BANKING;
        }
    }
    
    private boolean validateGoldAmount(int expectedAmount) {
        Logger.log("Validating gold amount - expecting " + expectedAmount + " gp");
        
        // Check if they have coins/gold in their trade screen
        boolean hasCorrectAmount = Trade.contains(false, expectedAmount, "Coins") || 
                                  Trade.contains(false, expectedAmount, "Gold");
        
        if (hasCorrectAmount) {
            Logger.log("Gold amount validation: PASSED (" + expectedAmount + " gp)");
            return true;
        } else {
            Logger.log("Gold amount validation: FAILED");
            
            // Log what they actually offered
            if (Trade.getItem(false, "Coins") != null) {
                int actualAmount = Trade.getItem(false, "Coins").getAmount();
                Logger.log("They offered " + actualAmount + " gp, but we need " + expectedAmount + " gp");
            } else if (Trade.getItem(false, "Gold") != null) {
                int actualAmount = Trade.getItem(false, "Gold").getAmount();
                Logger.log("They offered " + actualAmount + " gold, but we need " + expectedAmount + " gp");
            } else {
                Logger.log("They offered no gold/coins");
            }
            return false;
        }
    }

    @Override
    public void onExit() {
        Logger.log("Runecraft Bot stopped!");
        if (gui != null) {
            gui.dispose();
        }
        if (controlPanel != null) {
            controlPanel.dispose();
        }
    }
    
    private int randomSleep(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }
}