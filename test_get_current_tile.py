#!/usr/bin/env python3
"""
Test script for the get_current_tile MCP tool
"""

import asyncio
import mcp.types as types
from mcp.client import Client
from mcp.client.session import ClientSession
from mcp.client.stdio import StdioServerParameters

async def test_get_current_tile():
    """Test the get_current_tile tool via MCP."""
    
    # Connect to the MCP server
    server_params = StdioServerParameters(
        command="python3",
        args=["/Users/jakedahl/Documents/Cline/MCP/runescape_mcp/server.py"]
    )
    
    async with mcp.client.stdio.stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            # Initialize the session
            await session.initialize()
            
            # List available tools to verify our tool is there
            tools = await session.list_tools()
            print("Available tools:")
            for tool in tools.tools:
                print(f"  - {tool.name}: {tool.description}")
            
            # Check if our tool is in the list
            get_current_tile_tool = None
            for tool in tools.tools:
                if tool.name == "get_current_tile":
                    get_current_tile_tool = tool
                    break
            
            if get_current_tile_tool:
                print(f"\n✓ Found get_current_tile tool: {get_current_tile_tool.description}")
                
                # Test calling the tool
                print("\nTesting get_current_tile tool...")
                try:
                    result = await session.call_tool(
                        name="get_current_tile",
                        arguments={}
                    )
                    
                    print("Tool call result:")
                    for content in result.content:
                        if content.type == "text":
                            print(f"  {content.text}")
                    
                    print("\n✓ get_current_tile tool test completed successfully!")
                    
                except Exception as e:
                    print(f"✗ Error calling get_current_tile tool: {e}")
            else:
                print("\n✗ get_current_tile tool not found in available tools")

if __name__ == "__main__":
    print("Testing get_current_tile MCP tool...")
    print("=" * 50)
    asyncio.run(test_get_current_tile())
