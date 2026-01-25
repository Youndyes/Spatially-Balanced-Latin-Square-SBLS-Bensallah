#!/bin/bash
# Script de compilation

echo  "Compilation du projet "

mkdir -p out
javac -cp "lib/*" -d out src/main/java/*.java

if [ $? -eq 0 ]; then
    echo "Compilation reussie !"
else
    echo "Erreur de compilation"
    exit 1
fi
