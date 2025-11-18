#!/usr/bin/env python3
"""
Combined Index Query Tool

Queries both framework code index and Wurm server index simultaneously.
Perfect for finding bytecode patch targets and understanding relationships.

Usage:
    python3 combined_index_query.py search "MethodName"
    python3 combined_index_query.py pattern event_handlers
    python3 combined_index_query.py compare "ServerHook"

Commands:
    search <term>       - Search both indexes for methods/classes
    pattern <type>      - Find patterns in both indexes
    compare <term>      - Compare results from both indexes side-by-side
    server-methods      - List all methods in server index (for patch targets)
    framework-hooks     - List all hooks/patches in framework
    overview            - Show overview of both indexes
"""

import sys
import json
from pathlib import Path
from typing import Dict, List, Any, Optional

# Reuse query functions from existing tool
sys.path.insert(0, str(Path(__file__).parent))
from query_code_index import (
    load_index,
    search_by_name,
    search_by_pattern,
    generate_overview,
    search_by_tag
)

# ============================================================================
# Index Paths
# ============================================================================

FRAMEWORK_INDEX = Path(__file__).parent / "code_index.json"
SERVER_INDEX = Path(__file__).parent / "wurm_server_index.json"


# ============================================================================
# Combined Query Functions
# ============================================================================

def load_both_indexes() -> tuple[Dict[str, Any], Optional[Dict[str, Any]]]:
    """Load both indexes, return (framework, server)"""
    framework = load_index(str(FRAMEWORK_INDEX))

    if SERVER_INDEX.exists():
        server = load_index(str(SERVER_INDEX))
    else:
        server = None
        print("⚠️  Wurm server index not found (still generating?)")

    return framework, server


def combined_search(term: str) -> None:
    """Search both indexes and display results"""
    framework, server = load_both_indexes()

    print("=" * 80)
    print(f"🔍 Combined Search: '{term}'")
    print("=" * 80)

    # Framework results
    print("\n📦 FRAMEWORK (WurmModLoader)")
    print("-" * 80)
    fw_results = search_by_name(framework, term)

    if fw_results:
        for i, result in enumerate(fw_results[:15], 1):
            file = result['file'].split('/')[-1]
            print(f"{i}. {result['signature']}")
            print(f"   📁 {file}")
            if result.get('tags'):
                print(f"   🏷️  {', '.join(result['tags'])}")
            if result.get('javadoc'):
                doc = result['javadoc'][:80] + "..." if len(result['javadoc']) > 80 else result['javadoc']
                print(f"   📝 {doc}")
            print()
    else:
        print("No results found in framework.\n")

    # Server results
    if server:
        print("\n🎮 WURM SERVER (Decompiled)")
        print("-" * 80)
        srv_results = search_by_name(server, term)

        if srv_results:
            for i, result in enumerate(srv_results[:15], 1):
                file = result['file'].split('/')[-1]
                sig = result.get('signature', result.get('name', 'Unknown'))
                print(f"{i}. {sig}")
                print(f"   📁 {file}")
                if result.get('javadoc'):
                    doc = result['javadoc'][:80] + "..." if len(result['javadoc']) > 80 else result['javadoc']
                    print(f"   📝 {doc}")
                print()
        else:
            print("No results found in server.\n")

    print("=" * 80)


def combined_pattern(pattern_type: str) -> None:
    """Search patterns in both indexes"""
    framework, server = load_both_indexes()

    print("=" * 80)
    print(f"🔍 Pattern Search: '{pattern_type}'")
    print("=" * 80)

    # Framework patterns
    print("\n📦 FRAMEWORK")
    print("-" * 80)
    fw_results = search_by_pattern(framework, pattern_type)

    if fw_results:
        for i, result in enumerate(fw_results[:20], 1):
            file = result['file'].split('/')[-1]
            print(f"{i}. {result['signature']}")
            print(f"   📁 {file}")
            print()
    else:
        print(f"No '{pattern_type}' patterns found in framework.\n")

    # Server patterns (if available)
    if server:
        print("\n🎮 WURM SERVER")
        print("-" * 80)
        srv_results = search_by_pattern(server, pattern_type)

        if srv_results:
            for i, result in enumerate(srv_results[:20], 1):
                file = result['file'].split('/')[-1]
                sig = result.get('signature', result.get('name', 'Unknown'))
                print(f"{i}. {sig}")
                print(f"   📁 {file}")
                print()
        else:
            print(f"No '{pattern_type}' patterns found in server.\n")

    print("=" * 80)


def compare_search(term: str) -> None:
    """Side-by-side comparison of framework vs server results"""
    framework, server = load_both_indexes()

    fw_results = search_by_name(framework, term)
    srv_results = search_by_name(server, term) if server else []

    print("=" * 80)
    print(f"⚖️  Comparison: '{term}'")
    print("=" * 80)

    print(f"\n📦 Framework: {len(fw_results)} results")
    print(f"🎮 Server: {len(srv_results)} results")

    if fw_results:
        print("\n" + "=" * 80)
        print("📦 FRAMEWORK MATCHES")
        print("=" * 80)
        for result in fw_results[:10]:
            print(f"• {result['signature']}")
            print(f"  File: {result['file'].split('/')[-1]}")
            if result.get('tags'):
                print(f"  Tags: {', '.join(result['tags'])}")
            print()

    if srv_results:
        print("=" * 80)
        print("🎮 SERVER MATCHES")
        print("=" * 80)
        for result in srv_results[:10]:
            sig = result.get('signature', result.get('name', 'Unknown'))
            print(f"• {sig}")
            print(f"  File: {result['file'].split('/')[-1]}")
            print()

    print("=" * 80)


def list_server_methods() -> None:
    """List all methods in server index (for finding patch targets)"""
    framework, server = load_both_indexes()

    if not server:
        print("❌ Server index not available")
        return

    print("=" * 80)
    print("🎮 Wurm Server Methods (Patch Targets)")
    print("=" * 80)

    methods = []
    for file_data in server.get('files', {}).values():
        for class_name, class_data in file_data.get('classes', {}).items():
            for method_name, method_data in class_data.get('methods', {}).items():
                methods.append({
                    'class': class_name,
                    'method': method_name,
                    'signature': method_data['signature'],
                    'file': file_data['file_path']
                })

    # Group by class
    by_class = {}
    for m in methods:
        cls = m['class']
        if cls not in by_class:
            by_class[cls] = []
        by_class[cls].append(m)

    # Display grouped
    for cls, methods_list in sorted(by_class.items())[:30]:
        print(f"\n📦 {cls}")
        print(f"   File: {methods_list[0]['file'].split('/')[-1]}")
        print(f"   Methods: {len(methods_list)}")
        for m in methods_list[:5]:
            print(f"     • {m['signature']}")
        if len(methods_list) > 5:
            print(f"     ... and {len(methods_list) - 5} more")

    print(f"\n📊 Total: {len(methods)} methods across {len(by_class)} classes")
    print("=" * 80)


def list_framework_hooks() -> None:
    """List all hooks and bytecode patches in framework"""
    framework, server = load_both_indexes()

    print("=" * 80)
    print("🔧 Framework Hooks & Patches")
    print("=" * 80)

    # Find all bytecode patches
    patches = search_by_tag(framework, 'bytecode_patch')

    print(f"\n🔨 Bytecode Patches: {len(patches)}")
    print("-" * 80)
    for patch in patches[:20]:
        print(f"• {patch['name']}")
        print(f"  File: {patch['file'].split('/')[-1]}")
        if patch.get('javadoc'):
            doc = patch['javadoc'][:80] + "..." if len(patch['javadoc']) > 80 else patch['javadoc']
            print(f"  {doc}")
        print()

    # Find all hooks
    hooks = search_by_tag(framework, 'hook')

    print(f"\n🪝 Event Hooks: {len(hooks)}")
    print("-" * 80)
    for hook in hooks[:20]:
        print(f"• {hook['signature']}")
        print(f"  File: {hook['file'].split('/')[-1]}")
        print()

    print("=" * 80)


def combined_overview() -> None:
    """Overview of both indexes"""
    framework, server = load_both_indexes()

    print("=" * 80)
    print("📊 Combined Index Overview")
    print("=" * 80)

    print("\n📦 FRAMEWORK (WurmModLoader)")
    print("-" * 80)
    fw_overview = generate_overview(framework)
    print(f"Files: {fw_overview['total_files']}")
    print(f"Classes: {fw_overview['total_classes']}")
    print(f"Methods: {fw_overview['total_methods']}")
    print(f"Last Build: {fw_overview['last_build']}")

    if 'top_tags' in fw_overview:
        print("\nTop Tags:")
        for tag, count in list(fw_overview['top_tags'].items())[:10]:
            print(f"  {tag}: {count}")

    if server:
        print("\n\n🎮 WURM SERVER (Decompiled)")
        print("-" * 80)
        srv_overview = generate_overview(server)
        print(f"Files: {srv_overview['total_files']}")
        print(f"Classes: {srv_overview['total_classes']}")
        print(f"Methods: {srv_overview['total_methods']}")
        print(f"Last Build: {srv_overview['last_build']}")

        print("\n\n📈 COMBINED TOTALS")
        print("-" * 80)
        print(f"Total Files: {fw_overview['total_files'] + srv_overview['total_files']}")
        print(f"Total Classes: {fw_overview['total_classes'] + srv_overview['total_classes']}")
        print(f"Total Methods: {fw_overview['total_methods'] + srv_overview['total_methods']}")

    print("\n" + "=" * 80)


# ============================================================================
# CLI
# ============================================================================

def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)

    command = sys.argv[1].lower()

    if command == "search":
        if len(sys.argv) < 3:
            print("Usage: combined_index_query.py search <term>")
            sys.exit(1)
        combined_search(sys.argv[2])

    elif command == "pattern":
        if len(sys.argv) < 3:
            print("Usage: combined_index_query.py pattern <type>")
            sys.exit(1)
        combined_pattern(sys.argv[2])

    elif command == "compare":
        if len(sys.argv) < 3:
            print("Usage: combined_index_query.py compare <term>")
            sys.exit(1)
        compare_search(sys.argv[2])

    elif command == "server-methods":
        list_server_methods()

    elif command == "framework-hooks":
        list_framework_hooks()

    elif command == "overview":
        combined_overview()

    else:
        print(f"Unknown command: {command}")
        print(__doc__)
        sys.exit(1)


if __name__ == '__main__':
    main()
