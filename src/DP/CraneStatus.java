package DP;

import java.util.ArrayList;

import data.Crane;
import data.Job;

public class CraneStatus {
	public Crane crane;
	public boolean idle;
	public boolean inBetween;
	public int column;
	public int row;
	public int completeByTime;
	public int leg1;
	public int leg2;
	public int completion1;
	public Job job;
	public ArrayList<Integer> craneTrajectory;
	public ArrayList<Integer> jobTrajectory;
	public int startTime;

	public boolean myEquals(CraneStatus cs) {
		return this.crane.id == cs.crane.id;
	}
}
