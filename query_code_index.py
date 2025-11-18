#!/usr/bin/env python3
"""
Query and filter the code index for LLM context
Generates compact, relevant views of the codebase
"""

import json
import sys
import argparse
from collections import defaultdict

def load_index(index_path='code_index.json'):
    """Load the code index"""
    with open(index_path, 'r', encoding='utf-8') as f:
        return json.load(f)

def search_by_tag(index, tag):
    """Find all methods/classes with a specific tag"""
    results = []
    for file_path, file_data in index['index'].items():
        for method_name, method_data in file_data.get('methods', {}).items():
            if tag in method_data.get('tags', []):
                results.append({
                    'file': file_path,
                    'name': method_name,
                    'signature': method_data['signature'],
                    'type': method_data['type'],
                    'tags': method_data['tags']
                })
    return results

def search_by_name(index, search_term):
    """Search for methods/classes by name"""
    results = []
    search_lower = search_term.lower()

    for file_path, file_data in index['index'].items():
        # Search methods
        for method_name, method_data in file_data.get('methods', {}).items():
            if search_lower in method_name.lower():
                results.append({
                    'file': file_path,
                    'type': 'method',
                    'name': method_name,
                    'signature': method_data['signature'],
                    'method_type': method_data['type'],
                    'javadoc': method_data.get('javadoc_preview', ''),
                    'tags': method_data['tags'],
                    'line': method_data.get('line', 0)
                })

        # Search classes
        for class_name in file_data.get('classes', []):
            if search_lower in class_name.lower():
                results.append({
                    'file': file_path,
                    'type': 'class',
                    'name': class_name,
                    'package': file_data.get('package', '')
                })

    return results

def get_file_summary(index, file_path):
    """Get a summary of a specific file"""
    for path, data in index['index'].items():
        if file_path in path:
            summary = {
                'file': path,
                'package': data.get('package', ''),
                'classes': data.get('classes', []),
                'methods': [
                    {
                        'name': name,
                        'signature': info['signature'],
                        'type': info['type'],
                        'tags': info['tags']
                    }
                    for name, info in data.get('methods', {}).items()
                ],
                'priority': data.get('priority_level', ''),
                'imports': data.get('imports', [])[:10]  # Limit imports
            }
            return summary
    return None

def generate_overview(index):
    """Generate a compact overview of the codebase"""
    tag_counts = defaultdict(int)
    priority_counts = defaultdict(int)
    package_counts = defaultdict(int)

    for file_path, file_data in index['index'].items():
        # Count priorities
        priority_counts[file_data.get('priority_level', 'unknown')] += 1

        # Count packages
        pkg = file_data.get('package', '')
        if pkg:
            # Get top-level package
            top_pkg = '.'.join(pkg.split('.')[:4]) if pkg else 'unknown'
            package_counts[top_pkg] += 1

        # Count tags
        for method_data in file_data.get('methods', {}).values():
            for tag in method_data.get('tags', []):
                tag_counts[tag] += 1

    overview = {
        'total_files': index['total_files'],
        'total_classes': index['total_classes'],
        'total_methods': index['total_methods'],
        'last_build': index['last_build_time'],
        'priority_distribution': dict(priority_counts),
        'top_packages': dict(sorted(package_counts.items(), key=lambda x: x[1], reverse=True)[:15]),
        'top_tags': dict(sorted(tag_counts.items(), key=lambda x: x[1], reverse=True)[:20])
    }

    return overview

def get_high_priority_files(index):
    """Get list of high priority files with summaries"""
    high_priority = []

    for file_path, file_data in index['index'].items():
        if file_data.get('priority_level') == 'high':
            high_priority.append({
                'file': file_path,
                'package': file_data.get('package', ''),
                'classes': file_data.get('classes', []),
                'method_count': len(file_data.get('methods', {})),
                'key_methods': [
                    f"{name}: {info['type']}"
                    for name, info in list(file_data.get('methods', {}).items())[:5]
                ]
            })

    return sorted(high_priority, key=lambda x: x['method_count'], reverse=True)

def search_by_pattern(index, pattern_type):
    """Search for specific patterns"""
    patterns = {
        'event_handlers': ['event_handler', 'event', 'hook'],
        'bytecode_patches': ['bytecode_patch', 'bytecode_modification'],
        'config': ['configuration', 'config'],
        'combat': ['combat'],
        'initialization': ['initialization'],
        'api': ['api']
    }

    if pattern_type not in patterns:
        return []

    results = []
    tags_to_search = patterns[pattern_type]

    for file_path, file_data in index['index'].items():
        for method_name, method_data in file_data.get('methods', {}).items():
            if any(tag in method_data.get('tags', []) for tag in tags_to_search):
                results.append({
                    'file': file_path,
                    'name': method_name,
                    'signature': method_data['signature'],
                    'type': method_data['type'],
                    'tags': method_data['tags'],
                    'javadoc': method_data.get('javadoc_preview', '')[:100]
                })

    return results

def generate_module_summary(index, module_path):
    """Generate summary for a specific module/directory"""
    files = []
    total_methods = 0
    total_classes = 0

    for file_path, file_data in index['index'].items():
        if module_path in file_path:
            files.append({
                'file': file_path.replace(module_path, '').lstrip('/'),
                'classes': file_data.get('classes', []),
                'method_count': len(file_data.get('methods', {}))
            })
            total_methods += len(file_data.get('methods', {}))
            total_classes += len(file_data.get('classes', []))

    return {
        'module': module_path,
        'file_count': len(files),
        'total_classes': total_classes,
        'total_methods': total_methods,
        'files': sorted(files, key=lambda x: x['method_count'], reverse=True)
    }

def format_compact_output(data, max_lines=100):
    """Format output to fit within LLM context constraints"""
    output = json.dumps(data, indent=2)
    lines = output.split('\n')

    if len(lines) <= max_lines:
        return output

    # Truncate and add indicator
    truncated = '\n'.join(lines[:max_lines])
    remaining = len(lines) - max_lines
    truncated += f"\n... ({remaining} more lines truncated)"
    return truncated

def main():
    parser = argparse.ArgumentParser(description='Query WurmModLoader code index')
    parser.add_argument('--index', default='code_index.json', help='Path to code index')

    subparsers = parser.add_subparsers(dest='command', help='Command to run')

    # Overview command
    subparsers.add_parser('overview', help='Generate codebase overview')

    # Search commands
    search_name = subparsers.add_parser('search', help='Search by name')
    search_name.add_argument('term', help='Search term')

    search_tag = subparsers.add_parser('tag', help='Search by tag')
    search_tag.add_argument('tag', help='Tag to search for')

    # File command
    file_cmd = subparsers.add_parser('file', help='Get file summary')
    file_cmd.add_argument('path', help='File path (partial match)')

    # High priority
    subparsers.add_parser('high-priority', help='List high priority files')

    # Pattern search
    pattern_cmd = subparsers.add_parser('pattern', help='Search by pattern')
    pattern_cmd.add_argument('pattern_type',
                           choices=['event_handlers', 'bytecode_patches', 'config', 'combat', 'initialization', 'api'],
                           help='Pattern type to search')

    # Module summary
    module_cmd = subparsers.add_parser('module', help='Summarize a module/directory')
    module_cmd.add_argument('path', help='Module path')

    # Compact output option
    parser.add_argument('--max-lines', type=int, default=200, help='Max output lines (default: 200)')

    args = parser.parse_args()

    # Load index
    try:
        index = load_index(args.index)
    except FileNotFoundError:
        print(f"Error: Index file '{args.index}' not found. Run index_code_index.py first.")
        sys.exit(1)

    # Execute command
    if args.command == 'overview':
        result = generate_overview(index)
    elif args.command == 'search':
        result = search_by_name(index, args.term)
    elif args.command == 'tag':
        result = search_by_tag(index, args.tag)
    elif args.command == 'file':
        result = get_file_summary(index, args.path)
    elif args.command == 'high-priority':
        result = get_high_priority_files(index)
    elif args.command == 'pattern':
        result = search_by_pattern(index, args.pattern_type)
    elif args.command == 'module':
        result = generate_module_summary(index, args.path)
    else:
        parser.print_help()
        sys.exit(1)

    # Format and print
    output = format_compact_output(result, args.max_lines)
    print(output)

if __name__ == '__main__':
    main()
