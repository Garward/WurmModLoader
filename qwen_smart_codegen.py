#!/home/garward/ClaudeEVO/claude/venv/bin/python3
"""
Smart Qwen Code Generator with Code Index Integration

Automatically extracts minimal context from code index instead of passing full files.
This saves 90%+ tokens compared to traditional context files.

Usage:
    qwen_smart_codegen.py <prompt_file> --search <term>
    qwen_smart_codegen.py <prompt_file> --pattern <pattern_type>
    qwen_smart_codegen.py <prompt_file> --context-from-index <file_path>
"""

import os
import sys
import argparse
import json
from pathlib import Path

# Add current directory to path for imports
sys.path.insert(0, str(Path(__file__).parent))

from query_code_index import load_index, search_by_name, search_by_pattern, get_file_summary

# Import the original qwen_codegen functions
qwen_codegen_path = Path(__file__).parent.parent / "qwen_codegen.py"
if not qwen_codegen_path.exists():
    qwen_codegen_path = Path("/home/garward/Scripts/Games/WurmUnlimited/qwen_codegen.py")

# Import from original
import importlib.util
spec = importlib.util.spec_from_file_location("qwen_codegen", qwen_codegen_path)
qwen_codegen = importlib.util.module_from_spec(spec)
spec.loader.exec_module(qwen_codegen)

def extract_minimal_context(results, max_items=15):
    """Extract just signatures, javadoc, and structure for code generation"""
    context_parts = []

    # Group by file
    by_file = {}
    for item in results[:max_items]:
        file = item.get('file', 'unknown')
        if file not in by_file:
            by_file[file] = {'classes': [], 'methods': []}

        if item.get('type') == 'class':
            by_file[file]['classes'].append(item)
        else:
            by_file[file]['methods'].append(item)

    # Format as compact context
    for file, items in by_file.items():
        context_parts.append(f"## Referenced from: {file}\n")

        # List classes
        if items['classes']:
            context_parts.append("**Classes:**")
            for cls in items['classes']:
                pkg = cls.get('package', '')
                context_parts.append(f"  - {pkg}.{cls['name']}")
            context_parts.append("")

        # List methods with signatures
        if items['methods']:
            context_parts.append("**Methods:**")
            for method in items['methods'][:10]:  # Limit methods per file
                sig = method.get('signature', method.get('name', ''))
                ret_type = method.get('return_type', '')
                if ret_type:
                    context_parts.append(f"  - {ret_type} {sig}")
                else:
                    context_parts.append(f"  - {sig}")

                # Add javadoc if present
                javadoc = method.get('javadoc', '')
                if javadoc:
                    first_line = javadoc.split('\n')[0][:120]
                    context_parts.append(f"    // {first_line}")

                # Add tags
                tags = method.get('tags', [])
                if tags:
                    context_parts.append(f"    // Tags: {', '.join(tags[:5])}")
            context_parts.append("")

    return '\n'.join(context_parts)

def create_smart_context(index_query_type, query_arg):
    """Create minimal context from code index (both framework and server)"""
    print(f"🔍 Extracting context from code indexes ({index_query_type}: {query_arg})...")

    # Load both indexes (use absolute path)
    import os
    script_dir = os.path.dirname(os.path.abspath(__file__))

    framework_index_path = os.path.join(script_dir, 'code_index.json')
    server_index_path = os.path.join(script_dir, 'wurm_server_index.json')

    framework_index = load_index(framework_index_path)
    print(f"   📚 Loaded framework index")

    # Try to load server index (optional if it doesn't exist yet)
    server_index = None
    if os.path.exists(server_index_path):
        server_index = load_index(server_index_path)
        print(f"   🎮 Loaded server index")
    else:
        print(f"   ⚠️  Server index not found (will only use framework)")

    # Get results from framework
    framework_results = []
    if index_query_type == 'search':
        framework_results = search_by_name(framework_index, query_arg)
    elif index_query_type == 'pattern':
        framework_results = search_by_pattern(framework_index, query_arg)
    elif index_query_type == 'file':
        file_summary = get_file_summary(framework_index, query_arg)
        if file_summary:
            # Convert to results format
            for method_name, method_data in file_summary.get('methods', {}).items():
                framework_results.append({
                    'type': 'method',
                    'name': method_name,
                    'signature': method_data['signature'],
                    'return_type': method_data.get('return_type', ''),
                    'file': file_summary['file'],
                    'javadoc': method_data.get('javadoc_preview', ''),
                    'tags': method_data.get('tags', [])
                })
            for cls_name in file_summary.get('classes', []):
                framework_results.append({
                    'type': 'class',
                    'name': cls_name,
                    'package': file_summary.get('package', ''),
                    'file': file_summary['file']
                })

    # Get results from server index if available
    server_results = []
    if server_index:
        if index_query_type == 'search':
            server_results = search_by_name(server_index, query_arg)
        elif index_query_type == 'pattern':
            server_results = search_by_pattern(server_index, query_arg)
        elif index_query_type == 'file':
            file_summary = get_file_summary(server_index, query_arg)
            if file_summary:
                for method_name, method_data in file_summary.get('methods', {}).items():
                    server_results.append({
                        'type': 'method',
                        'name': method_name,
                        'signature': method_data['signature'],
                        'return_type': method_data.get('return_type', ''),
                        'file': file_summary['file'],
                        'javadoc': method_data.get('javadoc_preview', ''),
                        'tags': method_data.get('tags', []),
                        'source': 'wurm_server'
                    })

    # Merge results (server results first for bytecode patches, then framework)
    results = server_results + framework_results

    if not results:
        print(f"⚠️  No results found for: {query_arg}")
        return ""

    # Extract minimal context
    context = extract_minimal_context(results)

    # Estimate token count
    word_count = len(context.split())
    estimated_tokens = int(word_count * 1.3)  # Rough estimate

    print(f"✅ Context extracted: {len(results)} items (~{len(server_results)} server, ~{len(framework_results)} framework)")
    print(f"   ~{estimated_tokens} tokens (vs ~10,000+ tokens for full files)")

    return context

def main():
    parser = argparse.ArgumentParser(
        description="Smart Qwen code generation with code index integration"
    )
    parser.add_argument("prompt_file", help="File containing the generation prompt")
    parser.add_argument("--output-dir", default=".", help="Output directory")
    parser.add_argument("--dry-run", action="store_true", help="Print without writing")

    # Context from code index (new!)
    context_group = parser.add_mutually_exclusive_group()
    context_group.add_argument(
        "--search",
        help="Search code index by name (e.g., 'ItemDamage')"
    )
    context_group.add_argument(
        "--pattern",
        help="Search code index by pattern (e.g., 'event_handlers')"
    )
    context_group.add_argument(
        "--context-from-index",
        help="Get context from specific file in index (e.g., 'ServerHook')"
    )

    # Traditional context files (fallback)
    parser.add_argument(
        "--context-files",
        nargs="*",
        help="Traditional context files (full content - high token usage)"
    )

    args = parser.parse_args()

    # Read main prompt
    prompt_text = Path(args.prompt_file).read_text()

    # Build enhanced prompt with smart context
    enhanced_prompt_parts = []

    # Add code index context if requested
    if args.search:
        smart_context = create_smart_context('search', args.search)
        if smart_context:
            enhanced_prompt_parts.append("# Context from Code Index (Smart Extract)\n")
            enhanced_prompt_parts.append(smart_context)
            enhanced_prompt_parts.append("\n---\n\n")

    elif args.pattern:
        smart_context = create_smart_context('pattern', args.pattern)
        if smart_context:
            enhanced_prompt_parts.append("# Context from Code Index (Pattern Match)\n")
            enhanced_prompt_parts.append(smart_context)
            enhanced_prompt_parts.append("\n---\n\n")

    elif args.context_from_index:
        smart_context = create_smart_context('file', args.context_from_index)
        if smart_context:
            enhanced_prompt_parts.append("# Context from Code Index (File Summary)\n")
            enhanced_prompt_parts.append(smart_context)
            enhanced_prompt_parts.append("\n---\n\n")

    # Auto-inject bytecode patch example if prompt mentions bytecode
    if 'bytecode' in prompt_text.lower():
        enhanced_prompt_parts.append("# Framework Pattern Example (Javassist Bytecode Patching)\n\n")
        enhanced_prompt_parts.append("```java\n")
        enhanced_prompt_parts.append("""public void apply(Object hookManagerObj) {
    HookManager hookManager = (HookManager) hookManagerObj;
    try {
        ClassPool classPool = hookManager.getClassPool();
        CtClass ctItem = classPool.get(targetClassName());

        patchContainerMethods(ctItem);

        LOGGER.info("[BytecodePatch] Registered patch successfully");
    } catch (NotFoundException | CannotCompileException e) {
        throw new IllegalStateException("Unable to install patch", e);
    }
}

private void patchContainerMethods(CtClass ctItem) throws NotFoundException, CannotCompileException {
    // Patch method using Javassist insertBefore
    try {
        CtMethod method = ctItem.getDeclaredMethod("getContainerVolume");
        method.insertBefore(
            "{ " +
            "  if (this.isHollow()) { " +
            "    int originalVolume = this.template.getContainerVolume(); " +
            "    int modifiedVolume = " + ProxyServerHook.class.getName() + ".fireContainerVolumeEvent(" +
            "      this.getWurmId(), " +
            "      this.getName(), " +
            "      originalVolume, " +
            "      0 " +
            "    ); " +
            "    if (modifiedVolume != originalVolume) { " +
            "      return modifiedVolume; " +
            "    } " +
            "  } " +
            "}"
        );
    } catch (NotFoundException e) {
        LOGGER.warning("Method not found, skipping patch");
    }
}
```\n\n""")
        enhanced_prompt_parts.append("**Key Points:**\n")
        enhanced_prompt_parts.append("- Use Javassist (CtClass, CtMethod), NOT ASM\n")
        enhanced_prompt_parts.append("- Fire events via ProxyServerHook.fire*Event() methods\n")
        enhanced_prompt_parts.append("- Use insertBefore/insertAfter for code injection\n")
        enhanced_prompt_parts.append("- Target class uses full package: com.wurmonline.server.*\n")
        enhanced_prompt_parts.append("\n---\n\n")
        print("🔧 Auto-injected bytecode patch example (detected 'bytecode' in prompt)")

    # Add main prompt
    enhanced_prompt_parts.append("# Generation Task\n\n")
    enhanced_prompt_parts.append(prompt_text)

    enhanced_prompt = ''.join(enhanced_prompt_parts)

    # Create temporary prompt file
    temp_prompt = Path("/tmp/qwen_smart_prompt.txt")
    temp_prompt.write_text(enhanced_prompt)

    print(f"\n📝 Enhanced prompt created (~{len(enhanced_prompt.split())} words)")

    # Call original qwen_codegen with our enhanced prompt
    print("🤖 Calling Qwen via original qwen_codegen.py...")

    # Build args for original script
    original_args = [
        str(temp_prompt),
        "--output-dir", args.output_dir
    ]

    if args.dry_run:
        original_args.append("--dry-run")

    # If traditional context files were provided, add them
    if args.context_files:
        print("⚠️  Warning: Using traditional context files (high token usage)")
        original_args.extend(["--context"] + args.context_files)

    # Parse args for original function
    original_parser = argparse.ArgumentParser()
    original_parser.add_argument("prompt_file")
    original_parser.add_argument("--output-dir", default=".")
    original_parser.add_argument("--context", nargs="*")
    original_parser.add_argument("--dry-run", action="store_true")
    original_parsed = original_parser.parse_args(original_args)

    # Read enhanced prompt
    prompt = temp_prompt.read_text()

    # Call Qwen
    response = qwen_codegen.call_qwen(prompt, original_parsed.context)

    # Extract files
    files = qwen_codegen.extract_files_from_response(response)

    if not files:
        print("❌ No files generated")
        print("\n" + "="*60)
        print("Response:")
        print(response)
        sys.exit(1)

    # Write files
    output_dir = Path(args.output_dir)
    for filename, content in files.items():
        file_path = output_dir / filename

        if args.dry_run:
            print(f"\n📄 Would create: {file_path}")
            print("─" * 60)
            print(content)
            print("─" * 60)
        else:
            file_path.parent.mkdir(parents=True, exist_ok=True)
            file_path.write_text(content)
            print(f"✅ Created: {file_path}")

    print(f"\n✨ Generated {len(files)} file(s)")

    # Clean up temp file
    temp_prompt.unlink()

if __name__ == "__main__":
    main()
