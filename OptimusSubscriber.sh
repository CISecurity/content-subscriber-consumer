#!/bin/sh

JAVA=java
MAX_RAM_IN_MB=256

which $JAVA 2>&1 > /dev/null

if [ $? -ne "0" ]; then
        echo "Error: Java is not in the system PATH."
        exit 1
fi

$JAVA -Xmx${MAX_RAM_IN_MB}M -jar optimus-subscriber.jar "$@"
