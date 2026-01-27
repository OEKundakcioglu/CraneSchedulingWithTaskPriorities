package tsnSolution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import data.Job;
import data.Problem;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;

public class TsConstraints {

	private Network network;
	private Problem problem;
	private HashMap<Node, HashMap<Node, GRBVar>> X;
	private GRBModel model;
	public Enum tsCase;

	public TsConstraints(Network network, Problem problem, HashMap<Node, HashMap<Node, GRBVar>> X, GRBModel model,
			Enum tsCase) throws GRBException {

		this.network = network;
		this.problem = problem;
		this.X = X;
		this.model = model;
		this.tsCase = tsCase;

	}

	void setConstraints() throws GRBException {
		setSourceCons();
		setSinkCons();
		setFlowCons();
		setAssingmentCons();
		if (!this.tsCase.toString().equals("ConflictFree")) {
			setConflictTaskCons();
			setConflictTaskCornerCaseCons();
			setConflictTaskTravelCons();
			setConflictTaskPushCons();
			setConflictTaskGroundCons();
			setConflictTaskSourceCons();
		}

		setPushAvodanceCons();
		setConsecutivePushCons();

	}

	public void setConflictFreeConstraints() throws GRBException {
		setSourceCons();
		setSinkCons();
		setFlowCons();
		setPushAvodanceCons();
		setConsecutivePushCons();

	}

	public void setSourceCons() throws GRBException {
		// Eq 10b
		// Flow constraint at source node for each crane
		Node[] fromSources = { network.source1, network.source2 };
		for (Node from : fromSources) {
			GRBLinExpr cons1 = new GRBLinExpr();

			for (Node to : X.get(from).keySet()) {
				GRBVar X_a = X.get(from).get(to);
				cons1.addTerm(1, X_a);

			}
			model.addConstr(cons1, GRB.EQUAL, 1.0, "SourceCons" + from.name);

		}
	}

	public void setSinkCons() throws GRBException {
		// Eq 10c
		// Flow constraint at sink node
		GRBLinExpr cons2 = new GRBLinExpr();
		for (Node from : X.keySet()) {
			for (Node to : X.get(from).keySet()) {
				if (to.name.equals("SinkNode")) {
					GRBVar X_a = X.get(from).get(to);
					cons2.addTerm(1, X_a);
				}
			}
		}
		model.addConstr(cons2, GRB.EQUAL, 2.0, "SinkCons");
	}

	public void setFlowCons() throws GRBException {
		// Eq 10d and Eq 10e
		HashMap<String, List<Node>> groupedNodesForTaskFlow = new HashMap<>();
		for (Node from : X.keySet()) {
			for (Node to : X.get(from).keySet()) {
				if (!to.name.equals("SinkNode")) {
					String key = to.time + "_" + to.locationId;
					groupedNodesForTaskFlow.computeIfAbsent(key, k -> new ArrayList<>()).add(to);
				}
			}
		}
		for (Node from : X.keySet()) {
			if (!from.name.equals("SourceNode1") && !from.name.equals("SourceNode2")) {
				String key = from.time + "_" + from.locationId;
				groupedNodesForTaskFlow.computeIfAbsent(key, k -> new ArrayList<>()).add(from);
			}
		}
		// Cons inflow = outflow for each time line
		for (List<Node> group : groupedNodesForTaskFlow.values()) {
			HashMap<Integer, List<GRBVar>> inflowOrigin = new HashMap<>();
			HashMap<Integer, List<GRBVar>> inflowDest = new HashMap<>();
			HashMap<Integer, List<GRBVar>> outflowDest = new HashMap<>();
			HashMap<Integer, List<GRBVar>> outflowOrigin = new HashMap<>();
			for (Node nodeX : group) {
				// in flow
				for (Node from : X.keySet()) {
					for (Node to : X.get(from).keySet()) {
						if (to == nodeX) {
							Integer jobID = to.job.jobID;
							Job toJob = to.job;

							if (to.locationId == toJob.fromColumn) {
								inflowOrigin.computeIfAbsent(jobID, k -> new ArrayList<>()).add(X.get(from).get(to));
							}
							if (to.locationId == toJob.toColumn) {
								inflowDest.computeIfAbsent(jobID, k -> new ArrayList<>()).add(X.get(from).get(to));
							}
						}
					}
				}

				// out flow
				for (Node from : X.keySet()) {
					for (Node to : X.get(from).keySet()) {
						if (from == nodeX) {
							Integer jobID = from.job.jobID;
							Job fromJob = from.job;

							if (from.locationId == fromJob.fromColumn) {
								outflowOrigin.computeIfAbsent(jobID, k -> new ArrayList<>()).add(X.get(from).get(to));
							}
							if (from.locationId == fromJob.toColumn) {
								outflowDest.computeIfAbsent(jobID, k -> new ArrayList<>()).add(X.get(from).get(to));
							}

						}
					}
				}
			}

			// Origin: Create separate constraints for each job
			for (Entry<Integer, List<GRBVar>> entry : inflowOrigin.entrySet()) {
				Integer job = entry.getKey();
				List<GRBVar> uniqueInflowTerms = entry.getValue().stream().distinct().collect(Collectors.toList());
				List<GRBVar> uniqueOutflowTerms = outflowOrigin.getOrDefault(job, new ArrayList<>()).stream().distinct()
						.collect(Collectors.toList());

				GRBLinExpr inFlow = new GRBLinExpr();
				GRBLinExpr outFlow = new GRBLinExpr();

				for (GRBVar var : uniqueInflowTerms) {

					inFlow.addTerm(1, var);
				}

				for (GRBVar var : uniqueOutflowTerms) {

					outFlow.addTerm(1, var);
				}

				model.addConstr(inFlow, GRB.EQUAL, outFlow,
						"TaskFlowOrigin" + group.get(0).locationId + "_" + group.get(0).time + "_Job" + job);
				model.addConstr(inFlow, GRB.LESS_EQUAL, 1,
						"OriginInflow" + group.get(0).locationId + "_" + group.get(0).time + "_Job" + job);
				model.addConstr(outFlow, GRB.LESS_EQUAL, 1,
						"OriginOutlow" + group.get(0).locationId + "_" + group.get(0).time + "_Job" + job);

			}

			// Destination: Create separate constraints for each job
			for (Entry<Integer, List<GRBVar>> entry : inflowDest.entrySet()) {
				Integer job = entry.getKey();
				List<GRBVar> uniqueInflowTerms = entry.getValue().stream().distinct().collect(Collectors.toList());
				List<GRBVar> uniqueOutflowTerms = outflowDest.getOrDefault(job, new ArrayList<>()).stream().distinct()
						.collect(Collectors.toList());

				GRBLinExpr inFlow = new GRBLinExpr();
				GRBLinExpr outFlow = new GRBLinExpr();

				for (GRBVar var : uniqueInflowTerms) {
					inFlow.addTerm(1, var);
				}

				for (GRBVar var : uniqueOutflowTerms) {
					outFlow.addTerm(1, var);
				}

				model.addConstr(inFlow, GRB.EQUAL, outFlow,
						"TaskFlowDestination" + group.get(0).locationId + "_" + group.get(0).time + "_Job" + job);
				model.addConstr(inFlow, GRB.LESS_EQUAL, 2,
						"DestInflow" + group.get(0).locationId + "_" + group.get(0).time + "_Job" + job);
				model.addConstr(outFlow, GRB.LESS_EQUAL, 2,
						"DestOutlow" + group.get(0).locationId + "_" + group.get(0).time + "_Job" + job); 
			}
		}

	}

	public void setAssingmentCons() throws GRBException {
		// Eq 10f
		// task can be processed once
		for (Job myJob : problem.myJobs) {
			GRBLinExpr cons5 = new GRBLinExpr();
			Node[] fromNodes = network.network.get(myJob)[0];

			for (Node fromNode : fromNodes) {
				if (fromNode != null) {
					for (Node to : X.get(fromNode).keySet()) {
						if (!to.name.equals("GroundArc") && !to.name.equals("Push")) {
							GRBVar X_a = X.get(fromNode).get(to);
							cons5.addTerm(1, X_a);
						}
					}
				}
			}
			model.addConstr(cons5, GRB.LESS_EQUAL, 1.0, "Cons5_Job" + myJob.jobID);
		}
	}

	public void setConflictTaskCons() throws GRBException {
		// Eq 11 and Eq 14
		// check if job pairs conflict through trajectory
		for (Job j1 : problem.myJobs) {
			for (Job j2 : problem.myJobs) {
				if (!j1.equals(j2) && utils.checkCraneConflict(j1, j2, problem.timeInterval)) {
					for (Node fromNode1 : network.network.get(j1)[0]) {
						if (fromNode1 != null) {
							for (Node to1 : X.get(fromNode1).keySet()) { 
								for (Node fromNode2 : network.network.get(j2)[0]) {																			
									if (fromNode2 != null && fromNode2.time <= fromNode1.time) {										
										for (Node to2 : X.get(fromNode2).keySet()) {
											if( !(to2.time == fromNode1.time && to2.locationId == fromNode1.locationId)) {
												if (!to1.name.equals("GroundArc") && !to2.name.equals("GroundArc")) {
													if(fromNode1.time == 47 && to1.time == 63 && fromNode2.time == 45 && to2.time == 57 && fromNode1.locationId == 3 && fromNode2.locationId == 1) {
														int a = 5;
													}
													utils utils = new utils(problem);
													// Eq 14
													// Crane conflicts that occurred due discritization can be taken only if followed by sink
													if (fromNode2 != null && utils.checkConflicTime(fromNode1, fromNode2,
															to1, to2, network)) {
														if (utils.intervalConflict) {
															GRBLinExpr lhs = new GRBLinExpr();
															if (to2.time <= to1.time) {
																GRBVar X_a1 = X.get(fromNode1).get(to1);
																lhs.addTerm(1, X_a1); 
			
																GRBVar X_a2 = X.get(fromNode2).get(to2);
																
																ArrayList <Node> outgoingOfto2 = to2.getOutgoing(); 
																for(Node o : outgoingOfto2) {
																	if(o.name=="SinkNode") {
																		GRBVar X_a2_sink = X.get(to2).get(o);
																		lhs.addTerm(-1, X_a2_sink);
																	}
																}
																lhs.addTerm(1, X_a2);
																model.addConstr(lhs, GRB.LESS_EQUAL, 1, "ConflictCornerSinkJobs"
																		+ fromNode2.time + "_" + fromNode2.locationId);
															}
															else {
																GRBVar X_a1 = X.get(fromNode1).get(to1);
																lhs.addTerm(1, X_a1); 
			 
																GRBVar X_a2 = X.get(fromNode2).get(to2);
																
																ArrayList <Node> outgoingOfto1 = to1.getOutgoing(); 
																for(Node o : outgoingOfto1) {
																	if(o.name=="SinkNode") {
																		GRBVar X_a1_sink = X.get(to1).get(o);
																		lhs.addTerm(-1, X_a1_sink);
																	}
																}
																lhs.addTerm(1, X_a2);
																model.addConstr(lhs, GRB.LESS_EQUAL, 1, "ConflictCornerSinkJobs"
																		+ fromNode2.time + "_" + fromNode2.locationId);
															}
															
															
														}
														// Eq 11
														// Conflicting tasks can not be taken together
														else {
															GRBLinExpr lhs = new GRBLinExpr();
															
															GRBVar X_a1 = X.get(fromNode1).get(to1);
															lhs.addTerm(1, X_a1);
		
															GRBVar X_a2 = X.get(fromNode2).get(to2);
															lhs.addTerm(1, X_a2);
															model.addConstr(lhs, GRB.LESS_EQUAL, 1, "ConflictJobs"
																	+ fromNode2.time + "_" + fromNode2.locationId);
														}
														
													}
												}
											}
										}																				
									}
								}
							}
						}
					} 
				} 
			}
		}
	}
	
	public void setConflictTaskCornerCaseCons() throws GRBException {
		// Eq 13  
		// Location overlaps constraint, consecutive arc can be taken by the same crane
		for (Job j1 : problem.myJobs) {
			for (Job j2 : problem.myJobs) {
				if (!j1.equals(j2) && utils.checkCraneConflict(j1, j2, problem.timeInterval)) {
					for (Node fromNode1 : network.network.get(j1)[0]) {
						if (fromNode1 != null) {
							for (Node to1 : X.get(fromNode1).keySet()) {
								for (Node fromNode2 : network.network.get(j2)[0]) {																		
									if (fromNode2 != null && fromNode2.time <= fromNode1.time) {
										for (Node to2 : X.get(fromNode2).keySet()) {
											if(to2.time == fromNode1.time && to2.locationId == fromNode1.locationId) {
												if (!to1.name.equals("GroundArc") && !to2.name.equals("GroundArc")) {
													
													try {
														if (fromNode2 != null && utils.checkConflictCornerTime(fromNode1, fromNode2,
																to1, to2, problem.timeInterval)) {
															ArrayList<Node> outgoingNodes = to2.getOutgoing();
															
															GRBLinExpr lhs = new GRBLinExpr();
															GRBVar X_a1 = X.get(fromNode1).get(to1);
															lhs.addTerm(1, X_a1);

															GRBVar X_a2 = X.get(fromNode2).get(to2);
															lhs.addTerm(1, X_a2);
															
															for (Node outgoingNode : outgoingNodes) {
																if((outgoingNode.name == "TaskNode" && outgoingNode.job.jobID == fromNode1.job.jobID) || (outgoingNode.name == "SinkNode")) {
																											
																	GRBVar X_a = X.get(to2).get(outgoingNode);
																	lhs.addTerm(-1, X_a);

																}
																							            					
															}
															model.addConstr(lhs, GRB.LESS_EQUAL, 1, "ConflictJobsCornerCase"
																	+ fromNode2.time + "_" + fromNode2.locationId);
														}
													} catch (GRBException e) {									
														e.printStackTrace();
													}
													
												}
											}
										}	
									}																			
								}
							}
						}
					}
				}
			}
		}
	}
	

	public void setConflictTaskTravelCons() throws GRBException {
		// Eq 12
		// Conflicting travel arc and task arc can not be taken at the same time
		for (Job j1 : problem.myJobs) {
			for (Job j2 : problem.myJobs) {
				if (!j1.equals(j2)) {
					for (Node fromNode1 : network.network.get(j1)[0]) {
						if (fromNode1 != null) {
							for (Node to1 : X.get(fromNode1).keySet()) {
								for (Node fromNode2 : network.network.get(j2)[0]) {
									if (fromNode2 != null) {
										for (Node to2 : X.get(fromNode2).keySet()) {
											ArrayList<Node> outgoingsOfTo2 = to2.getOutgoing();
											for (Node outgoingTo2 : outgoingsOfTo2) {
												if (!to1.name.equals("GroundArc")
														&& !outgoingTo2.name.equals("GroundArc")
														&& !outgoingTo2.name.equals("Push")
														&& !outgoingTo2.name.equals("SinkNode")
														&& !fromNode1.job.equals(outgoingTo2.job)) {

													ArrayList<Integer> jobTrajectory1 = utils.createJobTrajectory(j1 , network.timeInterval);
													ArrayList<Integer> travelTrajectory2 = utils
															.createTravelTrajectory(to2.locationId,
																	outgoingTo2.locationId);
				
													if (utils.checkConflicTravelArcs(jobTrajectory1,
															travelTrajectory2)) {
														if(fromNode1.time == 9 && to1.time == 13 && to2.time == 12 && outgoingTo2.time == 14 && fromNode1.locationId == 7 && to1.locationId ==10 && to2.locationId == 11) {
															int a = 5;
														}
														if (utils.checkConflicTravelTime(fromNode1, to2, to1,
																outgoingTo2, problem.timeInterval)) {
															GRBLinExpr lhs = new GRBLinExpr();
															GRBVar X1 = X.get(fromNode1).get(to1);
															GRBVar X2 = X.get(to2).get(outgoingTo2);

															lhs.addTerm(1.0, X1);
															lhs.addTerm(1.0, X2);

															model.addConstr(lhs, GRB.LESS_EQUAL, 1.0,
																	"ConflictTravelTask_" + fromNode1.time + "_"
																			+ to1.time + "_" + to2.time + "_"
																			+ outgoingTo2.time);
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	public void setConflictTaskPushCons() throws GRBException {
		// Eq 12
		// conflicting push travel and task arc
		for (Job j1 : problem.myJobs) {
			for (Job j2 : problem.myJobs) {
				if (!j1.equals(j2)) {
					for (Node fromNode1 : network.network.get(j1)[0]) {
						if (fromNode1 != null) {
							for (Node to1 : X.get(fromNode1).keySet()) {
								for (Node fromNode2 : network.network.get(j2)[0]) {
									if (fromNode2 != null) {
										for (Node to2 : X.get(fromNode2).keySet()) {
											ArrayList<Node> outgoingsOfTo2 = to2.getOutgoing();
											for (Node outgoingTo2 : outgoingsOfTo2) {
												if (!to1.name.equals("GroundArc") && outgoingTo2.name.equals("Push")
														&& !fromNode1.job.equals(outgoingTo2.job)) {

													ArrayList<Integer> jobTrajectory1 = utils.createJobTrajectory(j1, network.timeInterval);
													ArrayList<Integer> travelTrajectory2 = utils
															.createTravelTrajectory(to2.locationId,
																	outgoingTo2.locationId);

													if (utils.checkConflicTravelArcs(jobTrajectory1,
															travelTrajectory2)) {
														if (utils.checkConflicTravelTime(fromNode1, to2, to1,
																outgoingTo2, problem.timeInterval)) {
															GRBLinExpr lhs = new GRBLinExpr();
															GRBVar X1 = X.get(fromNode1).get(to1);
															GRBVar X2 = X.get(to2).get(outgoingTo2);

															lhs.addTerm(1.0, X1);
															lhs.addTerm(1.0, X2);

															model.addConstr(lhs, GRB.LESS_EQUAL, 1.0,
																	"ConflictPushTask_" + fromNode1.time + "_"
																			+ to1.time + "_" + to2.time + "_"
																			+ outgoingTo2.time);
															model.addConstr(lhs, GRB.LESS_EQUAL, 1.0,
																	"ConflictPushTask_" + fromNode1.time + "_"
																			+ to1.time + "_" + to2.time + "_"
																			+ outgoingTo2.time);
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	public void setConflictTaskGroundCons() throws GRBException {
		// Eq 12
		// Conflicting a task arc and a ground arc can not be taken at the same time
		for (Job j1 : problem.myJobs) {
			for (Node fromNode2 : X.keySet()) {
				if (fromNode2 != null) {
					for (Node fromNode1 : network.network.get(j1)[0]) {
						if (fromNode1 != null) {
							for (Node to1 : X.get(fromNode1).keySet()) {
								for (Node to2 : X.get(fromNode2).keySet()) {
									if (!j1.equals(to2.job)) {
										if (!to1.name.equals("GroundArc") && to2.name.equals("GroundArc")
												&& !fromNode1.job.equals(to2.job)) {

											ArrayList<Integer> jobTrajectory1 = utils.createJobTrajectory(j1, network.timeInterval);
											ArrayList<Integer> travelTrajectory2 = utils
													.createTravelTrajectory(fromNode2.locationId, to2.locationId);

											if (utils.checkConflicTravelArcs(jobTrajectory1, travelTrajectory2)) {
												if(fromNode1.time == 41 && to1.time == 57 && fromNode2.time == 46 && to2.time == 47 && fromNode1.locationId == 3 && fromNode2.locationId == 5) {
													int a = 5;
												}
												if (utils.checkConflicGroundTime(fromNode1, fromNode2, to1, to2,
														problem.timeInterval)) {
													GRBLinExpr lhs = new GRBLinExpr();
													GRBVar X1 = X.get(fromNode1).get(to1);
													GRBVar X2 = X.get(fromNode2).get(to2);

													lhs.addTerm(1.0, X1);
													lhs.addTerm(1.0, X2);

													model.addConstr(lhs, GRB.LESS_EQUAL, 1.0,
															"ConflictGroundTask_" + fromNode1.time + "_" + to1.time
																	+ "_" + fromNode2.time + "_" + to2.time);
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	public void setConflictTaskSourceCons() throws GRBException {
		// Eq 12
		// Conflicting a source arc and a task arc can not be taken at the same time
		for (Job j1 : problem.myJobs) {
			for (Node fromNode2 : X.keySet()) {
				if (fromNode2 != null
						&& (fromNode2.name.equals("SourceNode1") || fromNode2.name.equals("SourceNode2"))) {
					for (Node fromNode1 : network.network.get(j1)[0]) {
						if (fromNode1 != null) {
							for (Node to1 : X.get(fromNode1).keySet()) {
								for (Node to2 : X.get(fromNode2).keySet()) {
									if (!j1.equals(to2.job)) {
										if (!to1.name.equals("GroundArc") && !fromNode1.job.equals(to2.job)) {

											ArrayList<Integer> jobTrajectory1 = utils.createJobTrajectory(j1, network.timeInterval);
											ArrayList<Integer> travelTrajectory2 = utils
													.createTravelTrajectory(fromNode2.locationId, to2.locationId);

											if (utils.checkConflicTravelArcs(jobTrajectory1, travelTrajectory2)) {
												if (utils.checkConflicSourceTime(fromNode1, fromNode2, to1, to2,
														problem.timeInterval)) {
													GRBLinExpr lhs = new GRBLinExpr();
													GRBVar X1 = X.get(fromNode1).get(to1);
													GRBVar X2 = X.get(fromNode2).get(to2);

													lhs.addTerm(1.0, X1);
													lhs.addTerm(1.0, X2);

													model.addConstr(lhs, GRB.LESS_EQUAL, 1.0,
															"ConflictSourceTask_" + fromNode1.time + "_" + to1.time
																	+ "_" + fromNode2.time + "_" + to2.time);
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	public void setPushAvodanceCons() throws GRBException {
		// Eq 15
		// Avoid taking irrelavant push arc
		for (Job j1 : problem.myJobs) {
			for (Job j2 : problem.myJobs) {
				if (!j1.equals(j2)) {
					for (Node fromNode1 : network.network.get(j1)[0]) {
						if (fromNode1 != null) {
							for (Node to1 : X.get(fromNode1).keySet()) {
								GRBLinExpr lhs = new GRBLinExpr();
								for (Node fromNode2 : network.network.get(j2)[0]) {
									if (fromNode2 != null) {
										for (Node to2 : X.get(fromNode2).keySet()) {
											ArrayList<Node> outgoingsOfTo2 = to2.getOutgoing();
											for (Node outgoingTo2 : outgoingsOfTo2) {
												if (!to1.name.equals("GroundArc") && outgoingTo2.name.equals("Push")
														&& !fromNode1.job.equals(outgoingTo2.job)) {
													if (outgoingTo2.time == to1.time
															&& outgoingTo2.locationId == to1.locationId) {
														GRBVar X1 = X.get(fromNode1).get(to1);
														GRBVar X2 = X.get(to2).get(outgoingTo2);

														lhs.addTerm(1.0, X2);
														model.addConstr(lhs, GRB.LESS_EQUAL, X1,
																"PushAvodance_" + fromNode1.time + "_" + to1.time + "_"
																		+ to2.time + "_" + outgoingTo2.time);
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	public void setConsecutivePushCons() throws GRBException {
		// Eq 16
		// Consecutive push arcs can not be taken same time
		for (Job j1 : problem.myJobs) {
			for (Job j2 : problem.myJobs) {
				if (!j1.equals(j2)) {
					for (Node fromNode1 : network.network.get(j1)[0]) {
						if (fromNode1 != null) {
							for (Node to1 : X.get(fromNode1).keySet()) {
								ArrayList<Node> outgoingsOfTo1 = to1.getOutgoing();
								for (Node fromNode2 : network.network.get(j2)[0]) {
									if (fromNode2 != null) {
										for (Node to2 : X.get(fromNode2).keySet()) {
											ArrayList<Node> outgoingsOfTo2 = to2.getOutgoing();
											for (Node outgoingTo2 : outgoingsOfTo2) {
												for (Node outgoingTo1 : outgoingsOfTo1) {
													if (outgoingTo1.name.equals("Push")
															&& outgoingTo2.name.equals("Push")
															&& !fromNode1.job.equals(outgoingTo2.job)
															&& to1.time < to2.time) {
														if (outgoingTo1.time == to2.time
																&& outgoingTo1.locationId == to2.locationId) {
															GRBLinExpr lhs = new GRBLinExpr();
															GRBVar X1 = X.get(to1).get(outgoingTo1);
															GRBVar X2 = X.get(to2).get(outgoingTo2);

															lhs.addTerm(1.0, X1);
															lhs.addTerm(1.0, X2);

															model.addConstr(lhs, GRB.LESS_EQUAL, 1.0,
																	"ConsecutivePush_" + to1.time + "_"
																			+ outgoingTo1.time + "_" + to2.time + "_"
																			+ outgoingTo2.time);
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
}
