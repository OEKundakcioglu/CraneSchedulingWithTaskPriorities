package DP;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Stopwatch;

import data.Job;
import data.Problem;
import com.gurobi.gurobi.*;

public class GurobiSolution {

	public Problem problem;
	public int gurobiReward;
	public double elapsedTimeGurobi;
	public double totalReward;
	public double[] tasksCentroid;
	public Snapshot snap;
	public String estimations;

	public GurobiSolution(Problem problem, Snapshot snap) {

		this.problem = problem;
		this.snap = snap;

		RunGurobi(snap);

	}

	private void RunGurobi(Snapshot snap) {

		ArrayList<Job> setOfCompletedJobs = new ArrayList<Job>();
		CraneStatus[] craneStatus = new CraneStatus[this.problem.crane.length];
		for (int i = 0; i < this.problem.crane.length; i++) {
			craneStatus[i] = new CraneStatus();
			craneStatus[i].crane = this.problem.crane[i];
			craneStatus[i].idle = true;
			craneStatus[i].column = this.problem.crane[i].initColumn;
			craneStatus[i].row = this.problem.crane[i].initRow;
			craneStatus[i].completeByTime = 0;
			craneStatus[i].leg1 = 0;
			craneStatus[i].leg2 = 0;
			craneStatus[i].completion1 = 0;
			craneStatus[i].job = null;
			craneStatus[i].startTime = 0;
			craneStatus[i].craneTrajectory = new ArrayList<Integer>();
			craneStatus[i].jobTrajectory = new ArrayList<Integer>();
		}

		Stopwatch stopWatch = Stopwatch.createStarted();
		tasksCentroid = centroidOfTasks(snap);

		CompletableFuture<Double> task = CompletableFuture.supplyAsync(() -> {
			this.totalReward = solve(snap, tasksCentroid);
			return this.totalReward;
		});

		try {
			boolean isCompletedSuccessfully = task.get(this.problem.timeLimit, TimeUnit.SECONDS) != null;
			if (isCompletedSuccessfully) {
				stopWatch.stop();
				elapsedTimeGurobi = stopWatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0;
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			stopWatch.stop();
			System.out.println(e);
			elapsedTimeGurobi = stopWatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0;
		}
		elapsedTimeGurobi = stopWatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0;
	}

	private double solve(Snapshot snap, double[] centroid) {
		try {

			/*
			 * PARAMETERS
			 * c: set of cranes
			 * t: set of jobs
			 * l: set of locations
			 * a_tl: amount of time task t requires at location l
			 * r_t: reward of completing job t
			 * p: planning horizon
			 * 
			 * VARİABLES
			 * X_ct: binary 1 if crane c is assigned to task j
			 * Y_cl: estimated idle time for crane c due to conflict that occurs at location
			 * l
			 */

			// Create empty environment, set options, and start
			// Create a log file to redirect the solver output

			double timeLeft = problem.numberOfTimePoints - snap.currentTime;

			GRBEnv env = new GRBEnv(true);
			env.start();

			// Create empty model
			GRBModel model = new GRBModel(env);

			// Create variables
			GRBVar[] Xcrane1 = new GRBVar[snap.getSetOfNonCompletedJobs().size()];
			GRBVar[] Xcrane2 = new GRBVar[snap.getSetOfNonCompletedJobs().size()];
			GRBVar[] Ycrane1 = new GRBVar[problem.numberOfColumns];
			GRBVar[] Ycrane2 = new GRBVar[problem.numberOfColumns];
			// GRBVar[] ZCrane = new GRBVar[problem.crane.length];
			GRBVar[] Ocrane1 = new GRBVar[problem.numberOfColumns];
			GRBVar[] Ocrane2 = new GRBVar[problem.numberOfColumns];

			double[] Z = new double[problem.crane.length];
			for (int i = 0; i < problem.crane.length; i++) {
				double lowerBound = estimateTtFromInit(snap, snap.craneStatus[i]);
				double craneTimeLeft = (this.problem.numberOfTimePoints - snap.craneStatus[i].completeByTime);

				if (craneTimeLeft < lowerBound) {
					Z[i] = craneTimeLeft;

				} else {
					if (timeLeft > lowerBound) {
						Z[i] = lowerBound;

					} else {
						Z[i] = craneTimeLeft;

					}

				}
			}


			for (int i = 0; i < snap.getSetOfNonCompletedJobs().size(); i++) {

				Xcrane1[i] = model.addVar(0, 1, 0, GRB.BINARY, "Xc1_" + i);
				Xcrane2[i] = model.addVar(0, 1, 0, GRB.BINARY, "Xc2_" + i);
			}

			// Conflict variable boundary fix
			for (int i = 0; i < problem.numberOfColumns; i++) {

				Ycrane1[i] = model.addVar(0, timeLeft, 0, GRB.CONTINUOUS, "Yc1_" + i);
			}
			for (int i = 0; i < problem.numberOfColumns; i++) {

				Ycrane2[i] = model.addVar(0, timeLeft, 0, GRB.CONTINUOUS, "Yc2_" + i);
			}

			for (int i = 0; i < problem.numberOfColumns; i++) {
				Ocrane1[i] = model.addVar(0, 1, 0, GRB.BINARY, "Oc1_" + i);
				Ocrane2[i] = model.addVar(0, 1, 0, GRB.BINARY, "Oc2_" + i);
			}

			GRBLinExpr obj = new GRBLinExpr();

			for (int i = 0; i < snap.getSetOfNonCompletedJobs().size(); i++) {
				obj.addTerm((int) Math.floor(snap.getSetOfNonCompletedJobs().get(i).prio), Xcrane1[i]);
				obj.addTerm((int) Math.floor(snap.getSetOfNonCompletedJobs().get(i).prio), Xcrane2[i]);
	
			}
			model.setObjective(obj, GRB.MAXIMIZE);

			for (int i = 0; i < snap.getSetOfNonCompletedJobs().size(); i++) {
				GRBLinExpr cons1 = new GRBLinExpr();
				cons1.addTerm(1, Xcrane1[i]);
				cons1.addTerm(1, Xcrane2[i]);
				model.addConstr(cons1, GRB.LESS_EQUAL, 1, "cons1_" + i);
			}

			// crane 1
			GRBLinExpr cons2c1 = new GRBLinExpr();
			for (int l = 0; l < problem.numberOfColumns; l++) {
				for (int t = 0; t < snap.getSetOfNonCompletedJobs().size(); t++) {
					if (snap.getSetOfNonCompletedJobs().get(t).estimatedDurations.containsKey(l)) {
						cons2c1.addTerm(snap.getSetOfNonCompletedJobs().get(t).estimatedDurations.get(l), Xcrane1[t]); // TODO
					}
				}
			}

			double[] preTtCrane1 = estimatedTtRatioPreCrane1(snap);
			double[] postTtCrane1 = estimatedTtRatioPostCrane1(snap);
			for (int t = 0; t < snap.getSetOfNonCompletedJobs().size(); t++) {
				cons2c1.addTerm(preTtCrane1[t], Xcrane1[t]);
				cons2c1.addTerm(postTtCrane1[t], Xcrane1[t]);
			}
			for (int l = 0; l < problem.numberOfColumns; l++) {
				if (Ycrane1[l] != null) {
					cons2c1.addTerm(1, Ycrane1[l]);
				}

			}
			// estimated travel time for crane 1

			// compute total time left for crane 1 by multiply estimated travel time ratio

			double timeLeftc1 = (this.problem.numberOfTimePoints - snap.craneStatus[0].completeByTime);
			timeLeftc1 = timeLeftc1 - Z[0];


			model.addConstr(cons2c1, GRB.LESS_EQUAL, timeLeftc1, "cons2crane1");
			// crane 2
			GRBLinExpr cons2c2 = new GRBLinExpr();
			for (int t = 0; t < snap.getSetOfNonCompletedJobs().size(); t++) {
				for (int l = 0; l < problem.numberOfColumns; l++) {
					if (snap.getSetOfNonCompletedJobs().get(t).estimatedDurations.containsKey(l)) {
						cons2c2.addTerm(snap.getSetOfNonCompletedJobs().get(t).estimatedDurations.get(l), Xcrane2[t]);
					}
				}
			}

			double[] preTtCrane2 = estimatedTtRatioPreCrane2(snap);
			double[] postTtCrane2 = estimatedTtRatioPostCrane2(snap);
			for (int t = 0; t < snap.getSetOfNonCompletedJobs().size(); t++) {
				cons2c2.addTerm(preTtCrane2[t], Xcrane2[t]);
				cons2c2.addTerm(postTtCrane2[t], Xcrane2[t]);
			}

			for (int l = 0; l < problem.numberOfColumns; l++) {
				if (Ycrane2[l] != null) {
					cons2c2.addTerm(1, Ycrane2[l]);
				}

			}
			// estimated travel time for crane 2
			// compute total time left for crane 2 by multiply estimated travel time ratio

			double timeLeftc2 = this.problem.numberOfTimePoints - snap.craneStatus[1].completeByTime;
			timeLeftc2 = timeLeftc2 - Z[1];


			model.addConstr(cons2c2, GRB.LESS_EQUAL, timeLeftc2, "cons2crane2");

			Double M = calculateLongestDuration();

			// Theta left crane
			// First Constraint
			for (int l = 0; l < this.problem.numberOfColumns; l++) {
				GRBLinExpr cons3Left = new GRBLinExpr(); // for each location l we create
				for (int t = 0; t < snap.getSetOfNonCompletedJobs().size(); t++) {
					if (snap.getSetOfNonCompletedJobs().get(t).estimatedDurations.containsKey(l)) {
						cons3Left.addTerm(snap.getSetOfNonCompletedJobs().get(t).estimatedDurations.get(l), Xcrane1[t]);
					}
				}

				cons3Left.addTerm(-M, Ocrane1[l]);
				model.addConstr(cons3Left, GRB.LESS_EQUAL, 0, "cons3_left" + l);
			}

			for (int l = 0; l < this.problem.numberOfColumns; l++) {
				GRBLinExpr cons3LeftBoundry = new GRBLinExpr();
				for (int t = 0; t < snap.getSetOfNonCompletedJobs().size(); t++) {
					if (snap.getSetOfNonCompletedJobs().get(t).estimatedDurations.containsKey(l)) {
						cons3LeftBoundry.addTerm(-snap.getSetOfNonCompletedJobs().get(t).estimatedDurations.get(l),
								Xcrane1[t]);
					}
				}

				cons3LeftBoundry.addTerm(1, Ocrane1[l]);
				model.addConstr(cons3LeftBoundry, GRB.LESS_EQUAL, 0, "cons3_left_boundry" + l);
			}

			// Second Constraint
			for (int l = 0; l < this.problem.numberOfColumns; l++) {
				GRBLinExpr cons4Left = new GRBLinExpr();
				for (int t = 0; t < snap.getSetOfNonCompletedJobs().size(); t++) {
					if (snap.getSetOfNonCompletedJobs().get(t).estimatedDurations.containsKey(l)) {
						cons4Left.addTerm(snap.getSetOfNonCompletedJobs().get(t).estimatedDurations.get(l), Xcrane1[t]);
					}
				}
				cons4Left.addTerm(M, Ocrane1[l]);
				cons4Left.addTerm(M, Ocrane2[l]);
				cons4Left.addTerm(-1, Ycrane1[l]);

				if ((Ycrane1[l] != null)) {
					model.addConstr(cons4Left, GRB.LESS_EQUAL, 2 * M, "cons4_left" + l);
				}

			}

			// Third constraint
			for (int l = 0; l < this.problem.numberOfColumns; l++) {
				GRBLinExpr cons5Left1 = new GRBLinExpr();
				cons5Left1.addTerm(1, Ycrane1[l]);
				cons5Left1.addTerm(-M, Ocrane1[l]);
				model.addConstr(cons5Left1, GRB.LESS_EQUAL, 0, "cons5Left1" + l);
			}
			for (int l = 0; l < this.problem.numberOfColumns; l++) {
				GRBLinExpr cons5Left2 = new GRBLinExpr();
				cons5Left2.addTerm(1, Ycrane1[l]);
				cons5Left2.addTerm(-M, Ocrane2[l]);
				model.addConstr(cons5Left2, GRB.LESS_EQUAL, 0, "cons5Left2" + l);
			}

			// Right left crane
			for (int l = 0; l < this.problem.numberOfColumns; l++) {
				GRBLinExpr cons3Right = new GRBLinExpr(); // for each location l we create
				for (int t = 0; t < snap.getSetOfNonCompletedJobs().size(); t++) {
					if (snap.getSetOfNonCompletedJobs().get(t).estimatedDurations.containsKey(l)) {
						cons3Right.addTerm(snap.getSetOfNonCompletedJobs().get(t).estimatedDurations.get(l),
								Xcrane2[t]);
					}
				}
				cons3Right.addTerm(-M, Ocrane2[l]);
				model.addConstr(cons3Right, GRB.LESS_EQUAL, 0, "cons3_right" + l);
			}

			for (int l = 0; l < this.problem.numberOfColumns; l++) {
				GRBLinExpr cons3RightBoundry = new GRBLinExpr();
				for (int t = 0; t < snap.getSetOfNonCompletedJobs().size(); t++) {
					if (snap.getSetOfNonCompletedJobs().get(t).estimatedDurations.containsKey(l)) {
						cons3RightBoundry.addTerm(-snap.getSetOfNonCompletedJobs().get(t).estimatedDurations.get(l),
								Xcrane2[t]);
					}
				}

				cons3RightBoundry.addTerm(1, Ocrane2[l]);
				model.addConstr(cons3RightBoundry, GRB.LESS_EQUAL, 0, "cons3_right_boundry" + l);
			}

			// Second Constraint
			for (int l = 0; l < this.problem.numberOfColumns; l++) {
				GRBLinExpr cons4Right = new GRBLinExpr();
				for (int t = 0; t < snap.getSetOfNonCompletedJobs().size(); t++) {
					if (snap.getSetOfNonCompletedJobs().get(t).estimatedDurations.containsKey(l)) {
						cons4Right.addTerm(snap.getSetOfNonCompletedJobs().get(t).estimatedDurations.get(l),
								Xcrane2[t]);
					}
				}
				cons4Right.addTerm(M, Ocrane1[l]);
				cons4Right.addTerm(M, Ocrane2[l]);
				cons4Right.addTerm(-1, Ycrane2[l]);
				if ((Ycrane2[l] != null)) {
					model.addConstr(cons4Right, GRB.LESS_EQUAL, 2 * M, "cons4_right" + l);
				}

			}
			// Third constraint
			for (int l = 0; l < this.problem.numberOfColumns; l++) {
				GRBLinExpr cons5Right1 = new GRBLinExpr();
				cons5Right1.addTerm(1, Ycrane2[l]);
				cons5Right1.addTerm(-M, Ocrane2[l]);
				model.addConstr(cons5Right1, GRB.LESS_EQUAL, 0, "cons5Right1" + l);
			}
			for (int l = 0; l < this.problem.numberOfColumns; l++) {
				GRBLinExpr cons5Right2 = new GRBLinExpr();
				cons5Right2.addTerm(1, Ycrane2[l]);
				cons5Right2.addTerm(-M, Ocrane1[l]);
				model.addConstr(cons5Right2, GRB.LESS_EQUAL, 0, "cons5Right2" + l);
			}

			model.optimize();

			if (model.get(GRB.IntAttr.Status) == GRB.INFEASIBLE) {
				model.computeIIS();
				model.write("model.ilp");
				// Get the IIS (Irreducible Inconsistent Subsystem)
				GRBConstr[] conflictingConstraints = model.getConstrs();
				for (GRBConstr constr : conflictingConstraints) {
					String constrName = constr.get(GRB.StringAttr.ConstrName);
					if (model.getConstrByName(constrName).get(GRB.IntAttr.IISConstr) != 0) {
						System.out.println("Conflicting constraint: " + constrName);
					}
				}

				GRBVar[] variables = model.getVars();
				for (GRBVar variable : variables) {
					if (variable.get(GRB.IntAttr.VBasis) == GRB.BASIC &&
							variable.get(GRB.IntAttr.CBasis) == GRB.BASIC) {
						String variableName = variable.get(GRB.StringAttr.VarName);
						System.out.println("Conflicting bounds for variable: " + variableName);
					}
				}
			}

			Map<GRBVar, List<Integer>> XCrane1Durations = new HashMap<>();

			for (int t = 0; t < snap.getSetOfNonCompletedJobs().size(); t++) {

				for (int l = 0; l < this.problem.numberOfColumns; l++) {
					if (snap.getSetOfNonCompletedJobs().get(t).estimatedDurations.containsKey(l)) {
						if (XCrane1Durations.containsKey(Xcrane1[t])) {
							double count = snap.getSetOfNonCompletedJobs().get(t).estimatedDurations.get(l);
							List<Integer> keyList = XCrane1Durations.get(Xcrane1[t]);
							for (int i = 0; i < count; i++) {
								keyList.add(l);
							}
						} else {
							double count = snap.getSetOfNonCompletedJobs().get(t).estimatedDurations.get(l);
							List<Integer> keyList = new ArrayList<>();
							for (int i = 0; i < count; i++) {
								keyList.add(l);
							}
							XCrane1Durations.put(Xcrane1[t], keyList);
						}
					}
				}
			}

			Map<GRBVar, List<Integer>> XCrane2Durations = new HashMap<>();

			for (int t = 0; t < snap.getSetOfNonCompletedJobs().size(); t++) {

				for (int l = 0; l < this.problem.numberOfColumns; l++) {
					if (snap.getSetOfNonCompletedJobs().get(t).estimatedDurations.containsKey(l)) {
						if (XCrane2Durations.containsKey(Xcrane2[t])) {
							double count = snap.getSetOfNonCompletedJobs().get(t).estimatedDurations.get(l);
							List<Integer> keyList = XCrane2Durations.get(Xcrane2[t]);
							for (int i = 0; i < count; i++) {
								keyList.add(l);
							}
						} else {
							double count = snap.getSetOfNonCompletedJobs().get(t).estimatedDurations.get(l);
							List<Integer> keyList = new ArrayList<>();
							for (int i = 0; i < count; i++) {
								keyList.add(l);
							}
							XCrane2Durations.put(Xcrane2[t], keyList);
						}
					}
				}
			}
		
			
			String key = "";
			this.estimations = key + Z[0] + " " + Z[1];
			double objective = model.get(GRB.DoubleAttr.ObjVal);

			// Dispose of model and environment

			model.dispose();
			env.dispose();

			return objective;

		}

		catch (GRBException e)

		{
			System.out.println("Error code: " + e.getErrorCode());
			System.out.println(e.getMessage());
			e.printStackTrace();
			return 0;

		}

	}

	private Map<Integer, Double> calculateAllEstimatedDurations(Snapshot snap) {
		Map<Integer, Double> allEstimatedDurations = new HashMap<>();

		for (Job j : snap.getSetOfNonCompletedJobs()) {
			for (Map.Entry<Integer, Double> e : j.estimatedDurations.entrySet()) {

				if (allEstimatedDurations.containsKey(e.getKey())) {
					Double newValue = allEstimatedDurations.get(e.getKey()) + e.getValue();
					allEstimatedDurations.put(e.getKey(), newValue);
				} else {
					allEstimatedDurations.put(e.getKey(), e.getValue());
				}
			}
		}
		return allEstimatedDurations;

	}

	private double[] centroidOfTasks(Snapshot snap) {
		Map<Integer, Double> allEstimatedDurations = calculateAllEstimatedDurations(snap);

		double totalWeight = 0.0;
		double weightedSum = 0.0;

		for (Map.Entry<Integer, Double> entry : allEstimatedDurations.entrySet()) {
			int location = entry.getKey();
			double duration = entry.getValue();

			totalWeight += duration;
			weightedSum += duration * location;
		}
		double centroid = weightedSum / totalWeight;

		// Calculate the two centroids by dividing the tasks
		double[] centroids = new double[2];
		centroids[0] = calculateCentroid(allEstimatedDurations, 0, centroid);
		centroids[1] = calculateCentroid(allEstimatedDurations, centroid, problem.numberOfColumns);

		return centroids;
	}


	private double estimateTtFromInit(Snapshot snap, CraneStatus cc) {
		double estimatedTtFromInit = 0;
		// double initJobCentroid = initJobLocCalc(snap,cc);
		double[] taskCentroids = centroidOfTasks(snap);
		double[] estimatedInitRatio = estimatedTtRatioForInit(taskCentroids);

		// at the beginning crane not assigned any job
		if (cc.job != null) {
			if (cc.crane.id == 1) {
				int craneLoc = cc.job.toColumn;
				estimatedTtFromInit = Math.abs(craneLoc - (taskCentroids[0] * estimatedInitRatio[0]))
						/ problem.speedInColumnsPerTimeUnit;
			}

			else {
				int craneLoc = cc.job.toColumn;
				estimatedTtFromInit = Math.abs(craneLoc - (taskCentroids[1] * estimatedInitRatio[1]))
						/ problem.speedInColumnsPerTimeUnit;
			}
		} else {
			int craneLoc = cc.crane.initColumn;

			if (cc.crane.id == 1) {
				estimatedTtFromInit = Math.abs(craneLoc - (taskCentroids[0] * estimatedInitRatio[0]))
						/ problem.speedInColumnsPerTimeUnit;

			} else {
				estimatedTtFromInit = Math.abs(craneLoc - (taskCentroids[1] * estimatedInitRatio[1]))
						/ problem.speedInColumnsPerTimeUnit;
			}
		}
		return estimatedTtFromInit;
	}

	// fraction of initial locations for candidate tasks on the reverse side of
	// centroid
	private double[] estimatedTtRatioForInit(double[] centroids) {
		double leftCentroid = centroids[0];
		double rightCentroid = centroids[1];

		double totalWeightLeft = 0.0;
		double totalWeightRight = 0.0;

		double totalTasksLeft = 0.0;
		double totalTasksRight = 0.0;

		for (Job j1 : snap.getSetOfNonCompletedJobs()) {

			if (((j1.fromColumn < leftCentroid) && (leftCentroid < snap.craneStatus[0].column))
					|| ((j1.fromColumn > leftCentroid) && (snap.craneStatus[0].column < leftCentroid))) {
				totalWeightLeft++;
			}
			totalTasksLeft++;
			if (((j1.fromColumn < rightCentroid) && (rightCentroid < snap.craneStatus[1].column))
					|| ((j1.fromColumn > rightCentroid) && (snap.craneStatus[1].column < rightCentroid))) {
				totalWeightRight++;
			}
			totalTasksRight++;
		}

		double[] ratios = new double[2];
		ratios[0] = totalWeightLeft / totalTasksLeft; // Ratio for left centroid
		ratios[1] = totalWeightRight / totalTasksRight; // Ratio for right centroid

		return ratios;
	}


	private double calculateCentroid(Map<Integer, Double> allEstimatedDurations, double start, double end) {
		double totalWeight = 0.0;
		double weightedSum = 0.0;

		for (Map.Entry<Integer, Double> entry : allEstimatedDurations.entrySet()) {
			int location = entry.getKey();
			double duration = entry.getValue();

			if (location >= start && location < end) {
				totalWeight += duration;
				weightedSum += duration * location;
			}
		}

		return weightedSum / totalWeight;
	}

	private double[] estimatedTtRatioPreCrane1(Snapshot snap) {
		double[] taskCentroids = centroidOfTasks(snap);
		double[] pVisitCentroidPRE = new double[snap.getSetOfNonCompletedJobs().size()];
		double[] preTtCrane1 = new double[snap.getSetOfNonCompletedJobs().size()];

		for (int i = 0; i < snap.getSetOfNonCompletedJobs().size(); i++) {
			int jobsOnOtherSide = 0;
			int totalJobs = 0;
			Job j1 = snap.getSetOfNonCompletedJobs().get(i);
			for (Job j2 : snap.getSetOfNonCompletedJobs()) {
				if (!j1.equals(j2)) {
					if ((j1.fromColumn < taskCentroids[0] && j2.toColumn >= taskCentroids[0])
							|| ((j1.fromColumn > taskCentroids[0] && j2.toColumn <= taskCentroids[0]))) {
						jobsOnOtherSide++;

					}
					totalJobs++;
				}
			}
			// Calculate p_visit_centroidPRE for current job
			pVisitCentroidPRE[i] = (double) jobsOnOtherSide / totalJobs;
		}

		// Calculate PRE travel times for the left and right crane
		for (int i = 0; i < snap.getSetOfNonCompletedJobs().size(); i++) {
			preTtCrane1[i] = (Math.abs(snap.getSetOfNonCompletedJobs().get(i).fromColumn - taskCentroids[0]))
					* pVisitCentroidPRE[i];
		}

		return preTtCrane1;
	}

	private double[] estimatedTtRatioPreCrane2(Snapshot snap) {
		double[] taskCentroids = centroidOfTasks(snap);
		double[] pVisitCentroidPRE = new double[snap.getSetOfNonCompletedJobs().size()];
		double[] preTtCrane2 = new double[snap.getSetOfNonCompletedJobs().size()];

		for (int i = 0; i < snap.getSetOfNonCompletedJobs().size(); i++) {
			int jobsOnOtherSide = 0;
			int totalJobs = 0;
			Job j1 = snap.getSetOfNonCompletedJobs().get(i);
			for (Job j2 : snap.getSetOfNonCompletedJobs()) {
				if (!j1.equals(j2)) {
					if ((j1.fromColumn > taskCentroids[1] && j2.toColumn <= taskCentroids[1])
							|| (j1.fromColumn < taskCentroids[1] && j2.toColumn >= taskCentroids[1])) {
						jobsOnOtherSide++;
					}
					totalJobs++;
				}
			}
			// Calculate p_visit_centroidPRE for current job
			pVisitCentroidPRE[i] = (double) jobsOnOtherSide / totalJobs;

		}

		// Calculate PRE travel times for the left and right crane
		for (int i = 0; i < snap.getSetOfNonCompletedJobs().size(); i++) {
			preTtCrane2[i] = (Math.abs(snap.getSetOfNonCompletedJobs().get(i).fromColumn - taskCentroids[1]))
					* pVisitCentroidPRE[i];
		}

		return preTtCrane2;

	}

	private double[] estimatedTtRatioPostCrane1(Snapshot snap) {
		double[] taskCentroids = centroidOfTasks(snap);
		double[] pVisitCentroidPOST = new double[snap.getSetOfNonCompletedJobs().size()];
		double[] postTtCrane1 = new double[snap.getSetOfNonCompletedJobs().size()];

		for (int i = 0; i < snap.getSetOfNonCompletedJobs().size(); i++) {
			int jobsOnOtherSide = 0;
			int totalJobs = 0;
			Job j1 = snap.getSetOfNonCompletedJobs().get(i);
			for (Job j2 : snap.getSetOfNonCompletedJobs()) {
				if (!j1.equals(j2)) {
					if ((j1.toColumn < taskCentroids[0] && j2.fromColumn >= taskCentroids[0])
							|| ((j1.toColumn > taskCentroids[0] && j2.fromColumn <= taskCentroids[0]))) {
						jobsOnOtherSide++;
					}
					totalJobs++;
				}
			}
			// Calculate p_visit_centroidPRE for current job
			pVisitCentroidPOST[i] = (double) jobsOnOtherSide / totalJobs;

		}
		// Calculate POST travel times for the left and right crane
		for (int i = 0; i < snap.getSetOfNonCompletedJobs().size(); i++) {
			postTtCrane1[i] = (Math.abs(snap.getSetOfNonCompletedJobs().get(i).toColumn - taskCentroids[0]))
					* pVisitCentroidPOST[i];
		}

		return postTtCrane1;
	}

	private double[] estimatedTtRatioPostCrane2(Snapshot snap) {
		double[] taskCentroids = centroidOfTasks(snap);
		double[] pVisitCentroidPOST = new double[snap.getSetOfNonCompletedJobs().size()];
		double[] postTtCrane2 = new double[snap.getSetOfNonCompletedJobs().size()];

		for (int i = 0; i < snap.getSetOfNonCompletedJobs().size(); i++) {
			int jobsOnOtherSide = 0;
			int totalJobs = 0;
			Job j1 = snap.getSetOfNonCompletedJobs().get(i);
			for (Job j2 : snap.getSetOfNonCompletedJobs()) {
				if (!j1.equals(j2)) {
					if ((j1.toColumn < taskCentroids[0] && j2.fromColumn >= taskCentroids[1])
							|| (j1.toColumn > taskCentroids[0] && j2.fromColumn <= taskCentroids[1])) {
						jobsOnOtherSide++;
					}
					totalJobs++;
				}
			}
			// Calculate p_visit_centroidPRE for current job
			pVisitCentroidPOST[i] = (double) jobsOnOtherSide / totalJobs;

		}
		// Calculate POST travel times for the right crane

		for (int i = 0; i < snap.getSetOfNonCompletedJobs().size(); i++) {
			postTtCrane2[i] = (Math.abs(snap.getSetOfNonCompletedJobs().get(i).toColumn - taskCentroids[1]))
					* pVisitCentroidPOST[i];
		}

		return postTtCrane2;
	}

	private Double calculateLongestDuration() {
		Double maxDuration = Double.NEGATIVE_INFINITY;
		Map<Integer, Double> allEstimatedDurations = new HashMap<>();

		for (Job j : problem.myJobs) {
			for (Map.Entry<Integer, Double> e : j.estimatedDurations.entrySet()) {
				if (allEstimatedDurations.containsKey(e.getKey())) {
					Double newValue = allEstimatedDurations.get(e.getKey()) + e.getValue();
					allEstimatedDurations.put(e.getKey(), newValue);
				} else {
					allEstimatedDurations.put(e.getKey(), e.getValue());
				}
			}
		}

		for (Double dur : allEstimatedDurations.values()) {
			if (dur > maxDuration) {
				maxDuration = dur;
			}
		}
		return maxDuration;
	}

}
