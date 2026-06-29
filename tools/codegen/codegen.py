#!/usr/bin/env python3
"""
Solra 代码生成器 —— Proto → 多语言代码一键生成
支持: Java (gRPC), Python (gRPC), TypeScript (gRPC-Web), C++ (gRPC), Kotlin (gRPC), Swift (gRPC)
"""
import argparse
import os
import subprocess
import sys
from pathlib import Path
from typing import List, Optional

ROOT = Path(__file__).resolve().parent.parent.parent
PROTO_DIR = ROOT / "contracts" / "proto"
GEN_DIR = ROOT / "generated"

# ── 语言生成配置 ──────────────────────────────────────
LANG_CONFIG = {
    "java": {
        "plugin": "protoc-gen-grpc-java",
        "out": GEN_DIR / "java",
        "opts": [],
        "desc": "Java (Spring Boot gRPC)",
    },
    "python": {
        "plugin": "protoc-gen-grpc-python",
        "out": GEN_DIR / "python",
        "opts": [],
        "desc": "Python (gRPC)",
    },
    "typescript": {
        "plugin": "protoc-gen-grpc-web",
        "out": GEN_DIR / "typescript",
        "opts": ["import_style=commonjs", "mode=grpcwebtext"],
        "desc": "TypeScript (gRPC-Web)",
    },
    "cpp": {
        "plugin": "protoc-gen-grpc-cpp",
        "out": GEN_DIR / "cpp",
        "opts": [],
        "desc": "C++17 (gRPC)",
    },
    "kotlin": {
        "plugin": "protoc-gen-grpc-kotlin",
        "out": GEN_DIR / "kotlin",
        "opts": [],
        "desc": "Kotlin (Android gRPC)",
    },
    "swift": {
        "plugin": "protoc-gen-grpc-swift",
        "out": GEN_DIR / "swift",
        "opts": ["Visibility=Public"],
        "desc": "Swift (iOS gRPC)",
    },
}

DOMAINS = [
    "common", "avt", "spc", "auth", "saf",
    "crt", "soc", "grw", "not", "mon",
]


def find_proto_files(domains: Optional[List[str]] = None) -> List[Path]:
    """收集 .proto 文件"""
    targets = domains or DOMAINS
    files = []
    for d in targets:
        p = PROTO_DIR / d
        if p.is_dir():
            files.extend(sorted(p.glob("**/*.proto")))
    return files


def run_protoc(language: str, proto_files: List[Path]) -> bool:
    """执行 protoc 生成"""
    cfg = LANG_CONFIG[language]
    out_dir = Path(cfg["out"])
    out_dir.mkdir(parents=True, exist_ok=True)

    print(f"\n  [{cfg['desc']}] → {out_dir}")

    for proto_file in proto_files:
        cmd = [
            "buf", "generate",
            "--template", str(PROTO_DIR / "buf.gen.yaml"),
            str(proto_file.parent),
        ]
        # Buf-based generation for better compatibility
        try:
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                cwd=str(ROOT),
                timeout=120,
            )
            if result.returncode != 0:
                print(f"    ⚠ {proto_file.name}: {result.stderr.strip()[:200]}")
            else:
                print(f"    ✓ {proto_file.name}")
        except FileNotFoundError:
            print("    ⚠ buf not installed, falling back to protoc")
            fallback_cmd = _build_protoc_cmd(language, proto_file, out_dir)
            try:
                subprocess.run(fallback_cmd, capture_output=True, text=True, check=True)
                print(f"    ✓ {proto_file.name} (protoc)")
            except subprocess.CalledProcessError as e:
                print(f"    ✗ {proto_file.name}: {e.stderr[:200] if e.stderr else str(e)}")
                return False
    return True


def _build_protoc_cmd(language: str, proto_file: Path, out_dir: Path) -> List[str]:
    """构建 protoc 命令行"""
    cfg = LANG_CONFIG[language]
    include_paths = [
        str(PROTO_DIR),
    ]
    cmd = ["protoc"]
    for ip in include_paths:
        cmd.extend(["-I", ip])
    cmd.extend([f"--{language}_out={out_dir}"])
    if cfg["plugin"]:
        cmd.extend([f"--grpc_out={out_dir}", f"--plugin=protoc-gen-grpc={cfg['plugin']}"])
    cmd.append(str(proto_file))
    return cmd


def generate_all(languages: Optional[List[str]] = None, domains: Optional[List[str]] = None) -> bool:
    """全量生成"""
    targets = languages or list(LANG_CONFIG.keys())
    proto_files = find_proto_files(domains)

    if not proto_files:
        print("❌ 未找到 .proto 文件")
        return False

    print(f"\n📦 共 {len(proto_files)} 个 .proto 文件，生成 {len(targets)} 种语言代码\n")
    success = True
    for lang in targets:
        if not run_protoc(lang, proto_files):
            success = False
    return success


def main():
    parser = argparse.ArgumentParser(description="Solra 代码生成器 — Proto → 多语言")
    parser.add_argument("-l", "--lang", nargs="+", choices=list(LANG_CONFIG.keys()) + ["all"],
                        default=["all"], help="目标语言 (default: all)")
    parser.add_argument("-d", "--domain", nargs="+", choices=DOMAINS,
                        default=None, help="指定协议域 (default: 全部)")
    parser.add_argument("--list", action="store_true", help="列出支持的生成目标")
    args = parser.parse_args()

    if args.list:
        print("\n支持的语言生成目标：")
        for name, cfg in LANG_CONFIG.items():
            print(f"  {name:12s} → {cfg['desc']}")
        print(f"\n协议域：{', '.join(DOMAINS)}")
        return 0

    languages = list(LANG_CONFIG.keys()) if "all" in args.lang else args.lang

    print("=" * 60)
    print("  Solra 代码生成器")
    print("=" * 60)

    ok = generate_all(languages, args.domain)
    if ok:
        print(f"\n✅ 代码生成完成 → {GEN_DIR}")
        return 0
    else:
        print("\n❌ 部分生成失败")
        return 1


if __name__ == "__main__":
    sys.exit(main())
