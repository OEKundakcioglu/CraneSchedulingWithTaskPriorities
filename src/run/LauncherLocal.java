package run;

import java.util.ArrayList;
import java.util.List;

import data.Printing;
import data.Problem;
import data.ProblemSetting;
import com.gurobi.gurobi.GRBException;
import parameters.AlgorithmParameters;
import parameters.ProblemParameters;

public class LauncherLocal {

    public static void main(String[] args) throws GRBException {
    	
        ProblemSetting problemSetting = new ProblemSetting();
        ProblemParameters problemParameters = new ProblemParameters();
        AlgorithmParameters algorithmParameters = new AlgorithmParameters();

        int[] numberOfJobs = {5}; // 10, 12, 14, 15, 10,15,20, ,30,35
        int[] seedNo = {67 }; //  ,4,5,10,13//2,3,4,5,6,10,14,15,20,87,65,55,77,88,99,76,54,67 ,,, ,4,5, , 3,4,2,10
                                           // 10,13,20,87,65,55,77 , ,3, 6, 10 ,,, 2,,3,4, 5, 10
        int[] numberOfColumns = { 25 };
        Double[] stageCoeff = { 0.2,0.6, 0.8 }; // 0.2, 0.4, 
        String[] duration = { "s" };
        int[] pr = {7}; // 7 exact, 1,2,3,10,
        int[] prStage = { 4, 5 };  
        int[] timeInterval = {1};
        double coeff = 0;
        
        boolean loadFromJson = false;
        
        List<String[]> allArguments = new ArrayList<>();
        for (int job : numberOfJobs) {
            for (int seed : seedNo) {
                for (int col : numberOfColumns) {
                    for (String dur : duration) {
                        for (int pro : pr) {
                        	for(int time : timeInterval) {
                        		//for(double coeff : stageCoeff) {
                        			
                        		
                        		String[] argumentSet = {
                                        "0",
                                        String.valueOf(seed),
                                        String.valueOf(col),
                                        String.valueOf(job),
                                        String.valueOf(dur),
                                        String.valueOf(coeff),
                                        String.valueOf(pro),
                                        String.valueOf(time)
                                };
                                allArguments.add(argumentSet);
                        		//}
                        	}
                            
                        }
                        /*
                         * for(Double coeff : stageCoeff)
                         * {
                         * for (int pros : prStage)
                         * {
                         * String[] argumentSet = {
                         * "0",
                         * String.valueOf(seed),
                         * String.valueOf(col),
                         * String.valueOf(job),
                         * String.valueOf(dur),
                         * String.valueOf(coeff),
                         * String.valueOf(pros)
                         * };
                         * allArguments.add(argumentSet);
                         * }
                         * }
                         */
                    }
                }
            }
        }

        for (String[] argumentSet : allArguments) {
            Problem problem = new Problem(
                    Integer.parseInt(argumentSet[0]),
                    Integer.parseInt(argumentSet[3]),
                    Integer.parseInt(argumentSet[2]),
                    Integer.parseInt(argumentSet[1]),
                    problemParameters.numberOfCranes,
                    problemParameters.speedTimeUnitsforOneColumnTravel,
                    argumentSet[4],
                    problemParameters.timeLimit,
                    Double.parseDouble(argumentSet[5]),
                    Integer.parseInt(argumentSet[7]), loadFromJson);

            if (Integer.parseInt(argumentSet[6]) == 1) {
                Printing.writeCsv(problemSetting.solveSimpleGreedy(problem));
            }
            if (Integer.parseInt(argumentSet[6]) == 2) {
                Printing.writeCsv(problemSetting.solveGreedy(problem));
            }
            if (Integer.parseInt(argumentSet[6]) == 3) {
                Printing.writeCsv(problemSetting.solveGreedyConflict(problem));
            }
            if (Integer.parseInt(argumentSet[6]) == 4) {
                Printing.writeCsv(problemSetting.solveStage(problem));
            }
            if (Integer.parseInt(argumentSet[6]) == 5) {
                Printing.writeCsv(problemSetting.solveStageConflict(problem));
            }
            if (Integer.parseInt(argumentSet[6]) == 7) {
                Printing.writeCsv(problemSetting.solveExact(problem));
            }
           
            if (Integer.parseInt(argumentSet[6]) == 10) {
                Printing.writeCsv(problemSetting.solveADP(problem));
            }
           
            if (Integer.parseInt(argumentSet[6]) == 13) {

                Printing.writeCsv(problemSetting.solveADP_RDP(problem));
            }

            if (Integer.parseInt(argumentSet[6]) == 15) {

                Printing.writeCsv(problemSetting.solveTS(problem));
            }
            if (Integer.parseInt(argumentSet[6]) == 16) {

                Printing.writeCsv(problemSetting.solveTSBaseModel(problem));
            }

            if (Integer.parseInt(argumentSet[6]) == 17) {

                Printing.writeCsv(problemSetting.solveTsBounding(problem));
            }

        }

    }

}
