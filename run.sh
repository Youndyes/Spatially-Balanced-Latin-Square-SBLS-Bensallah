#!/bin/bash
# Script d'execution
# Usage: ./run.sh [n] [model]
#   n     : taille du carre (defaut: 4)
#   model : refined, baseline ou compare (defaut: refined)

N=${1:-4}
MODEL=${2:-refined}

java -cp "out:lib/*" Utils --n $N --model $MODEL
