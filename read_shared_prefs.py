#!/usr/bin/env python3
"""
从手机通过 ADB 读取 WeatherClock 的 SharedPreferences，
解析后输出 JSON 格式到 stdout。
"""
import subprocess
import xml.etree.ElementTree as ET
import json
import sys
import os

PACKAGE = "com.desk.weather"
PREFS_FILE = "/data/data/com.desk.weather/shared_prefs/com.desk.weather_preferences.xml"

def get_prefs_xml():
    result = subprocess.run(
        ["adb", "shell", f"run-as {PACKAGE} cat shared_prefs/com.desk.weather_preferences.xml"],
        capture_output=True, timeout=10, text=True
    )
    if result.returncode != 0:
        # Fallback: try without run-as (world-readable prefs)
        result = subprocess.run(
            ["adb", "shell", f"cat {PREFS_FILE}"],
            capture_output=True, timeout=10, text=True
        )
    return result.stdout

def parse_and_convert(xml_str):
    """
    将 Android SharedPreferences XML 转换为 Python dict。
    Android prefs XML format:
    <map>
      <string name="key">value</string>
      <boolean name="key" value="true" />
      <int name="key" value="123" />
      ...
    </map>
    """
    if not xml_str.strip():
        return {}

    result = {}
    try:
        root = ET.fromstring(xml_str)
        for child in root:
            name = child.attrib.get("name", "")
            tag = child.tag
            if tag == "string":
                result[name] = child.text or ""
            elif tag == "boolean":
                result[name] = child.attrib.get("value", "false").lower() == "true"
            elif tag == "int":
                result[name] = int(child.attrib.get("value", "0"))
            elif tag == "float":
                result[name] = float(child.attrib.get("value", "0.0"))
            elif tag == "long":
                result[name] = int(child.attrib.get("value", "0"))
            elif tag == "string-set":
                # Skip sets for now
                result[name] = []
            else:
                result[name] = child.text
    except ET.ParseError as e:
        print(f"XML parse error: {e}", file=sys.stderr)
        return {}
    return result

def main():
    xml_str = get_prefs_xml()
    if not xml_str.strip():
        print("无法读取手机配置（请确认应用已安装且 USB 调试正常）", file=sys.stderr)
        sys.exit(1)

    data = parse_and_convert(xml_str)
    # 输出 JSON
    print(json.dumps(data, indent=2, ensure_ascii=False))

if __name__ == "__main__":
    main()