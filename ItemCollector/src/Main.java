import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.item.GroundItems;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.items.GroundItem;

import java.util.Random;

@ScriptManifest(name = "Fish Collector", description = "Collects fish and banks them", author = "Auto Fisher",
        version = 1.0, category = Category.FISHING, image = "")

public class Main extends AbstractScript {
    private final Tile FISHING_TILE = new Tile(3108, 3433);
    private final Area FISHING_AREA = FISHING_TILE.getArea(11);
    private final Tile EDGEVILLE_BANK_TILE = new Tile(3094, 3492);
    private final Area EDGEVILLE_BANK_AREA = EDGEVILLE_BANK_TILE.getArea(5);
    private final String[] FISH_NAMES = {"Trout", "Salmon", "Raw trout", "Raw salmon"};
    private final Random random = new Random();
    
    private enum State {
        WALKING_TO_FISH,
        COLLECTING_FISH,
        WALKING_TO_BANK,
        BANKING,
        IDLE
    }
    
    private State getState() {
        if (Inventory.isFull()) {
            if (Bank.isOpen()) {
                return State.BANKING;
            } else if (isNearBank()) {
                return State.BANKING;
            } else {
                return State.WALKING_TO_BANK;
            }
        } else {
            if (FISHING_AREA.contains(Players.getLocal())) {
                if (hasGroundFish()) {
                    return State.COLLECTING_FISH;
                } else {
                    return State.IDLE;
                }
            } else {
                return State.WALKING_TO_FISH;
            }
        }
    }
    
    private boolean hasGroundFish() {
        for (String fishName : FISH_NAMES) {
            if (GroundItems.closest(fishName) != null) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isNearBank() {
        return EDGEVILLE_BANK_AREA.contains(Players.getLocal());
    }
    
    private void walkToFishingSpot() {
        if (!FISHING_AREA.contains(Players.getLocal())) {
            Logger.log("Walking to fishing spot...");
            Tile randomizedTile = FISHING_TILE.getRandomized(2);
            Walking.walk(randomizedTile);
            Sleep.sleepUntil(() -> FISHING_AREA.contains(Players.getLocal()), 
                          randomSleep(3000, 5000), randomSleep(50, 500));
        }
    }
    
    private void collectFish() {
        if (Inventory.isFull()) {
            return;
        }
        
        GroundItem fish = null;
        for (String fishName : FISH_NAMES) {
            fish = GroundItems.closest(fishName);
            if (fish != null) {
                break;
            }
        }
        
        if (fish != null) {
            Logger.log("Collecting " + fish.getName() + "...");
            if (fish.interact("Take")) {
                GroundItem finalFish = fish;
                Sleep.sleepUntil(() -> !finalFish.exists() || Inventory.isFull(),
                              randomSleep(2000, 3000), randomSleep(50, 500));
                randomSleep(50, 200);
            }
        }
    }
    
    private void walkToBank() {
        Logger.log("Walking to Edgeville bank...");
        Tile randomizedBankTile = EDGEVILLE_BANK_TILE.getRandomized(2);
        Walking.walk(randomizedBankTile);
        Sleep.sleepUntil(() -> EDGEVILLE_BANK_AREA.contains(Players.getLocal()), 
                      randomSleep(3000, 6000), randomSleep(50, 500));
    }
    
    private void handleBanking() {
        if (!Bank.isOpen()) {
            GameObject bank = GameObjects.closest("Bank booth");
            if (bank != null && bank.interact("Bank")) {
                Sleep.sleepUntil(() -> Bank.isOpen(), randomSleep(2000, 3000), randomSleep(50, 500));
            }
        } else {
            Logger.log("Depositing inventory...");
            if (Bank.depositAllItems()) {
                Sleep.sleepUntil(() -> Inventory.isEmpty(), randomSleep(1000, 2000), randomSleep(50, 500));
                randomSleep(200, 500);
                Bank.close();
                Sleep.sleepUntil(() -> !Bank.isOpen(), randomSleep(1000, 2000), randomSleep(50, 500));
            }
        }
    }
    
    private int randomSleep(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }
    
    @Override
    public int onLoop() {
        State currentState = getState();
        Logger.log("Current state: " + currentState);
        
        switch (currentState) {
            case WALKING_TO_FISH:
                walkToFishingSpot();
                break;
                
            case COLLECTING_FISH:
                collectFish();
                break;
                
            case WALKING_TO_BANK:
                walkToBank();
                break;
                
            case BANKING:
                handleBanking();
                break;
                
            case IDLE:
                Logger.log("No fish on ground, waiting...");
                break;
        }
        
        return randomSleep(50, 300);
    }
    
    @Override
    public void onStart() {
        Logger.log("Fish Collector started!");
        Logger.log("Target fishing tile: " + FISHING_TILE);
        Logger.log("Looking for: " + String.join(", ", FISH_NAMES));
    }
    
    @Override
    public void onExit() {
        Logger.log("Fish Collector stopped!");
    }
}
