#!/usr/bin/env python3
"""
Wurm Server Decompiled Code Indexer

Indexes the decompiled Wurm server JAR for bytecode patch generation.
This creates a separate index from the framework code.

Usage:
    python3 index_wurm_server.py

Outputs: wurm_server_index.json
"""

import os
import sys
from pathlib import Path

# Reuse the framework indexer code
sys.path.insert(0, str(Path(__file__).parent))
from index_code_index import build_code_index, write_code_index

def main():
    # Decompiled Wurm server path (just the server package - the important stuff)
    base_path = os.path.expanduser(
        '~/Scripts/Games/WurmUnlimited/PowerFantasy/Wurmguide/decompiled/server_decompiled/com/wurmonline/server'
    )

    # Output to separate file
    output_path = os.path.join(
        os.path.expanduser('~/Scripts/Games/WurmUnlimited/WurmModLoader'),
        'wurm_server_index.json'
    )

    print("=" * 70)
    print("Wurm Server Decompiled Code Indexer")
    print("=" * 70)
    print(f"📂 Scanning: {base_path}")
    print(f"📝 Output: {output_path}")
    print("=" * 70)

    # Build index
    index, summary = build_code_index(
        base_path,
        exclude_dirs={'.git', '.gradle', 'build', 'out', 'target', '.idea', 'bin'}
    )

    # Add metadata
    summary['source'] = 'wurm_server_decompiled'
    summary['purpose'] = 'bytecode_patch_targets'

    write_code_index(index, summary, output_path)

    print("\n" + "=" * 70)
    print(f"✅ Wurm Server index written to: {output_path}")
    print("=" * 70)
    print("📊 Summary:")
    print(f"   Files: {summary['files_scanned']}")
    print(f"   Classes: {summary['classes']}")
    print(f"   Methods: {summary['methods']}")
    print("=" * 70)
    print("\n💡 Use combined_index_query.py to search both indexes!")

if __name__ == '__main__':
    main()
