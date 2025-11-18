#!/usr/bin/env python3
"""
Code Index Extension for FastAPI/MCP servers
Add these endpoints to your existing server for code index queries
"""

import json
import os
from typing import Optional, List, Dict, Any
from pathlib import Path
from pydantic import BaseModel, Field

# Import query functions (adjust path as needed)
import sys
sys.path.insert(0, str(Path(__file__).parent))

from query_code_index import (
    load_index,
    generate_overview,
    search_by_name,
    search_by_tag,
    get_file_summary,
    get_high_priority_files,
    search_by_pattern,
    generate_module_summary
)

# ============================================================================
# Pydantic Models for API
# ============================================================================

class CodeIndexOverviewResponse(BaseModel):
    total_files: int
    total_classes: int
    total_methods: int
    last_build: str
    priority_distribution: Dict[str, int]
    top_packages: Dict[str, int]
    top_tags: Dict[str, int]


class CodeSearchRequest(BaseModel):
    term: str = Field(..., description="Search term for method/class names")
    max_results: int = Field(50, description="Maximum results to return")


class CodeTagRequest(BaseModel):
    tag: str = Field(..., description="Tag to filter by (e.g., 'event_handler', 'combat')")
    max_results: int = Field(50, description="Maximum results to return")


class CodePatternRequest(BaseModel):
    pattern_type: str = Field(
        ...,
        description="Pattern type: event_handlers, bytecode_patches, config, combat, initialization, api"
    )
    max_results: int = Field(80, description="Maximum results to return")


class CodeFileRequest(BaseModel):
    file_path: str = Field(..., description="Partial file path to match")


class CodeModuleRequest(BaseModel):
    module_path: str = Field(..., description="Module/directory path")


class CodeIndexStatus(BaseModel):
    index_available: bool
    index_path: str
    index_version: Optional[str] = None
    last_build: Optional[str] = None
    error: Optional[str] = None


# ============================================================================
# Code Index Manager
# ============================================================================

class CodeIndexManager:
    """Manages code index loading and caching"""

    def __init__(self, index_path: str = "code_index.json"):
        self.index_path = Path(index_path)
        self._index_cache = None
        self._cache_mtime = None

    def get_index(self) -> Dict[str, Any]:
        """Load index with caching based on file modification time"""
        if not self.index_path.exists():
            raise FileNotFoundError(f"Code index not found at {self.index_path}")

        current_mtime = self.index_path.stat().st_mtime

        # Reload if cache is stale
        if self._index_cache is None or self._cache_mtime != current_mtime:
            print(f"📚 Loading code index from {self.index_path}")
            self._index_cache = load_index(str(self.index_path))
            self._cache_mtime = current_mtime

        return self._index_cache

    def get_status(self) -> CodeIndexStatus:
        """Get index status"""
        try:
            if not self.index_path.exists():
                return CodeIndexStatus(
                    index_available=False,
                    index_path=str(self.index_path),
                    error="Index file not found"
                )

            index = self.get_index()
            return CodeIndexStatus(
                index_available=True,
                index_path=str(self.index_path),
                index_version=index.get('index_version'),
                last_build=index.get('last_build_time')
            )
        except Exception as e:
            return CodeIndexStatus(
                index_available=False,
                index_path=str(self.index_path),
                error=str(e)
            )


# ============================================================================
# FastAPI Endpoint Functions (add these to your FastAPI app)
# ============================================================================

def setup_codeindex_endpoints(app, index_path: str = "code_index.json"):
    """
    Setup code index endpoints on your FastAPI app

    Usage:
        from codeindex_server_extension import setup_codeindex_endpoints

        app = FastAPI()
        setup_codeindex_endpoints(app, index_path="/path/to/code_index.json")
    """

    manager = CodeIndexManager(index_path)

    @app.get("/codeindex/status", response_model=CodeIndexStatus)
    def codeindex_status():
        """Get code index availability and status"""
        return manager.get_status()

    @app.get("/codeindex/overview")
    def codeindex_overview():
        """Get codebase overview with stats and tag distribution"""
        try:
            index = manager.get_index()
            return generate_overview(index)
        except Exception as e:
            return {"error": str(e)}

    @app.post("/codeindex/search")
    def codeindex_search(request: CodeSearchRequest):
        """Search for methods/classes by name"""
        try:
            index = manager.get_index()
            results = search_by_name(index, request.term)
            return results[:request.max_results]
        except Exception as e:
            return {"error": str(e)}

    @app.post("/codeindex/tag")
    def codeindex_tag(request: CodeTagRequest):
        """Search by semantic tag (event_handler, combat, etc.)"""
        try:
            index = manager.get_index()
            results = search_by_tag(index, request.tag)
            return results[:request.max_results]
        except Exception as e:
            return {"error": str(e)}

    @app.post("/codeindex/pattern")
    def codeindex_pattern(request: CodePatternRequest):
        """Search by pattern (event_handlers, bytecode_patches, etc.)"""
        try:
            index = manager.get_index()
            results = search_by_pattern(index, request.pattern_type)
            return results[:request.max_results]
        except Exception as e:
            return {"error": str(e)}

    @app.post("/codeindex/module")
    def codeindex_module(request: CodeModuleRequest):
        """Get summary of a module/directory"""
        try:
            index = manager.get_index()
            return generate_module_summary(index, request.module_path)
        except Exception as e:
            return {"error": str(e)}

    @app.post("/codeindex/file")
    def codeindex_file(request: CodeFileRequest):
        """Get detailed summary of a specific file"""
        try:
            index = manager.get_index()
            result = get_file_summary(index, request.file_path)
            if result is None:
                return {"error": f"File not found matching: {request.file_path}"}
            return result
        except Exception as e:
            return {"error": str(e)}

    @app.get("/codeindex/high-priority")
    def codeindex_high_priority(max_results: int = 50):
        """Get list of high priority files"""
        try:
            index = manager.get_index()
            results = get_high_priority_files(index)
            return results[:max_results]
        except Exception as e:
            return {"error": str(e)}

    print("✅ Code Index endpoints registered:")
    print("   GET  /codeindex/status")
    print("   GET  /codeindex/overview")
    print("   POST /codeindex/search")
    print("   POST /codeindex/tag")
    print("   POST /codeindex/pattern")
    print("   POST /codeindex/module")
    print("   POST /codeindex/file")
    print("   GET  /codeindex/high-priority")

    return manager


# ============================================================================
# Standalone Test Server
# ============================================================================

if __name__ == "__main__":
    """Run standalone test server"""
    from fastapi import FastAPI
    import uvicorn

    app = FastAPI(
        title="Code Index API",
        description="Query code index for WurmModLoader",
        version="1.0.0"
    )

    # Setup endpoints
    index_path = Path(__file__).parent / "code_index.json"
    setup_codeindex_endpoints(app, str(index_path))

    @app.get("/")
    def root():
        return {
            "name": "Code Index API",
            "version": "1.0.0",
            "endpoints": [
                "/codeindex/status",
                "/codeindex/overview",
                "/codeindex/search",
                "/codeindex/tag",
                "/codeindex/pattern",
                "/codeindex/module",
                "/codeindex/file",
                "/codeindex/high-priority",
                "/docs - Interactive API docs"
            ]
        }

    print()
    print("=" * 70)
    print("🔍 Code Index API Server")
    print("=" * 70)
    print(f"📁 Index: {index_path}")
    print(f"🌐 Server: http://localhost:8091")
    print(f"📝 Docs: http://localhost:8091/docs")
    print("=" * 70)
    print()

    uvicorn.run(app, host="127.0.0.1", port=8091, log_level="info")
