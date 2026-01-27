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
import tsnSolution.Network;
import tsnSolution.Node;

public class HeuristicsNetworkSol {
	private Problem problem;
	public Enum caseSet;
	private Network network;
	public int reward;
	public double elapsedTime;
	public int timeInterval;
	public Enum tsCase;
	public ArrayList<Job> setOfCompletedJobsC1;
	public ArrayList<Job> setOfCompletedJobsC2;
	public CraneStatus[] craneStatus;

	public HeuristicsNetworkSol(Problem problem, Enum cases, int timeInterval, Enum tsCase, Network network) {
		this.problem = problem;
		this.caseSet = cases;
		this.timeInterval = timeInterval;
		this.tsCase = tsCase;
		this.network = network;
		
		this.setOfCompletedJobsC1 = new ArrayList<>();
		this.setOfCompletedJobsC2 = new ArrayList<>();
		
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
		stopWatch.reset();
		stopWatch.start();
	
		Snapshot snap1 = new Snapshot(setOfCompletedJobs, craneStatus, 0, problem.myJobs);
		this.reward = solve(snap1, this.network);
		this.craneStatus = snap1.craneStatus;
		stopWatch.stop();
		this.elapsedTime = stopWatch.elapsed(TimeUnit.MILLISECONDS) / 1000.0;
	}

	public int solve(Snapshot snap, Network network) {

		int reward = 0;
		ArrayList<Job> setOfCompletedJobs = new ArrayList<Job>();
		Helpers helper = new Helpers(this.problem);

		for (int t = 0; t <= network.spaceSize; t++) {
			if (t == network.spaceSize) {
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
						var topCandidates = computeFeasibleJobs(setOfCompletedJobs, t, cc, snap.craneStatus, snap,
								network);

						if (topCandidates.isEmpty()) {
							continue;
						}

						Job job = topCandidates.get(0).getKey();						

						cc.idle = false;
						cc.job = job;
						cc.startTime = t;
						if(job.jobID == 16) {
							int a =5;
						}
						

						ArrayList<Integer> newCraneTrajectory = new ArrayList<Integer>();

						for (int ct : cc.craneTrajectory) {
							newCraneTrajectory.add(ct);
						}

						cc.craneTrajectory = newCraneTrajectory;
						
						cc.jobTrajectory = jobNetworkTrajectory(cc, t, job, snap.craneStatus, snap);
						cc.craneTrajectory.addAll(cc.jobTrajectory);
						
						cc.leg1 = t + timeForNumberOfColumns((int) (helper.calculateTimeLeg_1(job, cc)), timeInterval);
						if (cc.leg1 <= job.availableTime) {
							cc.completion1 = job.availableTime +timeForNumberOfColumns(job.duration, timeInterval);
							cc.leg2 = job.availableTime +timeForNumberOfColumns((int) (job.duration + helper.calculateTimeLeg_2(job)), 
									timeInterval);
							cc.completeByTime = job.availableTime + timeForNumberOfColumns((int) (job.duration + helper.calculateTimeLeg_2(job)
									+ job.duration2), timeInterval);
						} else {
							cc.completion1 = t + timeForNumberOfColumns((int) (helper.calculateTimeLeg_1(job, cc) + job.duration), timeInterval);
							cc.leg2 = t + timeForNumberOfColumns((int) (helper.calculateTimeNoConflict(job, cc)), timeInterval);
							cc.completeByTime = t + timeForNumberOfColumns((int) (helper.calculateTimeNoConflict(job, cc) + job.duration2), timeInterval);
						}
						

						cc.column = cc.craneTrajectory.get(cc.craneTrajectory.size() - 1);

						setOfCompletedJobs.add(topCandidates.get(0).getKey());
						reward += cc.job.prio;
						if (cc.crane.initColumn == 0) {
							this.setOfCompletedJobsC1.add(topCandidates.get(0).getKey());
						} else {
							this.setOfCompletedJobsC2.add(topCandidates.get(0).getKey());
						}
					}
				}
			}
		}
		for (Job j : setOfCompletedJobs) {
			snap.completed(j);
		}

		return reward;
	}

	private List<Map.Entry<Job, Double>> computeFeasibleJobs(ArrayList<Job> setOfCompletedJobs, int currentTime,
			CraneStatus cc,
			CraneStatus[] craneStatus, Snapshot snapshot, Network network) {

		Helpers helper = new Helpers(this.problem);
		HashMap<Job, Double> candidateJobs = new HashMap<Job, Double>();

		for (Job j : problem.myJobs) {
			if (!setOfCompletedJobs.contains(j)) {

				int leg1 = currentTime + timeForNumberOfColumns((int) (helper.calculateTimeLeg_1(j, cc)), timeInterval);
				Node[] fromNodes = network.network.get(j)[0];
				double minDiff = Double.MAX_VALUE;
				Node closestNode = null;

				for (int i = 0; i < fromNodes.length; i++) 
				{
					var from = fromNodes[i];
					int diff = i - currentTime;

					if (diff >= 0 && diff <= minDiff && from != null && leg1 <= from.time) {
						minDiff = diff;
						closestNode = from;
						j.availableTime = from.time;
						break;
					} 
  
				}
			
				if (closestNode != null) {
					CraneStatus[] newCraneStatus = new CraneStatus[this.problem.crane.length];

					for (int i = 0; i < this.problem.crane.length; i++) {
						newCraneStatus[i] = new CraneStatus();

						if (snapshot.craneStatus[i].crane.id == cc.crane.id) {
							newCraneStatus[i].crane = cc.crane;
							newCraneStatus[i].idle = false;
							newCraneStatus[i].job = j;
							newCraneStatus[i].startTime = currentTime;
							newCraneStatus[i].leg1 = currentTime + timeForNumberOfColumns((int) (helper.calculateTimeLeg_1(j, cc)), timeInterval);

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
							newCraneStatus[i].jobTrajectory = jobNetworkTrajectory(newCraneStatus[i], currentTime, j,
									snapshot.craneStatus, snapshot);
							newCraneStatus[i].craneTrajectory.addAll(newCraneStatus[i].jobTrajectory);
							if (newCraneStatus[i].leg1 <= j.availableTime) {
								newCraneStatus[i].completion1 = j.availableTime +timeForNumberOfColumns(j.duration, timeInterval);
								newCraneStatus[i].leg2 = timeForNumberOfColumns((int) (j.availableTime + j.duration
										+ helper.calculateTimeLeg_2(j)), timeInterval);
								newCraneStatus[i].completeByTime = j.availableTime +timeForNumberOfColumns((int) (j.duration
										+ helper.calculateTimeLeg_2(j) + j.duration2), timeInterval);

							} else {
								newCraneStatus[i].completion1 = currentTime + timeForNumberOfColumns((int) (
										helper.calculateTimeLeg_1(j, newCraneStatus[i]) + j.duration), timeInterval);
								newCraneStatus[i].leg2 = currentTime + timeForNumberOfColumns((int) (
										helper.calculateTimeNoConflict(j, newCraneStatus[i])), timeInterval);
								newCraneStatus[i].completeByTime = currentTime + timeForNumberOfColumns((int) (
										helper.calculateTimeNoConflict(j, newCraneStatus[i]) + j.duration2), timeInterval);
							}

							
							if(!Helpers.checkCraneConflict(newCraneStatus[i],
									snapshot.craneStatus, newCraneStatus[i].craneTrajectory.size())) {
								newCraneStatus[i].craneTrajectory = helper.ConsideredCraneTrajectory(newCraneStatus[i],
										snapshot.craneStatus, newCraneStatus[i].craneTrajectory.size());								
								newCraneStatus[i].column = newCraneStatus[i].craneTrajectory
										.get(newCraneStatus[i].craneTrajectory.size() - 1);
							}
							
		
							

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
							
							if (Math.ceil((double) (newCraneStatus[i].craneTrajectory.size() - 1) / timeInterval) <= network.spaceSize) {
								if(!Helpers.checkCraneConflict(newCraneStatus[i],
										snapshot.craneStatus, newCraneStatus[i].craneTrajectory.size())) {									
									
									double jobReward = (double) newCraneStatus[i].job.prio
											/ (double) (newCraneStatus[i].completeByTime - newCraneStatus[i].startTime); 
																																																					 
									candidateJobs.put(j, jobReward);

								}
								
							}					
						}

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

	private ArrayList<Integer> jobNetworkTrajectory(CraneStatus cc, int currentTime, Job j, CraneStatus[] craneStatus,
			Snapshot snap) {

		Helpers helper = new Helpers(this.problem);
		cc.jobTrajectory = new ArrayList<Integer>();
		int a = 1;
		int b = 1;
		int c = 1;
		int d = 1;

		if (cc.craneTrajectory.size() != 0) {
			int startTimediff = (int) Math.abs((cc.craneTrajectory.size() - 1)  + helper.calculateTimeLeg_1(j, cc) -  j.availableTime * timeInterval); 
			
			for (CraneStatus oc : craneStatus) {
				if (!oc.myEquals(cc)) {									
					for (int l = 0; l < startTimediff; l++) {
						cc.jobTrajectory.add((int) (cc.column));
					}						
					
				}
			}

		}
		

		cc.leg1 = (int) (currentTime  + helper.calculateTimeLeg_1(j, cc));
		if (cc.leg1  <= j.availableTime ) {
			cc.completion1 = j.availableTime  + j.duration;
			cc.leg2 = (int) (j.availableTime + j.duration + helper.calculateTimeLeg_2(j));
			cc.completeByTime = (int) (j.availableTime  + j.duration + helper.calculateTimeLeg_2(j) + j.duration2);

		} else {
			cc.completion1 = (int) (currentTime + helper.calculateTimeLeg_1(j, cc) + j.duration);
			cc.leg2 = (int) (currentTime + helper.calculateTimeNoConflict(j, cc));
			cc.completeByTime = (int) (currentTime + helper.calculateTimeNoConflict(j, cc) + j.duration2);
		}

		for (int x = (int) (currentTime); x <= cc.completeByTime; x++) {
			if (x == 0) {
				if (cc.column < j.fromColumn) {
					if(cc.leg1  <= j.availableTime * timeInterval) {
						int diff = j.availableTime * timeInterval - cc.leg1;
						for (int i=0; i<= diff; i++) {
							cc.jobTrajectory.add((int) (cc.column));
						}
					} else {
						cc.jobTrajectory.add((int) (cc.column));
					} 
						
					
				} else {
					if(cc.leg1  <= j.availableTime * timeInterval) {
						int diff = j.availableTime * timeInterval - cc.leg1;
						for (int i=0; i<= diff; i++) {
							cc.jobTrajectory.add((int) (cc.column));
						}
					} else {
						cc.jobTrajectory.add((int) (cc.column));
					}
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
	
	private int timeForNumberOfColumns(int abs, int timeInterval) {
		abs = (int) Math.ceil((double)abs / timeInterval);
		return abs;
	}

}
