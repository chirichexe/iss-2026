#!/bin/bash

if [ $# -ne 2 ]; then
    echo "Usage: ./build.sh <0|1|2|3> <version>"
    exit 1
fi

SPRINT=$1
VERSION=$2

case "$SPRINT" in
    0|1|2|3)
        typst compile \
            --root . \
            "sprint$SPRINT/docs/sprint$SPRINT.typ" \
            "sprint$SPRINT/docs/sprint${SPRINT}_v${VERSION}.pdf"
        ;;
    *)
        echo "Error: sprint must be 0, 1, 2 or 3"
        exit 1
        ;;
esac