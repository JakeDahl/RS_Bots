#!/usr/bin/env python3

"""
Test script for the new use_item_on_item MCP tool
"""

import sys
import os
sys.path.append('../../Documents/Cline/MCP/runescape_mcp')

from java_caller import JavaMethodCaller

def test_use_item_on_item_tool():
    """Test the use_item_on_item functionality directly via JavaMethodCaller"""
    print("Testing use_item_on_item via JavaMethodCaller...")
    
    caller = JavaMethodCaller()
    
    # Test 1: Using item names
    print("\n=== Test 1: Using item names ===")
    result = caller.use_item_on_item("Logs", "Tinderbox", False)
    print(f"Result: {result}")
    
    # Test 2: Using item IDs 
    print("\n=== Test 2: Using item IDs ===")
    result = caller.use_item_on_item("1511", "590", True)  # Logs ID and Tinderbox ID
    print(f"Result: {result}")
    
    # Test 3: Error case - invalid items
    print("\n=== Test 3: Invalid items ===")
    result = caller.use_item_on_item("NonexistentItem", "AnotherFakeItem", False)
    print(f"Result: {result}")

if __name__ == "__main__":
    test_use_item_on_item_tool()
