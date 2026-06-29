#!/usr/bin/env python3
# Solra Multi-Service Release Script
# Usage: python release.py [--dry-run] [--services avt,spc,auth] [--version 0.2.0]
"""Orchestrate versioned releases across all Solra microservices."""

import argparse, json, os, subprocess, sys
from datetime import datetime
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SERVICES_DIR = ROOT / "services"
AI_SERVICES_DIR = ROOT / "ai-services"
CHANGELOG_DIR = ROOT / "01docs" / "changelogs"

ALL_SERVICES = {
    # Java services
    "auth-service":    SERVICES_DIR / "auth-service",
    "saf-service":     SERVICES_DIR / "saf-service",
    "avt-service":     SERVICES_DIR / "avt-service",
    "spc-service":     SERVICES_DIR / "spc-service",
    "soc-service":     SERVICES_DIR / "soc-service",
    "grw-service":     SERVICES_DIR / "grw-service",
    "not-service":     SERVICES_DIR / "not-service",
    "crt-service":     SERVICES_DIR / "crt-service",
    "mon-service":     SERVICES_DIR / "mon-service",
    # AI services
    "llm-router":                  AI_SERVICES_DIR / "llm-router",
    "safety-model-service":        AI_SERVICES_DIR / "safety-model-service",
    "embedding-service":           AI_SERVICES_DIR / "embedding-service",
    "recommendation-pipeline":     AI_SERVICES_DIR / "recommendation-pipeline",
    "tts-service":                 AI_SERVICES_DIR / "tts-service",
}

def run(cmd, cwd=None, capture=True):
    """Run shell command."""
    result = subprocess.run(cmd, shell=True, cwd=cwd,
                            capture_output=capture, text=True)
    return result

def get_service_version(service_path, lang="java"):
    """Extract current version from build file."""
    if lang == "java":
        gfile = service_path / "build.gradle.kts"
        if gfile.exists():
            content = gfile.read_text()
            for line in content.splitlines():
                if "version" in line and "=" in line:
                    return line.split("=")[-1].strip().strip('"\'')
    elif lang == "python":
        pfile = service_path / "pyproject.toml"
        if pfile.exists():
            for line in pfile.read_text().splitlines():
                if line.startswith("version"):
                    return line.split("=")[-1].strip().strip('"\'')
    return "unknown"

def bump_version(current, bump_type="patch"):
    """Bump semver."""
    parts = [int(x) for x in current.split(".")]
    if bump_type == "major":
        return f"{parts[0]+1}.0.0"
    elif bump_type == "minor":
        return f"{parts[0]}.{parts[1]+1}.0"
    else:
        return f"{parts[0]}.{parts[1]}.{parts[2]+1}"

def generate_changelog(service, from_ver, to_ver):
    """Generate changelog from git log between tags."""
    tag1 = f"{service}/v{from_ver}"
    tag2 = f"{service}/v{to_ver}"
    
    changelog = f"# {service} {to_ver} ({datetime.now().strftime('%Y-%m-%d')})\n\n"
    
    try:
        logs = run(f'git --no-pager log --oneline --no-merges {tag1}..HEAD .',
                   cwd=ROOT)
        if logs.stdout.strip():
            changelog += "## Changes\n\n"
            for line in logs.stdout.strip().splitlines():
                if line.strip():
                    changelog += f"- {line}\n"
        else:
            changelog += "No changes since last release.\n"
    except Exception:
        changelog += "(changelog generation skipped - no tags found)\n"
    
    CHANGELOG_DIR.mkdir(parents=True, exist_ok=True)
    cl_path = CHANGELOG_DIR / f"{service}-{to_ver}.md"
    cl_path.write_text(changelog)
    print(f"  📝 Changelog: {cl_path}")
    return cl_path

def build_service(service, path, dry_run=False):
    """Build service artifact."""
    if (path / "build.gradle.kts").exists():
        cmd = f"./gradlew :services:{service}:build -x test"
        if dry_run:
            print(f"  [DRY-RUN] {cmd}")
            return True
        result = run(cmd, cwd=ROOT)
        print(f"  🔨 Build: {'✅' if result.returncode == 0 else '❌'} {cmd}")
        return result.returncode == 0
    elif (path / "pyproject.toml").exists():
        cmd = f"cd {path} && poetry build"
        if dry_run:
            print(f"  [DRY-RUN] {cmd}")
            return True
        result = run(cmd, cwd=path)
        print(f"  🔨 Build: {'✅' if result.returncode == 0 else '❌'}")
        return result.returncode == 0
    else:
        print(f"  ⚠️  No build system detected for {service}")
        return True

def tag_release(service, version, dry_run=False):
    """Create git tag."""
    tag = f"{service}/v{version}"
    if dry_run:
        print(f"  [DRY-RUN] git tag {tag}")
        return
    result = run(f'git tag -a "{tag}" -m "Release {service} v{version}"', cwd=ROOT)
    print(f"  🏷️  Tag: {'✅' if result.returncode == 0 else '❌'} {tag}")

def main():
    parser = argparse.ArgumentParser(description="Solra Multi-Service Release")
    parser.add_argument("--dry-run", action="store_true", help="Preview without making changes")
    parser.add_argument("--services", default="all", help="Comma-separated service names or 'all'")
    parser.add_argument("--version", help="Target version (overrides auto-bump)")
    parser.add_argument("--bump", choices=["major", "minor", "patch"], default="patch")
    parser.add_argument("--skip-build", action="store_true")
    parser.add_argument("--skip-changelog", action="store_true")
    args = parser.parse_args()

    services = list(ALL_SERVICES.keys()) if args.services == "all" else args.services.split(",")
    
    print(f"\n🚀 Solra Release {'[DRY-RUN] ' if args.dry_run else ''}")
    print(f"   Services: {', '.join(services)}")
    print(f"   Bump: {args.bump}\n")

    for svc in services:
        path = ALL_SERVICES[svc]
        if not path.exists():
            print(f"  ⚠️  {svc}: path not found ({path})")
            continue
        
        lang = "java" if "services" in str(path) else "python"
        cur_ver = get_service_version(path, lang)
        
        if args.version:
            new_ver = args.version
        else:
            new_ver = bump_version(cur_ver, args.bump)
        
        print(f"📦 {svc}: {cur_ver} → {new_ver}")
        
        if not args.skip_changelog:
            generate_changelog(svc, cur_ver, new_ver)
        
        if not args.skip_build:
            build_service(svc, path, args.dry_run)
        
        tag_release(svc, new_ver, args.dry_run)
        print()

    if args.dry_run:
        print("✅ Dry-run complete. No changes made.")
    else:
        print("✅ Release complete. Run: git push --tags")

if __name__ == "__main__":
    main()
