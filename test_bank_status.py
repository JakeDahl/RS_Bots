#!/usr/bin/env python3
"""
Test script to verify that bank status checking works correctly
with the actual response from Java through the DreamBot shim.
"""

import sys
import os
import time

# Add the path to import the MCP modules
sys.path.append('/Users/jakedahl/Documents/Cline/MCP/runescape_mcp')

from java_caller import JavaMethodCaller

def test_bank_status():
    """Test the bank status check with response waiting enabled."""
    print("=" * 60)
    print("Testing Bank Operations with Response Waiting")
    print("=" * 60)
    
    # Create JavaMethodCaller with response waiting enabled
    java_caller = JavaMethodCaller(
        wait_for_responses=True,
        response_pipe_path="/tmp/dreambot_shim_response_pipe"
    )
    
    print("\n1. Testing bank status check...")
    print("   Sending bankIsOpen command to Java shim...")
    
    # Call the check_bank_open method
    response = java_caller.check_bank_open()
    
    print(f"\n   Response received:")
    print(f"   - Success: {response['success']}")
    print(f"   - Result: {response['result']}")
    print(f"   - Error: {response['error']}")
    
    # Interpret the result
    bank_is_open = False
    if response['success']:
        if response['result'] == "status_unknown":
            print("\n   ⚠️  Fallback mode - actual status unknown")
            print("   This means response waiting might not be working correctly")
        elif isinstance(response['result'], bool):
            bank_is_open = response['result']
            status = "OPEN" if bank_is_open else "CLOSED"
            print(f"\n   ✅ Bank is {status}")
            print("   Response waiting is working correctly!")
        elif isinstance(response['result'], str):
            # Handle string responses like "true" or "false"
            if response['result'].lower() == 'true':
                bank_is_open = True
                print("\n   ✅ Bank is OPEN")
            elif response['result'].lower() == 'false':
                bank_is_open = False
                print("\n   ✅ Bank is CLOSED")
            else:
                print(f"\n   Bank status: {response['result']}")
            print("   Response waiting is working correctly!")
        else:
            print(f"\n   Unexpected result type: {type(response['result'])}")
    else:
        print(f"\n   ❌ Error: {response['error']}")
    
    print("\n" + "=" * 60)
    
    # Test close bank functionality if bank is open
    if bank_is_open:
        print("\n2. Testing close bank functionality...")
        print("   Bank is currently open, testing close_bank command...")
        
        close_response = java_caller.close_bank()
        print(f"\n   Close bank response:")
        print(f"   - Success: {close_response['success']}")
        print(f"   - Result: {close_response['result']}")
        print(f"   - Error: {close_response['error']}")
        
        if close_response['success']:
            print("\n   ✅ Close bank command executed successfully!")
            
            # Check status again to confirm it's closed
            print("\n   Checking bank status after close...")
            recheck_response = java_caller.check_bank_open()
            if recheck_response['success']:
                if isinstance(recheck_response['result'], bool) and not recheck_response['result']:
                    print("   ✅ Bank is now CLOSED - close operation successful!")
                elif isinstance(recheck_response['result'], str) and recheck_response['result'].lower() == 'false':
                    print("   ✅ Bank is now CLOSED - close operation successful!")
                else:
                    print(f"   Bank status after close: {recheck_response['result']}")
        else:
            print(f"\n   ❌ Close bank failed: {close_response['error']}")
    else:
        print("\n2. Bank close test skipped...")
        print("   Bank is currently closed, no need to test close functionality")
    
    print("\n" + "=" * 60)
    
    # Test with fallback mode for comparison
    print("\n3. Testing with fallback mode (for comparison)...")
    fallback_caller = JavaMethodCaller(
        wait_for_responses=False,
        response_pipe_path="/tmp/dreambot_shim_response_pipe"
    )
    
    print("\n   Testing bank status check in fallback mode...")
    fallback_response = fallback_caller.check_bank_open()
    print(f"   - Success: {fallback_response['success']}")
    print(f"   - Result: {fallback_response['result']}")
    print(f"   - Error: {fallback_response['error']}")
    
    print("\n   Testing close bank in fallback mode...")
    fallback_close = fallback_caller.close_bank()
    print(f"   - Success: {fallback_close['success']}")
    print(f"   - Result: {fallback_close['result']}")
    print(f"   - Error: {fallback_close['error']}")
    
    print("\n" + "=" * 60)
    print("Test complete!")
    print("=" * 60)

if __name__ == "__main__":
    # Make sure the DreamBot shim is running
    print("\n⚠️  Make sure the DreamBot shim is running before testing!")
    print("Press Enter to continue or Ctrl+C to cancel...")
    input()
    
    test_bank_status()
