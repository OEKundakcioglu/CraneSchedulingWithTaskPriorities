package tsnSolution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Stopwatch;

import data.Job;
import data.Problem;
import DP.CraneStatus;
import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBCallback;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;
import heuristics.HeuristicsNetworkSol;
import parameters.AlgorithmParameters;

public class TsBounding {

    public Network network;
    public Problem problem;
    public double totalReward;
    public double elapsedTimeSolveModel;
    public Enum caseSet;
    public Enum tsCase;
    public double mipGap;

    public TsBounding(Problem problem, Enum cases, Enum tsCase) throws GRBException {
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
        	//System.out.println("Error code: " + e.getErrorCode() + ". " +
        	//         e.getMessage());
        }
    }
    
    private GRBEnv createEnvironment(int numberOfJobs) throws GRBException {
        GRBEnv env = new GRBEnv();
        env.set(GRB.IntParam.LogToConsole , 0);      
        String logFileName = "tsn_bounding" + numberOfJobs + ".log";
        env.set(GRB.StringParam.LogFile, logFileName);
               
        env.start();
        return env;
    }
    
    public GRBModel createModel() throws GRBException {

        this.network = new Network(problem, this.tsCase);
           
        GRBEnv env = createEnvironment(problem.numberOfJobs);

        GRBModel model = new GRBModel(env);
        model.set(GRB.IntParam.MIPFocus, 1);
        model.set(GRB.IntParam.Cuts, 2);

        Variables setVarParam = new Variables(problem, this.network, model);
        HashMap<Node, HashMap<Node, GRBVar>> variables = setVarParam.setVariables();
        HashMap<Node, HashMap<Node, Double>> parameters = setVarParam.setParameters();

        TsConstraints constraints = new TsConstraints(this.network, problem, variables, model, this.tsCase);
        constraints.setConstraints();

        Objective obj = new Objective(model, variables, parameters);
        obj.setObjective();
        
        model.update();

        setInitialHeuristicsSol(model, variables, this.network);
         
        return model;
    }

    public double solveModel(GRBModel model) throws GRBException {
          
        model.optimize();

        return model.get(GRB.DoubleAttr.ObjVal);
    }

    private void setInitialHeuristicsSol(GRBModel model,
            HashMap<Node, HashMap<Node, GRBVar>> variables, Network network) {
        try {
            
            HeuristicsNetworkSol greedyNetwork = new HeuristicsNetworkSol(problem,
                    AlgorithmParameters.caseSet.GreedyConflict, problem.timeInterval, AlgorithmParameters.tsCaseSet.HeuristicsNetwork, network);
            ArrayList<Job> setofCompletedTasksC1 = greedyNetwork.setOfCompletedJobsC1;
            ArrayList<Job> setofCompletedTasksC2 = greedyNetwork.setOfCompletedJobsC2;
            CraneStatus[] craneStatus = greedyNetwork.craneStatus;
            
            // initialize all variables as 0
            for (Node from : variables.keySet()) {
                for (Node to : variables.get(from).keySet()) {
                    GRBVar grbVar = variables.get(from).get(to);
                    grbVar.set(GRB.DoubleAttr.Start, 0.0);
                }
            }
            
            //set value of variables for crane1
            HashMap<String, HashMap<Node, Node>> varNodeMap1 = new HashMap<>();
            ArrayList<String> vars1 = new ArrayList<>();   
            setTaskVars(setofCompletedTasksC1, network, variables, varNodeMap1, vars1);
            setTravelVars(vars1, variables, varNodeMap1, problem.timeInterval);
            setGroundArcVars(vars1, variables, varNodeMap1, craneStatus[0].craneTrajectory, problem.timeInterval);
            setSinkVars(setofCompletedTasksC1, variables);
            setSourceVars(setofCompletedTasksC1, variables, network.source1);
            //set value of variables for crane2
            HashMap<String, HashMap<Node, Node>> varNodeMap2 = new HashMap<>();
            ArrayList<String> vars2 = new ArrayList<>();
            setTaskVars(setofCompletedTasksC2, network, variables, varNodeMap2, vars2);
            setTravelVars(vars2, variables, varNodeMap2, problem.timeInterval);
            setGroundArcVars(vars2, variables, varNodeMap2, craneStatus[1].craneTrajectory, problem.timeInterval);
            setSinkVars(setofCompletedTasksC2, variables);
            setSourceVars(setofCompletedTasksC2, variables, network.source2);
                                
            
        } catch (GRBException e) {
            e.printStackTrace();
           
        }
    }
    
    public void setTaskVars(ArrayList<Job> completedTasks, Network network, HashMap<Node, HashMap<Node, GRBVar>> variables, HashMap<String, HashMap<Node, Node>> varNodeMap, ArrayList<String> vars) throws GRBException {
    	 // set task variable
    	for (Job j : completedTasks) {
            Node[] fromNodes = network.network.get(j)[0];
            for (Node from : fromNodes) {
                if (from != null && from.time == j.availableTime) {
                    for (Node to : variables.get(from).keySet()) {
                        if (to.name.equals("outgoing")) {
                            GRBVar grbVar = variables.get(from).get(to);
                            grbVar.set(GRB.DoubleAttr.Start, 1.0);
                         
                            vars.add(grbVar.get(GRB.StringAttr.VarName));

                            if (!varNodeMap.containsKey(grbVar.get(GRB.StringAttr.VarName))) {
                                varNodeMap.put(grbVar.get(GRB.StringAttr.VarName), new HashMap<>());
                            }

                            HashMap<Node, Node> nodeMap = varNodeMap.get(grbVar.get(GRB.StringAttr.VarName));
                            nodeMap.put(from, to);
                        }
                    }
                }
            }
        }
    }
    
    private void setSourceVars(ArrayList<Job> completedTasks, HashMap<Node, HashMap<Node, GRBVar>> variables, Node source) throws GRBException {
    	 // set source variables 
    	if(!completedTasks.isEmpty()) {
         	Job firstTask2 = completedTasks.get(0);
             for (Node to : variables.get(source).keySet()) {
             	if(to.time == firstTask2.availableTime && to.job.jobID ==firstTask2.jobID) {
             		GRBVar grbVar = variables.get(source).get(to);
             		grbVar.set(GRB.DoubleAttr.Start, 1.0);
             		
             	}                	
             }
         }     	
    }
    
    private void setTravelVars( ArrayList<String> vars, HashMap<Node, HashMap<Node, GRBVar>> variables,  HashMap<String,
    		HashMap<Node, Node>> varNodeMap, int timeInterval) throws GRBException {
    	//set travel arc variable
        for (String v : vars) {
        	HashMap<Node, Node> nodes1 = varNodeMap.get(v);
        	Node fromNode1 = nodes1.keySet().iterator().next();
            Node toNode1 = nodes1.get(fromNode1);
        	for (String k : vars) {
        		if(!v.equals(k)) {
        			HashMap<Node, Node> nodes2 = varNodeMap.get(k);
                	Node fromNode2 = nodes2.keySet().iterator().next();
                    int diffTime = Math.abs(toNode1.time - fromNode2.time);
                    int diffLoc = (int) Math.ceil((double)(Math.abs(toNode1.locationId - fromNode2.locationId)) / timeInterval);
        			if(diffTime == diffLoc) {
        				ArrayList<Node> outgoingNodes = toNode1.getOutgoing();
        				for (Node outgoingNode : outgoingNodes) {
        					if(outgoingNode.job != null) {
        						if(outgoingNode.job.jobID == fromNode2.job.jobID) {
            						GRBVar grbVar = variables.get(toNode1).get(outgoingNode);
            						grbVar.set(GRB.DoubleAttr.Start, 1.0);
            				
            						break;
            					}
        					}        					
        				}
        				break;
        			}
        		} 
        	}        
        }    	
    }
    
    private void setGroundArcVars(ArrayList<String> vars, HashMap<Node, HashMap<Node, GRBVar>> variables,  HashMap<String,
    		HashMap<Node, Node>> varNodeMap, ArrayList<Integer> craneTrajectory, int timeInterval) throws GRBException {
    	
    	 for (String v : vars) {
         	HashMap<Node, Node> nodes1 = varNodeMap.get(v);
         	Node fromNode1 = nodes1.keySet().iterator().next();
             Node toNode1 = nodes1.get(fromNode1);
         	for (String k : vars) {
         		if(!v.equals(k)) {
         			HashMap<Node, Node> nodes2 = varNodeMap.get(k);
                 	Node fromNode2 = nodes2.keySet().iterator().next();
                     int diffTime = Math.abs(toNode1.time - fromNode2.time);
                     int diffLoc = (int) Math.ceil((double)(Math.abs(toNode1.locationId - fromNode2.locationId)) / timeInterval);
                     if(fromNode1.time <=  fromNode2.time) {
                     	if(diffTime != diffLoc) {
                     		// take ground arc at origin or destination
                     		// waiting at destination of corresponding task
                     		if(craneTrajectory.get(toNode1.time * timeInterval + 1) == toNode1.locationId) {
                     			ArrayList<Node> outgoingNodes = toNode1.getOutgoing();
                 				Node outgoingGroundNode = null;
                 				for (Node outgoingNode : outgoingNodes) {
                 					if (outgoingNode.name.equals("GroundArc")) {
                 						GRBVar grbVar = variables.get(toNode1).get(outgoingNode);
                 						grbVar.set(GRB.DoubleAttr.Start, 1.0);
                 						//System.out.println(grbVar.get(GRB.StringAttr.VarName));
                 						outgoingGroundNode = outgoingNode;
                 						break; 
                 					 } 
                 				}
             					int timeWait = Math.abs(toNode1.time - fromNode2.time) - diffLoc;
                 				for(int c=1; c< timeWait; c++) {
                 					
                 					Node[] fromNodes = network.network.get(toNode1.job)[0];
                         			for (Node fromNode : fromNodes) {
                         				if(fromNode != null) {
                         					for (Node outgoingNode : fromNode.getOutgoing()) {
                         		                if (!outgoingNode.name.equals("GroundArc")) {
                         		                	for (Node outgoingNodeofOutgoing : outgoingNode.getOutgoing()) {
                         		                		if (outgoingNodeofOutgoing.name.equals("GroundArc")) {
                         		                			if (outgoingNodeofOutgoing.time == outgoingGroundNode.time + c) {
 	                            		                    	Node nextGroundArcNode1 = outgoingNode;
 	                            		                    	if (nextGroundArcNode1 != null) {
 	                            		                    		GRBVar grbVar = variables.get(nextGroundArcNode1).get(outgoingNodeofOutgoing);
 	                            		                    		grbVar.set(GRB.DoubleAttr.Start, 1.0);
 	                                        						
 	                            		                    	}
 	                            		                    }
                         		                		}                            		                		
                         		                	}	                            		                    
                         		                }
                         		            }
                         				}
                         			}           					
                 				}
                 				// travel arc set
                 				Node[] fromNodes = network.network.get(fromNode1.job)[0];
                     			for (Node fromNode : fromNodes) {
                     				if(fromNode != null) {
                     					for (Node outgoingNode : fromNode.getOutgoing()) {
                     						for (Node outgoingNodeofOutgoing : outgoingNode.getOutgoing()) {
                     							if(!outgoingNodeofOutgoing.name.equals("SinkNode")) {
                     								if (outgoingNodeofOutgoing.job.jobID == fromNode2.job.jobID && 
                     										outgoingNode.time + diffLoc == fromNode2.time ) { 
                         								GRBVar grbVar = variables.get(outgoingNode).get(outgoingNodeofOutgoing);
                                 						grbVar.set(GRB.DoubleAttr.Start, 1.0);
                                 					
                                 						break;
                     							}
                     							
                     		                	
                     		                }}}}}
                 				
                 				break;
                     			}
                     		
                     		// waiting at origin of next task
                     		else {
                     			ArrayList<Node> outgoingNodes = toNode1.getOutgoing();
                 				Node outgoingGroundNode = null;
                 				for (Node outgoingNode : outgoingNodes) {
                 					if (outgoingNode.job.jobID == fromNode2.job.jobID) { 
                 						GRBVar grbVar = variables.get(toNode1).get(outgoingNode);
                 						grbVar.set(GRB.DoubleAttr.Start, 1.0);
                 						//System.out.println(grbVar.get(GRB.StringAttr.VarName));
                 						outgoingGroundNode = outgoingNode;
                 						
                 						GRBVar grbVarGround = findGroundArcVar(outgoingNode.job, network.network, outgoingNode, timeInterval, variables);
                 						grbVarGround.set(GRB.DoubleAttr.Start, 1.0);
                 						
                 						break; 
                 					 }
                 				}
                 				int timeWait = fromNode2.time - outgoingGroundNode.time;
                 				for(int c=2; c<= timeWait; c++) {
                 					Node[] fromNodes = network.network.get(fromNode2.job)[0];
                         			for (Node fromNode : fromNodes) {
                         				if(fromNode != null) {
                         					for (Node outgoingNode : fromNode.getOutgoing()) {
                         		                if (outgoingNode.name.equals("GroundArc")) {
                         		                    if (outgoingNode.time == outgoingGroundNode.time + c) {
                         		                    	Node nextGroundArcNode1 = outgoingNode;
                         		                    	if (nextGroundArcNode1 != null) {
                         		                    		GRBVar grbVar = variables.get(fromNode).get(nextGroundArcNode1);
                         		                    		grbVar.set(GRB.DoubleAttr.Start, 1.0);
                                     						
                         		                    	}
                         		                    }
                         		                }
                         		            }
                         				}
                         			}
                 				           					
                 				}
                 				break;
                     		}
             				
             			}
             			else {
             				break;
             			}
                     }
         			            			
         		}
         	}
      
         }
    }
    
    private void setSinkVars(ArrayList <Job> setOfCompletedJobs, HashMap<Node, HashMap<Node, GRBVar>> variables ) throws GRBException {
    	
    	 if(!setOfCompletedJobs.isEmpty()) {
         	Job lastTask2 = setOfCompletedJobs.get(setOfCompletedJobs.size()-1);
             Node[] nodesOther = network.network.get(lastTask2)[0];
         	for (Node node : nodesOther) {
         		if(node != null) {
             		if (node.time == lastTask2.availableTime) {
                         for (Node to : variables.get(node).keySet()) {
                         	ArrayList<Node> outgoingNodes = to.getOutgoing();
                         	for (Node outgoingNode : outgoingNodes) {
                         		if (outgoingNode.name.equals("SinkNode")) {
                 					GRBVar grbVar = variables.get(to).get(outgoingNode);
                                     grbVar.set(GRB.DoubleAttr.Start, 1.0);
                                     
                 				}
                         	}                  
                         }
                     }
         		}
         	}
         }
    }
    
    private GRBVar findGroundArcVar(Job job, HashMap<Job, Node[][]> network, Node fromNode1, int interval, HashMap<Node, HashMap<Node, GRBVar>> variables) {
	    Node[][] jobNetwork = network.get(job);
	    
	    int startingLocation = job.fromColumn;
	    int startingTime = fromNode1.time;

	    if (jobNetwork != null && startingLocation >= 0 && startingTime >= 0) {
	        Node fromNode = jobNetwork[0][startingTime];
	        if (fromNode != null) {
	            for (Node outgoingNode : fromNode.getOutgoing()) {
	                if (outgoingNode.name.equals("GroundArc")) {
	                    if (outgoingNode.time == startingTime + 1) {  
	                    	GRBVar grbVarGround = variables.get(fromNode).get(outgoingNode);
	                        return grbVarGround;
	                    }
	                }
	            }
	        }
	    }
	    return null;
	}
}


