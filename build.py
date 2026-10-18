#!/usr/bin/env python3
"""Build this own-source Morphe TEST bundle; no Maps input, signing or device access."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import zipfile

CLI_SHA256 = "1b506ab5f03d16a2f65026d5e0e1910d01fc1e2152f21eaeb44ed2f30856597b"
ARTIFACT = "maps-automotive-for-byd-morphe-preview.2.mpp"
VERSION = "0.1.0-preview.2"
PATCH_NAME = "Maps Automotive for BYD (MicroG-RE TEST)"
ROOT = Path(__file__).resolve().parent


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def require(condition, message):
    if not condition:
        raise SystemExit(message)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sdk", required=True, type=Path,
                        help="Android SDK directory with platform35 and build-tools36.0.0")
    parser.add_argument("--cli", required=True, type=Path,
                        help="Pinned Morphe Desktop1.18.1 all-JAR")
    parser.add_argument("--out", required=True, type=Path,
                        help="A new output directory; existing paths are never overwritten")
    args = parser.parse_args()
    sdk, cli, out = args.sdk.expanduser().resolve(), args.cli.expanduser().resolve(), args.out.expanduser().resolve()
    android = sdk / "platforms/android-35/android.jar"
    d8 = sdk / "build-tools/36.0.0" / ("d8.bat" if os.name == "nt" else "d8")
    require(cli.is_file(), "The --cli JAR does not exist.")
    require(sha256(cli) == CLI_SHA256, "Morphe Desktop JAR SHA-256 mismatch; use the pinned1.18.1 all-JAR.")
    require(android.is_file() and d8.is_file(), "Install Android SDK platform35 and build-tools36.0.0.")
    require(shutil.which("javac") and shutil.which("java"), "JDK17 or newer is required on PATH.")
    version = subprocess.run(["javac", "-version"], capture_output=True, text=True, check=True)
    match = re.search(r"javac\s+(\d+)", version.stdout + version.stderr)
    require(match and int(match.group(1)) >= 17, "JDK17 or newer is required on PATH.")
    patch_sources = sorted((ROOT / "src").rglob("*.java"))
    runtime_sources = sorted((ROOT / "runtime").rglob("*.java"))
    require(len(patch_sources) == 10 and len(runtime_sources) == 64,
            "The complete 10-file patch and 64-file staged runtime source trees are required.")
    for name in ("LICENSE", "UPSTREAM-NOTICE", "NOTICE"):
        require((ROOT / name).is_file(), "Missing required notice: " + name)
    # A fresh directory gives every build an immutable output location.
    require(not out.exists(), "The output directory already exists; choose a new --out path.")
    out.mkdir(parents=True, exist_ok=False)

    def run(*command):
        result = subprocess.run([str(arg) for arg in command], capture_output=True, text=True)
        with (out / "BUILD.log").open("a", encoding="utf-8") as log:
            log.write(result.stdout + result.stderr)
        if result.returncode:
            raise SystemExit("Build tool failed; inspect BUILD.log in the output directory.")

    classes, dex = out / "classes", out / "dex"
    extension_classes, extension = out / "extension-classes", out / "extension"
    dex.mkdir()
    extension.mkdir()
    run("javac", "--release", "17", "-cp", cli, "-d", classes, *patch_sources)
    run(d8, "--min-api", "30", "--lib", android, "--classpath", cli,
        "--output", dex, *sorted(classes.rglob("*.class")))
    run("javac", "-source", "8", "-target", "8", "-Xlint:-options", "-cp", android,
        "-d", extension_classes, *runtime_sources)
    run(d8, "--min-api", "30", "--lib", android, "--output", extension,
        *sorted(extension_classes.rglob("*.class")))

    manifest = (
        "Manifest-Version: 1.0\r\n"
        "Name: Maps Automotive for BYD (MicroG-RE TEST)\r\n"
        "Description: TEST native3D, UI scale, search, optional rounded corners\r\n"
        "Version: " + VERSION + "\r\n"
        "Author: romanchukg-cloud\r\n"
        "License: GPL-3.0-only\r\n"
        "Patcher-Version: 1.5.2\r\n\r\n"
    )
    bundle = out / ARTIFACT
    # Fixed member metadata makes bundle content independent of local source mtimes.
    def put(archive, name, data):
        member = zipfile.ZipInfo(name, date_time=(2026, 10, 17, 0, 0, 0))
        member.compress_type = zipfile.ZIP_DEFLATED
        member.external_attr = 0o100644 << 16
        archive.writestr(member, data)

    with zipfile.ZipFile(bundle, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        put(archive, "META-INF/MANIFEST.MF", manifest.encode())
        for path in sorted(classes.rglob("*.class")):
            put(archive, path.relative_to(classes).as_posix(), path.read_bytes())
        put(archive, "classes.dex", (dex / "classes.dex").read_bytes())
        put(archive, "extensions/aaos.mpe", (extension / "classes.dex").read_bytes())
        for path in patch_sources + runtime_sources:
            put(archive, path.relative_to(ROOT).as_posix(), path.read_bytes())
        for name in ("LICENSE", "UPSTREAM-NOTICE", "NOTICE"):
            put(archive, name, (ROOT / name).read_bytes())

    source_hashes = {path.relative_to(ROOT).as_posix(): sha256(path)
                     for path in patch_sources + runtime_sources}
    record = {
        "mpp": ARTIFACT,
        "sha256": sha256(bundle),
        "bytes": bundle.stat().st_size,
        "version": VERSION,
        "patch_name": PATCH_NAME,
        "internal_revision": 34,
        "internal_runtime_version": "patch-lab.34",
        "desktop_version": "1.18.1",
        "desktop_api_sha256": CLI_SHA256,
        "extension_sha256": sha256(extension / "classes.dex"),
        "patch_dex_sha256": sha256(dex / "classes.dex"),
        "source_sha256": source_hashes,
        "patch_java_count": len(patch_sources),
        "runtime_java_count": len(runtime_sources),
        "lab_only": False,
        "public_test_preview": True,
        "google_implementation_classes_bundled": False,
        "signing_material_bundled": False,
        "runtime_provider_guard": True,
        "diagnostics_uploads_disabled": True,
        "Morphe_Manager_tested": False,
    }
    (out / "BUILD.json").write_text(json.dumps(record, indent=2) + "\n", encoding="utf-8")
    (out / "SHA256SUMS").write_text(record["sha256"] + "  " + ARTIFACT + "\n", encoding="utf-8")
    print(json.dumps({key: record[key] for key in ("mpp", "sha256", "bytes", "version",
                                                    "extension_sha256", "public_test_preview")}, indent=2))


if __name__ == "__main__":
    main()
