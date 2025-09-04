import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.item.GroundItems;
import org.dreambot.api.methods.skills.Skills;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.combat.Combat;
import org.dreambot.api.methods.filter.Filter;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.items.GroundItem;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.methods.map.Tile;
import java.util.List;

public class TecuSalamanderHandler implements HuntingHandler {
    
    // Tecu Salamander hunting area coordinates
    private static final Tile HUNTING_AREA = new Tile(1475, 3095, 0);
    private static final int HUNTING_RADIUS = 10;
    private static final Tile BANK_TILE = new Tile(1543, 3040, 0); // Hunter Guild bank
    private static final int REQUIRED_LEVEL = 79;
    
    // Required items for salamander trapping
    private static final String[] REQUIRED_ITEMS = {
        "Small fishing net",
        "Rope"
    };
    
    @Override
    public boolean checkRequirements() {
        Logger.log("Checking Tecu salamander hunting requirements...");
        
        int hunterLevel = Skills.getRealLevel(Skill.HUNTER);
        if (hunterLevel < REQUIRED_LEVEL) {
            Logger.log("Hunter level too low for Tecu salamanders. Required: " + REQUIRED_LEVEL + ", Current: " + hunterLevel);
            return false;
        }
        
        Logger.log("Hunter level requirement met: " + hunterLevel + "/" + REQUIRED_LEVEL);
        return true;
    }
    
    @Override
    public boolean prepareEquipment() {
        Logger.log("Preparing equipment for Tecu salamander hunting...");
        
        // Check if we have required items
        boolean hasNets = Inventory.contains("Small fishing net");
        boolean hasRopes = Inventory.contains("Rope");
        
        if (!hasNets) {
            Logger.log("Missing small fishing nets! Please obtain some before starting.");
            return false;
        }
        
        if (!hasRopes) {
            Logger.log("Missing ropes! Please obtain some before starting.");
            return false;
        }
        
        Logger.log("Equipment ready: Small fishing nets and ropes found");
        return true;
    }
    
    @Override
    public boolean walkToHuntingArea() {
        Logger.log("Walking to Tecu salamander hunting area...");
        
        while (Players.getLocal().getTile().distance(HUNTING_AREA) > HUNTING_RADIUS) {
            if (Walking.shouldWalk(8)) {
                Logger.log("Walking to Tecu salamander area: " + HUNTING_AREA);
                if (Walking.walk(HUNTING_AREA)) {
                    Sleep.sleep(1500, 2200);
                } else {
                    Logger.log("Walk failed, retrying...");
                    Sleep.sleep(1000, 1500);
                }
            } else {
                Sleep.sleep(600, 1000);
            }
        }
        
        Logger.log("Successfully reached Tecu salamander hunting area");
        return true;
    }
    
    @Override
    public boolean hunt() {
        Logger.log("Hunting Tecu salamanders...");
        
        
        // FIRST PRIORITY: Reset any traps that need resetting (captures creatures inside)
        
        // Find any traps that need resetting (with catches or set traps) - extended range
        GameObject trapToReset = GameObjects.closest(new Filter<GameObject>() {
            @Override
            public boolean match(GameObject gameObject) {
                String name = gameObject.getName();
                return name != null && (name.contains("Net trap") || name.contains("trap")) &&
                       gameObject.getTile().distance(Players.getLocal().getTile()) <= 25; // Extended range
            }
        });
        
        if (trapToReset != null && trapToReset.exists()) {
            String trapName = trapToReset.getName();
            if (trapName.contains("caught") || trapName.contains("full")) {
                Logger.log("FIRST PRIORITY: Resetting trap with catch (captures creature)...");
            } else {
                Logger.log("FIRST PRIORITY: Resetting set trap...");
            }
            
            // Walk to trap if it's far away
            if (trapToReset.getTile().distance(Players.getLocal().getTile()) > 8) {
                Logger.log("Walking to distant trap...");
                if (Walking.walk(trapToReset.getTile())) {
                    Sleep.sleep(1500, 2500);
                }
            }
            
            if (trapToReset.interact("Check")) {
                Logger.log("Checking trap (caught something)...");
                Sleep.sleep(2000, 3000);
                return true;
            } else if (trapToReset.interact("Reset")) {
                Logger.log("Resetting trap...");
                Sleep.sleep(2000, 3000);
                return true;
            }
        }
        
        // Release only immature salamanders (for XP)
        if (Inventory.contains("Immature tecu salamander")) {
            Logger.log("Releasing immature salamanders for XP...");
            
            // Release immature salamanders only
            while (Inventory.contains("Immature tecu salamander")) {
                if (Inventory.interact("Immature tecu salamander", "Release")) {
                    Logger.log("Released an Immature tecu salamander");
                    Sleep.sleep(1000, 1500);
                } else {
                    break; // If we can't release, break to avoid infinite loop
                }
            }
        }
        
        // Pick up any ropes or nets from the ground first
        GroundItem groundNet = GroundItems.closest("Small fishing net");
        if (groundNet != null && groundNet.exists() && Inventory.fullSlotCount() < 28) {
            Logger.log("Found Small fishing net on ground, picking up...");
            if (groundNet.interact("Take")) {
                Sleep.sleep(1500, 2000);
                return true;
            }
        }
        
        GroundItem groundRope = GroundItems.closest("Rope");
        if (groundRope != null && groundRope.exists() && Inventory.fullSlotCount() < 28) {
            Logger.log("Found Rope on ground, picking up...");
            if (groundRope.interact("Take")) {
                Sleep.sleep(1500, 2000);
                return true;
            }
        }
        
        // Check if health is critically low
        int currentHealth = Combat.getHealthPercent();
        if (currentHealth <= 10) {
            Logger.log("CRITICAL: Health is very low (" + currentHealth + "%)! Need to bank for food.");
            return false;
        }
        
        // Check if inventory is getting full
        if (Inventory.fullSlotCount() >= 24) {
            Logger.log("Inventory is getting full (" + Inventory.fullSlotCount() + "/28 slots)! Need to bank.");
            return false;
        }
        
        // Count existing traps after prioritizing resets
        List<GameObject> allTraps = GameObjects.all(new Filter<GameObject>() {
            @Override
            public boolean match(GameObject gameObject) {
                String name = gameObject.getName();
                return name != null && (name.contains("Net trap") || name.contains("trap")) &&
                       gameObject.getTile().distance(Players.getLocal().getTile()) <= 15;
            }
        });
        
        int activeTraps = 0;
        int trapsWithCatch = 0;
        
        // Check trap states
        for (GameObject trap : allTraps) {
            if (trap.getName().contains("caught") || trap.getName().contains("full")) {
                trapsWithCatch++;
            } else {
                activeTraps++;
            }
        }
        
        Logger.log("Trap status: " + activeTraps + " active, " + trapsWithCatch + " with catches");
        
        // Set new traps if we have less than 4 total
        int totalTraps = activeTraps + trapsWithCatch;
        if (totalTraps < 4) {
            // Check if we have required items
            if (!Inventory.contains("Small fishing net") || !Inventory.contains("Rope")) {
                Logger.log("Missing nets or ropes for setting traps! Need to bank.");
                return false;
            }
            
            // Look for salamander spawns or suitable trap locations
            List<GameObject> trapSpots = GameObjects.all(new Filter<GameObject>() {
                @Override
                public boolean match(GameObject gameObject) {
                    String name = gameObject.getName();
                    if (name == null) return false;
                    
                    // Look for trees or specific trap spots
                    boolean isTrapSpot = name.contains("Tree") || name.contains("Young tree") || 
                                       name.equals("Jungle tree") || name.equals("Palm tree");
                    
                    if (!isTrapSpot) return false;
                    
                    // Make sure no trap is already there
                    boolean hasNearbyTrap = false;
                    for (GameObject trap : allTraps) {
                        if (trap.getTile().distance(gameObject.getTile()) <= 1) {
                            hasNearbyTrap = true;
                            break;
                        }
                    }
                    
                    return !hasNearbyTrap && gameObject.getTile().distance(Players.getLocal().getTile()) <= 12;
                }
            });
            
            if (!trapSpots.isEmpty()) {
                GameObject bestSpot = trapSpots.get(0); // Take the first one
                Logger.log("Setting trap " + (totalTraps + 1) + "/4 at location...");
                
                if (bestSpot.interact("Set-trap")) {
                    Logger.log("Setting net trap...");
                    Sleep.sleep(3000, 4000);
                    return true;
                } else {
                    // Try alternative interaction methods
                    if (Inventory.interact("Small fishing net", "Use")) {
                        Sleep.sleep(600, 1000);
                        if (bestSpot.interact("Use")) {
                            Logger.log("Set trap using item interaction");
                            Sleep.sleep(3000, 4000);
                            return true;
                        }
                    }
                }
            } else {
                Logger.log("No suitable trap locations found, searching area...");
                Sleep.sleep(2000, 3000);
                return true;
            }
        }
        
        // All traps are set and active, just wait (much faster checking)
        if (totalTraps >= 4 && trapsWithCatch == 0) {
            Logger.log("All 4 traps are active, checking again quickly...");
            Sleep.sleep(500, 1000);
            return true;
        }
        
        Logger.log("Continuing hunting cycle...");
        Sleep.sleep(500, 1000);
        return true;
    }
    
    @Override
    public boolean bankLoot() {
        Logger.log("Banking Tecu salamander loot...");
        
        // Walk to bank using smooth walking
        while (Players.getLocal().getTile().distance(BANK_TILE) > 5) {
            if (Walking.shouldWalk(8)) {
                Logger.log("Walking to bank: " + BANK_TILE);
                if (Walking.walk(BANK_TILE)) {
                    Sleep.sleep(1500, 2200);
                } else {
                    Logger.log("Walk to bank failed, retrying...");
                    Sleep.sleep(1000, 1500);
                }
            } else {
                Sleep.sleep(600, 1000);
            }
        }
        
        // Open bank
        GameObject bankBooth = GameObjects.closest("Bank booth");
        if (bankBooth == null) {
            bankBooth = GameObjects.closest("Bank");
        }
        
        if (bankBooth != null && bankBooth.interact("Bank")) {
            Logger.log("Opening bank...");
            Sleep.sleep(2000, 3000);
            
            if (Bank.isOpen()) {
                Logger.log("Bank opened successfully");
                
                // Deposit all items except required ones
                Logger.log("Depositing all items except required ones...");
                
                // Create comprehensive list of items to keep
                String[] keepItems = {
                    "Small fishing net",
                    "Rope", 
                    "Lobster",
                    "Salamander claw" // Don't deposit the goal item!
                };
                
                Logger.log("Items to keep: " + java.util.Arrays.toString(keepItems));
                
                // First, deposit everything else
                Bank.depositAllExcept(keepItems);
                Sleep.sleep(1500, 2500);
                
                Logger.log("Deposited all unnecessary items");
                
                // Handle health if low
                int currentHealth = Combat.getHealthPercent();
                if (currentHealth <= 50) {
                    Logger.log("Health is low (" + currentHealth + "%), withdrawing food...");
                    
                    // Calculate lobsters needed (each heals ~12 HP, assuming ~80 max HP)
                    int maxHealth = Skills.getRealLevel(Skill.HITPOINTS);
                    int healthNeeded = maxHealth - (maxHealth * currentHealth / 100);
                    int lobstersNeeded = Math.min((healthNeeded / 12) + 1, 5); // Max 5 lobsters
                    
                    if (Bank.contains("Lobster")) {
                        if (Bank.withdraw("Lobster", lobstersNeeded)) {
                            Logger.log("Withdrew " + lobstersNeeded + " lobsters");
                            Sleep.sleep(1000, 2000);
                            
                            // Eat lobsters to restore health
                            while (Inventory.contains("Lobster") && Combat.getHealthPercent() < 100) {
                                if (Inventory.interact("Lobster", "Eat")) {
                                    Logger.log("Eating lobster... Health: " + Combat.getHealthPercent() + "%");
                                    Sleep.sleep(1000, 1500);
                                }
                            }
                        }
                    }
                }
                
                // Ensure we have required items - exactly 4 nets and 4 ropes
                int netCount = Inventory.count("Small fishing net");
                if (netCount < 4) {
                    int netsNeeded = 4 - netCount;
                    if (Bank.contains("Small fishing net")) {
                        if (Bank.withdraw("Small fishing net", netsNeeded)) {
                            Logger.log("Withdrew " + netsNeeded + " small fishing nets (had " + netCount + ", now have 4)");
                            Sleep.sleep(1000, 2000);
                        }
                    } else {
                        Logger.log("ERROR: No small fishing nets in bank!");
                    }
                }
                
                int ropeCount = Inventory.count("Rope");
                if (ropeCount < 4) {
                    int ropesNeeded = 4 - ropeCount;
                    if (Bank.contains("Rope")) {
                        if (Bank.withdraw("Rope", ropesNeeded)) {
                            Logger.log("Withdrew " + ropesNeeded + " ropes (had " + ropeCount + ", now have 4)");
                            Sleep.sleep(1000, 2000);
                        }
                    } else {
                        Logger.log("ERROR: No ropes in bank!");
                    }
                }
                
                Bank.close();
                Logger.log("Banking completed successfully");
                return true;
            } else {
                Logger.log("Failed to open bank");
            }
        } else {
            Logger.log("Could not find bank booth");
        }
        
        return false;
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
        return new int[]{HUNTING_AREA.getX(), HUNTING_AREA.getY(), HUNTING_AREA.getZ()};
    }
    
    @Override
    public int[] getBankLocation() {
        return new int[]{BANK_TILE.getX(), BANK_TILE.getY(), BANK_TILE.getZ()};
    }
    
    /**
     * Goes to Guild Hunter Wolf with salamander claw
     */
    public boolean goToGuide() {
        Logger.log("Going to Guild Hunter Wolf (Master) with salamander claw...");
        
        if (Inventory.contains("Salamander claw")) {
            Logger.log("Salamander claw confirmed in inventory");
            Logger.log("Calling GuildUtils.goToBasementAndTalkToWolf()...");
            boolean result = GuildUtils.goToBasementAndTalkToWolf();
            Logger.log("GuildUtils.goToBasementAndTalkToWolf() returned: " + result);
            return result;
        } else {
            Logger.log("ERROR: No salamander claw found! Cannot proceed to guide.");
            return false;
        }
    }
}