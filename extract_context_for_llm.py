#!/usr/bin/env python3
"""
Extract minimal context from code index for local LLM code generation.

This creates a compact context package with just signatures, javadoc, and structure
that a local coding LLM (like Qwen) needs to generate correct code.

Usage:
    python3 extract_context_for_llm.py search <term>
    python3 extract_context_for_llm.py pattern <pattern_type>
    python3 extract_context_for_llm.py file <file_path>
"""

import sys
import json
from query_code_index import load_index, search_by_name, search_by_pattern, get_file_summary

def extract_minimal_context(results, max_items=10):
    """Extract just the essential info for code generation"""
    context = {
        "classes": [],
        "methods": [],
        "imports": set(),
        "patterns": []
    }

    count = 0
    for item in results[:max_items]:
        if count >= max_items:
            break

        if item.get('type') == 'class':
            context['classes'].append({
                'name': item['name'],
                'package': item.get('package', ''),
                'file': item['file']
            })
        elif item.get('type') in ['method', 'event_handler', 'static_method']:
            context['methods'].append({
                'name': item['name'],
                'signature': item.get('signature', ''),
                'return_type': item.get('return_type', ''),
                'class_file': item['file'],
                'javadoc': item.get('javadoc', ''),
                'tags': item.get('tags', [])
            })

        count += 1

    # Convert set to list for JSON
    context['imports'] = list(context['imports'])

    return context

def format_for_llm(context, task_description=""):
    """Format context as a concise LLM prompt addition"""
    output = []

    if task_description:
        output.append(f"# Task: {task_description}\n")

    output.append("# Available Classes:\n")
    for cls in context['classes']:
        output.append(f"  - {cls['package']}.{cls['name']}")
    output.append("")

    output.append("# Available Methods:\n")
    for method in context['methods'][:15]:  # Limit to 15 most relevant
        output.append(f"  - {method['signature']}")
        if method['javadoc']:
            # First line of javadoc only
            first_line = method['javadoc'].split('\n')[0][:100]
            output.append(f"    // {first_line}")
        output.append(f"    // Tags: {', '.join(method['tags'])}")
        output.append("")

    return '\n'.join(output)

def main():
    if len(sys.argv) < 3:
        print("Usage:")
        print("  extract_context_for_llm.py search <term> [task_description]")
        print("  extract_context_for_llm.py pattern <pattern_type> [task_description]")
        print("  extract_context_for_llm.py file <file_path> [task_description]")
        sys.exit(1)

    command = sys.argv[1]
    arg = sys.argv[2]
    task_desc = sys.argv[3] if len(sys.argv) > 3 else ""

    # Load index
    index = load_index('code_index.json')

    # Get results based on command
    if command == 'search':
        results = search_by_name(index, arg)
    elif command == 'pattern':
        results = search_by_pattern(index, arg)
    elif command == 'file':
        file_summary = get_file_summary(index, arg)
        if file_summary:
            # Convert file summary to results format
            results = []
            for method_name, method_data in file_summary.get('methods', {}).items():
                results.append({
                    'type': 'method',
                    'name': method_name,
                    'signature': method_data['signature'],
                    'return_type': method_data.get('return_type', ''),
                    'file': file_summary['file'],
                    'javadoc': method_data.get('javadoc_preview', ''),
                    'tags': method_data.get('tags', [])
                })
        else:
            print(f"File not found: {arg}")
            sys.exit(1)
    else:
        print(f"Unknown command: {command}")
        sys.exit(1)

    # Extract minimal context
    context = extract_minimal_context(results)

    # Format for LLM
    llm_context = format_for_llm(context, task_desc)

    print(llm_context)
    print("\n" + "="*60)
    print(f"Context extracted: {len(context['classes'])} classes, {len(context['methods'])} methods")
    print(f"Estimated tokens: ~{len(llm_context.split())} words")
    print("="*60)

if __name__ == '__main__':
    main()
