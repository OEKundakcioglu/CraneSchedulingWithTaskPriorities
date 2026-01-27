package DP;

import java.util.ArrayList;

import data.Job;

public class Snapshot {
	private ArrayList<Job> setOfCompletedJobs;
	private ArrayList<Job> setOfNonCompletedJobs;

	public ArrayList<Job> getSetOfCompletedJobs() {
		return setOfCompletedJobs;
	}

	public ArrayList<Job> getSetOfNonCompletedJobs() {
		return setOfNonCompletedJobs;
	}

	private void addSetOfCompletedJobs(Job newlyCompletedJob) {
		this.setOfCompletedJobs.add(newlyCompletedJob);
	}

	public CraneStatus[] craneStatus;
	public int currentTime;
	// public double tasksCentroid = centroidOfTasks();

	public Snapshot(ArrayList<Job> setOfCompletedJobs, CraneStatus[] craneStatus, int currentTime, Job[] myJobs) {
		this.setOfNonCompletedJobs = new ArrayList<Job>();
		for (Job j : myJobs) {
			if (!setOfCompletedJobs.contains(j))
				this.setOfNonCompletedJobs.add(j);
		}

		this.setOfCompletedJobs = new ArrayList<Job>();
		for (Job j : setOfCompletedJobs) {
			this.addSetOfCompletedJobs(j);
		}
		this.craneStatus = new CraneStatus[craneStatus.length];
		for (int i = 0; i < craneStatus.length; i++) {
			this.craneStatus[i] = new CraneStatus();
			this.craneStatus[i].crane = craneStatus[i].crane;
			this.craneStatus[i].idle = craneStatus[i].idle;
			this.craneStatus[i].column = craneStatus[i].column;
			this.craneStatus[i].row = craneStatus[i].row;
			this.craneStatus[i].completeByTime = craneStatus[i].completeByTime;
			this.craneStatus[i].job = craneStatus[i].job;
			this.craneStatus[i].leg1 = craneStatus[i].leg1;
			this.craneStatus[i].leg2 = craneStatus[i].leg2;
			this.craneStatus[i].completion1 = craneStatus[i].completion1;
			this.craneStatus[i].craneTrajectory = craneStatus[i].craneTrajectory;
			this.craneStatus[i].jobTrajectory = craneStatus[i].jobTrajectory;
			this.craneStatus[i].startTime = craneStatus[i].startTime;
		}
		this.currentTime = currentTime;

	}

	public void completed(Job j) {
		this.addSetOfCompletedJobs(j);
		this.removeFromNonComplete(j);
	}

	private void removeFromNonComplete(Job j) {
		this.setOfNonCompletedJobs.remove(j);
	}

}
