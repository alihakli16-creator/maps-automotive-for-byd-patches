#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-only
"""Automated patching and signing for Maps Automotive for BYD.

Builds ready-to-install BYD-Maps-TR-Ready.apk from original Maps Automotive 26.10.120001.E (1068833407)
and our compiled Morphe patch bundle (.mpp).
"""
from pathlib import Path
import argparse
import hashlib
import os
import shutil
import subprocess
import sys
import urllib.request

MAPS_SHA256 = "d9dc74c9fa0f5bee0911ed1594a19adc0761258d524b5031ed480674d81f5ff9"
CLI_SHA256 = "1b506ab5f03d16a2f65026d5e0e1910d01fc1e2152f21eaeb44ed2f30856597b"
PATCH_NAME = "Maps Automotive for BYD (MicroG-RE TEST)"


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def require(cond: bool, msg: str):
    if not cond:
        print(f"ERROR: {msg}", file=sys.stderr)
        sys.exit(1)


def find_base_apk(candidates):
    for c in candidates:
        if c and Path(c).is_file():
            p = Path(c).resolve()
            h = sha256(p)
            if h.lower() == MAPS_SHA256.lower():
                print(f"[+] Found valid original Maps APK: {p} (SHA-256 verified)")
                return p
            else:
                print(f"[-] Candidate {p} hash mismatch: {h} != {MAPS_SHA256}")
    return None


def download_base_apk(url: str, dest: Path) -> Path:
    print(f"[*] Downloading base APK from: {url}")
    dest.parent.mkdir(parents=True, exist_ok=True)
    req = urllib.request.Request(url, headers={"User-Agent": "BYD-Maps-Patcher/1.0"})
    with urllib.request.urlopen(req) as resp, dest.open("wb") as out:
        shutil.copyfileobj(resp, out)
    h = sha256(dest)
    require(h.lower() == MAPS_SHA256.lower(), f"Downloaded APK SHA-256 mismatch: {h} != {MAPS_SHA256}")
    print(f"[+] Successfully downloaded and verified base APK: {dest}")
    return dest


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-apk", type=Path, default=None, help="Original Maps Automotive 26.10 APK")
    parser.add_argument("--base-url", type=str, default=None, help="Download URL for original Maps Automotive 26.10 APK")
    parser.add_argument("--bundle", required=True, type=Path, help="Compiled Turkish .mpp bundle")
    parser.add_argument("--cli", required=True, type=Path, help="Morphe Desktop 1.18.1 all-JAR")
    parser.add_argument("--sdk", required=True, type=Path, help="Android SDK with build-tools 36.0.0")
    parser.add_argument("--out-apk", type=Path, default=Path("build-out/BYD-Maps-TR-Ready.apk"), help="Target ready APK path")
    parser.add_argument("--keystore", type=Path, default=None, help="Optional custom signing keystore")
    parser.add_argument("--alias", type=str, default="byd-maps", help="Keystore key alias")
    parser.add_argument("--ks-pass", type=str, default="android", help="Keystore password")
    parser.add_argument("--key-pass", type=str, default="android", help="Key password")
    args = parser.parse_args()

    sdk = args.sdk.expanduser().resolve()
    cli = args.cli.expanduser().resolve()
    bundle = args.bundle.expanduser().resolve()
    out_apk = args.out_apk.expanduser().resolve()
    out_apk.parent.mkdir(parents=True, exist_ok=True)

    require(cli.is_file(), f"Morphe CLI JAR not found: {cli}")
    require(bundle.is_file(), f"Patch bundle (.mpp) not found: {bundle}")
    require(sha256(cli) == CLI_SHA256, "Morphe Desktop CLI SHA-256 mismatch")

    bt = sdk / "build-tools/36.0.0"
    if not bt.is_dir():
        # Fallback to any available build-tools version
        bt_candidates = sorted((sdk / "build-tools").glob("*"), reverse=True)
        require(bool(bt_candidates), f"No build-tools found in {sdk / 'build-tools'}")
        bt = bt_candidates[0]
        print(f"[*] Using build-tools: {bt.name}")

    zipalign = bt / ("zipalign.exe" if os.name == "nt" else "zipalign")
    apksigner = bt / ("apksigner.bat" if os.name == "nt" else "apksigner")
    require(zipalign.is_file(), f"zipalign not found at {zipalign}")
    require(apksigner.is_file(), f"apksigner not found at {apksigner}")

    # Resolve base APK
    base_apk = None
    if args.base_apk and args.base_apk.is_file():
        base_apk = args.base_apk.resolve()
        require(sha256(base_apk).lower() == MAPS_SHA256.lower(), "Provided --base-apk SHA-256 mismatch")
    elif args.base_url:
        temp_dl = out_apk.parent / "maps-automotive-original.apk"
        base_apk = download_base_apk(args.base_url, temp_dl)
    else:
        # Search candidate locations
        candidates = [
            Path("base.apk"),
            Path("original.apk"),
            Path("maps-automotive.apk"),
            Path("maps-original.apk"),
            out_apk.parent / "maps-automotive-original.apk",
            out_apk.parent / "base.apk",
            Path.home() / "maps-automotive.apk"
        ]
        base_apk = find_base_apk(candidates)

    require(base_apk is not None,
            f"Original Maps Automotive 26.10 APK not provided or found! SHA-256 must match {MAPS_SHA256}")

    # Keystore preparation
    keystore = args.keystore.expanduser().resolve() if args.keystore else None
    if keystore is None or not keystore.is_file():
        keystore = out_apk.parent / "release.keystore"
        if not keystore.is_file():
            print(f"[*] Generating standard release signing keystore at {keystore}...")
            keytool = shutil.which("keytool")
            require(bool(keytool), "keytool command is required to generate signing key.")
            subprocess.run([
                keytool, "-genkeypair", "-v",
                "-keystore", str(keystore),
                "-alias", args.alias,
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-validity", "10000",
                "-storepass", args.ks_pass,
                "-keypass", args.key_pass,
                "-dname", "CN=BYD Maps, OU=Automotive, O=BYD, C=TR"
            ], check=True)
            print(f"[+] Keystore created: {keystore}")

    # Morphe patching
    unsigned_apk = out_apk.parent / "maps-automotive-unsigned.apk"
    aligned_apk = out_apk.parent / "maps-automotive-aligned.apk"
    scratch_dir = out_apk.parent / "patch-scratch"
    result_json = out_apk.parent / "patch-result.json"

    if scratch_dir.exists():
        shutil.rmtree(scratch_dir)
    scratch_dir.mkdir(parents=True, exist_ok=True)

    print(f"[*] Running Morphe Desktop CLI to patch base APK with {bundle.name}...")
    patch_cmd = [
        "java", "-Xmx4g", "-jar", str(cli), "patch",
        "--unsigned",
        "-e", PATCH_NAME,
        "-p", str(bundle),
        "-o", str(unsigned_apk),
        "-r", str(result_json),
        "-t", str(scratch_dir),
        str(base_apk)
    ]
    res = subprocess.run(patch_cmd, capture_output=True, text=True)
    if res.returncode != 0:
        print("[-] Morphe patching failed! Output:\n" + res.stdout + "\n" + res.stderr, file=sys.stderr)
        sys.exit(res.returncode)
    print(f"[+] Morphe patching completed successfully: {unsigned_apk}")

    # Zipalign
    print("[*] Running zipalign...")
    if aligned_apk.exists():
        aligned_apk.unlink()
    subprocess.run([str(zipalign), "-p", "-f", "4", str(unsigned_apk), str(aligned_apk)], check=True)
    print(f"[+] APK aligned: {aligned_apk}")

    # apksigner
    print("[*] Signing APK with apksigner...")
    if out_apk.exists():
        out_apk.unlink()
    sign_cmd = [
        str(apksigner), "sign",
        "--ks", str(keystore),
        "--ks-key-alias", args.alias,
        "--ks-pass", f"pass:{args.ks_pass}",
        "--key-pass", f"pass:{args.key_pass}",
        "--out", str(out_apk),
        str(aligned_apk)
    ]
    subprocess.run(sign_cmd, check=True)
    print(f"[+] Successfully signed ready APK: {out_apk}")

    # Verify signature
    print("[*] Verifying signed APK...")
    verify_cmd = [str(apksigner), "verify", "--verbose", str(out_apk)]
    subprocess.run(verify_cmd, check=True)

    # Cleanup temporary unsigned/aligned files
    for temp_f in (unsigned_apk, aligned_apk):
        if temp_f.exists():
            try:
                temp_f.unlink()
            except Exception:
                pass
    if scratch_dir.exists():
        shutil.rmtree(scratch_dir, ignore_errors=True)

    ready_sha = sha256(out_apk)
    ready_size = out_apk.stat().st_size
    print("\n=======================================================")
    print(f"READY APK GENERATED: {out_apk.name}")
    print(f"Path: {out_apk}")
    print(f"Size: {ready_size:,} bytes")
    print(f"SHA-256: {ready_sha}")
    print("=======================================================\n")


if __name__ == "__main__":
    main()
