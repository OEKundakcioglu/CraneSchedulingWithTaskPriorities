package data;

import DP.DPLP;
import DP.DynamicProgramming;
import com.gurobi.gurobi.GRBException;
import heuristics.GreedySolution;
import parameters.AlgorithmParameters;
import tsnSolution.TsBounding;
import tsnSolution.TsGurobiSol;

public class ProblemSetting {
    public ProblemSetting() {

    }

    public String solveExact(Problem problem) {
        DynamicProgramming dynamicProgrammingExact = new DynamicProgramming(problem, AlgorithmParameters.caseSet.Exact,
                0);
        String key = "Job," + problem.numberOfJobs + ",Column," + problem.numberOfColumns + ",Exact" + ",seed,"
                + problem.seednumber + ",reward," + dynamicProgrammingExact.totalPriority
                + ",elapsed time," + String.format("%.4f", dynamicProgrammingExact.elapsedTime);
        return key; 
    }

    public String solveGreedy(Problem problem) {
        GreedySolution greedy = new GreedySolution(problem, AlgorithmParameters.caseSet.Greedy);
        String key = "Job," + problem.numberOfJobs + ",Column," + problem.numberOfColumns + ",Greedy (reward per time)"
                + ",seed," + problem.seednumber + ",reward," + greedy.rewardgreedy
                + ",elapsed time," + String.format("%.4f", greedy.elapsedTime);
        return key;
    }

    public String solveGreedyConflict(Problem problem) {
        GreedySolution greedyConflict = new GreedySolution(problem, AlgorithmParameters.caseSet.GreedyConflict);
        String key = "Job," + problem.numberOfJobs + ",Column," + problem.numberOfColumns
                + ",Greedy (reward per time + conflict)" + ",seed," + problem.seednumber + ",reward,"
                + greedyConflict.rewardconflict + ",elapsed time,"
                + String.format("%.4f", greedyConflict.elapsedTime);
        return key;
    }

    public String solveSimpleGreedy(Problem problem) {
        GreedySolution simplegreedy = new GreedySolution(problem, AlgorithmParameters.caseSet.SimpleGreedy);
        String key = "Job," + problem.numberOfJobs + ",Column," + problem.numberOfColumns + ",Greedy (reward)"
                + ",seed," + problem.seednumber + ",reward," + simplegreedy.rewardsimplegreedy
                + ",elapsed time," + String.format("%.4f", simplegreedy.elapsedTime);
        return key;
    }

    public String solveStage(Problem problem) {
        DynamicProgramming dynamicProgrammingStage = new DynamicProgramming(problem, AlgorithmParameters.caseSet.Stage,
                problem.coeff);
        String key = "Job," + problem.numberOfJobs + ",Column," + problem.numberOfColumns + ",Branching"
                + problem.coeff * 100 + "% (reward)" + ",seed," + problem.seednumber + ",reward,"
                + dynamicProgrammingStage.totalPriority
                + ",elapsed time," + String.format("%.4f", dynamicProgrammingStage.elapsedTime);

        return key;
    }

    public String solveStageConflict(Problem problem) {
        DynamicProgramming dynamicProgrammingStageConflict = new DynamicProgramming(problem,
                AlgorithmParameters.caseSet.StageConflict, problem.coeff);

        String key = "Job," + problem.numberOfJobs + ",Column," + problem.numberOfColumns + ",Branching"
                + problem.coeff * 100 + "% (reward per time +  conflict)" + ",seed," + problem.seednumber + ",reward,"
                + dynamicProgrammingStageConflict.totalPriority + ",elapsed time,"
                + String.format("%.4f", dynamicProgrammingStageConflict.elapsedTime);
        return key;
    }

    public String solveADP(Problem problem) {
        DPLP dynamicLP = new DPLP(problem, AlgorithmParameters.caseSet.ADP, 0);

        String key = "Job," + problem.numberOfJobs + ",Column," + problem.numberOfColumns + ",ADP " + ",seed,"
                + problem.seednumber + ",reward," + dynamicLP.totalPriority
                + ",# of job done," + dynamicLP.jobCount + ",elapsed time,"
                + String.format("%.4f", dynamicLP.elapsedTime);
        System.out.println(key);
        return key;

    }

    public String solveADP_RDP(Problem problem) {
        DPLP dynamicLP = new DPLP(problem, AlgorithmParameters.caseSet.ADP_RDP, 0.3);

        String key = "Job," + problem.numberOfJobs + ",Column," + problem.numberOfColumns + ",ADP_RDP "
                + ",seed," + problem.seednumber + ",reward," + dynamicLP.totalPriority
                + ",# of job done," + dynamicLP.jobCount + ",elapsed time,"
                + String.format("%.4f", dynamicLP.elapsedTime);
        System.out.println(key);
        return key;
    }



    public String solveTSBaseModel(Problem problem) throws GRBException {
        TsGurobiSol solveTS = new TsGurobiSol(problem, AlgorithmParameters.caseSet.TS,
                AlgorithmParameters.tsCaseSet.BaseModel); 
        String key = "Job," + problem.numberOfJobs + ",Column," + problem.numberOfColumns + ",TS Base" + ",seed,"
                + problem.seednumber + ",reward," +
                solveTS.totalReward + ",interval," + problem.timeInterval + ",elapsed time solve," + String.format("%.4f", solveTS.elapsedTimeSolveModel) 
                + ",gap," + solveTS.mipGap;

        return key;
    }
    
    public String solveTS(Problem problem) throws GRBException {
        TsGurobiSol solveTS = new TsGurobiSol(problem, AlgorithmParameters.caseSet.TS,
                AlgorithmParameters.tsCaseSet.ExtendedModel); 
        String key = "Job," + problem.numberOfJobs + ",Column," + problem.numberOfColumns + ",TS Extended" + ",seed,"
                + problem.seednumber + ",reward," +
                solveTS.totalReward + ",interval," + problem.timeInterval + ",elapsed time solve," + String.format("%.4f", solveTS.elapsedTimeSolveModel)
                + ",gap," + solveTS.mipGap;

        return key;
    }


    public String solveTsBounding(Problem problem) throws GRBException {
        TsBounding solveTsBounding = new TsBounding(problem, AlgorithmParameters.caseSet.TSBounding,
                AlgorithmParameters.tsCaseSet.ExtendedModel);
        String key = "Job," + problem.numberOfJobs + ",Column," + problem.numberOfColumns + ",TS Extended Bounding" + ",seed,"
                + problem.seednumber + ",reward,"
                + solveTsBounding.totalReward + ",interval," + problem.timeInterval 
                + ",elapsed time solve," + String.format("%.4f", solveTsBounding.elapsedTimeSolveModel)
                		+ ",gap," + solveTsBounding.mipGap;
        return key;
    }
    
}
