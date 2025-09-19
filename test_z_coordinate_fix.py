#!/usr/bin/env python3

import sys
import os
sys.path.append(os.path.join(os.path.dirname(__file__), '..', 'Documents', 'Cline', 'MCP', 'runescape_mcp'))

from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

async def test_walk_to_location():
    """Test walking to a location with z-coordinate"""
    server_params = StdioServerParameters(
        command="python3",
        args=["/Users/jakedahl/Documents/Cline/MCP/runescape_mcp/server.py"],
        env=None
    )
    
    async with stdio_client(server_params) as (read, write):
        async with ClientSession(read, write) as session:
            # Initialize the session
            await session.initialize()
            
            print("Testing walk to location with z-coordinate...")
            
            # Test walking to coordinates with a specific z level
            result = await session.call_tool(
                "walk_to_location",
                {"x": 3164, "y": 3306, "z": 2}
            )
            
            print(f"Walk result: {result.content[0].text}")
            
            # Test getting current tile to see coordinates
            current_tile_result = await session.call_tool("get_current_tile", {})
            print(f"Current tile: {current_tile_result.content[0].text}")

if __name__ == "__main__":
    import asyncio
    asyncio.run(test_walk_to_location())
