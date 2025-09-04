import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.container.impl.equipment.Equipment;
import org.dreambot.api.methods.container.impl.equipment.EquipmentSlot;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.combat.Combat;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;

public class SunlightAntelopeHandler implements HuntingHandler {
    
    // Sunlight Antelope hunting area coordinates (Kebos Lowlands)
    private static final int HUNTING_AREA_X = 1752;
    private static final int HUNTING_AREA_Y = 3012;
    
    // Hunting Guild bank coordinates
    private static final int BANK_X = 1543;
    private static final int BANK_Y = 3040;
    
    // Required items for Sunlight Antelope pitfall hunting
    private static final String[] REQUIRED_ITEMS = {
        "Dragon axe",
        "Hunter cape",
        "Knife",
        "Teasing stick",
        "Logs"
    };
    
    // Valid axes for cutting logs
    private static final String[] VALID_AXES = {
        "Dragon axe", "Rune axe", "Adamant axe", "Mithril axe", "Steel axe", "Iron axe", "Bronze axe"
    };
    
    // Valid knives
    private static final String[] VALID_KNIVES = {
        "Dragon knife", "Rune knife", "Adamant knife", "Mithril knife", "Steel knife", "Iron knife", "Bronze knife", "Knife"
    };
    
    private static final int REQUIRED_LEVEL = 72;
    
    @Override
    public boolean checkRequirements() {
        Logger.log("Checking Sunlight Antelope hunting requirements...");
        
        // Check hunter level
        int currentLevel = Skills.getRealLevel(Skill.HUNTER);
        if (currentLevel < REQUIRED_LEVEL) {
            Logger.log("Hunter level too low: " + currentLevel + "/" + REQUIRED_LEVEL);
            return false;
        }
        
        Logger.log("Hunter level requirement met: " + currentLevel + "/" + REQUIRED_LEVEL);
        return true;
    }
    
    @Override
    public boolean prepareEquipment() {
        Logger.log("Preparing equipment for Sunlight Antelope pitfall hunting...");
        
        // Check if we have an axe
        boolean hasAxe = false;
        for (String axe : VALID_AXES) {
            if (Inventory.contains(axe)) {
                hasAxe = true;
                Logger.log("Found " + axe + " in inventory");
                break;
            }
        }
        
        if (!hasAxe) {
            Logger.log("Missing axe - need to get one from bank");
            if (walkToBank()) {
                return bankForEquipment();
            }
            return false;
        }
        
        // Check if we have a knife
        boolean hasKnife = false;
        for (String knife : VALID_KNIVES) {
            if (Inventory.contains(knife)) {
                hasKnife = true;
                Logger.log("Found " + knife + " in inventory");
                break;
            }
        }
        
        if (!hasKnife) {
            Logger.log("Missing knife - need to get one from bank");
            if (walkToBank()) {
                return bankForEquipment();
            }
            return false;
        }
        
        // Check if we have teasing stick
        if (!Inventory.contains("Teasing stick")) {
            Logger.log("Missing Teasing stick - need to get one from bank");
            if (walkToBank()) {
                return bankForEquipment();
            }
            return false;
        }
        
        // Check if we have hunter cape equipped
        if (!Equipment.contains("Hunter cape")) {
            Logger.log("Missing Hunter cape (equipped) - need to get one from bank and equip it");
            if (walkToBank()) {
                return bankForEquipment();
            }
            return false;
        }
        
        Logger.log("Equipment check complete - ready for pitfall hunting");
        return true;
    }
    
    private boolean bankForEquipment() {
        Logger.log("Getting equipment from bank...");
        
        if (!Bank.isOpen()) {
            if (Bank.open()) {
                Sleep.sleepUntil(Bank::isOpen, 5000);
            } else {
                Logger.log("Failed to open bank");
                return false;
            }
        }
        
        if (Bank.isOpen()) {
            // Withdraw axe if we don't have one
            boolean hasAxe = false;
            for (String axe : VALID_AXES) {
                if (Inventory.contains(axe)) {
                    hasAxe = true;
                    break;
                }
            }
            
            if (!hasAxe) {
                for (String axe : VALID_AXES) {
                    if (Bank.contains(axe)) {
                        Logger.log("Withdrawing " + axe + "...");
                        Bank.withdraw(axe, 1);
                        Sleep.sleepUntil(() -> Inventory.contains(axe), 3000);
                        hasAxe = true;
                        break;
                    }
                }
                if (!hasAxe) {
                    Logger.log("No axe found in bank! Please get one.");
                    return false;
                }
            }
            
            // Withdraw knife if we don't have one
            boolean hasKnife = false;
            for (String knife : VALID_KNIVES) {
                if (Inventory.contains(knife)) {
                    hasKnife = true;
                    break;
                }
            }
            
            if (!hasKnife) {
                for (String knife : VALID_KNIVES) {
                    if (Bank.contains(knife)) {
                        Logger.log("Withdrawing " + knife + "...");
                        Bank.withdraw(knife, 1);
                        Sleep.sleepUntil(() -> Inventory.contains(knife), 3000);
                        hasKnife = true;
                        break;
                    }
                }
                if (!hasKnife) {
                    Logger.log("No knife found in bank! Please get one.");
                    return false;
                }
            }
            
            // Withdraw Teasing stick if we don't have one
            if (!Inventory.contains("Teasing stick")) {
                if (Bank.contains("Teasing stick")) {
                    Logger.log("Withdrawing Teasing stick...");
                    Bank.withdraw("Teasing stick", 1);
                    Sleep.sleepUntil(() -> Inventory.contains("Teasing stick"), 3000);
                } else {
                    Logger.log("No Teasing stick found in bank! Please get one from the Hunting Guild.");
                    return false;
                }
            }
            
            // Withdraw and equip Hunter cape if not equipped
            if (!Equipment.contains("Hunter cape")) {
                if (Bank.contains("Hunter cape")) {
                    Logger.log("Withdrawing Hunter cape...");
                    Bank.withdraw("Hunter cape", 1);
                    Sleep.sleepUntil(() -> Inventory.contains("Hunter cape"), 3000);
                    
                    // Equip the hunter cape
                    if (Inventory.contains("Hunter cape")) {
                        Logger.log("Equipping Hunter cape...");
                        Inventory.interact("Hunter cape", "Wear");
                        Sleep.sleepUntil(() -> Equipment.contains("Hunter cape"), 3000);
                    }
                } else {
                    Logger.log("No Hunter cape found in bank! Please get one from the Hunting Guild.");
                    return false;
                }
            }
            
            Bank.close();
            Sleep.sleepUntil(() -> !Bank.isOpen(), 3000);
            
            Logger.log("Equipment obtained from bank");
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean walkToHuntingArea() {
        Logger.log("Walking to Sunlight Antelope hunting area...");
        
        while (!isAtHuntingArea()) {
            if (Walking.shouldWalk(8)) {
                Logger.log("Walking to Sunlight Antelope area at (" + HUNTING_AREA_X + ", " + HUNTING_AREA_Y + ")");
                if (Walking.walk(HUNTING_AREA_X, HUNTING_AREA_Y)) {
                    Sleep.sleep(1500, 2200);
                } else {
                    Logger.log("Walk failed, retrying...");
                    Sleep.sleep(1000, 1500);
                }
            } else {
                Sleep.sleep(600, 1000);
            }
        }
        
        Logger.log("Arrived at Sunlight Antelope hunting area");
        return true;
    }
    
    @Override
    public boolean hunt() {
        Logger.log("Starting Sunlight Antelope pitfall hunting...");
        
        // Check if we have hoof shard - if so, go to guild basement
        if (Inventory.contains("Antelope hoof shard")) {
            Logger.log("Found hoof shard! Going to guild basement to complete task.");
            return false; // Signal to change state to go to guide
        }
        
        // Check if health is critically low - if so, go bank immediately for food
        int currentHealth = Combat.getHealthPercent();
        if (currentHealth <= 10) {
            Logger.log("CRITICAL: Health is very low (" + currentHealth + "%)! Going to bank immediately for food.");
            return bankLoot();
        }
        
        // Check if inventory is getting full - if so, go bank at Hunting Guild
        if (Inventory.fullSlotCount() >= 24) {
            Logger.log("Inventory is getting full (" + Inventory.fullSlotCount() + "/28 slots)! Going to Hunting Guild bank to deposit loot.");
            return bankLoot();
        }
        
        // PRIORITY 1: Check if player is in combat or being attacked (aggressive antelope) - jump immediately!
        boolean inCombat = Players.getLocal().isInCombat();
        boolean healthBarVisible = Players.getLocal().isHealthBarVisible();
        boolean beingAttacked = Players.getLocal().isInteractedWith();
        
        // Debug logging every cycle to track combat status
        Logger.log("Combat status check - InCombat: " + inCombat + ", HealthBar: " + healthBarVisible + ", BeingAttacked: " + beingAttacked);
        
        if (inCombat || healthBarVisible || beingAttacked) {
            Logger.log("PRIORITY 1 TRIGGERED: Player is in combat/being attacked! Looking for spiked pit to jump over immediately!");
            
            GameObject spikedPit = GameObjects.closest("Spiked pit");
            if (spikedPit != null && spikedPit.exists()) {
                Logger.log("Found spiked pit at distance: " + spikedPit.distance() + " - attempting to jump!");
                Logger.log("Spiked pit actions: " + java.util.Arrays.toString(spikedPit.getActions()));
                
                // Try multiple possible jump actions
                String[] jumpActions = {"Jump-over", "Jump over", "Jump", "Leap-over", "Leap over", "Leap"};
                boolean jumped = false;
                
                for (String action : jumpActions) {
                    if (spikedPit.hasAction(action)) {
                        Logger.log("Trying to jump with action: " + action);
                        if (spikedPit.interact(action)) {
                            Logger.log("Successfully started jump with action: " + action);
                            jumped = true;
                            break;
                        } else {
                            Logger.log("Failed to interact with action: " + action);
                        }
                    }
                }
                
                if (jumped) {
                    Logger.log("Jumped over spiked pit while being chased!");
                    
                    // Keep jumping with small delays until trap is dismantled
                    int jumpAttempts = 0;
                    int maxAttempts = 10;
                    
                    while (jumpAttempts < maxAttempts) {
                        // Wait a bit for trap to potentially trigger
                        Sleep.sleep(1500, 2500);
                        
                        // Check if trap collapsed (caught something)
                        GameObject collapsedTrap = GameObjects.closest("Collapsed trap");
                        if (collapsedTrap != null && collapsedTrap.exists()) {
                            Logger.log("Trap collapsed! Dismantling to collect loot...");
                            if (collapsedTrap.interact("Dismantle")) {
                                Sleep.sleepUntil(() -> !Players.getLocal().isAnimating(), 5000);
                                Logger.log("Successfully dismantled trap and collected loot");
                                return true;
                            } else {
                                Logger.log("Failed to dismantle collapsed trap, trying again...");
                                Sleep.sleep(1000, 2000);
                                continue;
                            }
                        }
                        
                        // Check if spiked pit still exists to jump again
                        GameObject currentSpikedPit = GameObjects.closest("Spiked pit");
                        if (currentSpikedPit != null && currentSpikedPit.exists()) {
                            jumpAttempts++;
                            Logger.log("Trap didn't collapse yet, jumping again... (Attempt " + jumpAttempts + "/" + maxAttempts + ")");
                            
                            // Try to jump again with small delay
                            for (String action : jumpActions) {
                                if (currentSpikedPit.hasAction(action) && currentSpikedPit.interact(action)) {
                                    Logger.log("Jumped again with action: " + action);
                                    Sleep.sleep(800, 1500); // Small delay between jumps
                                    break;
                                }
                            }
                        } else {
                            Logger.log("Spiked pit disappeared - trap may have been triggered");
                            break;
                        }
                    }
                    
                    Logger.log("Finished jumping attempts");
                    return true;
                } else {
                    Logger.log("FAILED: Could not jump over spiked pit with any action! Distance: " + spikedPit.distance());
                    Logger.log("Available actions: " + java.util.Arrays.toString(spikedPit.getActions()));
                }
            } else {
                Logger.log("In combat but no spiked pit found!");
            }
        }
        
        // PRIORITY 2: Bury big bones if we have any
        if (Inventory.contains("Big bones")) {
            buryBigBones();
            return true; // Focus on one task at a time
        }
        
        // PRIORITY 3: Dismantle collapsed trap if exists
        GameObject collapsedTrap = GameObjects.closest("Collapsed trap");
        if (collapsedTrap != null && collapsedTrap.exists()) {
            Logger.log("Found collapsed trap, dismantling...");
            if (collapsedTrap.interact("Dismantle")) {
                Sleep.sleepUntil(() -> !Players.getLocal().isAnimating(), 5000);
                Logger.log("Dismantled collapsed trap");
            }
            return true; // Focus on one task at a time
        }
        
        // PRIORITY 4: Check for existing spiked pit - if exists, handle it (don't cut logs)
        GameObject spikedPit = GameObjects.closest("Spiked pit");
        if (spikedPit != null && spikedPit.exists()) {
            Logger.log("Found existing spiked pit, handling trap...");
            return handleExistingTrap(spikedPit);
        }
        
        // PRIORITY 5: If no spiked pit exists and no logs, cut logs
        if (!Inventory.contains("Logs")) {
            Logger.log("No spiked pit found and no logs in inventory, need to cut logs...");
            if (cutLogs()) {
                Logger.log("Successfully cut logs, will set pitfall next cycle");
            }
            return true; // Focus on one task at a time - set trap next cycle
        }
        
        // PRIORITY 6: If we have logs but no spiked pit, set up new pitfall trap
        if (Inventory.contains("Logs")) {
            Logger.log("Have logs and no spiked pit - setting up new pitfall trap...");
            if (setPitfall()) {
                Logger.log("Successfully set pitfall trap");
            }
            return true; // Focus on one task at a time
        }
        
        return true;
    }
    
    @Override
    public boolean bankLoot() {
        Logger.log("Banking Sunlight Antelope loot...");
        
        if (walkToBank()) {
            if (!Bank.isOpen()) {
                if (Bank.open()) {
                    Sleep.sleepUntil(Bank::isOpen, 5000);
                } else {
                    Logger.log("Failed to open bank");
                    return false;
                }
            }
            
            if (Bank.isOpen()) {
                // Deposit all items except required equipment and items
                Logger.log("Depositing all items except required ones...");
                
                // Create comprehensive list of items to keep
                String[] keepItems = new String[VALID_AXES.length + VALID_KNIVES.length + 3];
                System.arraycopy(VALID_AXES, 0, keepItems, 0, VALID_AXES.length);
                System.arraycopy(VALID_KNIVES, 0, keepItems, VALID_AXES.length, VALID_KNIVES.length);
                keepItems[VALID_AXES.length + VALID_KNIVES.length] = "Teasing stick";
                keepItems[VALID_AXES.length + VALID_KNIVES.length + 1] = "Lobster";
                keepItems[VALID_AXES.length + VALID_KNIVES.length + 2] = "Antelope hoof shard"; // Don't deposit the goal item!
                
                Logger.log("Items to keep: " + java.util.Arrays.toString(keepItems));
                
                // First, deposit everything else
                Bank.depositAllExcept(keepItems);
                Sleep.sleep(1500, 2500);
                
                Logger.log("Deposited all unnecessary items");
                
                // Check health and get lobsters if needed
                int currentHealth = Combat.getHealthPercent();
                int maxHealth = Skills.getRealLevel(Skill.HITPOINTS);
                
                Logger.log("Current health: " + currentHealth + "%");
                
                if (currentHealth < 50) {
                    Logger.log("Health below 50%! Getting lobsters to heal...");
                    
                    // Calculate how many lobsters needed (lobster heals 12hp)
                    int currentHp = (currentHealth * maxHealth) / 100;
                    int hpToHeal = maxHealth - currentHp;
                    int lobstersNeeded = Math.max(1, (hpToHeal + 11) / 12); // Round up, minimum 1
                    
                    Logger.log("Need " + lobstersNeeded + " lobsters to heal " + hpToHeal + " hp");
                    
                    if (Bank.contains("Lobster")) {
                        Logger.log("Withdrawing " + lobstersNeeded + " lobsters...");
                        Bank.withdraw("Lobster", lobstersNeeded);
                        Sleep.sleepUntil(() -> Inventory.contains("Lobster"), 3000);
                        
                        // Eat lobsters to heal to full health
                        Logger.log("Eating lobsters to restore to full health...");
                        while (Inventory.contains("Lobster") && Combat.getHealthPercent() < 100) {
                            if (Inventory.interact("Lobster", "Eat")) {
                                Logger.log("Eating lobster... Health: " + Combat.getHealthPercent() + "%");
                                Sleep.sleepUntil(() -> !Players.getLocal().isAnimating(), 3000);
                                Sleep.sleep(600, 1200); // Wait between eating
                            } else {
                                Logger.log("Failed to eat lobster");
                                break;
                            }
                        }
                        
                        Logger.log("Health restored to full: " + Combat.getHealthPercent() + "%");
                    } else {
                        Logger.log("WARNING: No lobsters in bank! Health is low.");
                    }
                }
                
                Bank.close();
                Sleep.sleepUntil(() -> !Bank.isOpen(), 3000);
                
                Logger.log("Banking complete");
                return true;
            }
        }
        
        return false;
    }
    
    private boolean walkToBank() {
        Logger.log("Walking to bank...");
        
        int playerX = Players.getLocal().getX();
        int playerY = Players.getLocal().getY();
        
        // Check if already at bank
        if (Math.abs(playerX - BANK_X) <= 10 && Math.abs(playerY - BANK_Y) <= 10) {
            Logger.log("Already at bank");
            return true;
        }
        
        // Walk to bank
        if (Walking.walk(BANK_X, BANK_Y)) {
            Logger.log("Walking to bank at (" + BANK_X + ", " + BANK_Y + ")");
            Sleep.sleepUntil(() -> !Players.getLocal().isMoving() || isNearBank(), 15000);
            
            if (isNearBank()) {
                Logger.log("Arrived at bank");
                return true;
            } else {
                Logger.log("Failed to reach bank");
                return false;
            }
        } else {
            Logger.log("Failed to start walking to bank");
            return false;
        }
    }
    
    private boolean isAtHuntingArea() {
        int playerX = Players.getLocal().getX();
        int playerY = Players.getLocal().getY();
        return Math.abs(playerX - HUNTING_AREA_X) <= 15 && Math.abs(playerY - HUNTING_AREA_Y) <= 15;
    }
    
    private boolean isNearBank() {
        int playerX = Players.getLocal().getX();
        int playerY = Players.getLocal().getY();
        return Math.abs(playerX - BANK_X) <= 10 && Math.abs(playerY - BANK_Y) <= 10;
    }
    
    private boolean cutLogs() {
        Logger.log("Looking for trees to cut...");
        
        // Try multiple tree name variations and actions
        String[] treeNames = {"Tree", "Willow", "Oak", "Yew", "Magic tree", "Regular tree"};
        String[] actions = {"Chop down", "Chop", "Cut down", "Cut"};
        
        for (String treeName : treeNames) {
            GameObject tree = GameObjects.closest(treeName);
            if (tree != null && tree.exists()) {
                Logger.log("Found " + treeName + ", attempting to cut...");
                
                for (String action : actions) {
                    if (tree.interact(action)) {
                        Logger.log("Successfully started cutting with action: " + action);
                        Logger.log("Patiently waiting for log cutting to complete...");
                        Sleep.sleepUntil(() -> !Players.getLocal().isAnimating() || Inventory.contains("Logs"), 15000);
                        
                        if (Inventory.contains("Logs")) {
                            Logger.log("Successfully obtained logs (1 set only)");
                            return true;
                        }
                        break; // Try next tree if this one didn't give logs
                    } else {
                        Logger.log("Failed to interact with " + treeName + " using action: " + action);
                        Sleep.sleep(1000, 2000); // Delay between action attempts
                    }
                }
                Sleep.sleep(1500, 2500); // Delay between different trees
            }
        }
        
        // Fallback: try generic tree search
        GameObject tree = GameObjects.closest(obj -> obj != null && 
            (obj.getName().toLowerCase().contains("tree") || 
             obj.getName().toLowerCase().contains("willow") ||
             obj.getName().toLowerCase().contains("oak")));
        
        if (tree != null && tree.exists()) {
            Logger.log("Found generic tree: " + tree.getName() + ", attempting to cut...");
            String[] fallbackActions = {"Chop down", "Chop", "Cut down", "Cut"};
            
            for (String action : fallbackActions) {
                if (tree.interact(action)) {
                    Logger.log("Started cutting with fallback action: " + action);
                    Logger.log("Patiently waiting for log cutting to complete...");
                    Sleep.sleepUntil(() -> !Players.getLocal().isAnimating() || Inventory.contains("Logs"), 15000);
                    
                    if (Inventory.contains("Logs")) {
                        Logger.log("Successfully obtained logs from " + tree.getName() + " (1 set only)");
                        return true;
                    }
                    break;
                } else {
                    Logger.log("Failed to interact with " + tree.getName() + " using fallback action: " + action);
                    Sleep.sleep(1000, 2000); // Delay between fallback action attempts
                }
            }
        }
        
        Logger.log("No suitable trees found or failed to cut logs");
        return false;
    }
    
    private boolean setPitfall() {
        Logger.log("Setting pitfall trap...");
        
        if (!Inventory.contains("Logs")) {
            Logger.log("No logs available for pitfall");
            return false;
        }
        
        // Look for a pit to set the trap on
        GameObject pit = GameObjects.closest("Pit");
        if (pit != null && pit.exists()) {
            Logger.log("Found pit, setting trap...");
            if (pit.interact("Trap")) {
                Logger.log("Setting pitfall trap on pit...");
                Sleep.sleepUntil(() -> !Players.getLocal().isAnimating(), 5000);
                
                // Check if spiked pit was created
                GameObject spikedPit = GameObjects.closest("Spiked pit");
                if (spikedPit != null && spikedPit.exists()) {
                    Logger.log("Successfully set pitfall trap");
                    return true;
                } else {
                    Logger.log("Failed to create spiked pit");
                    return false;
                }
            } else {
                Logger.log("Failed to interact with pit");
                return false;
            }
        } else {
            Logger.log("No pit found to set trap on");
            return false;
        }
    }
    
    private boolean handleExistingTrap(GameObject spikedPit) {
        Logger.log("Handling existing trap - looking for antelopes to tease...");
        
        // Look for Sunlight Antelope to tease (within 10 tiles of trap)
        NPC sunlightAntelope = NPCs.closest(npc -> 
            npc.getName() != null && 
            npc.getName().equals("Sunlight antelope") &&
            npc.getTile().distance(spikedPit.getTile()) <= 10
        );
        if (sunlightAntelope != null && sunlightAntelope.exists()) {
            Logger.log("Found Sunlight Antelope, patiently using teasing stick...");
            
            // Use teasing stick on the antelope
            if (Inventory.interact("Teasing stick", "Use")) {
                Sleep.sleep(800, 1500); // More patient timing
                if (sunlightAntelope.interact("Use")) {
                    Logger.log("Teasing Sunlight Antelope with teasing stick...");
                    Logger.log("Patiently waiting for teasing animation to complete...");
                    Sleep.sleepUntil(() -> !Players.getLocal().isAnimating(), 5000); // More patient timing
                    
                    Logger.log("Teasing complete - antelope should become aggressive soon...");
                    Sleep.sleep(1000, 2000); // Give time for antelope to react
                    
                    // Check combat status immediately after teasing
                    boolean inCombat = Players.getLocal().isInCombat();
                    boolean healthBarVisible = Players.getLocal().isHealthBarVisible();
                    boolean beingAttacked = Players.getLocal().isInteractedWith();
                    
                    Logger.log("Post-tease combat check - InCombat: " + inCombat + ", HealthBar: " + healthBarVisible + ", BeingAttacked: " + beingAttacked);
                    
                    if (inCombat || healthBarVisible || beingAttacked) {
                        Logger.log("Combat detected immediately after teasing! Using existing spiked pit to jump!");
                        if (spikedPit != null && spikedPit.exists()) {
                            Logger.log("Post-tease spiked pit at distance: " + spikedPit.distance() + " - attempting to jump!");
                            Logger.log("Post-tease spiked pit actions: " + java.util.Arrays.toString(spikedPit.getActions()));
                            
                            // Try multiple possible jump actions
                            String[] jumpActions = {"Jump-over", "Jump over", "Jump", "Leap-over", "Leap over", "Leap"};
                            boolean jumped = false;
                            
                            for (String action : jumpActions) {
                                if (spikedPit.hasAction(action)) {
                                    Logger.log("Post-tease trying to jump with action: " + action);
                                    if (spikedPit.interact(action)) {
                                        Logger.log("Post-tease successfully started jump with action: " + action);
                                        jumped = true;
                                        break;
                                    } else {
                                        Logger.log("Post-tease failed to interact with action: " + action);
                                    }
                                }
                            }
                            
                            if (jumped) {
                                Logger.log("Jumped over spiked pit after teasing!");
                                
                                // Keep jumping with small delays until trap is dismantled
                                int jumpAttempts = 0;
                                int maxAttempts = 10;
                                
                                while (jumpAttempts < maxAttempts) {
                                    // Wait a bit for trap to potentially trigger
                                    Sleep.sleep(1500, 2500);
                                    
                                    // Check if trap collapsed (caught something)
                                    GameObject collapsedTrap = GameObjects.closest("Collapsed trap");
                                    if (collapsedTrap != null && collapsedTrap.exists()) {
                                        Logger.log("Post-tease trap collapsed! Dismantling to collect loot...");
                                        if (collapsedTrap.interact("Dismantle")) {
                                            Sleep.sleepUntil(() -> !Players.getLocal().isAnimating(), 5000);
                                            Logger.log("Post-tease successfully dismantled trap and collected loot");
                                            return true;
                                        } else {
                                            Logger.log("Post-tease failed to dismantle collapsed trap, trying again...");
                                            Sleep.sleep(1000, 2000);
                                            continue;
                                        }
                                    }
                                    
                                    // Check if spiked pit still exists to jump again
                                    if (spikedPit != null && spikedPit.exists()) {
                                        jumpAttempts++;
                                        Logger.log("Post-tease trap didn't collapse yet, jumping again... (Attempt " + jumpAttempts + "/" + maxAttempts + ")");
                                        
                                        // Try to jump again with small delay
                                        for (String action : jumpActions) {
                                            if (spikedPit.hasAction(action) && spikedPit.interact(action)) {
                                                Logger.log("Post-tease jumped again with action: " + action);
                                                Sleep.sleep(800, 1500); // Small delay between jumps
                                                break;
                                            }
                                        }
                                    } else {
                                        Logger.log("Post-tease spiked pit disappeared - trap may have been triggered");
                                        break;
                                    }
                                }
                                
                                Logger.log("Post-tease finished jumping attempts");
                                return true;
                            } else {
                                Logger.log("POST-TEASE FAILED: Could not jump over spiked pit with any action! Distance: " + spikedPit.distance());
                                Logger.log("Post-tease available actions: " + java.util.Arrays.toString(spikedPit.getActions()));
                            }
                        } else {
                            Logger.log("Combat detected but spiked pit parameter is null after teasing!");
                        }
                    } else {
                        Logger.log("No immediate combat detected after teasing - will check again next cycle");
                    }
                    
                    return true;
                } else {
                    Logger.log("Failed to use teasing stick on antelope - will try again");
                }
            } else {
                Logger.log("Failed to interact with teasing stick - will try again");
            }
        } else {
            Logger.log("No Sunlight Antelope found nearby, waiting patiently...");
            Sleep.sleep(3000, 5000); // More patient waiting
        }
        
        return true;
    }
    
    private void buryBigBones() {
        Logger.log("Burying big bones...");
        
        while (Inventory.contains("Big bones")) {
            if (Inventory.interact("Big bones", "Bury")) {
                Logger.log("Buried big bones");
                Sleep.sleepUntil(() -> !Players.getLocal().isAnimating(), 3000);
            } else {
                Logger.log("Failed to bury big bones");
                break;
            }
        }
    }
    
    private boolean teleportToHuntingGuild() {
        Logger.log("Teleporting to Hunting Guild with equipped hunter cape...");
        
        if (Equipment.contains("Hunter cape")) {
            if (Equipment.interact(EquipmentSlot.CAPE, "Hunter Guild")) {
                Logger.log("Teleporting to Hunting Guild...");
                Sleep.sleepUntil(() -> !Players.getLocal().isAnimating() && !Players.getLocal().isMoving(), 10000);
                Logger.log("Successfully teleported to Hunting Guild");
                
                // After teleporting, walk to nearest bank
                Logger.log("Walking to nearest bank after teleport...");
                if (walkToBank()) {
                    Logger.log("Successfully reached bank after teleport");
                    return true;
                } else {
                    Logger.log("Failed to reach bank after teleport");
                    return false;
                }
            } else {
                Logger.log("Failed to use hunter cape teleport");
                return false;
            }
        } else {
            Logger.log("No hunter cape equipped");
            return false;
        }
    }
    
    @Override
    public int getRequiredLevel() {
        return REQUIRED_LEVEL;
    }
    
    @Override
    public String[] getRequiredItems() {
        return REQUIRED_ITEMS.clone();
    }
    
    @Override
    public int[] getHuntingAreaLocation() {
        return new int[]{HUNTING_AREA_X, HUNTING_AREA_Y};
    }
    
    @Override
    public int[] getBankLocation() {
        return new int[]{BANK_X, BANK_Y};
    }
    
    public boolean goToGuide() {
        Logger.log("Going to Guild Hunter Wolf (Master) with Antelope hoof shard...");
        
        if (Inventory.contains("Antelope hoof shard")) {
            Logger.log("Antelope hoof shard confirmed in inventory");
            Logger.log("Calling GuildUtils.goToBasementAndTalkToWolf()...");
            boolean result = GuildUtils.goToBasementAndTalkToWolf();
            Logger.log("GuildUtils.goToBasementAndTalkToWolf() returned: " + result);
            return result;
        } else {
            Logger.log("ERROR: No Antelope hoof shard found! Cannot proceed to guide.");
            return false;
        }
    }
}