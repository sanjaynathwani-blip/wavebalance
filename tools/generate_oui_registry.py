#!/usr/bin/env python3
"""
Builds app/src/main/assets/oui_registry.tsv from the IEEE MA-L registry
(the 24-bit OUIs that start a vendor's MAC addresses).

    python3 tools/generate_oui_registry.py            # downloads the registry
    python3 tools/generate_oui_registry.py oui.csv    # uses a local copy

Each output line is "<6 hex digits>\t<vendor name>". Registered company names
are shortened to the brand people know ("TP-LINK TECHNOLOGIES CO.,LTD." -> "TP-Link").
"""
import csv
import io
import re
import sys
import urllib.request
from pathlib import Path

REGISTRY_URL = "https://standards-oui.ieee.org/oui/oui.csv"
OUTPUT = Path(__file__).resolve().parent.parent / "app/src/main/assets/oui_registry.tsv"

# First match wins, so specific names (Cisco Meraki) come before general ones (Cisco)
BRANDS = [
    (r"tp-?link", "TP-Link"),
    (r"mercusys", "Mercusys"),
    (r"netgear", "Netgear"),
    (r"^apple\b", "Apple"),
    (r"^google\b", "Google"),
    (r"^nest labs", "Google Nest"),
    (r"samsung", "Samsung"),
    (r"asustek|^asus", "ASUS"),
    (r"ubiquiti", "Ubiquiti"),
    (r"meraki", "Cisco Meraki"),
    (r"linksys", "Linksys"),
    (r"^cisco", "Cisco"),
    (r"^belkin", "Belkin"),
    (r"^eero", "eero"),
    (r"^amazon", "Amazon"),
    (r"huawei", "Huawei"),
    (r"^honor device", "Honor"),
    (r"^zte\b", "ZTE"),
    (r"xiaomi|beijing xiaomi", "Xiaomi"),
    (r"^intel\b", "Intel"),
    (r"^qualcomm|atheros", "Qualcomm"),
    (r"^broadcom", "Broadcom"),
    (r"^mediatek", "MediaTek"),
    (r"^realtek", "Realtek"),
    (r"aruba", "Aruba (HPE)"),
    (r"hewlett packard enterprise", "HPE"),
    (r"^hewlett[- ]packard|^hp inc", "HP"),
    (r"d-link", "D-Link"),
    (r"sagemcom", "Sagemcom"),
    (r"arcadyan", "Arcadyan"),
    (r"technicolor", "Technicolor"),
    (r"sercomm", "Sercomm"),
    (r"hon hai|foxconn", "Foxconn"),
    (r"^murata", "Murata"),
    (r"^avm\b", "AVM (FRITZ!Box)"),
    (r"zyxel", "Zyxel"),
    (r"ruckus", "Ruckus"),
    (r"^juniper", "Juniper"),
    (r"^mist systems", "Juniper Mist"),
    (r"fortinet", "Fortinet"),
    (r"extreme networks", "Extreme Networks"),
    (r"^arris", "Arris"),
    (r"commscope", "CommScope"),
    (r"^nokia", "Nokia"),
    (r"^plume design", "Plume"),
    (r"tenda", "Tenda"),
    (r"^edimax", "Edimax"),
    (r"^synology", "Synology"),
    (r"raspberry pi", "Raspberry Pi"),
    (r"espressif", "Espressif"),
    (r"^sonos", "Sonos"),
    (r"^roku", "Roku"),
    (r"^nintendo", "Nintendo"),
    (r"^microsoft", "Microsoft"),
    (r"^sony\b", "Sony"),
    (r"^lg electronics|^lg innotek", "LG"),
    (r"^motorola", "Motorola"),
    (r"^dell\b", "Dell"),
    (r"lenovo", "Lenovo"),
    (r"oneplus", "OnePlus"),
    (r"guangdong oppo|^oppo", "OPPO"),
    (r"vivo mobile", "vivo"),
    (r"texas instruments", "Texas Instruments"),
    (r"^epson|seiko epson", "Epson"),
]
BRANDS = [(re.compile(pattern, re.IGNORECASE), name) for pattern, name in BRANDS]

# Company-form words dropped from the end of names that aren't in BRANDS
SUFFIX = re.compile(
    r"[\s,.]+(co|corp|corporation|company|inc|incorporated|ltd|limited|llc|l\.l\.c|gmbh|ag|sa|s\.a|sas|"
    r"s\.p\.a|spa|bv|b\.v|nv|oy|ab|as|a/s|kg|pty|plc|srl|s\.r\.l|technologies|technology|tech|"
    r"electronics|international|holdings|group|systems)\.?$",
    re.IGNORECASE,
)


def display_name(registered: str) -> str:
    name = " ".join(registered.split())
    for pattern, brand in BRANDS:
        if pattern.search(name):
            return brand
    previous = None
    while previous != name:
        previous = name
        name = SUFFIX.sub("", name).strip(" ,.&")
    # Registry names are often all caps; keep short acronyms like "AVM" as they are
    if name.isupper() and len(name) > 5:
        name = " ".join(w if len(w) <= 3 else w.capitalize() for w in name.split())
    return name or registered.strip()


def main() -> None:
    if len(sys.argv) > 1:
        text = Path(sys.argv[1]).read_text(encoding="utf-8")
    else:
        with urllib.request.urlopen(REGISTRY_URL) as response:
            text = response.read().decode("utf-8")

    entries = {}
    for row in csv.DictReader(io.StringIO(text)):
        # Blocks registered to the IEEE itself are split between many smaller vendors
        # (MA-M / MA-S), so their name says nothing about the device
        if row["Registry"] == "MA-L" and "IEEE Registration Authority" not in row["Organization Name"]:
            entries[row["Assignment"].upper()] = display_name(row["Organization Name"])

    lines = "".join(f"{oui}\t{name}\n" for oui, name in sorted(entries.items()))
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    # Plain text: the APK compresses it, and updates show up as readable diffs
    OUTPUT.write_text(lines, encoding="utf-8")
    print(f"{len(entries)} OUIs -> {OUTPUT}")


if __name__ == "__main__":
    main()
