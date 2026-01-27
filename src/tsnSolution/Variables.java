package tsnSolution;

import java.util.ArrayList;
import java.util.HashMap;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;

import data.Job;
import data.Problem;

public class Variables {

	public Problem problem;
	public Network network;
	public GRBModel model;
	public HashMap<Node, HashMap<Node, GRBVar>> X;
	public HashMap<Node, HashMap<Node, Double>> param;

	public Variables(Problem problem, Network network, GRBModel model) throws GRBException {
		this.problem = problem;
		this.network = network;
		this.model = model;

	}

	public HashMap<Node, HashMap<Node, GRBVar>> setVariables() throws GRBException {

		// Create variables
		HashMap<Node, HashMap<Node, GRBVar>> X = new HashMap<Node, HashMap<Node, GRBVar>>(); // from-to , variable on
																								// arc by indexing node,
 
		// source var
		Node[] fromSources = { network.source1, network.source2 };
		for (Node from : fromSources) {
			HashMap<Node, GRBVar> thisHash = new HashMap<Node, GRBVar>();
			ArrayList<Node> outgoings = from.getOutgoing();
			for (Node to : outgoings) {
				var thisVar = model.addVar(0, 1, 0, GRB.BINARY, "X_" + from.locationId + "_" + from.time + "_"
						+ to.locationId + "_" + to.time + "_" + from.name + to.job.jobID);
				thisHash.put(to, thisVar);
			}
			X.put(from, thisHash);

		}

		// task var
		for (Job j : problem.myJobs) {
			Node[] fromNodes = network.network.get(j)[0];
			for (int t = 0; t < fromNodes.length; t++) {
				var from = fromNodes[t];
				HashMap<Node, GRBVar> thisHash;
				if (fromNodes != null && fromNodes[t] != null) {
					thisHash = new HashMap<Node, GRBVar>();
					ArrayList<Node> outgoings = from.getOutgoing();
					for (Node to : outgoings) {
						var thisVar = model.addVar(0, 1, 0, GRB.BINARY, "X_" + from.locationId + "_" + from.time + "_"
								+ to.locationId + "_" + to.time + "_" + from.name + to.name + j.jobID);
						thisHash.put(to, thisVar);

					}
					X.put(from, thisHash);
				}
			}
		}

		// travel var
		for (Job j : problem.myJobs) {
			Node[] fromNodes = network.network.get(j)[0];
			for (int t = 0; t < fromNodes.length; t++) {
				var from = fromNodes[t];
				HashMap<Node, GRBVar> thisHash;
				if (fromNodes != null && fromNodes[t] != null) {
					ArrayList<Node> outgoings = from.getOutgoing();
					for (Node to : outgoings) {
						thisHash = new HashMap<Node, GRBVar>();
						ArrayList<Node> outgoingsOfOutgoings = to.getOutgoing();
						for (Node toOutgoing : outgoingsOfOutgoings) {
							if (toOutgoing.job != null) {
								var thisVar = model.addVar(0, 1, 0, GRB.BINARY,
										"X_" + to.locationId + "_" + to.time + "_" + toOutgoing.locationId + "_"
												+ toOutgoing.time + "_" + to.name + j.jobID + toOutgoing.name + "_"
												+ toOutgoing.job.jobID);
								thisHash.put(toOutgoing, thisVar);
							}

							else {
								var thisVar = model.addVar(0, 1, 0, GRB.BINARY,
										"X_" + to.locationId + "_" + to.time + "_" + toOutgoing.locationId + "_"
												+ toOutgoing.time + "_" + to.name + j.jobID + toOutgoing.name);
								thisHash.put(toOutgoing, thisVar);
							}
						}
						X.put(to, thisHash);
					}

				}
			}
		}
		return X;
	}

	public HashMap<Node, HashMap<Node, Double>> setParameters() {
		// reward
		// reward of task is zero following code should be deleted

		HashMap<Node, HashMap<Node, Double>> reward = new HashMap<Node, HashMap<Node, Double>>();
		for (Job j : problem.myJobs) {
			Node[] fromNodes = network.network.get(j)[0];
			for (int t = 0; t < fromNodes.length; t++) {
				var from = fromNodes[t];
				HashMap<Node, Double> thisHash;
				if (fromNodes != null && fromNodes[t] != null) {
					thisHash = new HashMap<Node, Double>();
					ArrayList<Node> outgoings = from.getOutgoing();
					for (Node to : outgoings) {
						var thisVar = to.getReward();
						thisHash.put(to, thisVar);
					}
					reward.put(from, thisHash);
				}
			}
		}
		Node[] fromSources = { network.source1, network.source2 };
		// reward of source
		for (Node from : fromSources) {
			HashMap<Node, Double> thisHash = new HashMap<Node, Double>();
			ArrayList<Node> outgoings = from.getOutgoing();
			for (Node to : outgoings) {
				var thisVar = to.getReward();
				thisHash.put(to, thisVar);
			}
			reward.put(from, thisHash);
		}
		// reward of travel arcs

		for (Job j : problem.myJobs) {
			Node[] fromNodes = network.network.get(j)[0];
			for (int t = 0; t < fromNodes.length; t++) {
				var from = fromNodes[t];
				HashMap<Node, Double> thisHash;
				if (fromNodes != null && fromNodes[t] != null) {
					ArrayList<Node> outgoings = from.getOutgoing();
					for (Node to : outgoings) {
						thisHash = new HashMap<Node, Double>();
						ArrayList<Node> outgoingsOfOutgoings = to.getOutgoing();
						for (Node toOutgoing : outgoingsOfOutgoings) {
							var thisVar = toOutgoing.getReward();
							thisHash.put(toOutgoing, thisVar);

						}
						reward.put(to, thisHash);
					}

				}
			}
		}
		return reward;

	}

}
