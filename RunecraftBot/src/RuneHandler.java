import org.dreambot.api.utilities.Logger;

public interface RuneHandler {
    
    boolean walkToAltar();
    
    boolean craftRunes();
    
    String getRuneType();
    
    int[] getBankLocation();
    
    int[] getAltarLocation();
}