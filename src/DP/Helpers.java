package DP;

import java.util.ArrayList;

import data.Job;
import data.Problem;

public class Helpers {
	public Problem problem;

	public Helpers(Problem problem) {
		this.problem = problem;
	}

	public double calculateTimeLeg_1(Job j, CraneStatus cs) {
		double lat1 = cs.column;
		double lat2 = j.fromColumn;

		double leg1Distance = Math.abs(lat1 - lat2);
		double distance = leg1Distance;
		double travelTime = (distance / this.problem.speedInColumnsPerTimeUnit);

		return travelTime; 
	}

	public double calculateTimeLeg_2(Job j) {
		double lat1 = j.toColumn;
		double lat2 = j.fromColumn;

		double leg2Distance = Math.abs(lat1 - lat2);
		double distance = leg2Distance;
		double travelTime = (distance / this.problem.speedInColumnsPerTimeUnit);

		return travelTime;
	}

	public double calculateTimeNoConflict(Job j, CraneStatus cs) {
		double lat1 = cs.column;
		double lat2 = j.fromColumn;
		double lat3 = j.toColumn;

		double leg1Distance = Math.abs(lat1 - lat2);
		double leg2Distance = Math.abs(lat2 - lat3);
		double distance = leg1Distance + leg2Distance;

		double travelTime = (distance / this.problem.speedInColumnsPerTimeUnit) + j.duration;

		return travelTime;

	}

	public void PushForward(CraneStatus cc, int currentIndex) {
		int pushedTo = cc.craneTrajectory.get(currentIndex - 1) + 1;
		int previousLocation = cc.craneTrajectory.get(currentIndex - 1);
		cc.craneTrajectory.add(currentIndex, pushedTo);
		cc.craneTrajectory.add(currentIndex + 1, previousLocation);

	}

	public void PushBackward(CraneStatus cc, int currentIndex) {
		int pushedTo = cc.craneTrajectory.get(currentIndex - 1) - 1;
		int previousLocation = cc.craneTrajectory.get(currentIndex - 1);
		cc.craneTrajectory.add(currentIndex, pushedTo);
		cc.craneTrajectory.add(currentIndex + 1, previousLocation);

	}

	public void PreviousLocation(CraneStatus cc, int currentIndex) {
		int previousLocation = cc.craneTrajectory.get(currentIndex - 1);
		cc.craneTrajectory.add(currentIndex, previousLocation); // insert previous element to ith position
	}

	public void UpdateCraneTime(CraneStatus cc, int conflict) // confict meant time that two crane conflicts
	{
		if (conflict <= cc.leg1) {
			cc.leg1 += 1;
			cc.completion1 += 1;
			cc.leg2 += 1;
		}
		if (cc.leg1 < conflict && conflict <= cc.completion1) {
			cc.completion1 += 1;
			cc.leg2 += 1;
		}
		if (cc.completion1 < conflict && conflict <= cc.leg2) {
			cc.leg2 += 1;
		}
	}

	public void UpdateCraneTimeTwoStep(CraneStatus cc, int conflict) // confict meant time that two crane conflicts
	{
		if (conflict <= cc.leg1) {
			cc.leg1 += 2;
			cc.completion1 += 2;
			cc.leg2 += 2;
		}
		if (cc.leg1 < conflict && conflict <= cc.completion1) {
			cc.completion1 += 2;
			cc.leg2 += 2;
		}
		if (cc.completion1 < conflict && conflict <= cc.leg2) {
			cc.leg2 += 2;
		}
	}

	public ArrayList<Integer> ConsideredCraneTrajectory(CraneStatus cc, CraneStatus[] craneStatus, int lenght) {
		// this function will control other crane's trajectory is it's not idle,
		// otherwise no need to check.
		// if there is a conflict somewhere in the trajectory, this function properly
		// repair considered crane trajectory.
		for (CraneStatus oc : craneStatus) {
			if (!oc.myEquals(cc)) {
				if ((!oc.idle) && (cc.startTime >= oc.startTime)) // if other crane is busy and have prioritize on me
				{
					int minTrajectory = Math.min(cc.craneTrajectory.size(), oc.craneTrajectory.size());
					for (int i = 0; i <= minTrajectory - 1; i++) {
						int currentIndex = i;
						if ((currentIndex != 0)) // (currentIndex != cc.crane.initColumn)
						{
							if (cc.craneTrajectory.get(i) == oc.craneTrajectory.get(i)) // there is a conflict at
																						// element ith, should be
																						// adjusted acc. to oc^s
																						// direction
							{
								if (oc.craneTrajectory.get(i) > oc.craneTrajectory.get(i - 1)) {
									if (cc.craneTrajectory.get(i) == cc.craneTrajectory.get(i - 1)) {

										int previousLocation = cc.craneTrajectory.get(currentIndex - 1);
										UpdateCraneTime(cc, currentIndex);
										cc.craneTrajectory.add(currentIndex, previousLocation + 1); // insert previous
																									// element to ith
																									// position
										ConsideredCraneTrajectory(cc, craneStatus, lenght);
									} else {
										UpdateCraneTime(cc, currentIndex);
										PreviousLocation(cc, currentIndex);
										ConsideredCraneTrajectory(cc, craneStatus, lenght);
									}

								}
								if (oc.craneTrajectory.get(i) < oc.craneTrajectory.get(i - 1)) {
									if (cc.craneTrajectory.get(i) == cc.craneTrajectory.get(i - 1)) {
										UpdateCraneTime(cc, currentIndex);
										int previousLocation = cc.craneTrajectory.get(currentIndex - 1);
										cc.craneTrajectory.add(currentIndex, previousLocation - 1); // insert previous
																									// element to ith
																									// position
										ConsideredCraneTrajectory(cc, craneStatus, lenght);
									} else {
										UpdateCraneTime(cc, currentIndex);
										PreviousLocation(cc, currentIndex);
										ConsideredCraneTrajectory(cc, craneStatus, lenght);
									}

								}
								if (oc.craneTrajectory.get(i) == oc.craneTrajectory.get(i - 1)
										&& cc.craneTrajectory.get(i - 1) != oc.craneTrajectory.get(i - 1)) {
									UpdateCraneTime(cc, currentIndex);
									PreviousLocation(cc, currentIndex);
									ConsideredCraneTrajectory(cc, craneStatus, lenght);
								}

							}
							if (i != 0) {
								if ((cc.craneTrajectory.get(i) == oc.craneTrajectory.get(i - 1))
										&& (cc.craneTrajectory.get(i - 1) == oc.craneTrajectory.get(i))) {
									if (oc.craneTrajectory.size() - 1 == i) {
										if (oc.craneTrajectory.get(i) > oc.craneTrajectory.get(i - 1)) // <-- pushed
																										// backward
										{
											UpdateCraneTimeTwoStep(cc, currentIndex);
											PushForward(cc, currentIndex);
											ConsideredCraneTrajectory(cc, craneStatus, lenght);
										}
										if ((oc.craneTrajectory.get(i) < oc.craneTrajectory.get(i - 1))) // --> push
																											// forward
										{
											UpdateCraneTimeTwoStep(cc, currentIndex);
											PushBackward(cc, currentIndex);
											ConsideredCraneTrajectory(cc, craneStatus, lenght);

										}
									} else {
										if (oc.craneTrajectory.get(i) > oc.craneTrajectory.get(i + 1)) // <-- pushed
																										// forward
										{
											if (cc.craneTrajectory.get(i) < oc.craneTrajectory.get(i)) {
												UpdateCraneTimeTwoStep(cc, currentIndex);
												PushForward(cc, currentIndex);
												ConsideredCraneTrajectory(cc, craneStatus, lenght);
											}

											else {
												UpdateCraneTimeTwoStep(cc, currentIndex);
												PushBackward(cc, currentIndex);
												ConsideredCraneTrajectory(cc, craneStatus, lenght);
											}
										}
										if ((oc.craneTrajectory.get(i) < oc.craneTrajectory.get(i + 1))) // --> push
																											// forward
										{
											if (cc.craneTrajectory.get(i) < oc.craneTrajectory.get(i)) {
												UpdateCraneTimeTwoStep(cc, currentIndex);
												PushForward(cc, currentIndex);
												ConsideredCraneTrajectory(cc, craneStatus, lenght);
											}

											else {
												UpdateCraneTimeTwoStep(cc, currentIndex);
												PushBackward(cc, currentIndex);
												ConsideredCraneTrajectory(cc, craneStatus, lenght);
											}
										}
										if (oc.craneTrajectory.get(i + 1) == oc.craneTrajectory.get(i)) {
											if (cc.craneTrajectory.get(i) < oc.craneTrajectory.get(i)) // forward
											{
												UpdateCraneTimeTwoStep(cc, currentIndex);
												PushForward(cc, currentIndex);
												ConsideredCraneTrajectory(cc, craneStatus, lenght);
											}

											else {
												UpdateCraneTimeTwoStep(cc, currentIndex);
												PushBackward(cc, currentIndex);
												ConsideredCraneTrajectory(cc, craneStatus, lenght);
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
		return cc.craneTrajectory;
	}
	public static boolean checkCraneConflict(CraneStatus cc, CraneStatus[] craneStatus, int lenght) {
		for (CraneStatus oc : craneStatus) {
			if (!oc.myEquals(cc)) {
				if ((cc.startTime >= oc.startTime)) // if other crane is busy and have prioritize on me
				{
					int minTrajectory = Math.min(cc.craneTrajectory.size(), oc.craneTrajectory.size());
					for (int i = 0; i <= minTrajectory - 1; i++) 
					{
						int currentIndex = i;
						if ((currentIndex != 0))
						{
							if (cc.craneTrajectory.get(i) == oc.craneTrajectory.get(i)) 
							{
								return true;
							}
							if (i != 0) {
								if ((cc.craneTrajectory.get(i) == oc.craneTrajectory.get(i - 1))
										&& (cc.craneTrajectory.get(i - 1) == oc.craneTrajectory.get(i))) {
								return true;
								}
							}
						}
					}
				}
			}
		}
		return false;
	}
}
