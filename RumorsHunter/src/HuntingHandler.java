public interface HuntingHandler {
    
    boolean checkRequirements();
    
    boolean prepareEquipment();
    
    boolean walkToHuntingArea();
    
    boolean hunt();
    
    boolean bankLoot();
    
    int getRequiredLevel();
    
    String[] getRequiredItems();
    
    int[] getHuntingAreaLocation();
    
    int[] getBankLocation();
}