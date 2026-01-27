package heuristics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.base.Stopwatch;

import data.Job;
import data.Problem;
import DP.CraneStatus;
import DP.Helpers;
import DP.Snapshot;

public class GreedySolution {
	public Problem problem;
	public int rewardgreedy;
	public int rewardconflict;
	public int rewardsimplegreedy;
	public double elapsedTime;
	public double elapsedSimpleTime;
	public double elapsedTimeConflict;
	public int jobCountGreedy;
	public int jobCountConflict;
	public int jobCountSimple;
	public Enum caseSet;

	public GreedySolution(Problem problem, Enum cases) {

		this.problem = problem;
		this.caseSet = cases;
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
		if (this.caseSet.toString().equals("Greedy")) {
			Stopwatch stopWatch = Stopwatch.createStarted();
			Snapshot snap = new Snapshot(setOfCompletedJobs, craneStatus, 0, problem.myJobs);
			this.rewardgreedy = (int) (solveGreedy(snap));
			stopWatch.stop();
			this.elapsedTime = stopWatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0;

		}
		if (this.caseSet.toString().equals("SimpleGreedy")) {
			Stopwatch stopWatch = Stopwatch.createStarted();
			stopWatch.reset();
			stopWatch.start();

			Snapshot snap2 = new Snapshot(setOfCompletedJobs, craneStatus, 0, problem.myJobs);
			this.rewardsimplegreedy = (int) (solveSimpleGreedy(snap2));
			stopWatch.stop();
			this.elapsedTime = stopWatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0;

		}

		if (this.caseSet.toString().equals("GreedyConflict")) {
			Stopwatch stopWatch = Stopwatch.createStarted();
			stopWatch.reset();
			stopWatch.start();
			Snapshot snap1 = new Snapshot(setOfCompletedJobs, craneStatus, 0, problem.myJobs);
			this.rewardconflict = (int) (solveGreedyConflict(snap1));
			stopWatch.stop();
			this.elapsedTime = stopWatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0;

		}
	}

	private double solveGreedy(Snapshot snap) {
		double reward = 0;
		ArrayList<Job> setOfCompletedJobs = new ArrayList<Job>();
		for (int t = 0; t <= this.problem.numberOfTimePoints; t++) {
			if (t == this.problem.numberOfTimePoints) {
				break;
			}

			for (CraneStatus cc : snap.craneStatus) {
				if (cc.idle == false && cc.completeByTime == t) {
					cc.idle = true;
				}
			}

			boolean allCranesBusy = true;
			for (CraneStatus cc : snap.craneStatus) {
				if (cc.idle) {
					allCranesBusy = false;
					break;
				}
			}

			if (!allCranesBusy) {
				for (CraneStatus cc : snap.craneStatus) {
					if (cc.idle) {
						for (Job j : snap.getSetOfCompletedJobs()) {
							setOfCompletedJobs.add(j);
						}
						var topCandidates = computeFeasibleJobs(setOfCompletedJobs, t, cc, snap.craneStatus, snap);

						if (topCandidates.isEmpty()) {
							continue;
						}

						setOfCompletedJobs.add(topCandidates.get(0).getKey());
						UpdateCraneStatus(cc, topCandidates.get(0).getKey(), t, snap.craneStatus, snap);
						reward += cc.job.prio;

					}
				}
			}

		}
		for (Job j : setOfCompletedJobs) {
			snap.completed(j);
		}
		this.jobCountGreedy = snap.getSetOfCompletedJobs().size();

		return reward;
	}

	private double solveSimpleGreedy(Snapshot snap) {

		double reward = 0;
		ArrayList<Job> setOfCompletedJobs = new ArrayList<Job>();
		for (int t = 0; t <= this.problem.numberOfTimePoints; t++) {
			if (t == this.problem.numberOfTimePoints) {
				break;
			}

			for (CraneStatus cc : snap.craneStatus) {
				if (cc.idle == false && cc.completeByTime == t) {
					cc.idle = true;
				}
			}

			boolean allCranesBusy = true;
			for (CraneStatus cc : snap.craneStatus) {
				if (cc.idle) {
					allCranesBusy = false;
					break;
				}
			}

			if (t == 0) {
				int frstCraneDist = 0;
				int secndCraneDist = 0;
				Job candidateJob1 = null;
				Job candidateJob2 = null;

				for (CraneStatus cc : snap.craneStatus) {
					if (cc.idle) {
						var topCandidates = computeFeasibleJobs(setOfCompletedJobs, t, cc, snap.craneStatus, snap);

						if (cc.crane.id == 1) {
							candidateJob1 = topCandidates.get(0).getKey();
							frstCraneDist = Math.abs(cc.column - candidateJob1.fromColumn);

						} else {
							candidateJob2 = topCandidates.get(0).getKey();
							secndCraneDist = Math.abs(cc.column - candidateJob2.fromColumn);
						}
					}
				}
				if (candidateJob1 == candidateJob2) {
					for (CraneStatus cc : snap.craneStatus) {
						var topCandidates = computeFeasibleJobs(setOfCompletedJobs, t, cc, snap.craneStatus, snap);
						if (frstCraneDist < secndCraneDist) {
							if (cc.crane.id == 1) {
								UpdateCraneStatus(cc, topCandidates.get(0).getKey(), t, snap.craneStatus, snap);
								reward += cc.job.prio;
								setOfCompletedJobs.add(topCandidates.get(0).getKey());
								break;
							}

						} else {
							if (cc.crane.id == 2) {
								UpdateCraneStatus(cc, topCandidates.get(0).getKey(), t, snap.craneStatus, snap);
								reward += cc.job.prio;
								setOfCompletedJobs.add(topCandidates.get(0).getKey());
								break;
							}
						}
					}
				}
			}

			if (!allCranesBusy) {
				for (CraneStatus cc : snap.craneStatus) {
					if (cc.idle) {
						for (Job j : snap.getSetOfCompletedJobs()) {
							setOfCompletedJobs.add(j);
						}
						var topCandidates = computeFeasibleJobs(setOfCompletedJobs, t, cc, snap.craneStatus, snap);

						if (topCandidates.isEmpty()) {
							continue;
						}

						setOfCompletedJobs.add(topCandidates.get(0).getKey());

						UpdateCraneStatus(cc, topCandidates.get(0).getKey(), t, snap.craneStatus, snap);
						reward += cc.job.prio;

					}
				}
			}
		}
		for (Job j : setOfCompletedJobs) {
			snap.completed(j);
		}
		this.jobCountSimple = snap.getSetOfCompletedJobs().size();

		return reward;
	}

	private double solveGreedyConflict(Snapshot snap1) {
		double reward = 0;
		ArrayList<Job> setOfCompletedJobs = new ArrayList<Job>();
		Helpers helper = new Helpers(this.problem);

		for (int t = 0; t <= this.problem.numberOfTimePoints; t++) {
			if (t == this.problem.numberOfTimePoints) {
				break;
			}

			for (CraneStatus cc : snap1.craneStatus) {
				if (cc.idle == false && cc.completeByTime == t) {
					cc.idle = true;
				}
			}

			boolean allCranesBusy = true;
			for (CraneStatus cc : snap1.craneStatus) {
				if (cc.idle) {
					allCranesBusy = false;
					break;
				}
			}

			if (!allCranesBusy) {
				for (CraneStatus cc : snap1.craneStatus) {
					if (cc.idle) {

						for (Job j : snap1.getSetOfCompletedJobs()) {
							setOfCompletedJobs.add(j);
						}

						var topCandidates = computeFeasibleJobs(setOfCompletedJobs, t, cc, snap1.craneStatus, snap1);

						if (topCandidates.isEmpty()) {
							continue;
						}

						Job job = topCandidates.get(0).getKey();
						cc.idle = false;
						cc.job = job;
						cc.startTime = t;
						cc.leg1 = (int) (t + helper.calculateTimeLeg_1(job, cc));

						ArrayList<Integer> newCraneTrajectory = new ArrayList<Integer>();

						for (int ct : cc.craneTrajectory) {
							newCraneTrajectory.add(ct);
						}

						cc.craneTrajectory = newCraneTrajectory;

						if (cc.leg1 < job.availableTime) {
							cc.completion1 = job.availableTime + job.duration;
							cc.leg2 = (int) (job.availableTime + job.duration + helper.calculateTimeLeg_2(job));
							cc.completeByTime = (int) (job.availableTime + job.duration + helper.calculateTimeLeg_2(job)
									+ job.duration2);

						} else {
							cc.completion1 = (int) (t + helper.calculateTimeLeg_1(job, cc) + job.duration);
							cc.leg2 = (int) (t + helper.calculateTimeNoConflict(job, cc));
							cc.completeByTime = (int) (t + helper.calculateTimeNoConflict(job, cc) + job.duration2);
						}
						cc.jobTrajectory = jobTrajectory(cc, t, job, snap1.craneStatus, snap1);
						cc.craneTrajectory.addAll(cc.jobTrajectory);

						cc.craneTrajectory = helper.ConsideredCraneTrajectory(cc, snap1.craneStatus,
								cc.craneTrajectory.size());

						cc.completeByTime = cc.craneTrajectory.size() - 1;
						cc.column = cc.craneTrajectory.get(cc.craneTrajectory.size() - 1);

						setOfCompletedJobs.add(topCandidates.get(0).getKey());
						reward += cc.job.prio;

						String key = "Crane " + cc.crane.id + " at stage " + setOfCompletedJobs.size() + " for JobID "
								+ cc.job.jobID + " cumsum " + reward;
						//Printing.writeGreedyStageCsv(key);

					}
				}
			}
		}
		for (Job j : setOfCompletedJobs) {
			snap1.completed(j);
		}
		this.jobCountConflict = snap1.getSetOfCompletedJobs().size();

		return reward;
	}

	private List<Map.Entry<Job, Double>> computeFeasibleJobs(ArrayList<Job> setOfCompletedJobs, int currentTime,
			CraneStatus cc, CraneStatus[] craneStatus, Snapshot snapshot) {
		ArrayList<Job> doableTasks = new ArrayList<Job>();
		int emptyCompletionTime = 0;
		Helpers helper = new Helpers(this.problem);

		HashMap<Job, Double> candidateJobs = new HashMap<Job, Double>();
		for (Job j : this.problem.myJobs) {
			if (j.availableTime <= currentTime + helper.calculateTimeLeg_1(j, cc) && !setOfCompletedJobs.contains(j)) // burayı
																														// tekrar
																														// kontrol
																														// edebilirsin
																														// trajectory
																														// ile
																														// eşleştirecek
																														// şekilde
			{
				CraneStatus[] newCraneStatus = new CraneStatus[this.problem.crane.length];

				for (int i = 0; i < this.problem.crane.length; i++) {
					newCraneStatus[i] = new CraneStatus();

					if (snapshot.craneStatus[i].crane.id == cc.crane.id) {
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
							newCraneStatus[i].column = snapshot.craneStatus[i].column;
						}

						if (newCraneStatus[i].leg1 < j.availableTime) {
							newCraneStatus[i].completion1 = j.availableTime + j.duration;
							newCraneStatus[i].leg2 = (int) (j.availableTime + j.duration
									+ helper.calculateTimeLeg_2(j));
							newCraneStatus[i].completeByTime = (int) (j.availableTime + j.duration
									+ helper.calculateTimeLeg_2(j) + j.duration2);

						} else {
							newCraneStatus[i].completion1 = (int) (currentTime
									+ helper.calculateTimeLeg_1(j, newCraneStatus[i]) + j.duration);
							newCraneStatus[i].leg2 = (int) (currentTime
									+ helper.calculateTimeNoConflict(j, newCraneStatus[i]));
							newCraneStatus[i].completeByTime = (int) (currentTime
									+ helper.calculateTimeNoConflict(j, newCraneStatus[i]) + j.duration2);
						}
						newCraneStatus[i].jobTrajectory = jobTrajectory(newCraneStatus[i], currentTime, j,
								snapshot.craneStatus, snapshot);
						newCraneStatus[i].craneTrajectory.addAll(newCraneStatus[i].jobTrajectory);
						emptyCompletionTime = newCraneStatus[i].craneTrajectory.size() - 1;
						newCraneStatus[i].craneTrajectory = helper.ConsideredCraneTrajectory(newCraneStatus[i],
								snapshot.craneStatus, newCraneStatus[i].craneTrajectory.size());
						newCraneStatus[i].completeByTime = newCraneStatus[i].craneTrajectory.size() - 1;
						newCraneStatus[i].column = newCraneStatus[i].craneTrajectory
								.get(newCraneStatus[i].craneTrajectory.size() - 1);

					} else {
						newCraneStatus[i].crane = snapshot.craneStatus[i].crane;
						newCraneStatus[i].idle = snapshot.craneStatus[i].idle;
						newCraneStatus[i].craneTrajectory = snapshot.craneStatus[i].craneTrajectory;
						newCraneStatus[i].jobTrajectory = snapshot.craneStatus[i].jobTrajectory;

						newCraneStatus[i].startTime = snapshot.craneStatus[i].startTime;

						newCraneStatus[i].column = snapshot.craneStatus[i].column;
						newCraneStatus[i].completeByTime = snapshot.craneStatus[i].completeByTime;
						newCraneStatus[i].leg1 = snapshot.craneStatus[i].leg1;
						newCraneStatus[i].leg2 = snapshot.craneStatus[i].leg2;
						newCraneStatus[i].completion1 = snapshot.craneStatus[i].completion1;
						newCraneStatus[i].job = snapshot.craneStatus[i].job;
					}
				}

				for (int i = 0; i < this.problem.crane.length; i++) {
					if (newCraneStatus[i].crane.id == cc.crane.id) {
						if (newCraneStatus[i].craneTrajectory.size() - 1 <= this.problem.numberOfTimePoints) {
							doableTasks.add(j);

							if (this.caseSet.toString() == "GreedyConflict") {
								double jobReward = (double) newCraneStatus[i].job.prio
										/ (double) (newCraneStatus[i].completeByTime - newCraneStatus[i].startTime); // actual
																														// completion
																														// time
								candidateJobs.put(j, jobReward);
							}
							if (this.caseSet.toString() == "Greedy") {
								double jobReward = (double) newCraneStatus[i].job.prio
										/ (double) (emptyCompletionTime - newCraneStatus[i].startTime); // empty
																										// completion
																										// time
								candidateJobs.put(j, jobReward);
							}
							if (this.caseSet.toString() == "SimpleGreedy") {
								double jobReward = (double) newCraneStatus[i].job.prio; // empty completion time
								candidateJobs.put(j, jobReward);
							}

						}
						newCraneStatus[i] = cc;
					}

				}

			}

		}
		List<Map.Entry<Job, Double>> topCandidates = candidateJobs.entrySet().stream()
				.sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
				.limit(5)
				.collect(Collectors.toList());

		return topCandidates;
	}

	private CraneStatus UpdateCraneStatus(CraneStatus cc, Job j, int currenTime, CraneStatus[] craneStatus,
			Snapshot snap) {
		Helpers helper = new Helpers(this.problem);
		cc.idle = false;
		cc.job = j;
		cc.startTime = currenTime;
		cc.leg1 = (int) (currenTime + helper.calculateTimeLeg_1(j, cc));

		ArrayList<Integer> newCraneTrajectory = new ArrayList<Integer>();

		for (int ct : cc.craneTrajectory) {
			newCraneTrajectory.add(ct);
		}

		cc.craneTrajectory = newCraneTrajectory;

		if (cc.leg1 < j.availableTime) {
			cc.completion1 = j.availableTime + j.duration;
			cc.leg2 = (int) (j.availableTime + j.duration + helper.calculateTimeLeg_2(j));
			cc.completeByTime = (int) (j.availableTime + j.duration + helper.calculateTimeLeg_2(j) + j.duration2);

		} else {
			cc.completion1 = (int) (currenTime + helper.calculateTimeLeg_1(j, cc) + j.duration);
			cc.leg2 = (int) (currenTime + helper.calculateTimeNoConflict(j, cc));
			cc.completeByTime = (int) (currenTime + helper.calculateTimeNoConflict(j, cc) + j.duration2);
		}
		cc.jobTrajectory = jobTrajectory(cc, currenTime, j, snap.craneStatus, snap);
		cc.craneTrajectory.addAll(cc.jobTrajectory);

		cc.craneTrajectory = helper.ConsideredCraneTrajectory(cc, snap.craneStatus, cc.craneTrajectory.size());
		cc.completeByTime = cc.craneTrajectory.size() - 1;
		cc.column = cc.craneTrajectory.get(cc.craneTrajectory.size() - 1);
		return cc;

	}

	private ArrayList<Integer> jobTrajectory(CraneStatus cc, int currentTime, Job j, CraneStatus[] craneStatus,
			Snapshot snap) {

		Helpers helper = new Helpers(this.problem);
		cc.jobTrajectory = new ArrayList<Integer>();
		int a = 1;
		int b = 1;
		int c = 1;
		int d = 1;

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

		cc.leg1 = (int) (currentTime + helper.calculateTimeLeg_1(j, cc));

		if (cc.leg1 < j.availableTime) {
			cc.completion1 = j.availableTime + j.duration;
			cc.leg2 = (int) (j.availableTime + j.duration + helper.calculateTimeLeg_2(j));
			cc.completeByTime = (int) (j.availableTime + j.duration + helper.calculateTimeLeg_2(j) + j.duration2);

		} else {
			cc.completion1 = (int) (currentTime + helper.calculateTimeLeg_1(j, cc) + j.duration);
			cc.leg2 = (int) (currentTime + helper.calculateTimeNoConflict(j, cc));
			cc.completeByTime = (int) (currentTime + helper.calculateTimeNoConflict(j, cc) + j.duration2);
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

	private CraneStatus initStatus(CraneStatus cc) {
		CraneStatus init = new CraneStatus();

		init.crane = cc.crane;
		init.column = cc.column;
		init.idle = cc.idle;
		init.row = cc.row;
		init.job = cc.job;
		init.startTime = cc.startTime;
		init.leg1 = cc.leg1;
		init.completion1 = cc.completion1;
		init.leg2 = cc.leg2;
		init.completeByTime = cc.completeByTime;
		init.jobTrajectory = cc.jobTrajectory;
		init.craneTrajectory = cc.craneTrajectory;

		return init;
	}

}
