"""
singbox_builder.py — Парсер VLESS-ссылок и генератор конфигов sing-box.

Поддерживает протоколы:
  - VLESS + Reality (основной)
  - VLESS + TLS + WebSocket
  - VLESS + TLS (tcp)
"""

from urllib.parse import urlparse, parse_qs, unquote

def parse_vless(link: str) -> dict:
    if not link.startswith("vless://"):
        raise ValueError("Invalid link format")

    parsed = urlparse(link)
    query = parse_qs(parsed.query)

    return {
        "uuid": parsed.username,
        "host": parsed.hostname,
        "port": parsed.port,
        "type": query.get("type", ["tcp"])[0],
        "security": query.get("security", ["none"])[0],
        "sni": query.get("sni", [""])[0],
        "pbk": query.get("pbk", [""])[0],
        "sid": query.get("sid", [""])[0],
        "path": query.get("path", [""])[0],
    }

def build_config(parsed_link: dict) -> dict:
    return {
        "outbounds": [
            {
                "type": "vless",
                "tag": "proxy",
                "server": parsed_link["host"],
                "server_port": parsed_link["port"],
                "uuid": parsed_link["uuid"],
                "network": parsed_link["type"],
                "tls": {
                    "enabled": parsed_link["security"] in ["tls", "reality"],
                    "server_name": parsed_link["sni"],
                    "reality": {
                        "enabled": parsed_link["security"] == "reality",
                        "public_key": parsed_link["pbk"],
                        "short_id": parsed_link["sid"]
                    } if parsed_link["security"] == "reality" else None
                }
            }
        ]
    }
