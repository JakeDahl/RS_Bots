#!/usr/bin/env python3

"""
Test script for inventory check functionality
Tests the new checkInventoryForItem and inventoryContainsItem methods
"""

import sys
import os

# Add the MCP directory to the path
sys.path.append(os.path.join(os.path.dirname(__file__), 'Documents', 'Cline', 'MCP', 'runescape_mcp'))

from java_caller import JavaCaller

def test_inventory_check_methods():
    """Test the new inventory check methods"""
    
    print("=== Testing Inventory Check Methods ===")
    caller = JavaCaller()
    
    try:
        # Test 1: Check inventory for a specific item by name (returns count)
        print("\n1. Testing checkInventoryForItem with item name...")
        result = caller.call_method("checkInventoryForItem", ["Lobster", False])
        print(f"   Result: {result}")
        print(f"   Type: {type(result)}")
        
        # Test 2: Check inventory for a specific item by ID (returns count)
        print("\n2. Testing checkInventoryForItem with item ID...")
        result = caller.call_method("checkInventoryForItem", ["379", True])  # 379 is Lobster ID
        print(f"   Result: {result}")
        print(f"   Type: {type(result)}")
        
        # Test 3: Check if inventory contains item by name (returns boolean)
        print("\n3. Testing inventoryContainsItem with item name...")
        result = caller.call_method("inventoryContainsItem", ["Lobster", False])
        print(f"   Result: {result}")
        print(f"   Type: {type(result)}")
        
        # Test 4: Check if inventory contains item by ID (returns boolean)
        print("\n4. Testing inventoryContainsItem with item ID...")
        result = caller.call_method("inventoryContainsItem", ["379", True])  # 379 is Lobster ID
        print(f"   Result: {result}")
        print(f"   Type: {type(result)}")
        
        # Test 5: Check for a non-existent item (should return -1 for checkInventoryForItem)
        print("\n5. Testing checkInventoryForItem with non-existent item...")
        result = caller.call_method("checkInventoryForItem", ["NonExistentItem", False])
        print(f"   Result: {result}")
        print(f"   Type: {type(result)}")
        
        # Test 6: Check for a non-existent item (should return False for inventoryContainsItem)
        print("\n6. Testing inventoryContainsItem with non-existent item...")
        result = caller.call_method("inventoryContainsItem", ["NonExistentItem", False])
        print(f"   Result: {result}")
        print(f"   Type: {type(result)}")
        
        # Test 7: Check for invalid item ID
        print("\n7. Testing checkInventoryForItem with invalid item ID...")
        result = caller.call_method("checkInventoryForItem", ["invalid_id", True])
        print(f"   Result: {result}")
        print(f"   Type: {type(result)}")
        
        # Test 8: Check current inventory count for reference
        print("\n8. Getting current inventory count for reference...")
        result = caller.call_method("getInventoryCount", [])
        print(f"   Current inventory slots used: {result}")
        
        print("\n=== All tests completed successfully! ===")
        
    except Exception as e:
        print(f"Error during testing: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    test_inventory_check_methods()
