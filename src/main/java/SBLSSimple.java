import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.search.strategy.selectors.variables.InputOrder;
import org.chocosolver.solver.variables.IntVar;

/**
 * Modele baseline: uniquement MOLS (sans equilibre spatial).
 * Sert de comparaison avec le modele raffine.
 * 
 * Contraintes: allDifferent (GAC) + scalar (BC)
 * Strategie: InputOrder + IntDomainMin
 */
public class SBLSSimple {
    
    private int n;
    private boolean verbose = true;
    private long timeout = 300;
    
    private int[][] solA;
    private int[][] solB;
    private long temps;
    private long noeuds;
    private long backtracks;
    private boolean trouve = false;

    public SBLSSimple(int n) {
        this.n = n;
    }

    public void setVerbose(boolean v) {
        this.verbose = v;
    }

    public void setTimeoutSeconds(long t) {
        this.timeout = t;
    }

    public boolean solve() {
        Model model = new Model("MOLS n=" + n);

        // Variables
        IntVar[][] A = new IntVar[n][n];
        IntVar[][] B = new IntVar[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                A[i][j] = model.intVar("A" + i + j, 0, n - 1);
                B[i][j] = model.intVar("B" + i + j, 0, n - 1);
            }
        }

        // Carres latins
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

        // Orthogonalite
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

        // Cassage de symetries
        model.arithm(A[0][0], "=", 0).post();
        model.arithm(B[0][0], "=", 0).post();

        // Recherche simple
        Solver solver = model.getSolver();
        IntVar[] vars = new IntVar[2 * n * n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                vars[i * n + j] = A[i][j];
                vars[n * n + i * n + j] = B[i][j];
            }
        }
        solver.setSearch(Search.intVarSearch(
            new InputOrder<>(model),
            new IntDomainMin(),
            vars
        ));
        solver.limitTime(timeout * 1000);

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
            if (verbose) afficherSolution();
        } else {
            if (verbose) System.out.println("Pas de solution");
        }
        return trouve;
    }
    
    private void afficherSolution() {
        System.out.println("\n=== MOLS n=" + n + " (baseline) ===");
        System.out.println("Matrice A:");
        afficherMatrice(solA);
        System.out.println("Matrice B:");
        afficherMatrice(solB);
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
