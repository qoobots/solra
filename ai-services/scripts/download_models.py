#!/usr/bin/env python3
"""
Solra AI Services - Model Download & Management Script
======================================================
Downloads all required models from HuggingFace Hub for offline/production use.

Usage:
    python download_models.py                     # Download all models (interactive)
    python download_models.py --all               # Download all models (non-interactive)
    python download_models.py --category llm      # Download only LLM models
    python download_models.py --category embedding # Download only embedding models
    python download_models.py --category safety   # Download only safety models
    python download_models.py --category tts      # Download only TTS models
    python download_models.py --list              # List available models
    python download_models.py --verify            # Verify downloaded models
    python download_models.py --dry-run           # Show what would be downloaded

Environment Variables:
    HF_ENDPOINT         - HuggingFace mirror endpoint (default: https://huggingface.co)
    HF_TOKEN            - HuggingFace access token (for gated models)
    SOLRA_MODELS_DIR    - Model storage directory (default: ./models)
"""

import argparse
import hashlib
import json
import logging
import os
import shutil
import subprocess
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional

# ── Configuration ──────────────────────────────────────────────────────────

MODELS_DIR = Path(os.environ.get("SOLRA_MODELS_DIR", Path(__file__).resolve().parent.parent / "models"))
HF_ENDPOINT = os.environ.get("HF_ENDPOINT", "https://huggingface.co")

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger("solra-models")

# ── Model Registry ─────────────────────────────────────────────────────────

@dataclass
class ModelEntry:
    """A downloadable model entry."""
    model_id: str              # HuggingFace model ID
    local_dir: str             # Relative path under MODELS_DIR
    category: str              # llm / embedding / safety / tts
    priority: str              # P0 / P1 / P2
    description: str
    size_gb: float = 0.0       # Approximate download size in GB
    files: list = field(default_factory=list)  # Specific files (empty = all)
    gated: bool = False        # Requires HF_TOKEN
    quant: str = ""            # Quantization info
    license_: str = "Apache-2.0"
    revision: str = "main"     # Git revision to pin

# Complete model catalog
MODEL_CATALOG: list[ModelEntry] = [
    # ── P0: On-device SLM (端侧推理) ──
    ModelEntry(
        model_id="Qwen/Qwen2.5-1.5B-Instruct-GGUF",
        local_dir="on-device/qwen2.5-1.5b-instruct",
        category="llm",
        priority="P0",
        description="Qwen2.5-1.5B GGUF Q4_K_M - On-device SLM for mobile inference",
        size_gb=0.99,
        files=["qwen2.5-1.5b-instruct-q4_k_m.gguf"],
        quant="Q4_K_M",
        license_="Apache-2.0",
    ),

    # ── P0: Cloud LLM (云端推理) ──
    ModelEntry(
        model_id="Qwen/Qwen2.5-7B-Instruct-AWQ",
        local_dir="cloud/qwen2.5-7b-instruct-awq",
        category="llm",
        priority="P0",
        description="Qwen2.5-7B AWQ INT4 - Cloud LLM for high-quality inference",
        size_gb=4.2,
        quant="AWQ INT4",
        license_="Apache-2.0",
    ),

    # ── P0: Safety models (多模态安全) ──
    ModelEntry(
        model_id="Qwen/Qwen2.5-Guard-7B",
        local_dir="safety/qwen2.5-guard-7b",
        category="safety",
        priority="P0",
        description="Qwen2.5-Guard-7B - Text safety classifier",
        size_gb=14.0,
        license_="Apache-2.0",
    ),
    ModelEntry(
        model_id="nomic-ai/nomic-embed-vision-v1.5",
        local_dir="safety/nomic-embed-vision",
        category="safety",
        priority="P0",
        description="Nomic Embed Vision v1.5 - Image safety embedding",
        size_gb=0.6,
        license_="Apache-2.0",
    ),

    # ── P1: Embedding models (嵌入模型) ──
    ModelEntry(
        model_id="shibing624/text2vec-large-chinese",
        local_dir="embedding/text2vec-large-chinese",
        category="embedding",
        priority="P1",
        description="text2vec-large-chinese - Chinese text embedding (326-dim)",
        size_gb=1.3,
        license_="Apache-2.0",
    ),
    ModelEntry(
        model_id="sentence-transformers/clip-ViT-B-32-multilingual-v1",
        local_dir="embedding/clip-vit-b32-multilingual",
        category="embedding",
        priority="P1",
        description="CLIP ViT-B/32 Multilingual - Cross-modal image-text embedding",
        size_gb=0.6,
        license_="MIT",
    ),

    # ── P1: TTS models (语音合成) ──
    ModelEntry(
        model_id="FunAudioLLM/CosyVoice2-0.5B",
        local_dir="tts/cosyvoice2-0.5b",
        category="tts",
        priority="P1",
        description="CosyVoice 2 (0.5B) - Chinese TTS with emotion control",
        size_gb=1.0,
        license_="Apache-2.0",
    ),
]

# Expected SHA256 checksums (populated after first download)
EXPECTED_CHECKSUMS = {
    # Will be populated with actual checksums after initial download
}


# ── Core Functions ─────────────────────────────────────────────────────────

def get_model_dir(local_dir: str) -> Path:
    """Get absolute path for a model's local directory."""
    return MODELS_DIR / local_dir


def is_model_downloaded(model: ModelEntry) -> bool:
    """Check if a model is already downloaded and valid."""
    model_dir = get_model_dir(model.local_dir)

    if not model_dir.exists():
        return False

    # Check for model files
    has_files = any(model_dir.iterdir())
    if not has_files:
        return False

    # Check manifest if it exists
    manifest_path = model_dir / "manifest.json"
    if manifest_path.exists():
        try:
            manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
            if manifest.get("model_id") != model.model_id:
                return False
        except (json.JSONDecodeError, KeyError):
            return False

    return True


def download_model(model: ModelEntry, force: bool = False) -> bool:
    """
    Download a single model from HuggingFace Hub.

    Uses huggingface_hub if available, falls back to git-lfs.
    """
    model_dir = get_model_dir(model.local_dir)

    if is_model_downloaded(model) and not force:
        logger.info(f"  ✓ Already downloaded: {model.model_id}")
        return True

    logger.info(f"  ↓ Downloading: {model.model_id} ({model.size_gb:.1f} GB)")
    logger.info(f"    → {model_dir}")

    # Create parent directory
    model_dir.mkdir(parents=True, exist_ok=True)

    # Try huggingface_hub CLI first
    try:
        cmd = [
            sys.executable, "-m", "huggingface_hub.commands.huggingface_cli",
            "download", model.model_id,
            "--local-dir", str(model_dir),
            "--local-dir-use-symlinks", "False",
            "--revision", model.revision,
        ]

        if model.files:
            for f in model.files:
                cmd.extend(["--include", f])
        else:
            cmd.extend(["--include", "*"])

        # Add HF endpoint override if set
        env = os.environ.copy()
        if HF_ENDPOINT != "https://huggingface.co":
            env["HF_ENDPOINT"] = HF_ENDPOINT

        if model.gated:
            token = os.environ.get("HF_TOKEN", "")
            if not token:
                logger.warning(f"    ⚠ Model {model.model_id} is gated but HF_TOKEN is not set")
            else:
                env["HF_TOKEN"] = token

        logger.debug(f"    Running: {' '.join(cmd)}")
        result = subprocess.run(cmd, capture_output=True, text=True, env=env, timeout=3600)

        if result.returncode != 0:
            logger.warning(f"    huggingface_hub CLI failed: {result.stderr[:200]}")
            raise RuntimeError("huggingface_hub failed")

        logger.info(f"    ✓ Download complete")

    except Exception as e:
        logger.warning(f"    huggingface_hub unavailable ({e}), trying git-lfs...")
        # Fallback: git clone with LFS
        try:
            clone_url = f"{HF_ENDPOINT}/{model.model_id}"
            temp_dir = model_dir.parent / f".tmp_{model_dir.name}"

            if temp_dir.exists():
                shutil.rmtree(temp_dir)

            env = os.environ.copy()
            env["GIT_LFS_SKIP_SMUDGE"] = "0"  # Download LFS files

            subprocess.run(
                ["git", "clone", "--depth", "1", "--branch", model.revision, clone_url, str(temp_dir)],
                check=True, capture_output=True, text=True, timeout=3600, env=env
            )

            # Move to final location
            if model_dir.exists():
                shutil.rmtree(model_dir)
            shutil.move(str(temp_dir), str(model_dir))

            logger.info(f"    ✓ git-lfs download complete")

        except subprocess.TimeoutExpired:
            logger.error(f"    ✗ Download timeout for {model.model_id}")
            return False
        except Exception as e2:
            logger.error(f"    ✗ Download failed: {e2}")
            return False

    # Write manifest
    manifest = {
        "model_id": model.model_id,
        "category": model.category,
        "priority": model.priority,
        "size_gb": model.size_gb,
        "quant": model.quant,
        "revision": model.revision,
        "license": model.license_,
        "description": model.description,
    }
    manifest_path = model_dir / "manifest.json"
    manifest_path.write_text(json.dumps(manifest, indent=2, ensure_ascii=False), encoding="utf-8")

    # Compute and store checksum for key files
    _store_checksums(model, model_dir)

    return True


def _store_checksums(model: ModelEntry, model_dir: Path) -> None:
    """Compute SHA256 checksums for model files and store in manifest."""
    checksums = {}
    for fpath in model_dir.rglob("*"):
        if fpath.is_file() and fpath.suffix not in (".json", ".md", ".txt", ".lock"):
            sha = hashlib.sha256(fpath.read_bytes()).hexdigest()
            checksums[str(fpath.relative_to(model_dir))] = sha

    if checksums:
        manifest_path = model_dir / "manifest.json"
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        manifest["checksums"] = checksums
        manifest_path.write_text(json.dumps(manifest, indent=2, ensure_ascii=False), encoding="utf-8")


def verify_model(model: ModelEntry) -> tuple[bool, str]:
    """Verify a downloaded model's integrity."""
    model_dir = get_model_dir(model.local_dir)

    if not model_dir.exists():
        return False, "Directory not found"

    if not any(model_dir.iterdir()):
        return False, "Directory is empty"

    manifest_path = model_dir / "manifest.json"
    if not manifest_path.exists():
        return False, "Missing manifest.json"

    try:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return False, "Invalid manifest.json"

    if manifest.get("model_id") != model.model_id:
        return False, f"Model ID mismatch: {manifest.get('model_id')} != {model.model_id}"

    # Verify checksums if available
    if "checksums" in manifest:
        for fname, expected_sha in manifest["checksums"].items():
            fpath = model_dir / fname
            if not fpath.exists():
                return False, f"Missing file: {fname}"
            actual_sha = hashlib.sha256(fpath.read_bytes()).hexdigest()
            if actual_sha != expected_sha:
                return False, f"Checksum mismatch: {fname}"

    return True, "OK"


# ── CLI Interface ──────────────────────────────────────────────────────────

def list_models() -> None:
    """Display all available models."""
    print("\n" + "=" * 80)
    print("  Solra AI Model Catalog")
    print("=" * 80)
    print(f"  Storage: {MODELS_DIR}")
    print()

    categories = {}
    for m in MODEL_CATALOG:
        categories.setdefault(m.category, []).append(m)

    for cat, models in categories.items():
        print(f"  [{cat.upper()}]")
        for m in models:
            downloaded = "✓" if is_model_downloaded(m) else "✗"
            gated = " 🔒" if m.gated else ""
            print(f"    {downloaded} [{m.priority}] {m.model_id}")
            print(f"         {m.description}")
            print(f"         Size: {m.size_gb:.1f} GB | License: {m.license_}{gated}")
            if m.quant:
                print(f"         Quant: {m.quant}")
        print()

    # Summary
    total = len(MODEL_CATALOG)
    downloaded_count = sum(1 for m in MODEL_CATALOG if is_model_downloaded(m))
    total_size = sum(m.size_gb for m in MODEL_CATALOG)
    downloaded_size = sum(m.size_gb for m in MODEL_CATALOG if is_model_downloaded(m))

    print(f"  Summary: {downloaded_count}/{total} models downloaded "
          f"({downloaded_size:.1f}/{total_size:.1f} GB)")
    print("=" * 80 + "\n")


def download_models(category: Optional[str] = None, force: bool = False, dry_run: bool = False) -> None:
    """Download models by category or all."""
    if category:
        models = [m for m in MODEL_CATALOG if m.category == category]
        if not models:
            logger.error(f"Unknown category: {category}")
            logger.info(f"Available categories: {set(m.category for m in MODEL_CATALOG)}")
            sys.exit(1)
    else:
        models = MODEL_CATALOG

    # Sort by priority
    models.sort(key=lambda m: {"P0": 0, "P1": 1, "P2": 2}.get(m.priority, 99))

    if dry_run:
        print("\n=== DRY RUN - Models to download ===")
        for m in models:
            status = "✓ Already cached" if is_model_downloaded(m) else "↓ Will download"
            print(f"  {status}: [{m.priority}] {m.model_id} ({m.size_gb:.1f} GB)")
        total_new = sum(m.size_gb for m in models if not is_model_downloaded(m))
        print(f"\n  Total new download: {total_new:.1f} GB")
        print("=" * 50 + "\n")
        return

    logger.info(f"Starting model downloads to: {MODELS_DIR}")
    MODELS_DIR.mkdir(parents=True, exist_ok=True)

    success_count = 0
    fail_count = 0
    skip_count = 0

    for i, model in enumerate(models, 1):
        logger.info(f"[{i}/{len(models)}] {model.model_id}")

        if is_model_downloaded(model) and not force:
            logger.info(f"  ✓ Already downloaded, skipping")
            skip_count += 1
            continue

        if download_model(model, force=force):
            success_count += 1
        else:
            fail_count += 1

    logger.info(f"\nDownload complete: {success_count} succeeded, {skip_count} skipped, {fail_count} failed")


def verify_models(category: Optional[str] = None) -> None:
    """Verify downloaded models."""
    if category:
        models = [m for m in MODEL_CATALOG if m.category == category]
    else:
        models = MODEL_CATALOG

    print("\n=== Model Verification ===")
    ok_count = 0
    fail_count = 0

    for m in models:
        ok, msg = verify_model(m)
        if ok:
            print(f"  ✓ {m.model_id}: {msg}")
            ok_count += 1
        else:
            print(f"  ✗ {m.model_id}: {msg}")
            fail_count += 1

    print(f"\n  Result: {ok_count} OK, {fail_count} FAILED")
    print("=" * 30 + "\n")


# ── Main ───────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="Solra AI Services - Model Download & Management",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python download_models.py --list              List all models
  python download_models.py --all               Download all models
  python download_models.py --category llm      Download LLM models only
  python download_models.py --verify            Verify all downloaded models
  python download_models.py --dry-run           Preview download plan
        """,
    )
    parser.add_argument("--all", action="store_true", help="Download all models")
    parser.add_argument("--category", choices=["llm", "embedding", "safety", "tts"],
                        help="Download models of a specific category")
    parser.add_argument("--list", action="store_true", help="List all available models")
    parser.add_argument("--verify", action="store_true", help="Verify downloaded models")
    parser.add_argument("--force", action="store_true", help="Force re-download even if cached")
    parser.add_argument("--dry-run", action="store_true",
                        help="Show what would be downloaded without downloading")

    args = parser.parse_args()

    if args.list:
        list_models()
    elif args.verify:
        verify_models(args.category)
    elif args.all or args.category:
        download_models(args.category, force=args.force, dry_run=args.dry_run)
    else:
        # Interactive mode
        list_models()
        if len([m for m in MODEL_CATALOG if not is_model_downloaded(m)]) == 0:
            logger.info("All models are already downloaded.")
            return

        response = input("Download missing models? [Y/n]: ").strip().lower()
        if response in ("", "y", "yes"):
            download_models(force=args.force, dry_run=args.dry_run)
        else:
            logger.info("Download cancelled.")


if __name__ == "__main__":
    main()
