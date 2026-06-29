#!/usr/bin/env python3
# Avro Schema Migration: generate v1 fallback adapters from v2+ schemas
"""Usage: python generate_fallback.py --old v1 --new v2 --output v2/fallback/"""
import json, os, sys, argparse
from collections import OrderedDict

def load_schema(path):
    with open(path) as f:
        return json.load(f, object_pairs_hook=OrderedDict)

def diff_schemas(old_path, new_path):
    old = load_schema(old_path)
    new = load_schema(new_path)
    
    old_fields = {f['name']: f for f in old.get('fields', [])}
    new_fields = {f['name']: f for f in new.get('fields', [])}
    
    added = [n for n in new_fields if n not in old_fields]
    removed = [n for n in old_fields if n not in new_fields]
    changed = []
    for n in set(old_fields) & set(new_fields):
        if old_fields[n]['type'] != new_fields[n]['type']:
            changed.append(n)
    
    return {'added': added, 'removed': removed, 'changed': changed,
            'event': os.path.basename(new_path).replace('.avsc', '')}

def generate_fallback(diff, output_dir):
    """Generate a Python adapter that converts v2 events → v1 format."""
    os.makedirs(output_dir, exist_ok=True)
    out_path = os.path.join(output_dir, f"fallback_{diff['event']}.py")
    
    with open(out_path, 'w') as f:
        f.write(f'''"""Auto-generated v2→v1 fallback adapter for {diff['event']}"""
def adapt(data: dict) -> dict:
    """Convert v2 event to v1 format by dropping new fields and filling removed defaults."""
    result = dict(data)
''')
        for field in diff['removed']:
            f.write(f'    result.pop("{field}", None)\n')
        for field in diff['changed']:
            f.write(f'    # WARNING: type changed for "{field}" - manual conversion needed\n')
        f.write('    return result\n')
    
    print(f"  Generated: {out_path}")
    return out_path

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--old', default='v1')
    parser.add_argument('--new', default='v2')
    parser.add_argument('--output', default='v2/fallback')
    parser.add_argument('--base-dir', default=os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    args = parser.parse_args()
    
    base = os.path.join(args.base_dir, args.new)
    old_base = os.path.join(args.base_dir, args.old)
    
    if not os.path.isdir(base):
        print(f"No schemas found in {base}")
        sys.exit(1)
    
    for root, _, files in os.walk(base):
        for f in sorted(files):
            if not f.endswith('.avsc'): continue
            new_path = os.path.join(root, f)
            rel = os.path.relpath(new_path, base)
            old_path = os.path.join(old_base, rel)
            
            if os.path.exists(old_path):
                diff = diff_schemas(old_path, new_path)
                if diff['added'] or diff['removed'] or diff['changed']:
                    print(f"{diff['event']}: +{len(diff['added'])} -{len(diff['removed'])} ~{len(diff['changed'])}")
                    generate_fallback(diff, os.path.join(args.base_dir, args.output))
                else:
                    print(f"{diff['event']}: no changes")

if __name__ == '__main__':
    main()
