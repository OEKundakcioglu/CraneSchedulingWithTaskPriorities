package DP;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Stopwatch;

import data.Job;
import data.Problem;


import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class DynamicProgramming {
	public Problem problem;
	public HashMap<Snapshot, Action> solution;
	public double elapsedTime;
	public int jobCount;
	public int totalPriority;
	public Enum caseSet;
	public double coeff; 
	public boolean stop = false;
	public Enum grbCaseSet;

	public DynamicProgramming(Problem problem, Enum cases, double coeff) {
		this.problem = problem;
		this.solution = new HashMap<Snapshot, Action>();
		this.caseSet = cases;
		this.coeff = coeff;
		RunAll();

	}

	public void RunAll() {
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
		// stopWatch.reset();
		Snapshot snap = new Snapshot(setOfCompletedJobs, craneStatus, 0, problem.myJobs);

		CompletableFuture<Double> task = CompletableFuture.supplyAsync(() -> {
			double reward = solve(snap);
			return reward;
		});

		try {
			boolean isCompletedSuccessfully = task.get(this.problem.timeLimit, TimeUnit.SECONDS) != null;
			if (isCompletedSuccessfully) {

				stopWatch.stop();
				int jobCount = jobReport(snap);
				elapsedTime = stopWatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0;
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			stopWatch.stop();
			//System.out.println(e);
			elapsedTime = stopWatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0;
		}

		// double reward = solve(snap);
		// int jobCount = jobReport(snap);
	}

	private int solve(Snapshot snap) {

		this.totalPriority = (int) (solveForASnapshot(snap));
		return this.totalPriority;
	}

	private double solveForASnapshot(Snapshot snap) {
		// if time is up, no need to proceed further
		if (snap.currentTime == problem.numberOfTimePoints) {
			return 0;
		}
		// COMPLETED CRANE UPDATES
		for (CraneStatus cc : snap.craneStatus) {
			if (cc.idle == false && cc.completeByTime + 1 == snap.currentTime) {
				cc.idle = true;
			}
		}
		double maxValue = -1;
		int bestCraneId = -1;
		Job bestTask = null;
		Snapshot movingTo = null;
		// if all cranes are still busy, no need to make a decision
		// let the time roll
		boolean allCranesBusy = true;
		for (CraneStatus cc : snap.craneStatus) {
			if (cc.idle) {
				allCranesBusy = false;
				break;
			}
		}
		
		if (!allCranesBusy) {
			HashMap<Job, Double> RewardsForCrane1 = new HashMap<Job, Double>();
			HashMap<Job, Double> RewardsForCrane2 = new HashMap<Job, Double>();
			HashMap<Job, Snapshot> SnapsForCrane1 = new HashMap<Job, Snapshot>();
			HashMap<Job, Snapshot> SnapsForCrane2 = new HashMap<Job, Snapshot>();
			for (CraneStatus cc : snap.craneStatus) {
				if (cc.idle) {
					ArrayList<Job> feasibleJobs = computeFeasibleJobs(snap.getSetOfCompletedJobs(), snap.currentTime,
							cc, snap.craneStatus, snap);
					Helpers helper = new Helpers(this.problem);

					for (Job j : feasibleJobs) {
						double addedPriority = j.prio;
						ArrayList<Job> newSetOfCompletedJobs = new ArrayList<Job>();
						for (Job s : snap.getSetOfCompletedJobs()) {
							newSetOfCompletedJobs.add(s);
						}
						newSetOfCompletedJobs.add(j);
						CraneStatus[] newCraneStatus = new CraneStatus[this.problem.crane.length];

						for (int i = 0; i < this.problem.crane.length; i++) {
							newCraneStatus[i] = new CraneStatus();
							if (snap.craneStatus[i].crane.id == cc.crane.id) {
								newCraneStatus[i].crane = cc.crane;
								newCraneStatus[i].idle = false;
								newCraneStatus[i].job = j;
								newCraneStatus[i].startTime = snap.currentTime;
								newCraneStatus[i].leg1 = (int) (snap.currentTime + helper.calculateTimeLeg_1(j, cc));

								ArrayList<Integer> newCraneTrajectory = new ArrayList<Integer>();
								for (int ct : snap.craneStatus[i].craneTrajectory) {
									newCraneTrajectory.add(ct);
								}

								newCraneStatus[i].craneTrajectory = newCraneTrajectory;
								if (newCraneStatus[i].craneTrajectory.size() != 0) {
									newCraneStatus[i].column = newCraneStatus[i].craneTrajectory
											.get(newCraneStatus[i].craneTrajectory.size() - 1);
								} else {
									newCraneStatus[i].column = snap.craneStatus[i].column;
								}
								if (newCraneStatus[i].leg1 < j.availableTime) {
									newCraneStatus[i].completion1 = j.availableTime + j.duration;
									newCraneStatus[i].leg2 = (int) (j.availableTime + j.duration
											+ helper.calculateTimeLeg_2(j));
									newCraneStatus[i].completeByTime = (int) (j.availableTime + j.duration
											+ helper.calculateTimeLeg_2(j) + j.duration2);
								} else {
									newCraneStatus[i].completion1 = (int) (snap.currentTime
											+ helper.calculateTimeLeg_1(j, newCraneStatus[i]) + j.duration);
									newCraneStatus[i].leg2 = (int) (snap.currentTime
											+ helper.calculateTimeNoConflict(j, newCraneStatus[i]));
									newCraneStatus[i].completeByTime = (int) (snap.currentTime
											+ helper.calculateTimeNoConflict(j, newCraneStatus[i]) + j.duration2);
								}
								newCraneStatus[i].jobTrajectory = JobTrajectory(newCraneStatus[i], snap.currentTime, j,
										snap.craneStatus, snap);
								newCraneStatus[i].craneTrajectory.addAll(newCraneStatus[i].jobTrajectory);
								newCraneStatus[i].craneTrajectory = helper.ConsideredCraneTrajectory(newCraneStatus[i],
										snap.craneStatus, newCraneStatus[i].craneTrajectory.size());
								newCraneStatus[i].completeByTime = newCraneStatus[i].craneTrajectory.size() - 1;
								newCraneStatus[i].column = newCraneStatus[i].craneTrajectory
										.get(newCraneStatus[i].craneTrajectory.size() - 1);
							} else // other crane
							{
								newCraneStatus[i].crane = snap.craneStatus[i].crane;
								newCraneStatus[i].idle = snap.craneStatus[i].idle;
								newCraneStatus[i].craneTrajectory = snap.craneStatus[i].craneTrajectory;
								newCraneStatus[i].jobTrajectory = snap.craneStatus[i].jobTrajectory;
								newCraneStatus[i].startTime = snap.craneStatus[i].startTime;
								newCraneStatus[i].column = snap.craneStatus[i].column;
								newCraneStatus[i].completeByTime = snap.craneStatus[i].completeByTime;
								newCraneStatus[i].leg1 = snap.craneStatus[i].leg1;
								newCraneStatus[i].leg2 = snap.craneStatus[i].leg2;
								newCraneStatus[i].completion1 = snap.craneStatus[i].completion1;
								newCraneStatus[i].job = snap.craneStatus[i].job;
							}
						}
						Snapshot newSnap = new Snapshot(newSetOfCompletedJobs, newCraneStatus, snap.currentTime,
								problem.myJobs);
						// insert the value + recursion -> hashmap
						if (cc.crane.id == 1) {
							SnapsForCrane1.put(j, newSnap);
							RewardsForCrane1.put(j, addedPriority + solveForASnapshot(newSnap));
						} else {
							SnapsForCrane2.put(j, newSnap);
							RewardsForCrane2.put(j, addedPriority + solveForASnapshot(newSnap));
						}
					}

				}
			}
			// find the maximum
			for (Job j : RewardsForCrane1.keySet()) {
				if (RewardsForCrane1.get(j) > maxValue) {
					maxValue = RewardsForCrane1.get(j);
					bestCraneId = 1;
					bestTask = j;
					movingTo = SnapsForCrane1.get(j);
				}
			}
			for (Job j : RewardsForCrane2.keySet()) {
				if (RewardsForCrane2.get(j) > maxValue) {
					maxValue = RewardsForCrane2.get(j);
					bestCraneId = 2;
					bestTask = j;
					movingTo = SnapsForCrane2.get(j);
				}
			}
			
		}
		for (CraneStatus cc : snap.craneStatus) {
			ArrayList<Integer> newCCTrajectory = new ArrayList<Integer>();
			for (int col : cc.craneTrajectory) {
				newCCTrajectory.add(col);
			}
			if ((!cc.idle) && (snap.currentTime <= cc.completeByTime)) {
				cc.column = newCCTrajectory.get(snap.currentTime);
			}
			if (cc.idle) {
				if ((cc.craneTrajectory.size() != 0) && (snap.currentTime != cc.completeByTime)) {
					cc.column = cc.craneTrajectory.get(cc.craneTrajectory.size() - 1);
				}
			}
		}
		Snapshot doNothingSnap = new Snapshot(snap.getSetOfCompletedJobs(), snap.craneStatus, snap.currentTime + 1,
				problem.myJobs);
		
		/*int objIfDoNothing = 0;
		Action action = new Action();
		if (objIfDoNothing > maxValue) {
			maxValue = solveForASnapshot(doNothingSnap);
			action.craneId = 0;
			action.job = null;
			action.movingToSnapshot = doNothingSnap;
		} else {
			action.craneId = bestCraneId;
			action.job = bestTask;
			action.movingToSnapshot = movingTo;
		}*/
		
		
		double rewardIfDoNothing = solveForASnapshot(doNothingSnap);
		Action action = new Action();
		if (rewardIfDoNothing > maxValue) {
		    maxValue = rewardIfDoNothing;
		    action.craneId = 0;
		    action.job = null;
		    action.movingToSnapshot = doNothingSnap;
		} else {
		    // Continue with the best action
		    action.craneId = bestCraneId;
		    action.job = bestTask;
		    action.movingToSnapshot = movingTo;
		}
		action.cumulativeReward = maxValue;
		this.solution.put(snap, action);

		return maxValue;
	}

	public ArrayList<Integer> JobTrajectory(CraneStatus cc, int currentTime, Job j, CraneStatus[] craneStatus,
			Snapshot snap) {
		cc.jobTrajectory = new ArrayList<Integer>();
		int a = 1;
		int b = 1;
		int c = 1;
		int d = 1;
		Helpers helper = new Helpers(this.problem);

		if (cc.column == cc.crane.initColumn && cc.startTime != 0) {
			for (int k = 0; k <= cc.startTime; k++) {
				cc.jobTrajectory.add((int) (cc.column));
			}
		}
		if (cc.craneTrajectory.size() != 0) {
			int index = cc.craneTrajectory.size() - 1;
			int diff = Math.abs(index - cc.startTime);

			for (CraneStatus oc : craneStatus) {
				if (!oc.myEquals(cc)) {
					if (diff != 1) {
						int min_trajectory = Math.min(Math.abs(cc.craneTrajectory.size() - oc.craneTrajectory.size()),
								diff);
						for (int l = 0; l < min_trajectory; l++) {
							if ((oc.craneTrajectory.size() - cc.craneTrajectory.size()) - cc.jobTrajectory.size() > 0) {
								if ((cc.craneTrajectory.size() + l < oc.craneTrajectory.size())
										&& (cc.craneTrajectory.size() + l != oc.craneTrajectory.size() - 1)) {
									if (oc.craneTrajectory.get(cc.craneTrajectory.size() + l) == cc.column) {
										if (oc.craneTrajectory.get(cc.craneTrajectory.size() + l) < oc.craneTrajectory
												.get(cc.craneTrajectory.size() + l + 1)) // push forward
										{
											cc.column += 1;
											cc.jobTrajectory.add((int) (cc.column));
										}
										if (oc.craneTrajectory.get(cc.craneTrajectory.size() + l) > oc.craneTrajectory
												.get(cc.craneTrajectory.size() + l + 1)) // push backward
										{
											cc.column -= 1;
											cc.jobTrajectory.add((int) (cc.column));
										}
										if (oc.craneTrajectory.get(cc.craneTrajectory.size() + l) == oc.craneTrajectory
												.get(cc.craneTrajectory.size() + l + 1)) {

											if (cc.jobTrajectory.size() != 0) {
												int ocDiff = oc.craneTrajectory.get(cc.craneTrajectory.size() + l)
														- oc.craneTrajectory.get(cc.craneTrajectory.size() + l - 1);
												cc.column = cc.column + ocDiff;
												cc.jobTrajectory.add((int) (cc.column));
											} else {
												int ocDiff = oc.craneTrajectory.get(cc.craneTrajectory.size() + l)
														- oc.craneTrajectory.get(cc.craneTrajectory.size() + l - 1);
												cc.column = cc.column + ocDiff;
												cc.jobTrajectory.add((int) (cc.column));
											}
										}
									} else {
										cc.jobTrajectory.add((int) (cc.column));
									}
								} else {
									if (oc.craneTrajectory.get(cc.craneTrajectory.size() + l - 1) == cc.column) {
										if (oc.craneTrajectory.get(cc.craneTrajectory.size() + l
												- 1) < oc.craneTrajectory.get(cc.craneTrajectory.size() + l)) // push
																												// forward
										{
											cc.column += 1;
											cc.jobTrajectory.add((int) (cc.column));
										}
										if (oc.craneTrajectory.get(cc.craneTrajectory.size() + l
												- 1) > oc.craneTrajectory.get(cc.craneTrajectory.size() + l)) // push
																												// backward
										{
											cc.column -= 1;
											cc.jobTrajectory.add((int) (cc.column));
										}
										if (oc.craneTrajectory.get(cc.craneTrajectory.size() + l
												- 1) == oc.craneTrajectory.get(cc.craneTrajectory.size() + l)) {
											if (cc.jobTrajectory.size() != 0) {
												int ocDiff = oc.craneTrajectory.get(cc.craneTrajectory.size() + l - 1)
														- oc.craneTrajectory.get(cc.craneTrajectory.size() + l);
												cc.jobTrajectory.add((int) (cc.column + ocDiff));
											} else {
												cc.jobTrajectory.add(
														(int) (cc.craneTrajectory.get(cc.craneTrajectory.size() - 2)));
											}
										}
									}
								}
							}
						}
						if (diff > min_trajectory) {
							int staySame = Math.abs(diff - min_trajectory);
							for (int s = 0; s < staySame; s++) {
								cc.jobTrajectory.add((int) (cc.column));
							}
						}
					}
				}
			}
		}
		cc.leg1 = (int) (snap.currentTime + helper.calculateTimeLeg_1(j, cc));
		if (cc.leg1 < j.availableTime) {
			cc.completion1 = j.availableTime + j.duration;
			cc.leg2 = (int) (j.availableTime + j.duration + helper.calculateTimeLeg_2(j));
			cc.completeByTime = (int) (j.availableTime + j.duration + helper.calculateTimeLeg_2(j) + j.duration2);
		} else {
			cc.completion1 = (int) (snap.currentTime + helper.calculateTimeLeg_1(j, cc) + j.duration);
			cc.leg2 = (int) (snap.currentTime + helper.calculateTimeNoConflict(j, cc));
			cc.completeByTime = (int) (snap.currentTime + helper.calculateTimeNoConflict(j, cc) + j.duration2);
		}
		for (int x = (int) (currentTime); x <= cc.completeByTime; x++) {
			if (x == 0) {
				if (cc.column < j.fromColumn) {
					cc.jobTrajectory.add((int) (cc.column));
				} else {
					cc.jobTrajectory.add((int) (cc.column));
				}
			}
			if (x <= cc.leg1 && x != 0 && x != cc.startTime) {
				if (cc.column < j.fromColumn) {
					cc.jobTrajectory.add((int) (cc.column + b));
					b += 1;
				} else {
					cc.jobTrajectory.add((int) (cc.column - c));
					c += 1;
				}
			}
			if (x > cc.leg1 && x <= cc.completion1) {
				cc.jobTrajectory.add((int) j.fromColumn);

			}
			if (x > cc.leg1 && x > cc.completion1 && x <= cc.leg2) {
				if (j.fromColumn < j.toColumn) {
					cc.jobTrajectory.add((int) ((int) j.fromColumn + a));
					a += 1;
				} else {
					cc.jobTrajectory.add((int) ((int) j.fromColumn - d));
					d += 1;
				}
			}
			if (x > cc.leg2) {
				cc.jobTrajectory.add((int) j.toColumn);
			}
		}
		return cc.jobTrajectory;
	}

	public int[] AvailableJobs(int numberOfJobs, double coeff) {
		int[] availableJobsAtEachBranch = new int[numberOfJobs];

		for (int i = 0; i < availableJobsAtEachBranch.length; i++) {
			availableJobsAtEachBranch[i] = (int) ((numberOfJobs - i) * coeff);
			if (availableJobsAtEachBranch[i] == 0) {
				availableJobsAtEachBranch[i] = 1;
			}
		}
		return availableJobsAtEachBranch;
	}

	private ArrayList<Job> computeFeasibleJobs(ArrayList<Job> setOfCompletedJobs, int currentTime, CraneStatus cc,
			CraneStatus[] craneStatus, Snapshot snap) {
		ArrayList<Job> doableTasks = new ArrayList<Job>();
		Helpers helper = new Helpers(this.problem);
		ArrayList<Job> unCompletedJobs = new ArrayList<Job>();
		for (Job j : this.problem.myJobs) {
			if (!setOfCompletedJobs.contains(j)) {
				unCompletedJobs.add(j);
			}
		}
		int[] k = AvailableJobs(this.problem.numberOfJobs, this.coeff);
		// Case 1: Exact solution
		if (this.caseSet.toString() == "Exact") // Exact solution
		{

			for (Job j : unCompletedJobs) {
				if (j.availableTime <= currentTime + helper.calculateTimeLeg_1(j, cc)
						&& !setOfCompletedJobs.contains(j)) {
					CraneStatus[] newCraneStatus = FeasibleJobStatus(cc, snap, snap.currentTime, j);
					for (int i = 0; i < this.problem.crane.length; i++) {
						if (newCraneStatus[i].crane.id == cc.crane.id) {
							if (newCraneStatus[i].craneTrajectory.size() - 1 <= this.problem.numberOfTimePoints) {
								doableTasks.add(j);
							}
									        
						}
						
					}
				
				}				
				
			}
			
		}
			
		if (this.caseSet.toString() == "Stage") // Branch solution; look n number of jobs at each branch
		{
			ArrayList<Job> temp1TaskList = new ArrayList<Job>();
			ArrayList<Job> temp2TaskList = new ArrayList<Job>();

			for (Job j : unCompletedJobs) {
				if (j.availableTime <= currentTime + helper.calculateTimeLeg_1(j, cc)
						&& !setOfCompletedJobs.contains(j)) {
					CraneStatus[] newCraneStatus = FeasibleJobStatus(cc, snap, snap.currentTime, j);
					for (int i = 0; i < this.problem.crane.length; i++) {
						if (newCraneStatus[i].crane.id == cc.crane.id) {
							if (newCraneStatus[i].craneTrajectory.size() - 1 <= this.problem.numberOfTimePoints) {
								temp1TaskList.add(j);
							}
						}
					}
				}
			}

			Collections.sort(temp1TaskList, Comparator.comparing(Job::getPriority).reversed());
			temp2TaskList = temp1TaskList;
			int nOfCompletedJobs = setOfCompletedJobs.size();

			int nOfCopmletedJobsAtLevel = k[nOfCompletedJobs];
		
			if (temp1TaskList.size() >= nOfCopmletedJobsAtLevel) {

				doableTasks = new ArrayList<>(temp2TaskList.subList(0, nOfCopmletedJobsAtLevel)); // slice the list from
																									// Topn.th element
			} else {
				doableTasks = temp2TaskList;
			}
		}
		if (this.caseSet.toString() == "StageConflict") // Branch solution; look n number of jobs at each branch based
														// on prio/conflict
		{
			ArrayList<Job> temp2TaskList = new ArrayList<Job>();
			HashMap<Job, Double> candidateJobs = new HashMap<Job, Double>();

			for (Job j : unCompletedJobs) {
				double jobReward = 0;
				if (j.availableTime <= currentTime + helper.calculateTimeLeg_1(j, cc)
						&& !setOfCompletedJobs.contains(j)) {
					CraneStatus[] newCraneStatus = FeasibleJobStatus(cc, snap, snap.currentTime, j);
					for (int i = 0; i < this.problem.crane.length; i++) {
						if (newCraneStatus[i].crane.id == cc.crane.id) {
							if (newCraneStatus[i].craneTrajectory.size() - 1 <= this.problem.numberOfTimePoints) {
								jobReward = (double) newCraneStatus[i].job.priority
										/ (double) (newCraneStatus[i].completeByTime - newCraneStatus[i].startTime);
								candidateJobs.put(j, jobReward);
							}
						}
					}
				}
			}
			List<Map.Entry<Job, Double>> topCandidates = new ArrayList<>(candidateJobs.entrySet());
			Collections.sort(topCandidates, new Comparator<Map.Entry<Job, Double>>() {
				public int compare(Map.Entry<Job, Double> o1, Map.Entry<Job, Double> o2) {
					return o2.getValue().compareTo(o1.getValue());
				}
			});

			Map<Job, Double> backToDictionary = new LinkedHashMap<>();
			for (Map.Entry<Job, Double> entry : topCandidates) {
				backToDictionary.put(entry.getKey(), entry.getValue());
			}

			ArrayList<Job> keyList = new ArrayList<Job>(backToDictionary.keySet());

			temp2TaskList = keyList;
			int nOfCompletedJobs = setOfCompletedJobs.size();
			int nOfCompletedJobsAtLevel = k[nOfCompletedJobs];

			if (keyList.size() >= nOfCompletedJobsAtLevel) {
				doableTasks = new ArrayList<Job>(temp2TaskList.subList(0, nOfCompletedJobsAtLevel));
			} else {
				doableTasks = temp2TaskList;
			}

		}

		return doableTasks;
	}

	private CraneStatus[] FeasibleJobStatus(CraneStatus cc, Snapshot snap, int currentTime, Job j) {
		CraneStatus[] newCraneStatus = new CraneStatus[this.problem.crane.length];
		Helpers helper = new Helpers(this.problem);
		for (int i = 0; i < this.problem.crane.length; i++) {
			newCraneStatus[i] = new CraneStatus();

			if (snap.craneStatus[i].crane.id == cc.crane.id) {
				newCraneStatus[i].crane = cc.crane;
				newCraneStatus[i].idle = false;
				newCraneStatus[i].job = j;
				newCraneStatus[i].startTime = currentTime;
				newCraneStatus[i].leg1 = (int) (currentTime + helper.calculateTimeLeg_1(j, cc));

				ArrayList<Integer> newCraneTrajectory = new ArrayList<Integer>();
				for (int ct : cc.craneTrajectory) {
					newCraneTrajectory.add(ct);
				}
				newCraneStatus[i].craneTrajectory = newCraneTrajectory;
				if (newCraneStatus[i].craneTrajectory.size() != 0) {
					newCraneStatus[i].column = newCraneStatus[i].craneTrajectory
							.get(newCraneStatus[i].craneTrajectory.size() - 1);
				} else {
					newCraneStatus[i].column = snap.craneStatus[i].column;
				}
				if (newCraneStatus[i].leg1 < j.availableTime) {
					newCraneStatus[i].completion1 = j.availableTime + j.duration;
					newCraneStatus[i].leg2 = (int) (j.availableTime + j.duration + helper.calculateTimeLeg_2(j));
					newCraneStatus[i].completeByTime = (int) (j.availableTime + j.duration
							+ helper.calculateTimeLeg_2(j) + j.duration2);
				} else {
					newCraneStatus[i].completion1 = (int) (currentTime + helper.calculateTimeLeg_1(j, newCraneStatus[i])
							+ j.duration);
					newCraneStatus[i].leg2 = (int) (currentTime + helper.calculateTimeNoConflict(j, newCraneStatus[i]));
					newCraneStatus[i].completeByTime = (int) (currentTime
							+ helper.calculateTimeNoConflict(j, newCraneStatus[i]) + j.duration2);
				}
				newCraneStatus[i].jobTrajectory = JobTrajectory(newCraneStatus[i], currentTime, j, snap.craneStatus,
						snap);
				newCraneStatus[i].craneTrajectory.addAll(newCraneStatus[i].jobTrajectory);
				newCraneStatus[i].craneTrajectory = helper.ConsideredCraneTrajectory(newCraneStatus[i],
						snap.craneStatus, newCraneStatus[i].craneTrajectory.size());
				newCraneStatus[i].completeByTime = newCraneStatus[i].craneTrajectory.size() - 1;
				newCraneStatus[i].column = newCraneStatus[i].craneTrajectory
						.get(newCraneStatus[i].craneTrajectory.size() - 1);
			} else {
				newCraneStatus[i].crane = snap.craneStatus[i].crane;
				newCraneStatus[i].idle = snap.craneStatus[i].idle;
				newCraneStatus[i].craneTrajectory = snap.craneStatus[i].craneTrajectory;
				newCraneStatus[i].jobTrajectory = snap.craneStatus[i].jobTrajectory;
				newCraneStatus[i].startTime = snap.craneStatus[i].startTime;
				newCraneStatus[i].column = snap.craneStatus[i].column;
				newCraneStatus[i].completeByTime = snap.craneStatus[i].completeByTime;
				newCraneStatus[i].leg1 = snap.craneStatus[i].leg1;
				newCraneStatus[i].leg2 = snap.craneStatus[i].leg2;
				newCraneStatus[i].completion1 = snap.craneStatus[i].completion1;
				newCraneStatus[i].job = snap.craneStatus[i].job;
			}
		}
		return newCraneStatus;
	}

	private int jobReport(Snapshot snap) {
		if (snap.currentTime < this.problem.numberOfTimePoints) {
			Action action = this.solution.get(snap);
			if (action != null) {
				Snapshot nextSnap = action.movingToSnapshot;
				jobReport(nextSnap);
			}
		}
		if (snap.currentTime == this.problem.numberOfTimePoints) {

			this.jobCount = snap.getSetOfCompletedJobs().size();

			if (problem.detailedPrint == 1) {
				/*
				 * ExcelReaderInterop excell = new ExcelReaderInterop();
				 * excell.ListToExcel(snap, this.problem, caseSet);
				 */
			}

		}
		return this.jobCount;
	}

}
