import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.combat.Combat;
import org.dreambot.api.wrappers.interactive.Player;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

@ScriptManifest(name = "Rumors Hunter", description = "Hunter bot with configurable UI for different hunting targets",
        author = "RumorsHunter", version = 1.0, category = Category.HUNTING, image = "")

public class Main extends AbstractScript {

    private final Random random = new Random();
    private JFrame gui;
    private JFrame controlPanel;
    private JComboBox<String> huntingTargetDropdown;
    private JLabel levelRequirementLabel;
    private JButton startButton;
    
    private String selectedHuntingTarget = "Sunlight Antelope";
    private boolean scriptStarted = false;
    private boolean guiClosed = false;
    
    private HuntingHandler currentHuntingHandler;
    
    private enum State {
        IDLE,
        CHECKING_REQUIREMENTS,
        PREPARING_EQUIPMENT,
        WALKING_TO_HUNTING_AREA,
        HUNTING,
        BANKING_LOOT,
        GOING_TO_GUIDE
    }
    
    private State currentState = State.IDLE;

    @Override
    public void onStart() {
        Logger.log("Rumors Hunter started!");
        
        createGUI();
        
        Logger.log("GUI created successfully");
    }
    
    private void createGUI() {
        SwingUtilities.invokeLater(() -> {
            if (gui != null) {
                gui.dispose();
            }
            
            gui = new JFrame("Rumors Hunter Configuration");
            gui.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            gui.setLayout(new BorderLayout());
            gui.setSize(400, 300);
            
            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            
            // Hunting Target Dropdown
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.WEST;
            mainPanel.add(new JLabel("Hunting Target:"), gbc);
            
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            huntingTargetDropdown = new JComboBox<>(new String[]{"Sunlight Antelope", "Moonlight Antelopes", "Moonlight Moths", "Tecu Salamanders", "Red Salamanders"});
            huntingTargetDropdown.addActionListener(e -> {
                selectedHuntingTarget = (String) huntingTargetDropdown.getSelectedItem();
                updateLevelRequirement();
                Logger.log("Selected hunting target: " + selectedHuntingTarget);
            });
            mainPanel.add(huntingTargetDropdown, gbc);
            
            // Level Requirement Label
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.gridwidth = 2;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            levelRequirementLabel = new JLabel();
            updateLevelRequirement();
            mainPanel.add(levelRequirementLabel, gbc);
            
            // Current Hunter Level
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.gridwidth = 2;
            int currentHunterLevel = Skills.getRealLevel(Skill.HUNTER);
            JLabel currentLevelLabel = new JLabel("<html><center><b>Current Hunter Level: " + currentHunterLevel + "</b></center></html>");
            currentLevelLabel.setHorizontalAlignment(SwingConstants.CENTER);
            mainPanel.add(currentLevelLabel, gbc);
            
            // Buttons Panel
            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.gridwidth = 2;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            
            JPanel buttonPanel = new JPanel(new FlowLayout());
            
            // Start/Update Button
            if (scriptStarted) {
                startButton = new JButton("Update Settings");
                startButton.addActionListener(e -> updateSettings());
            } else {
                startButton = new JButton("Start Hunter");
                startButton.addActionListener(e -> startBot());
            }
            buttonPanel.add(startButton);
            
            // Stop Bot Button (if running)
            if (scriptStarted) {
                JButton stopButton = new JButton("Stop Hunter");
                stopButton.addActionListener(e -> stopBot());
                buttonPanel.add(stopButton);
            }
            
            // Reopen GUI Button
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
            JLabel instructionLabel = new JLabel("<html><center>Select your hunting target and click Start Hunter<br/>Make sure you have the required level and equipment</center></html>");
            instructionLabel.setHorizontalAlignment(SwingConstants.CENTER);
            instructionPanel.add(instructionLabel);
            
            gui.add(mainPanel, BorderLayout.CENTER);
            gui.add(instructionPanel, BorderLayout.SOUTH);
            gui.setLocationRelativeTo(null);
            gui.setVisible(true);
        });
    }
    
    private void updateLevelRequirement() {
        int requiredLevel = getRequiredLevel(selectedHuntingTarget);
        int currentLevel = Skills.getRealLevel(Skill.HUNTER);
        
        String levelText = "<html><center>Required Level: " + requiredLevel;
        if (currentLevel >= requiredLevel) {
            levelText += " <font color='green'>✓ (You meet the requirement)</font>";
        } else {
            levelText += " <font color='red'>✗ (You need " + (requiredLevel - currentLevel) + " more levels)</font>";
        }
        levelText += "</center></html>";
        
        levelRequirementLabel.setText(levelText);
        levelRequirementLabel.setHorizontalAlignment(SwingConstants.CENTER);
    }
    
    private int getRequiredLevel(String huntingTarget) {
        switch (huntingTarget) {
            case "Sunlight Antelope":
                return 72;
            case "Moonlight Antelopes":
                return 72;
            case "Moonlight Moths":
                return 15;
            case "Tecu Salamanders":
                return 79;
            case "Red Salamanders":
                return 59;
            default:
                return 1;
        }
    }
    
    private void startBot() {
        int currentLevel = Skills.getRealLevel(Skill.HUNTER);
        int requiredLevel = getRequiredLevel(selectedHuntingTarget);
        
        if (currentLevel < requiredLevel) {
            JOptionPane.showMessageDialog(gui, 
                "You need level " + requiredLevel + " Hunter to hunt " + selectedHuntingTarget + ".\nYour current level is " + currentLevel + ".",
                "Level Requirement Not Met", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        Logger.log("Starting hunter bot with configuration:");
        Logger.log("Hunting Target: " + selectedHuntingTarget);
        Logger.log("Required Level: " + requiredLevel + " (Current: " + currentLevel + ")");
        
        // Initialize appropriate hunting handler
        switch (selectedHuntingTarget) {
            case "Sunlight Antelope":
                currentHuntingHandler = new SunlightAntelopeHandler();
                break;
            case "Moonlight Antelopes":
                currentHuntingHandler = new MoonlightAntelopeHandler();
                break;
            case "Moonlight Moths":
                currentHuntingHandler = new MoonlightMothHandler();
                break;
            case "Tecu Salamanders":
                currentHuntingHandler = new TecuSalamanderHandler();
                break;
            case "Red Salamanders":
                currentHuntingHandler = new RedSalamanderHandler();
                break;
            default:
                Logger.log("Unknown hunting target: " + selectedHuntingTarget);
                return;
        }
        
        scriptStarted = true;
        guiClosed = true;
        currentState = State.CHECKING_REQUIREMENTS;
        
        // Close main GUI and create small control panel
        if (gui != null) {
            gui.dispose();
        }
        
        createControlPanel();
        
        Logger.log("Hunter bot started successfully!");
        Logger.log("Use the control panel to reconfigure or stop the bot");
    }
    
    private void updateSettings() {
        Logger.log("Settings updated:");
        Logger.log("Hunting Target: " + selectedHuntingTarget);
        
        // Initialize appropriate hunting handler if target changed
        switch (selectedHuntingTarget) {
            case "Sunlight Antelope":
                if (!(currentHuntingHandler instanceof SunlightAntelopeHandler)) {
                    currentHuntingHandler = new SunlightAntelopeHandler();
                    Logger.log("Switched to Sunlight Antelope Handler");
                }
                break;
            case "Moonlight Antelopes":
                if (!(currentHuntingHandler instanceof MoonlightAntelopeHandler)) {
                    currentHuntingHandler = new MoonlightAntelopeHandler();
                    Logger.log("Switched to Moonlight Antelope Handler");
                }
                break;
            case "Moonlight Moths":
                if (!(currentHuntingHandler instanceof MoonlightMothHandler)) {
                    currentHuntingHandler = new MoonlightMothHandler();
                    Logger.log("Switched to Moonlight Moth Handler");
                }
                break;
            case "Tecu Salamanders":
                if (!(currentHuntingHandler instanceof TecuSalamanderHandler)) {
                    currentHuntingHandler = new TecuSalamanderHandler();
                    Logger.log("Switched to Tecu Salamander Handler");
                }
                break;
            case "Red Salamanders":
                if (!(currentHuntingHandler instanceof RedSalamanderHandler)) {
                    currentHuntingHandler = new RedSalamanderHandler();
                    Logger.log("Switched to Red Salamander Handler");
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
        Logger.log("Stopping hunter bot...");
        scriptStarted = false;
        currentState = State.IDLE;
        
        // Close GUI
        if (gui != null) {
            gui.dispose();
        }
        
        Logger.log("Hunter bot stopped - you can restart with new settings");
    }
    
    private void createControlPanel() {
        SwingUtilities.invokeLater(() -> {
            if (controlPanel != null) {
                controlPanel.dispose();
            }
            
            controlPanel = new JFrame("Rumors Hunter - Control Panel");
            controlPanel.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            controlPanel.setSize(300, 150);
            controlPanel.setAlwaysOnTop(true);
            
            JPanel panel = new JPanel(new GridLayout(4, 1, 5, 5));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            // Status label
            JLabel statusLabel = new JLabel("<html><center><b>Hunter Running</b><br/>Target: " + selectedHuntingTarget + "</center></html>");
            statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(statusLabel);
            
            // Level label
            int currentLevel = Skills.getRealLevel(Skill.HUNTER);
            JLabel levelLabel = new JLabel("<html><center>Hunter Level: " + currentLevel + "</center></html>");
            levelLabel.setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(levelLabel);
            
            // Buttons
            JPanel buttonPanel = new JPanel(new FlowLayout());
            
            JButton configButton = new JButton("Reconfigure");
            configButton.addActionListener(e -> {
                controlPanel.dispose();
                createGUI();
            });
            buttonPanel.add(configButton);
            
            JButton stopButton = new JButton("Stop Hunter");
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
                Logger.log("Hunter bot is idle...");
                return randomSleep(2000, 3000);
                
            case CHECKING_REQUIREMENTS:
                Logger.log("Checking requirements for " + selectedHuntingTarget + "...");
                if (currentHuntingHandler.checkRequirements()) {
                    currentState = State.PREPARING_EQUIPMENT;
                } else {
                    Logger.log("Requirements not met, stopping bot");
                    stopBot();
                }
                break;
                
            case PREPARING_EQUIPMENT:
                Logger.log("Preparing equipment for " + selectedHuntingTarget + "...");
                if (currentHuntingHandler.prepareEquipment()) {
                    currentState = State.WALKING_TO_HUNTING_AREA;
                }
                break;
                
            case WALKING_TO_HUNTING_AREA:
                Logger.log("Walking to " + selectedHuntingTarget + " hunting area...");
                Logger.log("Checking for wing before walking: " + Inventory.contains("Moonlight moth wing"));
                
                // Check for special items before walking to hunting area
                if (selectedHuntingTarget.equals("Moonlight Moths") && Inventory.contains("Moonlight moth wing")) {
                    Logger.log("WING DETECTED IN WALKING STATE! Should go to guide instead of hunting area...");
                    currentState = State.GOING_TO_GUIDE;
                    break;
                }
                
                if (selectedHuntingTarget.equals("Sunlight Antelope") && Inventory.contains("Antelope hoof shard")) {
                    Logger.log("HOOF SHARD DETECTED IN WALKING STATE! Should go to guide instead of hunting area...");
                    currentState = State.GOING_TO_GUIDE;
                    break;
                }
                
                if (selectedHuntingTarget.equals("Tecu Salamanders") && Inventory.contains("Salamander claw")) {
                    Logger.log("CLAW DETECTED IN WALKING STATE! Should go to guide instead of hunting area...");
                    currentState = State.GOING_TO_GUIDE;
                    break;
                }
                
                if (selectedHuntingTarget.equals("Red Salamanders") && Inventory.contains("Red salamander claw")) {
                    Logger.log("RED CLAW DETECTED IN WALKING STATE! Should go to guide instead of hunting area...");
                    currentState = State.GOING_TO_GUIDE;
                    break;
                }
                
                if (selectedHuntingTarget.equals("Moonlight Antelopes") && Inventory.contains("Antelope hoof shard")) {
                    Logger.log("MOONLIGHT HOOF SHARD DETECTED IN WALKING STATE! Should go to guide instead of hunting area...");
                    currentState = State.GOING_TO_GUIDE;
                    break;
                }
                
                // Check if inventory is full before walking to hunting area
                if (shouldBank()) {
                    Logger.log("Inventory is full (" + Inventory.fullSlotCount() + "/28), need to bank before hunting");
                    currentState = State.BANKING_LOOT;
                    break;
                }
                
                Logger.log("Actually walking to hunting area now...");
                if (currentHuntingHandler.walkToHuntingArea()) {
                    Logger.log("Successfully reached hunting area, switching to hunting state");
                    currentState = State.HUNTING;
                } else {
                    Logger.log("Failed to reach hunting area, staying in walking state");
                }
                break;
                
            case HUNTING:
                Logger.log("Hunting " + selectedHuntingTarget + "...");
                Logger.log("Current inventory contains moonlight moth wing: " + Inventory.contains("Moonlight moth wing"));
                Logger.log("Current inventory contains antelope hoof shard: " + Inventory.contains("Antelope hoof shard"));
                Logger.log("Current inventory contains salamander claw: " + Inventory.contains("Salamander claw"));
                Logger.log("Current inventory contains red salamander claw: " + Inventory.contains("Red salamander claw"));
                
                // Special check for moonlight moth wing FIRST
                if (selectedHuntingTarget.equals("Moonlight Moths") && Inventory.contains("Moonlight moth wing")) {
                    Logger.log("WING DETECTED! Moonlight moth wing obtained! Going to guide...");
                    currentState = State.GOING_TO_GUIDE;
                    break;
                }
                
                // Special check for antelope hoof shard
                if (selectedHuntingTarget.equals("Sunlight Antelope") && Inventory.contains("Antelope hoof shard")) {
                    Logger.log("HOOF SHARD DETECTED! Antelope hoof shard obtained! Going to guide...");
                    currentState = State.GOING_TO_GUIDE;
                    break;
                }
                
                // Special check for salamander claw
                if (selectedHuntingTarget.equals("Tecu Salamanders") && Inventory.contains("Salamander claw")) {
                    Logger.log("CLAW DETECTED! Salamander claw obtained! Going to guide...");
                    currentState = State.GOING_TO_GUIDE;
                    break;
                }
                
                // Special check for red salamander claw
                if (selectedHuntingTarget.equals("Red Salamanders") && Inventory.contains("Red salamander claw")) {
                    Logger.log("RED CLAW DETECTED! Red salamander claw obtained! Going to guide...");
                    currentState = State.GOING_TO_GUIDE;
                    break;
                }
                
                // Special check for moonlight antelope hoof shard
                if (selectedHuntingTarget.equals("Moonlight Antelopes") && Inventory.contains("Antelope hoof shard")) {
                    Logger.log("MOONLIGHT HOOF SHARD DETECTED! Antelope hoof shard obtained! Going to guide...");
                    currentState = State.GOING_TO_GUIDE;
                    break;
                }
                
                boolean huntResult = currentHuntingHandler.hunt();
                Logger.log("Hunt method returned: " + huntResult);
                
                // Check again after hunt() in case wing/hoof shard was just obtained
                if (selectedHuntingTarget.equals("Moonlight Moths") && Inventory.contains("Moonlight moth wing")) {
                    Logger.log("WING DETECTED AFTER HUNT! Moonlight moth wing obtained! Going to guide...");
                    currentState = State.GOING_TO_GUIDE;
                    break;
                }
                
                if (selectedHuntingTarget.equals("Sunlight Antelope") && Inventory.contains("Antelope hoof shard")) {
                    Logger.log("HOOF SHARD DETECTED AFTER HUNT! Antelope hoof shard obtained! Going to guide...");
                    currentState = State.GOING_TO_GUIDE;
                    break;
                }
                
                if (selectedHuntingTarget.equals("Tecu Salamanders") && Inventory.contains("Salamander claw")) {
                    Logger.log("CLAW DETECTED AFTER HUNT! Salamander claw obtained! Going to guide...");
                    currentState = State.GOING_TO_GUIDE;
                    break;
                }
                
                if (selectedHuntingTarget.equals("Red Salamanders") && Inventory.contains("Red salamander claw")) {
                    Logger.log("RED CLAW DETECTED AFTER HUNT! Red salamander claw obtained! Going to guide...");
                    currentState = State.GOING_TO_GUIDE;
                    break;
                }
                
                if (selectedHuntingTarget.equals("Moonlight Antelopes") && Inventory.contains("Antelope hoof shard")) {
                    Logger.log("MOONLIGHT HOOF SHARD DETECTED AFTER HUNT! Antelope hoof shard obtained! Going to guide...");
                    currentState = State.GOING_TO_GUIDE;
                    break;
                }
                
                if (huntResult) {
                    // Check if inventory is full or needs banking
                    if (shouldBank()) {
                        Logger.log("Banking needed, switching to banking state");
                        currentState = State.BANKING_LOOT;
                    } else {
                        Logger.log("Hunt successful, staying in hunting state");
                    }
                } else {
                    Logger.log("Hunt returned false - this should have been caught by wing detection above");
                }
                break;
                
            case BANKING_LOOT:
                Logger.log("Banking loot from " + selectedHuntingTarget + "...");
                
                // Check for special items before banking (shouldn't bank if we have completion items!)
                if (selectedHuntingTarget.equals("Moonlight Moths") && Inventory.contains("Moonlight moth wing")) {
                    Logger.log("WING DETECTED IN BANKING STATE! Going to guide instead of banking...");
                    currentState = State.GOING_TO_GUIDE;
                    break;
                }
                
                if (selectedHuntingTarget.equals("Sunlight Antelope") && Inventory.contains("Antelope hoof shard")) {
                    Logger.log("HOOF SHARD DETECTED IN BANKING STATE! Going to guide instead of banking...");
                    currentState = State.GOING_TO_GUIDE;
                    break;
                }
                
                if (selectedHuntingTarget.equals("Tecu Salamanders") && Inventory.contains("Salamander claw")) {
                    Logger.log("CLAW DETECTED IN BANKING STATE! Going to guide instead of banking...");
                    currentState = State.GOING_TO_GUIDE;
                    break;
                }
                
                if (selectedHuntingTarget.equals("Red Salamanders") && Inventory.contains("Red salamander claw")) {
                    Logger.log("RED CLAW DETECTED IN BANKING STATE! Going to guide instead of banking...");
                    currentState = State.GOING_TO_GUIDE;
                    break;
                }
                
                if (selectedHuntingTarget.equals("Moonlight Antelopes") && Inventory.contains("Antelope hoof shard")) {
                    Logger.log("MOONLIGHT HOOF SHARD DETECTED IN BANKING STATE! Going to guide instead of banking...");
                    currentState = State.GOING_TO_GUIDE;
                    break;
                }
                
                if (currentHuntingHandler.bankLoot()) {
                    Logger.log("Banking completed, going back to hunting area");
                    currentState = State.WALKING_TO_HUNTING_AREA;
                } else {
                    Logger.log("Banking failed, staying in banking state");
                }
                break;
                
            case GOING_TO_GUIDE:
                Logger.log("Going to Guild Hunter Wolf with obtained item...");
                
                // Handle moonlight moths
                if (selectedHuntingTarget.equals("Moonlight Moths")) {
                    // Verify we still have the wing
                    if (!Inventory.contains("Moonlight moth wing")) {
                        Logger.log("ERROR: Moonlight moth wing no longer in inventory! Returning to hunting...");
                        currentState = State.WALKING_TO_HUNTING_AREA;
                        break;
                    }
                    
                    if (currentHuntingHandler instanceof MoonlightMothHandler) {
                        MoonlightMothHandler mothHandler = (MoonlightMothHandler) currentHuntingHandler;
                        Logger.log("Attempting to go to guide with moonlight moth wing...");
                        if (mothHandler.goToGuide()) {
                            Logger.log("Successfully talked to Guild Hunter Wolf! Task completed.");
                            currentState = State.IDLE;
                            stopBot();
                        } else {
                            Logger.log("Failed to reach or talk to Guild Hunter Wolf, retrying in next loop...");
                        }
                    }
                }
                // Handle sunlight antelope
                else if (selectedHuntingTarget.equals("Sunlight Antelope")) {
                    // Verify we still have the hoof shard
                    if (!Inventory.contains("Antelope hoof shard")) {
                        Logger.log("ERROR: Antelope hoof shard no longer in inventory! Returning to hunting...");
                        currentState = State.WALKING_TO_HUNTING_AREA;
                        break;
                    }
                    
                    if (currentHuntingHandler instanceof SunlightAntelopeHandler) {
                        SunlightAntelopeHandler antelopeHandler = (SunlightAntelopeHandler) currentHuntingHandler;
                        Logger.log("Attempting to go to guide with antelope hoof shard...");
                        if (antelopeHandler.goToGuide()) {
                            Logger.log("Successfully talked to Guild Hunter Wolf! Task completed.");
                            currentState = State.IDLE;
                            stopBot();
                        } else {
                            Logger.log("Failed to reach or talk to Guild Hunter Wolf, retrying in next loop...");
                        }
                    }
                }
                // Handle tecu salamanders
                else if (selectedHuntingTarget.equals("Tecu Salamanders")) {
                    // Verify we still have the salamander claw
                    if (!Inventory.contains("Salamander claw")) {
                        Logger.log("ERROR: Salamander claw no longer in inventory! Returning to hunting...");
                        currentState = State.WALKING_TO_HUNTING_AREA;
                        break;
                    }
                    
                    if (currentHuntingHandler instanceof TecuSalamanderHandler) {
                        TecuSalamanderHandler salamanderHandler = (TecuSalamanderHandler) currentHuntingHandler;
                        Logger.log("Attempting to go to guide with salamander claw...");
                        if (salamanderHandler.goToGuide()) {
                            Logger.log("Successfully talked to Guild Hunter Wolf! Task completed.");
                            currentState = State.IDLE;
                            stopBot();
                        } else {
                            Logger.log("Failed to reach or talk to Guild Hunter Wolf, retrying in next loop...");
                        }
                    }
                }
                // Handle red salamanders
                else if (selectedHuntingTarget.equals("Red Salamanders")) {
                    // Verify we still have the red salamander claw
                    if (!Inventory.contains("Red salamander claw")) {
                        Logger.log("ERROR: Red salamander claw no longer in inventory! Returning to hunting...");
                        currentState = State.WALKING_TO_HUNTING_AREA;
                        break;
                    }
                    
                    if (currentHuntingHandler instanceof RedSalamanderHandler) {
                        RedSalamanderHandler redSalamanderHandler = (RedSalamanderHandler) currentHuntingHandler;
                        Logger.log("Attempting to go to guide with red salamander claw...");
                        if (redSalamanderHandler.goToGuide()) {
                            Logger.log("Successfully talked to Guild Hunter Wolf! Task completed.");
                            currentState = State.IDLE;
                            stopBot();
                        } else {
                            Logger.log("Failed to reach or talk to Guild Hunter Wolf, retrying in next loop...");
                        }
                    }
                }
                // Handle moonlight antelopes
                else if (selectedHuntingTarget.equals("Moonlight Antelopes")) {
                    // Verify we still have the antelope hoof shard
                    if (!Inventory.contains("Antelope hoof shard")) {
                        Logger.log("ERROR: Antelope hoof shard no longer in inventory! Returning to hunting...");
                        currentState = State.WALKING_TO_HUNTING_AREA;
                        break;
                    }
                    
                    if (currentHuntingHandler instanceof MoonlightAntelopeHandler) {
                        MoonlightAntelopeHandler moonlightAntelopeHandler = (MoonlightAntelopeHandler) currentHuntingHandler;
                        Logger.log("Attempting to go to guide with antelope hoof shard...");
                        if (moonlightAntelopeHandler.goToGuide()) {
                            Logger.log("Successfully talked to Guild Hunter Wolf! Task completed.");
                            currentState = State.IDLE;
                            stopBot();
                        } else {
                            Logger.log("Failed to reach or talk to Guild Hunter Wolf, retrying in next loop...");
                        }
                    }
                }
                else {
                    Logger.log("Invalid state - unknown hunting target or wrong handler type");
                    currentState = State.IDLE;
                }
                break;
        }
        
        return randomSleep(100, 500);
    }
    
    private boolean shouldBank() {
        // Check if health is critically low
        int currentHealth = Combat.getHealthPercent();
        if (currentHealth <= 10) {
            Logger.log("CRITICAL: Health is very low (" + currentHealth + "%)! Need to bank for food immediately.");
            return true;
        }
        
        // Check if inventory is getting full (leave some space for new catches)
        return Inventory.fullSlotCount() >= 24;
    }

    @Override
    public void onExit() {
        Logger.log("Rumors Hunter stopped!");
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