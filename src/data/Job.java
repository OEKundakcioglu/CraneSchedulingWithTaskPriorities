package data;

import java.util.HashMap;
import java.util.Random;

public class Job {
	public int jobID;
	public int availableTime;
	public int fromColumn;
	public double priority;
	public int prio;
	public int duration; // fromDuration
	public int toColumn;
	public int duration2; // toDuration
	private int maxPriority = 4;
	private double speedInColumnsPerTimeUnit;
	public HashMap<Integer, Double> estimatedDurations;
	
	public Job() {
    }
	

	public Job(int id, Random random, int timePoints, int fromColumns, int toColumns, int duration,
			double speedInColumnsPerTimeUnit) {
		this.jobID = id;
		this.availableTime = random.nextInt(1);
		this.fromColumn = random.nextInt(fromColumns);
		this.priority = 1 + random.nextInt(maxPriority);
		this.prio = (int) (this.priority);
		this.toColumn = random.nextInt(toColumns);
		this.speedInColumnsPerTimeUnit = speedInColumnsPerTimeUnit;
		while (this.toColumn == this.fromColumn) {
			this.toColumn = random.nextInt(toColumns);
		}
		if (duration <= 3) {
			this.duration = random.nextInt(3) + 1;
			this.duration2 = random.nextInt(3) + 1;
		}
		if (duration > 3) {
			this.duration = random.nextInt(3) + 3;
			this.duration2 = random.nextInt(3) + 3;
		}

		estimateDurations(speedInColumnsPerTimeUnit);

	}

	private void estimateDurations(double speedInColumnsPerTimeUnit) {
		this.estimatedDurations = new HashMap<Integer, Double>();
		this.estimatedDurations.put(this.fromColumn, (double) this.duration);
		this.estimatedDurations.put(this.toColumn, (double) this.duration2);
		int smaller = this.fromColumn;
		int larger = this.toColumn;

		if (this.fromColumn > this.toColumn) {
			smaller = this.toColumn;
			larger = this.fromColumn;

		}

		for (int i = smaller + 1; i < larger; i++) {
			this.estimatedDurations.put(i, 1 / speedInColumnsPerTimeUnit);
		}

	}

	public double getPriority() {
		return priority;
	}

	public double getJobID() {
		return jobID;
	}

}
