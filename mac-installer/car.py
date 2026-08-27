#!/usr/bin/env python3
"""car.py - drive a command list over the Haval H6 head unit's raw telnet (port 23).

Usage:  python3 car.py <host> <commands-file>

The commands file has one shell command per line (empty lines and lines starting
with # are skipped). After each command a poll marker is echoed and the script
reads until the marker comes back, so long-running commands (curl of a 115 MB
APK, pm install) are captured completely.

Telnet IAC negotiation bytes (0xff + 2) are filtered from the output.
"""
import os
import re
import socket
import sys
import time

HOST = sys.argv[1]
CMDS_FILE = sys.argv[2]
PORT = int(os.environ.get("HAVAL_PORT", "23"))  # HAVAL_PORT=2323 for the mock
IDLE_READ_S = 1.0
DEFAULT_TIMEOUT_S = 120
SLOW_MARKERS = ("curl", "pm install")  # lines containing these get extra time
SLOW_TIMEOUT_S = 900


def filter_iac(data: bytes) -> str:
    out = []
    i = 0
    while i < len(data):
        b = data[i]
        if b == 0xFF and i + 2 < len(data):  # telnet IAC sequence: skip 3 bytes
            i += 3
            continue
        out.append(chr(b))
        i += 1
    return "".join(out)


def main():
    s = socket.create_connection((HOST, PORT), timeout=15)
    s.settimeout(1.0)
    s.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)

    # read the banner until the socket goes quiet
    banner = ""
    deadline = time.time() + 10
    while time.time() < deadline:
        try:
            data = s.recv(65536)
            if not data:
                break
            banner += filter_iac(data)
        except socket.timeout:
            break
    print("=== banner ===")
    print(banner.strip())

    with open(CMDS_FILE, "r") as f:
        commands = [ln.strip() for ln in f if ln.strip() and not ln.lstrip().startswith("#")]

    for n, cmd in enumerate(commands):
        marker = f"__HAVX_{n}__"
        slow = any(m in cmd for m in SLOW_MARKERS)
        timeout = SLOW_TIMEOUT_S if slow else DEFAULT_TIMEOUT_S
        print(f"=== $ {cmd} (timeout {timeout}s) ===")
        try:
            s.sendall((cmd + "\n").encode())
        except BrokenPipeError:
            print("!! connection closed by head unit")
            sys.exit(1)
        s.sendall((f"echo {marker}\n").encode())

        # The head unit echoes the command line, so "echo __HAVX_n__" appears
        # in the stream before its output does. Matching the bare marker with
        # `in`/split() would return on the ECHOED line and leave the real marker
        # output in the socket, polluting the next command. Match the marker
        # only when it starts a line (that is the marker OUTPUT line).
        buf = ""
        deadline = time.time() + timeout
        m = None
        while time.time() < deadline and m is None:
            try:
                data = s.recv(65536)
                if not data:
                    print("!! connection closed by head unit")
                    sys.exit(1)
                buf += filter_iac(data)
            except socket.timeout:
                pass
            m = re.search(r"(?:\A|\n)" + re.escape(marker), buf)
        if m is None:
            print(f"!! TIMEOUT waiting for completion of: {cmd}")
            sys.exit(2)
        # print everything captured before the marker output line
        body = buf[: m.start()]
        print(body.rstrip())
    s.close()
    print("=== done ===")


if __name__ == "__main__":
    main()
