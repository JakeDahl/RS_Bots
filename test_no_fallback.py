#!/usr/bin/env python3
"""
Test script to verify that fallback functionality has been removed
and all operations now require actual Java responses.
"""

import sys
import os

# Add the path to import the MCP modules
sys.path.append('/Users/jakedahl/Documents/Cline/MCP/runescape_mcp')

from java_caller import JavaMethodCaller

def test_no_fallback():
    """Test that operations fail gracefully when Java is not available."""
    print("=" * 60)
    print("Testing No Fallback Mode - All Operations Require Java")
    print("=" * 60)
    
    # Create JavaMethodCaller (no more wait_for_responses parameter)
    java_caller = JavaMethodCaller()
    
    print("\n1. Testing with Java shim NOT running...")
    print("   (All operations should timeout or fail gracefully)")
    
    # Test operations when Java is not available
    test_operations = [
        ("Bank Status Check", lambda: java_caller.check_bank_open()),
        ("Withdraw Item", lambda: java_caller.withdraw_item("Test item", 5)),
        ("Deposit Item", lambda: java_caller.deposit_item("Test item", 3)),
        ("Deposit All", lambda: java_caller.deposit_all()),
        ("Close Bank", lambda: java_caller.close_bank()),
        ("Get Inventory Count", lambda: java_caller.get_inventory_count()),
    ]
    
    for op_name, op_func in test_operations:
        print(f"\n   Testing {op_name}...")
        try:
            response = op_func()
            print(f"     - Success: {response['success']}")
            print(f"     - Result: {response['result']}")
            print(f"     - Error: {response['error']}")
            
            # Verify no fallback responses
            if response['success'] and response['result'] in ['status_unknown', 'count_unknown', 'state_unknown']:
                print(f"     ❌ FALLBACK DETECTED: {response['result']}")
            elif not response['success']:
                print(f"     ✅ No fallback - properly failed/timed out")
            else:
                print(f"     ✅ Got actual response from Java")
                
        except Exception as e:
            print(f"     ❌ Exception: {e}")
    
    print("\n" + "=" * 60)
    print("\n2. Testing constructor changes...")
    
    # Test that old constructor parameters are handled
    try:
        # This should work with new simplified constructor
        caller1 = JavaMethodCaller()
        print("   ✅ Default constructor works")
        
        caller2 = JavaMethodCaller(response_pipe_path="/tmp/custom_pipe")
        print("   ✅ Constructor with custom response pipe works")
        
        # Test that wait_for_responses parameter is no longer available
        try:
            caller3 = JavaMethodCaller(wait_for_responses=True)
            print("   ❌ wait_for_responses parameter still exists")
        except TypeError:
            print("   ✅ wait_for_responses parameter removed successfully")
            
    except Exception as e:
        print(f"   ❌ Constructor test failed: {e}")
    
    print("\n" + "=" * 60)
    print("No fallback test complete!")
    print("All operations now require actual Java responses.")
    print("=" * 60)

if __name__ == "__main__":
    print("\n⚠️  This test verifies that fallback functionality has been removed.")
    print("It should be run with the DreamBot shim NOT running to verify")
    print("that operations properly fail/timeout instead of returning fallback responses.")
    print("\nPress Enter to continue or Ctrl+C to cancel...")
    input()
    
    test_no_fallback()
