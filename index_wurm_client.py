#!/usr/bin/env python3
"""
Wurm Client Decompiled Code Indexer

Indexes the decompiled Wurm client JAR for bytecode patch generation.
This creates a separate index from the framework code.

Usage:
    python3 index_wurm_client.py

Outputs: wurm_client_index.json
"""

import os
import sys
from pathlib import Path

# Reuse the framework indexer code
sys.path.insert(0, str(Path(__file__).parent))
from index_code_index import build_code_index, write_code_index

def main():
    # Decompiled Wurm client path (just the client package - the important stuff)
    base_path = os.path.expanduser(
        '~/Scripts/Games/WurmUnlimited/PowerFantasy/Wurmguide/decompiled/client_decompiled/com/wurmonline/client'
    )

    # Output to separate file
    output_path = os.path.join(
        os.path.expanduser('~/Scripts/Games/WurmUnlimited/WurmModLoader'),
        'wurm_client_index.json'
    )

    print("=" * 70)
    print("Wurm Client Decompiled Code Indexer")
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
    summary['source'] = 'wurm_client_decompiled'
    summary['purpose'] = 'bytecode_patch_targets'
    summary['decompilers'] = 'CFR 0.152 + Procyon 0.6.0'
    summary['notes'] = 'WurmClientBase.class decompiled with Procyon (monolith class)'

    write_code_index(index, summary, output_path)

    print("\n" + "=" * 70)
    print(f"✅ Wurm Client index written to: {output_path}")
    print("=" * 70)
    print("📊 Summary:")
    print(f"   Files: {summary['files_scanned']}")
    print(f"   Classes: {summary['classes']}")
    print(f"   Methods: {summary['methods']}")
    print("=" * 70)
    print("\n💡 Use combined_index_query.py to search both server and client indexes!")

if __name__ == '__main__':
    main()
