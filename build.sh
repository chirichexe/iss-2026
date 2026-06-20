#!/bin/bash

if [ $# -ne 1 ]; then
    echo "Usage: ./build.sh <0|1|2|3>"
    exit 1
fi

case "$1" in
    0|1|2|3)
        typst compile --root . "sprint$1/docs/sprint$1.typ" "sprint$1/docs/sprint$1.pdf"
        ;;
    *)
        echo "Error: sprint must be 0, 1, 2 or 3"
        exit 1
        ;;
esac