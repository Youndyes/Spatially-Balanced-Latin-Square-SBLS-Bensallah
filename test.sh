#!/bin/bash
# Script de test complet

echo "=== Tests du projet SBLS ==="
echo ""

# Compilation
./compile.sh
if [ $? -ne 0 ]; then
    exit 1
fi

echo ""
echo "--- Test 1: Modele simple n=3 ---"
java -cp "out:lib/*" Utils --n 3 --model baseline

echo ""
echo "--- Test 2: Notre methode n=3 ---"
java -cp "out:lib/*" Utils --n 3 --model refined

echo ""
echo "--- Test 3: Notre methode n=4 ---"
java -cp "out:lib/*" Utils --n 4 --model refined

echo ""
echo "--- Test 4: Notre methode n=5 ---"
java -cp "out:lib/*" Utils --n 5 --model refined

echo ""
echo "--- Test 5: Comparaison des modeles ---"
java -cp "out:lib/*" Utils --model compare --maxN 5

echo ""
echo "=== Tous les tests sont termines ==="
