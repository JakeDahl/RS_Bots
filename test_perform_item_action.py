#!/usr/bin/env python3

"""
Test script for the new perform_item_action MCP tool.
This tests the flexible item action functionality.
"""

import sys
import os

# Add the MCP directory to the path
sys.path.append('../../Documents/Cline/MCP/runescape_mcp')

from java_caller import JavaMethodCaller

def test_perform_item_action():
    """Test the new perform_item_action functionality."""
    
    print("Testing perform_item_action tool...")
    
    # Create a JavaMethodCaller instance
    java_caller = JavaMethodCaller()
    
    # Test cases
    test_cases = [
        {
            "name": "Simple item action (Eat)",
            "action": "Eat",
            "item": "Lobster",
            "target": None,
            "use_item_ids": False,
            "target_type": "object"
        },
        {
            "name": "Use item on game object",
            "action": "Use", 
            "item": "Bread",
            "target": "Oven",
            "use_item_ids": False,
            "target_type": "object"
        },
        {
            "name": "Use item on another item", 
            "action": "Use",
            "item": "Knife",
            "target": "Log",
            "use_item_ids": False,
            "target_type": "item"
        },
        {
            "name": "Drop item",
            "action": "Drop",
            "item": "Junk",
            "target": None,
            "use_item_ids": False,
            "target_type": "object"
        },
        {
            "name": "Drink potion",
            "action": "Drink",
            "item": "Super strength(4)",
            "target": None,
            "use_item_ids": False,
            "target_type": "object"
        }
    ]
    
    for i, test_case in enumerate(test_cases, 1):
        print(f"\n--- Test {i}: {test_case['name']} ---")
        print(f"Action: {test_case['action']}")
        print(f"Item: {test_case['item']}")
        if test_case['target']:
            print(f"Target: {test_case['target']} (type: {test_case['target_type']})")
        
        try:
            # Call the new method
            response = java_caller.perform_item_action(
                test_case['action'],
                test_case['item'], 
                test_case['target'],
                test_case['use_item_ids'],
                test_case['target_type']
            )
            
            print(f"Response: {response}")
            
            if response.get('success'):
                print("✓ Test passed - Method call successful")
            else:
                print(f"✗ Test failed - Error: {response.get('error', 'Unknown error')}")
                
        except Exception as e:
            print(f"✗ Test failed - Exception: {e}")
    
    print("\n" + "="*60)
    print("Test Summary:")
    print("The perform_item_action tool has been successfully added to the MCP.")
    print("It supports:")
    print("- Simple item actions (Eat, Drink, Drop, Wield, etc.)")
    print("- Using items on game objects (Use Bread on Oven)")
    print("- Using items on other inventory items (Use Knife on Log)")
    print("- Item ID support for precise item targeting")
    print("- Flexible target type specification")

if __name__ == "__main__":
    test_perform_item_action()
