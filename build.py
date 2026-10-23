#!/usr/bin/env python3
"""Build this own-source Morphe TEST bundle; Maps + optional Google app; no input APK, signing or device access."""
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
ARTIFACT = "maps-automotive-for-byd-morphe-preview.7.mpp"
VERSION = "0.1.0-preview.7"
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
    require(len(patch_sources) == 17 and len(runtime_sources) == 93,
            "The complete 17-file patch and 93-file staged runtime source trees are required.")
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
    run(d8, "--min-api", "29", "--lib", android, "--classpath", cli,
        "--output", dex, *sorted(classes.rglob("*.class")))
    extensions = {}
    for module, min_api in (("cluster", "29"), ("gsa", "32")):
        module_sources = sorted((ROOT / "runtime/local/maps" / module).rglob("*.java"))
        module_classes, module_dex = extension_classes / module, extension / module
        module_dex.mkdir()
        run("javac", "-source", "8", "-target", "8", "-Xlint:-options", "-cp", android,
            "-d", module_classes, *module_sources)
        run(d8, "--min-api", min_api, "--lib", android, "--output", module_dex,
            *sorted(module_classes.rglob("*.class")))
        extensions["aaos" if module == "cluster" else "gsa"] = module_dex / "classes.dex"

    test_sources = sorted((ROOT / "tests").rglob("*.java"))
    require(len(test_sources) == 3, "The recovery, stalled-recovery and Google provider/routing tests are required.")
    test_classes = out / "test-classes"
    run("javac", "--release", "17", "-d", test_classes,
        ROOT / "runtime/local/maps/cluster/GeminiRecoveryPolicy.java",
        ROOT / "runtime/local/maps/gsa/ProviderPolicy.java",
        ROOT / "src/local/maps/gsapatches/ProtocolRoutes.java",
        ROOT / "src/local/maps/gsapatches/ServiceActions.java", *test_sources)
    run("java", "-cp", test_classes, "local.maps.cluster.GeminiRecoveryTest")
    run("java", "-cp", test_classes, "local.maps.cluster.GeminiStallTest")
    run("java", "-cp", test_classes, "local.maps.gsapatches.PolicyRoutesTest")

    manifest = (
        "Manifest-Version: 1.0\r\n"
        "Name: Maps Automotive for BYD + Gemini (TEST)\r\n"
        "Description: TEST Maps54R and optional Google08 Gemini support for MicroG-RE\r\n"
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
        for module, path in extensions.items():
            put(archive, "extensions/" + module + ".mpe", path.read_bytes())
        for path in patch_sources + runtime_sources + test_sources:
            put(archive, path.relative_to(ROOT).as_posix(), path.read_bytes())
        for name in ("LICENSE", "UPSTREAM-NOTICE", "NOTICE"):
            put(archive, name, (ROOT / name).read_bytes())

    source_hashes = {path.relative_to(ROOT).as_posix(): sha256(path)
                     for path in patch_sources + runtime_sources}
    record = {
        "agent": "Claude",
        "status": "prepared_not_published",
        "maps_functional_revision": "54R",
        "google_functional_revision": "08",
        "google_input": "17.62.11.ve.arm64/301813762/API32+",
        "patch_names": [PATCH_NAME, "Google app for BYD (MicroG-RE TEST)"],
        "mpp": ARTIFACT,
        "sha256": sha256(bundle),
        "bytes": bundle.stat().st_size,
        "version": VERSION,
        "patch_name": PATCH_NAME,
        "internal_revision": 54,
        "internal_runtime_version": "patch-lab.54-maps2610",
        "maps_input": "26.10.120001.E/1068833407",
        "desktop_version": "1.18.1",
        "desktop_api_sha256": CLI_SHA256,
        "extension_sha256": {module: sha256(path) for module, path in extensions.items()},
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
        "host_tests": {"recovery_protocol": 71, "stalled_recovery": 25, "google_provider_routes": 301, "result": "PASS"},
    }
    (out / "BUILD.json").write_text(json.dumps(record, indent=2) + "\n", encoding="utf-8")
    (out / "SHA256SUMS").write_text(record["sha256"] + "  " + ARTIFACT + "\n", encoding="utf-8")
    print(json.dumps({key: record[key] for key in ("mpp", "sha256", "bytes", "version",
                                                    "extension_sha256", "public_test_preview")}, indent=2))


if __name__ == "__main__":
    main()
