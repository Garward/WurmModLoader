#!/usr/bin/env python3
"""
Java Codebase Indexer for WurmModLoader
Extracts classes, methods, fields, and annotations from Java source files
"""

import os
import json
import hashlib
import re
from datetime import datetime

def extract_java_annotations(text):
    """Extract Java annotations from a text block"""
    annotations = []
    # Match @AnnotationName and @AnnotationName(...)
    pattern = r'@(\w+)(?:\([^)]*\))?'
    matches = re.findall(pattern, text)
    return list(set(matches))

def extract_java_imports(content):
    """Extract Java import statements"""
    imports = []
    pattern = r'import\s+(static\s+)?([a-zA-Z0-9_.]+(?:\.\*)?);'
    matches = re.findall(pattern, content)
    for static, imp in matches:
        imports.append(imp)
    return list(set(imports))

def extract_package(content):
    """Extract Java package declaration"""
    match = re.search(r'package\s+([a-zA-Z0-9_.]+);', content)
    return match.group(1) if match else ""

def classify_java_method(name, modifiers, annotations):
    """Classify Java methods by their characteristics"""
    name_lower = name.lower()

    # Check annotations first
    if 'Override' in annotations:
        return 'override'
    if any(a in annotations for a in ['EventHandler', 'SubscribeEvent', 'Hook']):
        return 'event_handler'
    if any(a in annotations for a in ['Patch', 'Insert', 'Redirect']):
        return 'bytecode_patch'

    # Check modifiers
    if 'static' in modifiers:
        return 'static_method'

    # Check naming patterns
    if name == '<init>':
        return 'constructor'
    if name_lower.startswith(('on', 'handle', 'process')):
        return 'handler'
    if name_lower.startswith(('get', 'is', 'has')):
        return 'getter'
    if name_lower.startswith('set'):
        return 'setter'

    return 'method'

def extract_java_classes(content, source_lines):
    """Extract Java class definitions and their details"""
    classes = []

    # Pattern for class declarations (handles nested classes)
    class_pattern = r'(?:public|private|protected|static|\s)*\s*(?:class|interface|enum)\s+(\w+)'

    for match in re.finditer(class_pattern, content):
        class_name = match.group(1)
        line_num = content[:match.start()].count('\n') + 1

        # Extract annotations before class
        lines_before = source_lines[max(0, line_num - 10):line_num]
        annotations = extract_java_annotations('\n'.join(lines_before))

        classes.append({
            'name': class_name,
            'line': line_num,
            'annotations': annotations
        })

    return classes

def extract_java_methods(content, source_lines):
    """Extract Java method definitions"""
    methods = {}

    # Pattern for method declarations
    # Captures: modifiers, return type, method name, parameters
    method_pattern = r'((?:public|private|protected|static|final|synchronized|abstract|\s)+)\s+(\w+(?:<[^>]+>)?(?:\[\])?)\s+(\w+)\s*\(([^)]*)\)'

    for match in re.finditer(method_pattern, content):
        modifiers_str = match.group(1).strip()
        return_type = match.group(2)
        method_name = match.group(3)
        params = match.group(4)

        # Skip if this looks like a class declaration
        if any(keyword in modifiers_str for keyword in ['class', 'interface', 'enum']):
            continue

        line_num = content[:match.start()].count('\n') + 1

        # Extract modifiers
        modifiers = [m.strip() for m in modifiers_str.split() if m.strip()]

        # Extract annotations before method
        lines_before = source_lines[max(0, line_num - 10):line_num]
        annotations = extract_java_annotations('\n'.join(lines_before))

        # Extract javadoc
        javadoc = extract_javadoc(source_lines, line_num)

        # Get context snippet
        context = extract_context_snippet(source_lines, line_num)

        # Classify method
        method_type = classify_java_method(method_name, modifiers, annotations)

        # Create signature
        signature = f"{method_name}({params})"

        methods[method_name] = {
            'type': method_type,
            'signature': signature,
            'return_type': return_type,
            'modifiers': modifiers,
            'annotations': annotations,
            'javadoc_preview': javadoc,
            'tags': infer_java_tags(method_name, modifiers, annotations, ''),
            'context_snippet': context,
            'line': line_num
        }

    return methods

def extract_javadoc(source_lines, line_num):
    """Extract JavaDoc comment before a method/class"""
    javadoc_lines = []
    idx = line_num - 2  # Start one line before the declaration

    while idx >= 0:
        line = source_lines[idx].strip()
        if line.startswith('*/'):
            # Found end of javadoc, collect backwards
            while idx >= 0:
                line = source_lines[idx].strip()
                javadoc_lines.insert(0, line)
                if line.startswith('/**'):
                    break
                idx -= 1
            break
        elif line.startswith('//'):
            javadoc_lines.insert(0, line)
            idx -= 1
        elif not line:
            idx -= 1
        else:
            break

    # Clean up javadoc
    cleaned = '\n'.join(javadoc_lines)
    cleaned = re.sub(r'/\*\*|\*/|^\s*\*\s?', '', cleaned, flags=re.MULTILINE)

    # Limit to 3 lines, 300 chars
    lines = cleaned.strip().split('\n')[:3]
    preview = '\n'.join(lines)
    return preview[:300] if len(preview) > 300 else preview

def extract_context_snippet(source_lines, line_num):
    """Extract first 10 lines starting from line_num"""
    try:
        start = line_num - 1
        end = min(start + 10, len(source_lines))
        return '\n'.join(source_lines[start:end]).strip()
    except:
        return ""

def infer_java_tags(name, modifiers, annotations, file_path):
    """Infer semantic tags for Java code based on WurmModLoader patterns"""
    tags = []
    name_lower = name.lower()
    path_lower = file_path.lower()

    # Directory-based tags (WurmModLoader specific)
    if 'bytecode/patches' in path_lower or 'patch' in path_lower:
        tags.append('bytecode_patch')
    if 'hook' in path_lower or 'hooks' in path_lower:
        tags.append('hook')
    if 'event' in path_lower or 'events' in path_lower:
        tags.append('event')
    if '/mods/' in path_lower:
        tags.append('mod')
    if 'config' in path_lower:
        tags.append('configuration')
    if 'api' in path_lower:
        tags.append('api')
    if 'core' in path_lower:
        tags.append('core')
    if 'proxy' in path_lower:
        tags.append('proxy')
    if 'reflection' in path_lower:
        tags.append('reflection')

    # Annotation-based tags
    annotation_str = ' '.join(annotations)
    if 'Patch' in annotation_str or 'Insert' in annotation_str:
        tags.append('bytecode_patch')
    if 'EventHandler' in annotation_str or 'SubscribeEvent' in annotation_str:
        tags.append('event_handler')
    if 'Override' in annotation_str:
        tags.append('override')
    if 'Deprecated' in annotation_str:
        tags.append('deprecated')

    # Name pattern tags
    if any(x in name_lower for x in ['patch', 'inject', 'redirect']):
        tags.append('bytecode_modification')
    if any(x in name_lower for x in ['hook', 'callback']):
        tags.append('hook')
    if any(x in name_lower for x in ['event', 'listener']):
        tags.append('event')
    if any(x in name_lower for x in ['init', 'initialize', 'setup']):
        tags.append('initialization')
    if any(x in name_lower for x in ['config', 'setting']):
        tags.append('configuration')
    if any(x in name_lower for x in ['get', 'fetch', 'retrieve']):
        tags.append('accessor')
    if any(x in name_lower for x in ['set', 'update', 'modify']):
        tags.append('mutator')
    if any(x in name_lower for x in ['handle', 'process', 'execute']):
        tags.append('handler')
    if any(x in name_lower for x in ['combat', 'damage', 'attack']):
        tags.append('combat')
    if any(x in name_lower for x in ['skill', 'difficulty']):
        tags.append('skill')
    if any(x in name_lower for x in ['item', 'container', 'inventory']):
        tags.append('item')
    if any(x in name_lower for x in ['creature', 'player', 'npc']):
        tags.append('creature')
    if any(x in name_lower for x in ['spell', 'magic', 'enchant']):
        tags.append('magic')

    # Modifier-based tags
    if 'static' in modifiers:
        tags.append('static')
    if 'abstract' in modifiers:
        tags.append('abstract')
    if 'final' in modifiers:
        tags.append('final')

    return list(set(tags)) if tags else ['system']

def get_priority_level(file_path):
    """Determine priority based on WurmModLoader directory structure"""
    path_lower = file_path.lower()

    # High priority: core framework components
    if any(x in path_lower for x in ['bytecode/patches', 'hook', 'event', 'core/bytecode', 'serverhook']):
        return 'high'
    # Medium priority: API, utilities, mod implementation
    elif any(x in path_lower for x in ['api/', '/mods/', 'config', 'proxy']):
        return 'medium'
    # Low priority: tests, examples, documentation
    elif any(x in path_lower for x in ['test', 'example', 'doc']):
        return 'low'
    else:
        return 'medium'

def get_file_hash(filepath):
    """Generate SHA256 hash of file contents"""
    try:
        with open(filepath, 'rb') as f:
            return hashlib.sha256(f.read()).hexdigest()
    except:
        return ""

def get_file_mtime(filepath):
    """Get file modification time in ISO format"""
    try:
        mtime = os.path.getmtime(filepath)
        return datetime.fromtimestamp(mtime).isoformat()
    except:
        return ""

def extract_definitions(filepath, rel_path):
    """Extract all definitions from a Java source file"""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
            source_lines = content.split('\n')
    except Exception as e:
        print(f"❌ Error reading {filepath}: {e}")
        return None

    try:
        # Extract package
        package = extract_package(content)

        # Extract imports
        imports = extract_java_imports(content)

        # Extract classes
        classes_list = extract_java_classes(content, source_lines)
        class_names = [c['name'] for c in classes_list]

        # Extract methods
        methods = extract_java_methods(content, source_lines)

        # Get file metadata
        priority_level = get_priority_level(rel_path)
        last_modified = get_file_mtime(filepath)
        file_hash = get_file_hash(filepath)

        return {
            'package': package,
            'methods': methods,
            'classes': class_names,
            'class_details': classes_list,
            'imports': imports,
            'priority_level': priority_level,
            'last_modified': last_modified,
            'file_hash': file_hash
        }
    except Exception as e:
        print(f"❌ Error parsing {filepath}: {e}")
        return None

def build_code_index(base_path, exclude_dirs=None):
    """Build index of Java codebase"""
    if exclude_dirs is None:
        exclude_dirs = {'.git', '.gradle', 'build', 'out', 'target', '.idea', 'bin'}

    index = {}
    total_methods = 0
    total_classes = 0
    total_files = 0

    print(f"🔍 Scanning Java files in: {base_path}")

    for root, dirs, files in os.walk(base_path):
        # Filter out excluded directories
        dirs[:] = [d for d in dirs if d not in exclude_dirs]

        for file in files:
            if file.endswith('.java'):
                path = os.path.join(root, file)
                rel_path = os.path.relpath(path, base_path)

                result = extract_definitions(path, rel_path)
                if result and (result['methods'] or result['classes']):
                    index[rel_path] = result
                    total_methods += len(result['methods'])
                    total_classes += len(result['classes'])
                    total_files += 1

                    if total_files % 50 == 0:
                        print(f"  Processed {total_files} files...")

    summary = {
        'timestamp': datetime.now().isoformat(),
        'files_scanned': total_files,
        'methods': total_methods,
        'classes': total_classes,
    }

    return index, summary

def write_code_index(index, summary, output_path):
    """Write index to JSON file"""
    full = {
        'index_version': '2.0.0-java',
        'last_build_time': datetime.now().isoformat(),
        'total_methods': summary['methods'],
        'total_classes': summary['classes'],
        'total_files': summary['files_scanned'],
        'language': 'java',
        'project': 'WurmModLoader',
        'context_injection_version': '3.0.0',
        'enriched_schema_features': [
            'semantic_tags',
            'priority_levels',
            'javadoc_preview',
            'method_signatures',
            'import_tracking',
            'change_detection',
            'context_snippets',
            'annotation_tracking',
            'package_tracking'
        ],
        'index': index,
        'summary': summary
    }

    os.makedirs(os.path.dirname(output_path) if os.path.dirname(output_path) else '.', exist_ok=True)
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(full, f, indent=2)

def main():
    # WurmModLoader paths
    base_path = os.path.expanduser('~/Scripts/Games/WurmUnlimited/WurmModLoader')
    output_path = os.path.join(base_path, 'code_index.json')

    print("=" * 60)
    print("WurmModLoader Java Codebase Indexer")
    print("=" * 60)

    index, summary = build_code_index(base_path)
    write_code_index(index, summary, output_path)

    print("\n" + "=" * 60)
    print(f"✅ Code index written to: {output_path}")
    print("=" * 60)
    print(json.dumps(summary, indent=2))
    print("=" * 60)

if __name__ == '__main__':
    main()
