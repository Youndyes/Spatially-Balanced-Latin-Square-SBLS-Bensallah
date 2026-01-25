/**
 * Point d'entree du programme.
 * Usage: java Utils [options]
 *   --n <val>       : taille du carre (defaut: 4)
 *   --model <type>  : baseline, refined ou compare
 *   --maxN <val>    : pour compare, taille max (defaut: 5)
 */
public class Utils {
    
    public static void main(String[] args) {
        int n = 4;
        String mode = "refined";
        int maxN = 5;
        
        // Parsing des arguments
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--n") && i + 1 < args.length) {
                n = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--model") && i + 1 < args.length) {
                mode = args[++i];
            } else if (args[i].equals("--maxN") && i + 1 < args.length) {
                maxN = Integer.parseInt(args[++i]);
            }
        }
        
        afficherInfoSysteme();
        
        switch (mode) {
            case "baseline":
                System.out.println("\nBaseline (MOLS) pour n=" + n);
                SBLSSimple baseline = new SBLSSimple(n);
                baseline.solve();
                break;
                
            case "refined":
                System.out.println("\nModele raffine (SBLS) pour n=" + n);
                SBLSMethod refined = new SBLSMethod(n);
                refined.solve();
                break;
                
            case "compare":
                System.out.println("\nComparaison baseline vs refined");
                comparerModeles(maxN);
                break;
                
            default:
                System.out.println("Mode inconnu: " + mode);
        }
    }
    
    private static void comparerModeles(int maxN) {
        System.out.println("\n  n  | Baseline (ms) | Refined (ms)");
        System.out.println("-----|---------------|-------------");
        
        for (int n = 3; n <= maxN; n++) {
            SBLSSimple b = new SBLSSimple(n);
            b.setVerbose(false);
            b.solve();
            
            SBLSMethod r = new SBLSMethod(n);
            r.setVerbose(false);
            r.solve();
            
            System.out.printf("  %d  |     %6d    |    %6d\n", 
                n, b.getSolveTimeMs(), r.getSolveTimeMs());
        }
    }
    
    private static void afficherInfoSysteme() {
        System.out.println("=== Info systeme ===");
        System.out.println("Java: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name"));
    }
}
