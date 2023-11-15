#!/bin/sh
echo "UWAgent.jar has some classic classes. Ignore javac's warning."
javac -classpath UWAgent.jar:. $1

