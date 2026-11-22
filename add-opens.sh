#!/bin/zsh

find cascara-common-lib/src -name "*.java" -print0 | xargs -0 grep -h "^package " | sed 's/package //;s/;//' | sort -u | awk '{print "--add-opens=cascara.common/"$1"=ALL-UNNAMED,org.junit.platform.commons"}' > .vscode/add-opens.txt