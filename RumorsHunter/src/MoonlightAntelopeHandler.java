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
import org.dreambot.api.methods.map.Tile;

public class MoonlightAntelopeHandler implements HuntingHandler {
    
    // Moonlight Antelope hunting area coordinates
    private static final int HUNTING_AREA_X = 1562;
    private static final int HUNTING_AREA_Y = 9420;
    
    // Lure location for resetting antelope positions
    private static final Tile LURE_TILE = new Tile(1560, 9420, 0);
    
    // Jump tracking for trap effectiveness
    private int jumpsSinceLastDismantle = 0;
    
    // Hunting Guild bank coordinates
    private static final int BANK_X = 1543;
    private static final int BANK_Y = 3040;
    
    // Required items for Moonlight Antelope pitfall hunting
    private static final String[] REQUIRED_ITEMS = {
        "Hunter cape",
        "Knife",
        "Teasing stick",
        "Willow logs"
    };
    
    // Valid knives
    private static final String[] VALID_KNIVES = {
        "Dragon knife", "Rune knife", "Adamant knife", "Mithril knife", "Steel knife", "Iron knife", "Bronze knife", "Knife"
    };
    
    private static final int REQUIRED_LEVEL = 72;
    
    @Override
    public boolean checkRequirements() {
        Logger.log("Checking Moonlight Antelope hunting requirements...");
        
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
        Logger.log("Preparing equipment for Moonlight Antelope pitfall hunting...");
        
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
            Logger.log("Missing teasing stick - need to get one from bank");
            if (walkToBank()) {
                return bankForEquipment();
            }
            return false;
        }
        
        // Check if we have willow logs
        int logCount = Inventory.count("Willow logs");
        if (logCount < 7) {
            Logger.log("Need more willow logs (" + logCount + "/7) - need to get more from bank");
            if (walkToBank()) {
                return bankForEquipment();
            }
            return false;
        }
        
        Logger.log("Equipment prepared for Moonlight Antelope hunting");
        return true;
    }
    
    private boolean walkToBank() {
        Logger.log("Walking to Hunting Guild bank...");
        
        int attempts = 0;
        while (!isAtBank() && attempts < 10) {
            if (Walking.shouldWalk(8)) {
                Logger.log("Walking to bank at (" + BANK_X + ", " + BANK_Y + ")");
                if (Walking.walk(BANK_X, BANK_Y)) {
                    Sleep.sleep(1500, 2200);
                } else {
                    Logger.log("Walk to bank failed, retrying...");
                    Sleep.sleep(1000, 1500);
                }
            } else {
                Sleep.sleep(600, 1000);
            }
            attempts++;
        }
        
        if (isAtBank()) {
            Logger.log("Arrived at Hunting Guild bank");
            return true;
        } else {
            Logger.log("Failed to reach bank after " + attempts + " attempts");
            return false;
        }
    }
    
    private boolean isAtBank() {
        return Players.getLocal().getTile().distance(new Tile(BANK_X, BANK_Y, 0)) <= 5;
    }
    
    private boolean bankForEquipment() {
        Logger.log("Banking for Moonlight Antelope hunting equipment...");
        
        // Open bank
        if (!Bank.isOpen()) {
            if (Bank.open()) {
                Sleep.sleepUntil(Bank::isOpen, 5000);
            } else {
                Logger.log("Failed to open bank");
                return false;
            }
        }
        
        if (Bank.isOpen()) {
            // Withdraw willow logs if we need more
            int currentLogCount = Inventory.count("Willow logs");
            if (currentLogCount < 7) {
                int logsNeeded = 7 - currentLogCount;
                if (Bank.contains("Willow logs")) {
                    Logger.log("Withdrawing " + logsNeeded + " Willow logs...");
                    Bank.withdraw("Willow logs", logsNeeded);
                    Sleep.sleepUntil(() -> Inventory.count("Willow logs") >= 7, 3000);
                } else {
                    Logger.log("ERROR: No willow logs found in bank!");
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
                    Logger.log("ERROR: No knife found in bank!");
                    return false;
                }
            }
            
            // Withdraw teasing stick if we don't have one
            if (!Inventory.contains("Teasing stick")) {
                if (Bank.contains("Teasing stick")) {
                    Logger.log("Withdrawing Teasing stick...");
                    Bank.withdraw("Teasing stick", 1);
                    Sleep.sleepUntil(() -> Inventory.contains("Teasing stick"), 3000);
                } else {
                    Logger.log("ERROR: No teasing stick found in bank!");
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
        Logger.log("Walking to Moonlight Antelope hunting area...");
        
        while (!isAtHuntingArea()) {
            if (Walking.shouldWalk(8)) {
                Logger.log("Walking to Moonlight Antelope area at (" + HUNTING_AREA_X + ", " + HUNTING_AREA_Y + ")");
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
        
        Logger.log("Arrived at Moonlight Antelope hunting area");
        return true;
    }
    
    private boolean isAtHuntingArea() {
        return Players.getLocal().getTile().distance(new Tile(HUNTING_AREA_X, HUNTING_AREA_Y, 0)) <= 15;
    }
    
    @Override
    public boolean hunt() {
        Logger.log("Hunting Moonlight Antelopes...");
        
        // Check if health is critically low first
        int currentHealth = Combat.getHealthPercent();
        if (currentHealth <= 25) {
            Logger.log("CRITICAL: Health is very low (" + currentHealth + "%)! Banking for food or logging out for safety.");
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
            Logger.log("PRIORITY 1 TRIGGERED: Player is in combat/being attacked!");
            
            // First, check if there's an existing trap we can jump over
            GameObject spikedPit = GameObjects.closest("Spiked pit");
            if (spikedPit != null && spikedPit.exists()) {
                Logger.log("Found existing spiked pit at distance: " + spikedPit.distance() + " - attempting to jump!");
                Logger.log("Spiked pit actions: " + java.util.Arrays.toString(spikedPit.getActions()));
                
                // Try multiple possible jump actions
                String[] jumpActions = {"Jump-over", "Jump over", "Jump", "Leap-over", "Leap over", "Leap"};
                boolean jumped = false;
                
                for (String action : jumpActions) {
                    if (spikedPit.hasAction(action)) {
                        Logger.log("Trying to jump with action: " + action);
                        if (spikedPit.interact(action)) {
                            Logger.log("Successfully started jump with action: " + action);
                            jumpsSinceLastDismantle++; // Track jumps without catches
                            jumped = true;
                            break;
                        } else {
                            Logger.log("Failed to interact with action: " + action);
                        }
                    }
                }
                
                if (!jumped) {
                    Logger.log("Could not jump over spiked pit! Trying to walk away from danger...");
                    // Walk away from current position as emergency measure
                    Walking.walk(Players.getLocal().getTile().translate(-5, -5));
                }
                
                Sleep.sleep(2000, 3000); // Wait for jump/movement animation
                return true; // Return to prioritize safety
            } else {
                // STRATEGIC THINKING: No existing trap, but we're in combat!
                // The aggressive antelope is chasing us - this is the perfect opportunity
                // to set a trap RIGHT NOW so the antelope falls into it while pursuing us!
                Logger.log("STRATEGIC OPPORTUNITY: No trap exists but we're being chased!");
                Logger.log("Setting emergency trap near aggressive antelope to catch it!");
                
                // Look for nearby pit to set emergency trap - ONLY in designated area
                GameObject emergencyPit = GameObjects.closest(pit -> {
                    if (pit.getName() == null) return false;
                    if (!pit.getName().equals("Pit") && !pit.getName().equals("Empty pit")) return false;
                    
                    Tile pitTile = pit.getTile();
                    int pitX = pitTile.getX();
                    int pitY = pitTile.getY();
                    
                    // Check if pit is in the specific area: x=1563, y between 9423-9426
                    // And close enough for emergency setup
                    return pitX == 1563 && pitY >= 9423 && pitY <= 9426 && pit.distance() <= 12;
                });
                
                if (emergencyPit != null && emergencyPit.exists() && Inventory.contains("Willow logs")) {
                    Logger.log("Found nearby pit for emergency trap! Setting trap while being chased...");
                    
                    // Quickly set the trap
                    if (Inventory.interact("Willow logs", "Use")) {
                        Sleep.sleep(300, 500); // Minimal delay - we're in danger!
                        if (emergencyPit.interact("Use")) {
                            Logger.log("EMERGENCY TRAP SET! Antelope should fall into it while chasing us!");
                            Sleep.sleep(1000, 1500); // Brief wait for trap setup
                            
                            // Now we can jump over our newly created trap to escape
                            GameObject newTrap = GameObjects.closest("Spiked pit");
                            if (newTrap != null) {
                                Logger.log("Jumping over our emergency trap to escape!");
                                newTrap.interact("Jump-over");
                                Sleep.sleep(2000, 3000);
                            }
                            return true;
                        }
                    }
                } else if (!Inventory.contains("Willow logs")) {
                    Logger.log("No willow logs for emergency trap! Running away...");
                } else {
                    Logger.log("No suitable pit nearby for emergency trap! Running away...");
                }
                
                // Fallback: Run towards center of hunting area
                Logger.log("Emergency escape - running to safer area!");
                Walking.walk(new Tile(HUNTING_AREA_X, HUNTING_AREA_Y, 0));
                Sleep.sleep(2000, 3000);
                return true;
            }
        }
        
        // PRIORITY 2: Check for collapsed trap first (needs dismantling)
        GameObject collapsedTrap = GameObjects.closest("Collapsed trap");
        if (collapsedTrap != null && collapsedTrap.exists()) {
            Logger.log("Found collapsed trap, dismantling it...");
            if (collapsedTrap.interact("Dismantle")) {
                Logger.log("Dismantling collapsed trap...");
                jumpsSinceLastDismantle = 0; // Reset counter - we got a catch!
                Sleep.sleep(2000, 3000);
                return true;
            }
        }
        
        // PRIORITY 3: Check for existing spiked pit trap
        GameObject existingTrap = GameObjects.closest("Spiked pit");
        if (existingTrap != null && existingTrap.exists()) {
            Logger.log("Found existing spiked pit trap, handling it...");
            return handleExistingTrap(existingTrap);
        }
        
        // PRIORITY 4: If no trap exists, check if we can set one
        if (!Inventory.contains("Teasing stick")) {
            Logger.log("Missing teasing stick! Cannot hunt without it. Need to bank.");
            return false;
        }
        
        // Look for a pit to set trap on - ONLY use pit between (1563,9423) and (1563,9426)
        GameObject trapSpot = GameObjects.closest(pit -> {
            if (pit.getName() == null) return false;
            if (!pit.getName().equals("Pit") && !pit.getName().equals("Empty pit")) return false;
            
            Tile pitTile = pit.getTile();
            int pitX = pitTile.getX();
            int pitY = pitTile.getY();
            
            // Check if pit is in the specific area: x=1563, y between 9423-9426
            return pitX == 1563 && pitY >= 9423 && pitY <= 9426;
        });
        
        if (trapSpot != null && trapSpot.exists()) {
            Logger.log("Found trap setting location: " + trapSpot.getName());
            
            // Check if we have willow logs for the trap
            if (!Inventory.contains("Willow logs")) {
                Logger.log("No willow logs available! Need to bank for more logs.");
                return false;
            }
            
            // We have willow logs, set the trap
            Logger.log("Setting pitfall trap with willow logs...");
            
            // Try to set trap by using willow logs on the pit
            if (Inventory.interact("Willow logs", "Use")) {
                Sleep.sleep(600, 1000);
                if (trapSpot.interact("Use")) {
                    Logger.log("Setting pitfall trap on pit...");
                    Sleep.sleepUntil(() -> GameObjects.closest("Spiked pit") != null, 5000);
                    return true;
                }
            }
        } else {
            Logger.log("No trap spots (pits) found! Walking around to find trap location...");
            // Walk around the hunting area to find trap spots
            int randomX = HUNTING_AREA_X + (int)(Math.random() * 10 - 5);
            int randomY = HUNTING_AREA_Y + (int)(Math.random() * 10 - 5);
            Walking.walkExact(new Tile(randomX, randomY, 0));
            Sleep.sleep(2000, 3000);
            return true;
        }
        
        Logger.log("Continuing hunting cycle...");
        Sleep.sleep(2000, 3000);
        return true;
    }
    
    private boolean handleExistingTrap(GameObject spikedPit) {
        Logger.log("Handling existing trap - looking for antelopes to tease...");
        
        // Check if we've jumped too many times without dismantling a trap - need to lure
        if (jumpsSinceLastDismantle >= 3) {
            Logger.log("LURE STRATEGY: Jumped " + jumpsSinceLastDismantle + " times with no dismantled traps!");
            Logger.log("Running to lure tile (1560,9420) to reset antelope positions and try fresh approach...");
            
            // Run to lure position to draw antelopes into the open
            Walking.walk(LURE_TILE);
            Sleep.sleep(3000, 4000); // Wait at lure position for antelopes to follow
            
            // Reset counter after luring
            jumpsSinceLastDismantle = 0;
            
            Logger.log("Lure complete! Antelopes should be repositioned. Continuing hunt...");
            return true;
        }
        
        // Look for Moonlight Antelope to tease (within 10 tiles of trap)
        NPC moonlightAntelope = NPCs.closest(npc -> 
            npc.getName() != null && 
            npc.getName().equals("Moonlight antelope") &&
            npc.getTile().distance(spikedPit.getTile()) <= 10
        );
        if (moonlightAntelope != null && moonlightAntelope.exists()) {
            Logger.log("Found Moonlight Antelope, patiently using teasing stick...");
            
            // Use teasing stick on the antelope
            if (Inventory.interact("Teasing stick", "Use")) {
                Sleep.sleep(600, 1000);
                if (moonlightAntelope.interact("Use")) {
                    Logger.log("Used teasing stick on Moonlight Antelope! Waiting for it to fall into trap...");
                    Sleep.sleep(3000, 5000); // Wait for antelope to move and potentially fall into trap
                    return true;
                }
            }
        } else {
            Logger.log("No Moonlight Antelopes found near trap, waiting or searching...");
            
            // Check if trap caught something by looking for hoof shard
            if (Inventory.contains("Antelope hoof shard")) {
                Logger.log("SUCCESS! Got Antelope hoof shard!");
                return true;
            }
            
            // Wait a bit for antelopes to spawn or move around
            Sleep.sleep(3000, 5000);
            return true;
        }
        
        return true;
    }
    
    @Override
    public boolean bankLoot() {
        Logger.log("Banking Moonlight Antelope loot...");
        
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
                String[] keepItems = new String[VALID_KNIVES.length + 4];
                System.arraycopy(VALID_KNIVES, 0, keepItems, 0, VALID_KNIVES.length);
                keepItems[VALID_KNIVES.length] = "Teasing stick";
                keepItems[VALID_KNIVES.length + 1] = "Lobster";
                keepItems[VALID_KNIVES.length + 2] = "Willow logs";
                keepItems[VALID_KNIVES.length + 3] = "Antelope hoof shard"; // Don't deposit the goal item!
                
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
                        
                        // Eat lobsters to heal to full health WHILE AT BANK
                        Logger.log("Eating lobsters at bank to restore to full health...");
                        while (Inventory.contains("Lobster") && Combat.getHealthPercent() < 100) {
                            if (Inventory.interact("Lobster", "Eat")) {
                                Logger.log("Eating lobster at bank... Health: " + Combat.getHealthPercent() + "%");
                                Sleep.sleepUntil(() -> !Players.getLocal().isAnimating(), 3000);
                                Sleep.sleep(600, 1200); // Wait between eating
                            } else {
                                break;
                            }
                        }
                        Logger.log("Finished eating lobsters. Final health: " + Combat.getHealthPercent() + "%");
                    } else {
                        Logger.log("No lobsters in bank! Please stock up on food.");
                    }
                }
                
                // Ensure we have enough willow logs - maintain 7
                int currentLogCount = Inventory.count("Willow logs");
                if (currentLogCount < 7) {
                    int logsNeeded = 7 - currentLogCount;
                    if (Bank.contains("Willow logs")) {
                        Logger.log("Withdrawing " + logsNeeded + " willow logs to maintain 7...");
                        Bank.withdraw("Willow logs", logsNeeded);
                        Sleep.sleepUntil(() -> Inventory.count("Willow logs") >= 7, 3000);
                    } else {
                        Logger.log("ERROR: No willow logs in bank! Please stock up.");
                        return false;
                    }
                }
                
                Bank.close();
                Logger.log("Banking completed");
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Goes to Guild Hunter Wolf with antelope hoof shard
     */
    public boolean goToGuide() {
        Logger.log("Going to Guild Hunter Wolf (Master) with antelope hoof shard...");
        
        if (Inventory.contains("Antelope hoof shard")) {
            Logger.log("Antelope hoof shard confirmed in inventory");
            Logger.log("Calling GuildUtils.goToBasementAndTalkToWolf()...");
            boolean result = GuildUtils.goToBasementAndTalkToWolf();
            Logger.log("GuildUtils.goToBasementAndTalkToWolf() returned: " + result);
            return result;
        } else {
            Logger.log("ERROR: No antelope hoof shard found! Cannot proceed to guide.");
            return false;
        }
    }
    
    @Override
    public int getRequiredLevel() {
        return REQUIRED_LEVEL;
    }
    
    @Override
    public String[] getRequiredItems() {
        return REQUIRED_ITEMS;
    }
    
    @Override
    public int[] getHuntingAreaLocation() {
        return new int[]{HUNTING_AREA_X, HUNTING_AREA_Y, 0};
    }
    
    @Override
    public int[] getBankLocation() {
        return new int[]{BANK_X, BANK_Y, 0};
    }
}