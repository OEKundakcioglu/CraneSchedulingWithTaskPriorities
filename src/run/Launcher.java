package run;

import data.Printing;
import data.Problem;
import data.ProblemSetting;
import com.gurobi.gurobi.GRBException;
import parameters.AlgorithmParameters;
import parameters.ProblemParameters;

public class Launcher {
    public static void main(String[] args) throws GRBException {

    	System.out.println("Classpath: " + System.getProperty("java.class.path"));
        ProblemSetting problemSettings = new ProblemSetting();
        ProblemParameters problemParameter = new ProblemParameters();
        AlgorithmParameters algorithmParameters = new AlgorithmParameters();
        
        if (args.length < 7) {
            System.out.println(
                    "Configurations not fulfilled. Please re-enter: detailedPrint-seed-#col-#job-duration-coeff");
        } else {
        	boolean loadFromJson = false;
            Problem problem = new Problem(Integer.parseInt(args[0]), Integer.parseInt(args[3]),
                    Integer.parseInt(args[2]), Integer.parseInt(args[1]), problemParameter.numberOfCranes,
                    problemParameter.speedTimeUnitsforOneColumnTravel, args[4], problemParameter.timeLimit,
                    Double.parseDouble(args[5]), Integer.parseInt(args[7]), loadFromJson);
            if (Integer.parseInt(args[6]) == 1) {
                Printing.writeCsv(problemSettings.solveSimpleGreedy(problem));
            }
            if (Integer.parseInt(args[6]) == 2) {
                Printing.writeCsv(problemSettings.solveGreedy(problem));
            }
            if (Integer.parseInt(args[6]) == 3) {
                Printing.writeCsv(problemSettings.solveGreedyConflict(problem));
            }
            if (Integer.parseInt(args[6]) == 4) {
                Printing.writeCsv(problemSettings.solveStage(problem));
            }
            if (Integer.parseInt(args[6]) == 5) {
                Printing.writeCsv(problemSettings.solveStageConflict(problem));
            }
            if (Integer.parseInt(args[6]) == 6) {
                Printing.writeCsv(problemSettings.solveADP(problem));
            }

            if (Integer.parseInt(args[6]) == 7) {
                Printing.writeCsv(problemSettings.solveExact(problem));
            }
            if (Integer.parseInt(args[6]) == 8) {
                Printing.writeCsv(problemSettings.solveADP_RDP(problem));
            }
            if (Integer.parseInt(args[6]) == 9) {
            	Printing.writeCsv(problemSettings.solveTS(problem));
            }
            if (Integer.parseInt(args[6]) == 10) {
            	Printing.writeCsv(problemSettings.solveTsBounding(problem));
            }
            if (Integer.parseInt(args[6]) == 11) {
            	Printing.writeCsv(problemSettings.solveTSBaseModel(problem));
            }
        }  
    }
}
