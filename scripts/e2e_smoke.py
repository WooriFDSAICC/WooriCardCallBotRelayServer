#!/usr/bin/env python3
"""E2E smoke — Relay inbound/outbound WebSocket 1건 + health checks."""
from __future__ import annotations

import argparse
import asyncio
import json
import sys
import urllib.request

import websockets

DEFAULT_RELAY = "ws://localhost:8080"
DEFAULT_CTI = "http://localhost:9000"


async def smoke_ws(base_url: str, path: str, chunks: int = 6) -> dict:
    url = f"{base_url}{path}"
    pcm = b"\x00" * 3200
    last_json: dict = {}

    async with websockets.connect(url, open_timeout=15) as ws:
        await asyncio.sleep(0.5)
        for _ in range(chunks):
            await ws.send(pcm)
            try:
                msg = await asyncio.wait_for(ws.recv(), timeout=8)
                if isinstance(msg, str):
                    last_json = json.loads(msg)
                    if last_json.get("event"):
                        break
            except asyncio.TimeoutError:
                await asyncio.sleep(0.2)
        await asyncio.sleep(0.3)
    return last_json


def check_health(name: str, url: str) -> bool:
    try:
        with urllib.request.urlopen(url, timeout=10) as resp:
            body = resp.read().decode()
            print(f"[OK] {name}: {resp.status} {body[:100]}")
            return True
    except Exception as exc:
        print(f"[FAIL] {name}: {exc}")
        return False


async def main() -> int:
    parser = argparse.ArgumentParser(description="Woori callbot E2E smoke test")
    parser.add_argument("--relay", default=DEFAULT_RELAY)
    parser.add_argument("--cti", default=DEFAULT_CTI)
    args = parser.parse_args()

    ok = True
    ok &= check_health("Callbot", "http://localhost:8000/health")
    ok &= check_health("FDS", "http://localhost:8010/health")
    ok &= check_health("Relay", "http://localhost:8080/actuator/health")
    ok &= check_health("CTI", args.cti.rstrip("/") + "/health")

    relay_http = args.relay.replace("ws://", "http://").replace("wss://", "https://")
    if not relay_http.startswith("http"):
        relay_http = "http://localhost:8080"

    for label, path in [
        ("inbound", "/voice/inbound/smoke-in-001"),
        ("outbound", "/voice/outbound/smoke-out-001?campaignId=camp-demo"),
    ]:
        try:
            result = await smoke_ws(args.relay, path)
            event = result.get("event", "?")
            direction = result.get("call_direction", "?")
            print(f"[OK] WS {label}: event={event} direction={direction}")
        except Exception as exc:
            print(f"[FAIL] WS {label}: {exc}")
            ok = False

    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(asyncio.run(main()))
