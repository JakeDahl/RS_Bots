#!/usr/bin/env python3
"""
Test script to demonstrate the enhanced game object functionality.
This script tests the new methods for listing and searching game objects.
"""

import sys
import os

# Add the MCP server path to Python path
sys.path.append('/Users/jakedahl/Documents/Cline/MCP/runescape_mcp')

from java_caller import JavaMethodCaller

def test_game_object_methods():
    """Test all the new game object methods"""
    caller = JavaMethodCaller()
    
    print("=== Testing Enhanced Game Object Functionality ===\n")
    
    # Test 1: List nearby game objects
    print("1. Testing listNearbyGameObjects():")
    print("-" * 40)
    try:
        result = caller.call_method("listNearbyGameObjects")
        print(result)
    except Exception as e:
        print(f"Error: {e}")
    print("\n")
    
    # Test 2: Get objects for specific actions
    print("2. Testing getGameObjectsForAction() with various actions:")
    print("-" * 50)
    
    actions_to_test = [
        "go up",
        "go down", 
        "enter",
        "mine",
        "bank",
        "climb up",
        "open"
    ]
    
    for action in actions_to_test:
        print(f"\nAction: '{action}'")
        try:
            result = caller.call_method("getGameObjectsForAction", [action])
            print(result)
        except Exception as e:
            print(f"Error: {e}")
    
    print("\n" + "="*60 + "\n")
    
    # Test 3: Search for specific objects
    print("3. Testing searchGameObjects() with search terms:")
    print("-" * 45)
    
    search_terms = [
        "door",
        "ladder", 
        "stairs",
        "tree",
        "rock",
        "bank"
    ]
    
    for term in search_terms:
        print(f"\nSearching for: '{term}'")
        try:
            result = caller.call_method("searchGameObjects", [term])
            print(result)
        except Exception as e:
            print(f"Error: {e}")
    
    print("\n" + "="*60 + "\n")
    
    # Test 4: Get details for specific objects (if any are found)
    print("4. Testing getObjectDetails() for common objects:")
    print("-" * 48)
    
    common_objects = [
        "Door",
        "Ladder", 
        "Tree",
        "Rock"
    ]
    
    for obj_name in common_objects:
        print(f"\nGetting details for: '{obj_name}'")
        try:
            result = caller.call_method("getObjectDetails", [obj_name])
            print(result)
        except Exception as e:
            print(f"Error: {e}")
    
    print("\n=== Test Complete ===")

if __name__ == "__main__":
    test_game_object_methods()
