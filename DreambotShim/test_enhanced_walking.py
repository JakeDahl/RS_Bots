#!/usr/bin/env python3
"""
Test script for the enhanced walking functionality in DreamBot Shim
This script demonstrates the new walking method that continues until reaching the destination
"""

import time
from python_caller import JavaMethodCaller

def test_enhanced_walking():
    """Test the enhanced walking functionality"""
    print("=== Testing Enhanced Walking Functionality ===")
    
    caller = JavaMethodCaller()
    
    # Wait for Java receiver to be ready
    print("Waiting for DreamBot Shim to be ready...")
    time.sleep(2)
    
    # Test basic connectivity
    print("\n1. Testing basic connectivity:")
    caller.hello_world()
    time.sleep(1)
    
    # Test player location methods
    print("\n2. Testing player location methods:")
    caller.get_player_location()
    time.sleep(0.5)
    
    caller.get_player_x()
    time.sleep(0.5)
    
    caller.get_player_y()
    time.sleep(0.5)
    
    caller.is_player_moving()
    time.sleep(0.5)
    
    # Test enhanced walking - this now includes distance checking loop
    print("\n3. Testing enhanced walking (with distance checking loop):")
    print("Walking to Lumbridge (3222, 3218)...")
    caller.walk_to_location(3222, 3218)
    time.sleep(2)
    
    print("Walking to Draynor Village bank (3092, 3243)...")
    caller.walk_to_location(3092, 3243)
    time.sleep(2)
    
    print("Walking to Varrock center (3210, 3424)...")
    caller.walk_to_location(3210, 3424)
    time.sleep(2)
    
    # Test banking methods
    print("\n4. Testing enhanced banking methods:")
    caller.check_bank_open()
    time.sleep(0.5)
    
    caller.open_bank()
    time.sleep(1)
    
    caller.bank_contains("Coins")
    time.sleep(0.5)
    
    caller.get_bank_item_count("Coins")
    time.sleep(0.5)
    
    caller.close_bank()
    time.sleep(1)
    
    # Test inventory methods
    print("\n5. Testing inventory methods:")
    caller.get_inventory_count()
    time.sleep(0.5)
    
    # Test player state methods
    print("\n6. Testing player state methods:")
    caller.is_player_animating()
    time.sleep(0.5)
    
    caller.get_skill_level("Woodcutting")
    time.sleep(0.5)
    
    print("\n=== Enhanced Walking Test Completed ===")

def test_banking_workflow():
    """Test a complete banking workflow"""
    print("\n=== Testing Complete Banking Workflow ===")
    
    caller = JavaMethodCaller()
    
    print("1. Opening bank...")
    caller.open_bank()
    time.sleep(1)
    
    print("2. Depositing all except Dragon axe...")
    caller.deposit_all_except("Dragon axe")
    time.sleep(1)
    
    print("3. Withdrawing 5 logs...")
    caller.withdraw_item("Logs", 5)
    time.sleep(1)
    
    print("4. Closing bank...")
    caller.close_bank()
    time.sleep(1)
    
    print("Banking workflow test completed!")

if __name__ == "__main__":
    import sys
    
    if len(sys.argv) > 1 and sys.argv[1].lower() == "banking":
        test_banking_workflow()
    else:
        test_enhanced_walking()
