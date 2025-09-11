import org.dreambot.api.utilities.Logger;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.skills.Skill;

/**
 * Manages player state information and queries
 */
public class PlayerStateManager {
    
    /**
     * Get player's current location as formatted string
     */
    public String getPlayerLocation() {
        try {
            if (Players.getLocal() != null) {
                Tile location = Players.getLocal().getTile();
                String locationStr = "(" + location.getX() + ", " + location.getY() + ", " + location.getZ() + ")";
                Logger.log("Python->Java: Player location: " + locationStr);
                return locationStr;
            } else {
                return "Player location: UNKNOWN (player is null)";
            }
        } catch (Exception e) {
            return "Player location: ERROR - " + e.getMessage();
        }
    }
    
    /**
     * Get player's current X coordinate
     */
    public int getPlayerX() {
        try {
            if (Players.getLocal() != null) {
                int x = Players.getLocal().getTile().getX();
                Logger.log("Python->Java: Player X: " + x);
                return x;
            } else {
                Logger.log("Python->Java: Player X: -1 (player is null)");
                return -1;
            }
        } catch (Exception e) {
            Logger.log("Python->Java: Error getting player X: " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * Get player's current Y coordinate
     */
    public int getPlayerY() {
        try {
            if (Players.getLocal() != null) {
                int y = Players.getLocal().getTile().getY();
                Logger.log("Python->Java: Player Y: " + y);
                return y;
            } else {
                Logger.log("Python->Java: Player Y: -1 (player is null)");
                return -1;
            }
        } catch (Exception e) {
            Logger.log("Python->Java: Error getting player Y: " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * Check if player is moving
     */
    public boolean isPlayerMoving() {
        try {
            boolean moving = Players.getLocal() != null && Players.getLocal().isMoving();
            Logger.log("Python->Java: Player moving: " + moving);
            return moving;
        } catch (Exception e) {
            Logger.log("Python->Java: Error checking if player is moving: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get player's skill level
     */
    public int getSkillLevel(String skillName) {
        try {
            Skill skill = Skill.valueOf(skillName.toUpperCase());
            int level = skill.getLevel();
            Logger.log("Python->Java: " + skillName + " level: " + level);
            return level;
        } catch (Exception e) {
            Logger.log("Python->Java: Error getting " + skillName + " level: " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * Check if player is animating (doing an action)
     */
    public boolean isPlayerAnimating() {
        try {
            boolean animating = Players.getLocal() != null && Players.getLocal().isAnimating();
            Logger.log("Python->Java: Player animating: " + animating);
            return animating;
        } catch (Exception e) {
            Logger.log("Python->Java: Error checking animation: " + e.getMessage());
            return false;
        }
    }
}
