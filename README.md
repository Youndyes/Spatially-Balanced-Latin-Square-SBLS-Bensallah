# Projet SBLS - Spatially Balanced Latin Square

**Auteur :** Bensallah Younes  
**Formation :** Master 1 Informatique - Université Côte d'Azur  
**Rendu :** 25 Janvier 2026

## Description

Ce projet implémente un solveur de contraintes pour le problème des **Spatially Balanced Latin Square (SBLS)** en utilisant Choco Solver.

## Structure du projet

```
lib/
├── choco-solver-4.10.14.jar
└── trove4j-3.0.3.jar
src/
└── main/java/
    ├── SBLSMethod.java     # Notre méthode avec équilibre spatial
    ├── SBLSSimple.java     # Modèle simple (MOLS uniquement)
    └── Utils.java          # Point d'entrée et utilitaires
compile.sh                  # Script de compilation
run.sh                      # Script d'exécution
test.sh                     # Script de test complet
```

## Prérequis

- Java 21+

## Utilisation

### Compilation
```bash
chmod +x *.sh
./compile.sh
```

### Exécution
```bash
./run.sh 4 refined    # Méthode pour n=4
./run.sh 5 baseline   # Modèle simple pour n=5
./run.sh 5 compare    # Comparaison
```

### Tests complets
```bash
./test.sh
```

## Résultats

| n | Simple (ms) | Notre méthode (ms) | Nœuds (Simple) | Nœuds (Notre méthode) |
|---|-------------|-------------------|----------------|----------------------|
| 3 | 8 | 18 | 4 | 2 |
| 4 | 11 | 141 | 10 | 24 |
| 5 | 590 | 1373 | 6735 | 327 |

