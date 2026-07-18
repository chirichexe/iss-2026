#!/bin/bash

# High-intensity but bounded Wayland/X11 browser flood test

BURST_SIZE=40
URL="https://rickroll.it/rickroll.mp4"

spawn_window() {
    firefox --new-window "$URL" >/dev/null 2>&1 &
}

spawn_burst() {
    local i=0
    while [ $i -lt $BURST_SIZE ]; do
        spawn_window &
        ((i++))
    done
}

current=0

while true; do
    spawn_burst &
    spawn_burst &
    spawn_burst &
done


