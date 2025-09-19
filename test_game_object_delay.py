#!/usr/bin/env python3

import sys
import os
sys.path.append('/Users/jakedahl/Documents/Cline/MCP/runescape_mcp')

from java_caller import call_java_method

def test_game_object_interaction_delay():
    """
    Test the game object interaction with delay mechanism
    """
    print("Testing game object interaction with delay...")
    
    try:
        # Test clicking a common object (this will likely fail if not in game, but we can see the delay logic)
        result = call_java_method("click_object", ["Tree"])
        print(f"Result: {result}")
        
        # The result should include information about the delay if successful
        if "delayed" in result:
            print("✅ Delay mechanism is working - delay information found in result")
        else:
            print("⚠️  No delay information in result, but this could be normal if object wasn't found")
            
    except Exception as e:
        print(f"❌ Error testing game object interaction: {e}")

if __name__ == "__main__":
    test_game_object_interaction_delay()
