#!/usr/bin/env python3

import sys
import os
import time
import json

# Add the MCP server path to sys.path
mcp_path = "/Users/jakedahl/Documents/Cline/MCP"
sys.path.append(mcp_path)

from runescape_mcp.java_caller import JavaCaller

def test_dialogue_handler_no_dialogue():
    """Test that dialogue handler returns quickly when no dialogue is active"""
    print("Testing DialogueHandler - No Active Dialogue Scenario")
    
    caller = JavaCaller()
    
    # Test 1: Call dialogue handler with no NPC name (should return immediately)
    print("\n1. Testing dialogue handler with no NPC name...")
    start_time = time.time()
    
    try:
        result = caller.call_method("handleNPCDialogue", ["", "10"])  # Empty NPC name, 10 second timeout
        end_time = time.time()
        elapsed_time = end_time - start_time
        
        print(f"Result: {result}")
        print(f"Time elapsed: {elapsed_time:.2f} seconds")
        
        # Parse the JSON response
        response = json.loads(result)
        
        if response.get("success") and elapsed_time < 1.0:  # Should complete in under 1 second
            print("✅ PASS: DialogueHandler returned quickly with no active dialogue")
        else:
            print(f"❌ FAIL: DialogueHandler took {elapsed_time:.2f} seconds (expected < 1.0s)")
            
    except Exception as e:
        print(f"❌ ERROR: {e}")
    
    # Test 2: Call dialogue handler with non-existent NPC (should return after brief wait)
    print("\n2. Testing dialogue handler with non-existent NPC...")
    start_time = time.time()
    
    try:
        result = caller.call_method("handleNPCDialogue", ["NonExistentNPC", "10"])  # Non-existent NPC, 10 second timeout
        end_time = time.time()
        elapsed_time = end_time - start_time
        
        print(f"Result: {result}")
        print(f"Time elapsed: {elapsed_time:.2f} seconds")
        
        # Parse the JSON response
        response = json.loads(result)
        
        if not response.get("success") and elapsed_time < 1.0:  # Should fail quickly
            print("✅ PASS: DialogueHandler returned quickly for non-existent NPC")
        else:
            print(f"❌ FAIL: DialogueHandler took {elapsed_time:.2f} seconds (expected < 1.0s)")
            
    except Exception as e:
        print(f"❌ ERROR: {e}")

if __name__ == "__main__":
    test_dialogue_handler_no_dialogue()
