import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.search.strategy.selectors.variables.DomOverWDeg;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.List;

/**
 * Modele raffine pour le probleme SBLS (Spatially Balanced Latin Squares).
 * 
 * Contraintes utilisees:
 * - allDifferent (GAC): lignes/colonnes des carres latins + orthogonalite
 * - scalar (BC): encodage des paires P = n*A + B
 * - reifyXeqC: indicateurs booleens pour les positions des symboles
 * - times (BC): contributions conditionnelles aux distances
 * - sum (BC): somme des distances par paire de symboles
 * - arithm (AC): egalites et cassage de symetries
 * 
 * Strategie: DomOverWDeg + IntDomainMin
 */
public class SBLSMethod {
    
    private int n;
    private boolean verbose = true;
    private long timeout = 300;
    
    private int[][] solA;
    private int[][] solB;
    private long temps;
    private long noeuds;
    private long backtracks;
    private boolean trouve = false;

    public SBLSMethod(int n) {
        this.n = n;
    }

    public void setVerbose(boolean v) {
        this.verbose = v;
    }

    public void setTimeoutSeconds(long t) {
        this.timeout = t;
    }

    public boolean solve() {
        Model model = new Model("SBLS n=" + n);

        // Variables: deux matrices n x n avec valeurs 0 a n-1
        IntVar[][] A = new IntVar[n][n];
        IntVar[][] B = new IntVar[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                A[i][j] = model.intVar("A" + i + j, 0, n - 1);
                B[i][j] = model.intVar("B" + i + j, 0, n - 1);
            }
        }

        // Contrainte 1: carres latins (allDifferent sur lignes et colonnes)
        for (int i = 0; i < n; i++) {
            model.allDifferent(A[i], "AC").post();
            model.allDifferent(B[i], "AC").post();
        }
        for (int j = 0; j < n; j++) {
            IntVar[] colA = new IntVar[n];
            IntVar[] colB = new IntVar[n];
            for (int i = 0; i < n; i++) {
                colA[i] = A[i][j];
                colB[i] = B[i][j];
            }
            model.allDifferent(colA, "AC").post();
            model.allDifferent(colB, "AC").post();
        }

        // Contrainte 2: orthogonalite (MOLS)
        // On encode chaque paire (A[i][j], B[i][j]) comme P = n*A + B
        IntVar[] P = new IntVar[n * n];
        int k = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                P[k] = model.intVar("P" + i + j, 0, n * n - 1);
                model.scalar(new IntVar[]{A[i][j], B[i][j]}, new int[]{n, 1}, "=", P[k]).post();
                k++;
            }
        }
        model.allDifferent(P, "AC").post();

        // Contrainte 3: equilibre spatial
        // Pour chaque paire (p,q), la somme des distances entre cellules 
        // contenant p et q doit etre identique
        
        // Precalcul des distances au carre entre cellules
        int[][] dist = new int[n * n][n * n];
        for (int c1 = 0; c1 < n * n; c1++) {
            int i1 = c1 / n, j1 = c1 % n;
            for (int c2 = 0; c2 < n * n; c2++) {
                int i2 = c2 / n, j2 = c2 % n;
                dist[c1][c2] = (i1 - i2) * (i1 - i2) + (j1 - j2) * (j1 - j2);
            }
        }

        // Indicateurs: isA[cell][s] = 1 ssi A contient s a la cellule cell
        BoolVar[][] isA = new BoolVar[n * n][n];
        BoolVar[][] isB = new BoolVar[n * n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int cell = i * n + j;
                for (int s = 0; s < n; s++) {
                    isA[cell][s] = model.boolVar();
                    isB[cell][s] = model.boolVar();
                    model.reifyXeqC(A[i][j], s, isA[cell][s]);
                    model.reifyXeqC(B[i][j], s, isB[cell][s]);
                }
            }
        }

        // Calcul des sommes de distances pour chaque paire de symboles
        int nbPaires = n * (n - 1) / 2;
        int maxSum = n * n * 2 * (n - 1) * (n - 1);
        IntVar[] distA = new IntVar[nbPaires];
        IntVar[] distB = new IntVar[nbPaires];
        
        int idx = 0;
        for (int p = 0; p < n; p++) {
            for (int q = p + 1; q < n; q++) {
                List<IntVar> termesA = new ArrayList<>();
                List<IntVar> termesB = new ArrayList<>();
                
                for (int c1 = 0; c1 < n * n; c1++) {
                    for (int c2 = c1 + 1; c2 < n * n; c2++) {
                        int d = dist[c1][c2];
                        if (d > 0) {
                            // Matrice A: contribution si (c1=p et c2=q) ou (c1=q et c2=p)
                            BoolVar pqA = model.boolVar();
                            BoolVar qpA = model.boolVar();
                            model.arithm(isA[c1][p], "+", isA[c2][q], "=", 2).reifyWith(pqA);
                            model.arithm(isA[c1][q], "+", isA[c2][p], "=", 2).reifyWith(qpA);
                            
                            IntVar contribA = model.intVar(0, 1);
                            model.arithm(pqA, "+", qpA, "=", contribA).post();
                            IntVar termA = model.intVar(0, d);
                            model.times(contribA, d, termA).post();
                            termesA.add(termA);
                            
                            // Matrice B: idem
                            BoolVar pqB = model.boolVar();
                            BoolVar qpB = model.boolVar();
                            model.arithm(isB[c1][p], "+", isB[c2][q], "=", 2).reifyWith(pqB);
                            model.arithm(isB[c1][q], "+", isB[c2][p], "=", 2).reifyWith(qpB);
                            
                            IntVar contribB = model.intVar(0, 1);
                            model.arithm(pqB, "+", qpB, "=", contribB).post();
                            IntVar termB = model.intVar(0, d);
                            model.times(contribB, d, termB).post();
                            termesB.add(termB);
                        }
                    }
                }
                
                distA[idx] = model.intVar("dA_" + p + "_" + q, 0, maxSum);
                distB[idx] = model.intVar("dB_" + p + "_" + q, 0, maxSum);
                model.sum(termesA.toArray(new IntVar[0]), "=", distA[idx]).post();
                model.sum(termesB.toArray(new IntVar[0]), "=", distB[idx]).post();
                idx++;
            }
        }

        // Toutes les sommes doivent etre egales
        for (int i = 1; i < nbPaires; i++) {
            model.arithm(distA[0], "=", distA[i]).post();
            model.arithm(distB[0], "=", distB[i]).post();
        }

        // Cassage de symetries
        model.arithm(A[0][0], "=", 0).post();
        model.arithm(B[0][0], "=", 0).post();
        for (int j = 0; j < n; j++) {
            model.arithm(A[0][j], "=", j).post();
        }
        for (int i = 0; i < n; i++) {
            model.arithm(A[i][0], "=", i).post();
        }

        // Strategie de recherche: DomOverWDeg
        Solver solver = model.getSolver();
        IntVar[] vars = new IntVar[2 * n * n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                vars[i * n + j] = A[i][j];
                vars[n * n + i * n + j] = B[i][j];
            }
        }
        solver.setSearch(Search.intVarSearch(
            new DomOverWDeg(vars, 0L),
            new IntDomainMin(),
            vars
        ));
        solver.limitTime(timeout * 1000);

        // Resolution
        long t0 = System.currentTimeMillis();
        Solution sol = solver.findSolution();
        temps = System.currentTimeMillis() - t0;
        noeuds = solver.getNodeCount();
        backtracks = solver.getBackTrackCount();
        
        if (sol != null) {
            trouve = true;
            solA = new int[n][n];
            solB = new int[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    solA[i][j] = sol.getIntVal(A[i][j]);
                    solB[i][j] = sol.getIntVal(B[i][j]);
                }
            }
            if (verbose) afficherSolution(sol, distA, distB);
        } else {
            if (verbose) {
                System.out.println("Pas de solution" + (solver.isStopCriterionMet() ? " (timeout)" : ""));
            }
        }
        return trouve;
    }
    
    private void afficherSolution(Solution sol, IntVar[] distA, IntVar[] distB) {
        System.out.println("\n=== Solution SBLS n=" + n + " ===");
        System.out.println("Matrice A:");
        afficherMatrice(solA);
        System.out.println("Matrice B:");
        afficherMatrice(solB);
        
        System.out.println("Distances (equilibre spatial):");
        int idx = 0;
        for (int p = 0; p < n; p++) {
            for (int q = p + 1; q < n; q++) {
                System.out.println("  (" + p + "," + q + "): A=" + sol.getIntVal(distA[idx]) 
                    + ", B=" + sol.getIntVal(distB[idx]));
                idx++;
            }
        }
        System.out.println("Stats: " + noeuds + " noeuds, " + backtracks + " backtracks, " 
            + (temps/1000.0) + "s");
    }

    private void afficherMatrice(int[][] m) {
        for (int[] ligne : m) {
            System.out.print("  ");
            for (int v : ligne) System.out.print(v + " ");
            System.out.println();
        }
    }

    public boolean isSolutionFound() { return trouve; }
    public int[][] getSolutionA() { return solA; }
    public int[][] getSolutionB() { return solB; }
    public long getSolveTimeMs() { return temps; }
    public long getNodeCount() { return noeuds; }
    public long getBacktrackCount() { return backtracks; }
}
