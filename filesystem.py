#!/usr/bin/env python3
import os
import json

def index_folders(root_path, max_depth=None):
    result = {}

    root_path = os.path.abspath(os.path.expanduser(root_path))
    root_len = len(root_path.split(os.sep))

    for current, dirs, _ in os.walk(root_path):
        depth = len(current.split(os.sep)) - root_len
        if max_depth is not None and depth > max_depth:
            continue

        rel = os.path.relpath(current, root_path)
        if rel == ".":
            rel = "/"

        result[rel] = sorted(dirs)

    return result

if __name__ == "__main__":
    root = input("Directory to index: ").strip()
    depth = input("Max depth (blank for unlimited): ").strip()
    depth = int(depth) if depth else None

    index = index_folders(root, depth)
    out_file = "folder_index.json"
    with open(out_file, "w") as f:
        json.dump(index, f, indent=2)

    print(f"Folder index written to {out_file}")
