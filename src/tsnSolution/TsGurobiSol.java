package tsnSolution;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Stopwatch;

import data.Problem;
import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;

public class TsGurobiSol {
	
	public Network network;
	public Problem problem;
	public double totalReward;
    public double elapsedTimeSolveModel;
    public Enum caseSet;
    public Enum tsCase;
    public double mipGap;
	
public TsGurobiSol(Problem problem, Enum cases, Enum tsCase) throws GRBException {
	this.problem = problem;	
	this.caseSet = cases;	
	this.tsCase = tsCase;
	 
	Stopwatch stopWatch = Stopwatch.createStarted();

    try {
        // Create the model
        GRBModel model = createModel();

        
            CompletableFuture<Double> task = CompletableFuture.supplyAsync(() -> {
                double reward = 0;
                try {
                    reward = solveModel(model);
                } catch (GRBException e) {
                    e.printStackTrace();
                }
                this.totalReward = reward;
                return this.totalReward;
            });

            try {
            	boolean isCompletedSuccessfully = task.get(this.problem.timeLimit, TimeUnit.SECONDS) != null;
            	if (isCompletedSuccessfully) {
    				stopWatch.stop();
    				elapsedTimeSolveModel = stopWatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0;  
    				mipGap = model.get(GRB.DoubleAttr.MIPGap);;
            	}
                      
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
    			stopWatch.stop();
  
    			elapsedTimeSolveModel = stopWatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0;
    			
    		}
        
    } catch (GRBException e) {
        e.printStackTrace();
    }
	}
	private GRBEnv createEnvironment(int numberOfJobs) throws GRBException {
    GRBEnv env = new GRBEnv();
    env.set(GRB.IntParam.LogToConsole , 0);  
    String logFileName = "tsn" + numberOfJobs + ".log";
    env.set(GRB.StringParam.LogFile, logFileName);
    
    
    env.start();
    return env;
	}

	public GRBModel createModel() throws GRBException {
		
		Network network = new Network(problem, this.tsCase);
		
		GRBEnv env = createEnvironment(problem.numberOfJobs);
      
        GRBModel model = new GRBModel(env);
        
        model.set(GRB.IntParam.MIPFocus, 1);
        model.set(GRB.IntParam.Cuts, 2); 
        
		Variables setVarParam = new Variables(problem, network, model);
		HashMap<Node, HashMap<Node, GRBVar>> variables = setVarParam.setVariables();
		HashMap<Node, HashMap<Node, Double>> parameters = setVarParam.setParameters();
		
		TsConstraints constraints = new TsConstraints(network, problem, variables, model, this.tsCase);
		constraints.setConstraints();
		
		Objective obj = new Objective(model, variables, parameters);
		obj.setObjective();
		
		return model;	
		
	}
	
	public double solveModel(GRBModel model) throws GRBException {		
		model.optimize();		    
		return model.get(GRB.DoubleAttr.ObjVal);
	}
	

}
