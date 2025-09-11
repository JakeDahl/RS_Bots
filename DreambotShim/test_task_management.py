#!/usr/bin/env python3
"""
Test script to demonstrate the task management functionality in the DreamBot shim.
This script shows how to add upcoming steps that will be displayed in the UI.
"""

import json
import time

def send_to_dreambot(method_name, *args):
    """Send a method call to the DreamBot named pipe."""
    pipe_name = "/tmp/dreambot_shim_pipe"
    
    try:
        request = {
            "method": method_name,
            "args": list(args)
        }
        
        json_request = json.dumps(request)
        print(f"Sending: {json_request}")
        
        with open(pipe_name, 'w') as pipe:
            pipe.write(json_request + '\n')
            pipe.flush()
        
        print(f"✓ Sent {method_name} successfully")
        
    except Exception as e:
        print(f"✗ Error sending {method_name}: {e}")

def test_task_management():
    """Test the task management functionality."""
    print("=== DreamBot Task Management Test ===")
    print()
    
    # Clear any existing steps
    print("1. Clearing existing upcoming steps...")
    send_to_dreambot("clearUpcomingSteps")
    time.sleep(1)
    
    # Add some upcoming steps
    print("\n2. Adding upcoming steps...")
    send_to_dreambot("addUpcomingStep", "Walk to bank area")
    time.sleep(0.5)
    
    send_to_dreambot("addUpcomingStep", "Open bank booth")
    time.sleep(0.5)
    
    send_to_dreambot("addUpcomingStep", "Deposit all items except pickaxe")
    time.sleep(0.5)
    
    send_to_dreambot("addUpcomingStep", "Withdraw 28 iron ore")
    time.sleep(0.5)
    
    send_to_dreambot("addUpcomingStep", "Close bank")
    time.sleep(0.5)
    
    send_to_dreambot("addUpcomingStep", "Walk to furnace")
    time.sleep(0.5)
    
    # Check how many steps we have
    print("\n3. Checking upcoming steps count...")
    send_to_dreambot("getUpcomingStepsCount")
    time.sleep(1)
    
    # Peek at the next step
    print("\n4. Peeking at next step...")
    send_to_dreambot("peekNextStep")
    time.sleep(1)
    
    # Set current step and simulate some work
    print("\n5. Simulating task execution...")
    send_to_dreambot("setCurrentStep", "Preparing to start mining run...")
    time.sleep(2)
    
    # Remove and execute the first step
    print("\n6. Getting and executing next step...")
    send_to_dreambot("getNextStep")
    time.sleep(1)
    
    # Simulate walking to bank
    send_to_dreambot("setCurrentStep", "Walking to bank area...")
    time.sleep(3)
    
    # Get the next step and execute it
    send_to_dreambot("getNextStep")
    time.sleep(1)
    
    # Simulate opening bank
    send_to_dreambot("openBank")
    time.sleep(2)
    
    # Check remaining steps
    print("\n7. Checking remaining steps...")
    send_to_dreambot("getUpcomingStepsCount")
    time.sleep(1)
    
    print("\n=== Test Complete ===")
    print("Check the DreamBot UI to see the upcoming steps displayed!")
    print("You should see the remaining steps in the 'Upcoming Steps' section.")

if __name__ == "__main__":
    test_task_management()
