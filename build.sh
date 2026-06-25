#!/bin/bash

if [ $# -lt 1 ] || [ $# -gt 2 ]; then
    echo "Usage: ./build.sh [<sprint>] <version>"
    echo "  sprint:  0, 1, 2, or 3 (default: 0)"
    echo "  version: version number"
    exit 1
fi

if [ $# -eq 1 ]; then
    SPRINT=0
    VERSION=$1
else
    SPRINT=$1
    VERSION=$2
fi

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