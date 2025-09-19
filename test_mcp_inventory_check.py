#!/usr/bin/env python3

"""
Test script for MCP inventory check functionality
Tests the new check_inventory_for_item and inventory_contains_item MCP tools
"""

import asyncio
import sys
import os

# Add the MCP directory to the path
sys.path.append(os.path.join(os.path.dirname(__file__), 'Documents', 'Cline', 'MCP', 'runescape_mcp'))

from java_caller import JavaMethodCaller

async def test_mcp_inventory_check_methods():
    """Test the new MCP inventory check methods"""
    
    print("=== Testing MCP Inventory Check Methods ===")
    
    # Import the handlers directly to test them
    from handlers import _handle_check_inventory_for_item, _handle_inventory_contains_item
    
    # Create a java caller instance
    java_caller = JavaMethodCaller()
    
    try:
        # Test 1: check_inventory_for_item with item name
        print("\n1. Testing check_inventory_for_item MCP tool with item name...")
        args = {"item_name": "Lobster", "use_item_id": False}
        result = await _handle_check_inventory_for_item(java_caller, args)
        print(f"   Result: {result[0].text}")
        
        # Test 2: check_inventory_for_item with item ID
        print("\n2. Testing check_inventory_for_item MCP tool with item ID...")
        args = {"item_name": "379", "use_item_id": True}
        result = await _handle_check_inventory_for_item(java_caller, args)
        print(f"   Result: {result[0].text}")
        
        # Test 3: inventory_contains_item with item name
        print("\n3. Testing inventory_contains_item MCP tool with item name...")
        args = {"item_name": "Lobster", "use_item_id": False}
        result = await _handle_inventory_contains_item(java_caller, args)
        print(f"   Result: {result[0].text}")
        
        # Test 4: inventory_contains_item with item ID
        print("\n4. Testing inventory_contains_item MCP tool with item ID...")
        args = {"item_name": "379", "use_item_id": True}
        result = await _handle_inventory_contains_item(java_caller, args)
        print(f"   Result: {result[0].text}")
        
        # Test 5: check_inventory_for_item with non-existent item
        print("\n5. Testing check_inventory_for_item MCP tool with non-existent item...")
        args = {"item_name": "NonExistentItem", "use_item_id": False}
        result = await _handle_check_inventory_for_item(java_caller, args)
        print(f"   Result: {result[0].text}")
        
        # Test 6: inventory_contains_item with non-existent item
        print("\n6. Testing inventory_contains_item MCP tool with non-existent item...")
        args = {"item_name": "NonExistentItem", "use_item_id": False}
        result = await _handle_inventory_contains_item(java_caller, args)
        print(f"   Result: {result[0].text}")
        
        # Test 7: Error handling - missing item_name
        print("\n7. Testing error handling with missing item_name...")
        args = {}
        result = await _handle_check_inventory_for_item(java_caller, args)
        print(f"   Result: {result[0].text}")
        
        print("\n=== All MCP tests completed successfully! ===")
        
    except Exception as e:
        print(f"Error during MCP testing: {e}")
        import traceback
        traceback.print_exc()

def test_direct_java_caller():
    """Test the JavaMethodCaller methods directly"""
    
    print("\n=== Testing JavaMethodCaller Methods Directly ===")
    caller = JavaMethodCaller()
    
    try:
        # Test the new methods in JavaMethodCaller
        print("\n1. Testing check_inventory_for_item JavaMethodCaller method...")
        result = caller.check_inventory_for_item("Lobster", False)
        print(f"   Result: {result}")
        
        print("\n2. Testing inventory_contains_item JavaMethodCaller method...")
        result = caller.inventory_contains_item("Lobster", False)
        print(f"   Result: {result}")
        
        print("\n=== JavaMethodCaller tests completed! ===")
        
    except Exception as e:
        print(f"Error during JavaMethodCaller testing: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    # Test direct java caller methods
    test_direct_java_caller()
    
    # Test MCP handlers
    asyncio.run(test_mcp_inventory_check_methods())
