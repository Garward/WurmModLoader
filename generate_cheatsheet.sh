#!/bin/bash
# Generate a compact LLM-friendly cheatsheet of the codebase

echo "# WurmModLoader Codebase Cheatsheet"
echo "Generated: $(date)"
echo ""
echo "## Overview"
python3 query_code_index.py overview
echo ""
echo "---"
echo ""
echo "## High Priority Files (Top 20)"
python3 query_code_index.py --max-lines 120 high-priority
echo ""
echo "---"
echo ""
echo "## Event System"
python3 query_code_index.py --max-lines 60 pattern event_handlers
echo ""
echo "---"
echo ""
echo "## Bytecode Patches"
python3 query_code_index.py --max-lines 60 pattern bytecode_patches
echo ""
echo "---"
echo ""
echo "## API Overview"
python3 query_code_index.py --max-lines 100 module "wurmmodloader-api"
echo ""
echo "---"
echo ""
echo "## Core Hooks"
python3 query_code_index.py --max-lines 80 search "Hook" | head -100
