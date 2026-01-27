package tsnSolution;

import java.util.HashMap;
import java.util.logging.Logger;

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;

public class Objective {
	public HashMap<Node, HashMap<Node, GRBVar>> X;
	public GRBModel model;
	public HashMap<Node, HashMap<Node, Double>> reward;

	public Objective(GRBModel model, HashMap<Node, HashMap<Node, GRBVar>> X,
			HashMap<Node, HashMap<Node, Double>> reward) throws GRBException {
		this.X = X;
		this.model = model;
		this.reward = reward;

		setObjective();
	}

	public void setObjective() throws GRBException {
		// Eq 10a
		// Objective function
		GRBLinExpr obj = new GRBLinExpr();

		for (Node from : X.keySet()) {
			for (Node to : X.get(from).keySet()) {
				GRBVar X_a = X.get(from).get(to);
				Double nodeReward = reward.get(from).get(to);

				obj.addTerm(nodeReward, X_a);
			}
		}
		model.setObjective(obj, GRB.MAXIMIZE);
	}
}
