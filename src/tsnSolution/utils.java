package tsnSolution;

import java.util.ArrayList;

import data.Job;
import data.Problem;

public class utils {
	public Problem problem;
	public boolean intervalConflict;

	public utils(Problem problem) {
		this.problem = problem;

	}

	// Check whether there is conflict between jobs
	public static boolean checkCraneConflict(Job j1, Job j2, int timeInterval) {
		ArrayList<Integer> jobTrajectory1 = createJobTrajectory(j1, timeInterval);
		ArrayList<Integer> jobTrajectory2 = createJobTrajectory(j2, timeInterval);

		// Check if there is any common element between the two trajectories
		for (Integer location1 : jobTrajectory1) {
			for (Integer location2 : jobTrajectory2) {
				if (location1.equals(location2)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean checkConflicTime(Node fromNode1, Node fromNode2, Node to1, Node to2, Network network) {
		// check starting and finishing time range of jobs whether they are conflicting

		ArrayList<Integer> jobTrajectory1 = createJobTrajectory(fromNode1.job, problem.timeInterval);
		ArrayList<Integer> jobTrajectory2 = createJobTrajectory(fromNode2.job, problem.timeInterval);

		boolean checkTimeRange = (to1.time > fromNode2.time) && (to2.time >= fromNode1.time);

		if (!checkTimeRange) {   
			return false;

		}

		if (checkTimeRange) {
			if(to2.time <= to1.time) {
				if(checkWait(jobTrajectory2, problem.timeInterval)) {
					ArrayList<Integer> waitedJobTrajectory2 = UpdateJobTrajectory(jobTrajectory2, fromNode2.job, problem.timeInterval);
					
					int conflictLower = Math.max(fromNode1.time, fromNode2.time)* problem.timeInterval;
					int conflictUpper = Math.min(to1.time, to2.time)* problem.timeInterval;
					for (int t = conflictLower; t <= conflictUpper; t++) {
						int index1 = t - fromNode1.time * problem.timeInterval; // Adjusted index for jobTrajectory1
						int index2 = t - fromNode2.time * problem.timeInterval;

						if (index1 >= 0 && index1 < jobTrajectory1.size() && index2 >= 0 && index2 < waitedJobTrajectory2.size()) {

							int location1 = jobTrajectory1.get(index1);
							int location2 = waitedJobTrajectory2.get(index2);

							if (location1 == location2) {
								if(index2 > jobTrajectory2.size()) {
									this.intervalConflict = true;
									return true;
								} else {
									this.intervalConflict = false;
									return true; // Conflict found
								}
								
							}

							if ((index1 == 0 && index2 != 0) || (index1 != 0 && index2 == 0)) {
								if (index1 == 0 && index2 != 0) {

									int prevLocation2 = waitedJobTrajectory2.get(index2 - 1);
									if (location1 == prevLocation2) {	
										
										int nextLocation1 = jobTrajectory1.get(jobTrajectory1.size() - 1); // Last element of Crane 2's trajectory
							            if ((location1 - nextLocation1) * (prevLocation2 - location2) > 0) {
							                return false; // No conflict
							            } else {
							                if(index2 > jobTrajectory2.size()) {
							                	this.intervalConflict = true;
												return true;
											} else {
												this.intervalConflict = false;
												return true; // Conflict: Crane 2 is following Crane 1's path but with a delay
											}
							            }
									}

								}
								if (index1 != 0 && index2 == 0) {
									int prevLocation1 = jobTrajectory1.get(index1 - 1);
									if (prevLocation1 == location2) {
										if(index2 > jobTrajectory2.size()) {
											this.intervalConflict = true;
											return true;
										} else {
											this.intervalConflict = false;
											return true; // Special case: conflicts at the starting time
										}
									} 
								}
							}

							if (index1 > 0 && index2 > 0) {
								int prevLocation1 = jobTrajectory1.get(index1 - 1);
								int prevLocation2 = waitedJobTrajectory2.get(index2 - 1);

								if (location1 == prevLocation2 && prevLocation1 == location2) { 
									if(index2 > jobTrajectory2.size()) {
										this.intervalConflict = true;
										return true;
									} else {
										this.intervalConflict = false;
										return true; // Overlapping segments, conflict found
									}
								}
							}
						}
					}
				}
				
				else {
					this.intervalConflict = false;
					int conflictLower = Math.max(fromNode1.time, fromNode2.time)* problem.timeInterval;
					int conflictUpper = Math.min(to1.time, to2.time)* problem.timeInterval;
					for (int t = conflictLower; t <= conflictUpper; t++) {
						int index1 = t - fromNode1.time * problem.timeInterval; // Adjusted index for jobTrajectory1
						int index2 = t - fromNode2.time * problem.timeInterval;

						if (index1 >= 0 && index1 < jobTrajectory1.size() && index2 >= 0 && index2 < jobTrajectory2.size()) {

							int location1 = jobTrajectory1.get(index1);
							int location2 = jobTrajectory2.get(index2);

							if (location1 == location2) {
								return true; // Conflict found
							}

							if ((index1 == 0 && index2 != 0) || (index1 != 0 && index2 == 0)) {
								if (index1 == 0 && index2 != 0) {

									int prevLocation2 = jobTrajectory2.get(index2 - 1);
									if (location1 == prevLocation2) {	
										
										int nextLocation1 = jobTrajectory1.get(jobTrajectory1.size() - 1); // Last element of Crane 2's trajectory
							            if ((location1 - nextLocation1) * (prevLocation2 - location2) > 0) {
							                return false; // No conflict
							            } else {
							                return true; // Conflict: Crane 2 is following Crane 1's path but with a delay
							            }
									}

								}
								if (index1 != 0 && index2 == 0) {
									int prevLocation1 = jobTrajectory1.get(index1 - 1);
									if (prevLocation1 == location2) {
										return true; // Special case: conflicts at the starting time
									} 
								}
							}

							if (index1 > 0 && index2 > 0) {
								int prevLocation1 = jobTrajectory1.get(index1 - 1);
								int prevLocation2 = jobTrajectory2.get(index2 - 1);

								if (location1 == prevLocation2 && prevLocation1 == location2) { 
									return true; // Overlapping segments, conflict found
								}
							}

						}

					}
				}

			}
			else {
				if(checkWait(jobTrajectory1, problem.timeInterval)) {
					ArrayList<Integer> waitedJobTrajectory1 = UpdateJobTrajectory(jobTrajectory1, fromNode1.job, problem.timeInterval);
					
					int conflictLower = Math.max(fromNode1.time, fromNode2.time)* problem.timeInterval;
					int conflictUpper = Math.min(to1.time, to2.time)* problem.timeInterval;
					for (int t = conflictLower; t <= conflictUpper; t++) {
						int index1 = t - fromNode1.time * problem.timeInterval; // Adjusted index for jobTrajectory1
						int index2 = t - fromNode2.time * problem.timeInterval;

						if (index1 >= 0 && index2 < jobTrajectory2.size() && index2 >= 0 && index1 < waitedJobTrajectory1.size()) {

							int location1 = waitedJobTrajectory1.get(index1);
							int location2 = jobTrajectory2.get(index2);

							if (location1 == location2) {
								if(index2 > jobTrajectory1.size()) {
									this.intervalConflict = true;
									return true;
								} else {
									this.intervalConflict = false;
									return true; // Conflict found
								}
								
							}

							if ((index1 == 0 && index2 != 0) || (index1 != 0 && index2 == 0)) {
								if (index1 == 0 && index2 != 0) {

									int prevLocation2 = jobTrajectory2.get(index2 - 1);
									if (location1 == prevLocation2) {	
										
										int nextLocation1 = waitedJobTrajectory1.get(waitedJobTrajectory1.size() - 1); // Last element of Crane 2's trajectory
							            if ((location1 - nextLocation1) * (prevLocation2 - location2) > 0) {
							                return false; // No conflict
							            } else {
							                if(index2 > jobTrajectory1.size()) {
							                	this.intervalConflict = true;
												return true;
											} else {
												this.intervalConflict = false;
												return true; // Conflict: Crane 2 is following Crane 1's path but with a delay
											}
							            }
									}

								}
								if (index1 != 0 && index2 == 0) {
									int prevLocation1 = jobTrajectory1.get(index1 - 1);
									if (prevLocation1 == location2) {
										if(index2 > jobTrajectory1.size()) {
											this.intervalConflict = true;
											return true;
										} else {
											this.intervalConflict = false;
											return true; // Special case: conflicts at the starting time
										}
									} 
								}
							}

							if (index1 > 0 && index2 > 0) {
								int prevLocation1 = waitedJobTrajectory1.get(index1 - 1);
								int prevLocation2 = jobTrajectory2.get(index2 - 1);

								if (location1 == prevLocation2 && prevLocation1 == location2) { 
									if(index2 > jobTrajectory1.size()) {
										this.intervalConflict = true;
										return true;
									} else {
										this.intervalConflict = false;
										return true; // Overlapping segments, conflict found
									}
								}
							}
						}
					}
				}
				
				else {
					this.intervalConflict = false;
					int conflictLower = Math.max(fromNode1.time, fromNode2.time)* problem.timeInterval;
					int conflictUpper = Math.min(to1.time, to2.time)* problem.timeInterval;
					for (int t = conflictLower; t <= conflictUpper; t++) {
						int index1 = t - fromNode1.time * problem.timeInterval; // Adjusted index for jobTrajectory1
						int index2 = t - fromNode2.time * problem.timeInterval;

						if (index1 >= 0 && index1 < jobTrajectory1.size() && index2 >= 0 && index2 < jobTrajectory2.size()) {

							int location1 = jobTrajectory1.get(index1);
							int location2 = jobTrajectory2.get(index2);

							if (location1 == location2) {
								return true; // Conflict found
							}

							if ((index1 == 0 && index2 != 0) || (index1 != 0 && index2 == 0)) {
								if (index1 == 0 && index2 != 0) {

									int prevLocation2 = jobTrajectory2.get(index2 - 1);
									if (location1 == prevLocation2) {	
										
										int nextLocation1 = jobTrajectory1.get(jobTrajectory1.size() - 1); // Last element of Crane 2's trajectory
							            if ((location1 - nextLocation1) * (prevLocation2 - location2) > 0) {
							                return false; // No conflict
							            } else {
							                return true; // Conflict: Crane 2 is following Crane 1's path but with a delay
							            }
									}

								}
								if (index1 != 0 && index2 == 0) {
									int prevLocation1 = jobTrajectory1.get(index1 - 1);
									if (prevLocation1 == location2) {
										return true; // Special case: conflicts at the starting time
									} 
								}
							}

							if (index1 > 0 && index2 > 0) {
								int prevLocation1 = jobTrajectory1.get(index1 - 1);
								int prevLocation2 = jobTrajectory2.get(index2 - 1);

								if (location1 == prevLocation2 && prevLocation1 == location2) { 
									return true; // Overlapping segments, conflict found
								}
							}

						}

					}
				}
			}
						

		}
		this.intervalConflict = false;
		return false;
	}
	
	public static boolean checkConflictCornerTime(Node fromNode1, Node fromNode2, Node to1, Node to2, int timeInterval) {
		
		ArrayList<Integer> jobTrajectory1 = createJobTrajectory(fromNode1.job, timeInterval);
		ArrayList<Integer> jobTrajectory2 = createJobTrajectory(fromNode2.job, timeInterval);

		boolean checkTimeRange = (to1.time > fromNode2.time) && (to2.time >= fromNode1.time);

		if (!checkTimeRange) {
			return false;

		}

		
		int conflictLower = Math.max(fromNode1.time, fromNode2.time)* timeInterval;
		int conflictUpper = Math.min(to1.time, to2.time)* timeInterval;
		for (int t = conflictLower; t <= conflictUpper; t++) {
			int index1 = t - fromNode1.time * timeInterval; // Adjusted index for jobTrajectory1
			int index2 = t - fromNode2.time * timeInterval;

			if (index1 >= 0 && index1 < jobTrajectory1.size() && index2 >= 0 && index2 < jobTrajectory2.size()) {

				int location1 = jobTrajectory1.get(index1);
				int location2 = jobTrajectory2.get(index2);

				if (location1 == location2) {
					return true; // Conflict found
				}

				if ((index1 == 0 && index2 != 0) || (index1 != 0 && index2 == 0)) {
					if (index1 == 0 && index2 != 0) {

						int prevLocation2 = jobTrajectory2.get(index2 - 1);
						if (location1 == prevLocation2) {	
							
							int nextLocation1 = jobTrajectory1.get(jobTrajectory1.size() - 1); // Last element of Crane 2's trajectory
				            if ((location1 - nextLocation1) * (prevLocation2 - location2) > 0) {
				                return false; // No conflict
				            } else {
				                return true; // Conflict: Crane 2 is following Crane 1's path but with a delay
				            }
						}

					}
					if (index1 != 0 && index2 == 0) {
						int prevLocation1 = jobTrajectory1.get(index1 - 1);
						if (prevLocation1 == location2) {
							return true; // Special case: conflicts at the starting time
						} 
					}
				}

				if (index1 > 0 && index2 > 0) {
					int prevLocation1 = jobTrajectory1.get(index1 - 1);
					int prevLocation2 = jobTrajectory2.get(index2 - 1);

					if (location1 == prevLocation2 && prevLocation1 == location2) { 
						return true; // Overlapping segments, conflict found
					}
				}

			}

		}
		return false;
	}			


	public static boolean checkConflicTravelArcs(ArrayList<Integer> travelTrajectory1,
			ArrayList<Integer> travelTrajectory2) {

		// Check if there is any common element between the two trajectories
		if (travelTrajectory1 != null && travelTrajectory2 != null) {
			for (Integer location1 : travelTrajectory1) {
				for (Integer location2 : travelTrajectory2) {
					if (location1.equals(location2)) {
						return true; // Conflict found
					}
				}
			}

		}

		return false; // No conflict
	}

	public static boolean checkConflicTravelTime(Node fromNode1, Node fromNode2, Node to1, Node to2, int timeInterval) {
		// check starting and finishing time range of jobs whether they are conflicting

		ArrayList<Integer> jobTrajectory1 = createJobTrajectory(fromNode1.job, timeInterval);
		ArrayList<Integer> jobTrajectory2 = createTravelTrajectory(fromNode2.locationId, to2.locationId);

		boolean checkTimeRange = (to1.time > fromNode2.time) && (to2.time >= fromNode1.time);

		if (!checkTimeRange) {  
			return false;

		}

		if (checkTimeRange) {

		
			int conflictLower = Math.max(fromNode1.time, fromNode2.time) * timeInterval;
			int conflictUpper = Math.min(to1.time, to2.time) * timeInterval;
						
			for (int t = conflictLower; t <= conflictUpper; t++) {
				int index1; int index2;
				
				if(jobTrajectory2.size() < timeInterval){
					
					index1 = t - fromNode1.time * timeInterval + timeInterval - jobTrajectory2.size() + 1; 
					index2 = t - fromNode2.time * timeInterval;
				}
				else {
					
					index1 = t - fromNode1.time * timeInterval; 
					index2 = t - fromNode2.time * timeInterval;
				}
				

				if (index1 >= 0 && index1 < jobTrajectory1.size() && index2 >= 0 && index2 < jobTrajectory2.size()) {

					int location1 = jobTrajectory1.get(index1);
					int location2 = jobTrajectory2.get(index2);

					if (location1 == location2) {
						return true; // Conflict found
					}

					if ((index1 == 0 && index2 != 0) || (index1 != 0 && index2 == 0)) {
						if (index1 == 0 && index2 != 0) {

							int prevLocation2 = jobTrajectory2.get(index2 - 1);
							if (location1 == prevLocation2) {	
								if((jobTrajectory1.get(index1) - jobTrajectory1.get(jobTrajectory1.size()-1) < 0 &&  jobTrajectory2.get(index2) - jobTrajectory2.get(jobTrajectory2.size()-1) > 0 )
										|| (jobTrajectory1.get(index1) - jobTrajectory1.get(jobTrajectory1.size()-1) > 0 &&  jobTrajectory2.get(index2) - jobTrajectory2.get(jobTrajectory2.size()-1) < 0 )) {
									return false; 
								}
								if((location1 - jobTrajectory1.get(index1 + 1)) >  (prevLocation2 - location2 )) {
									return false; 
								}
								else {
									return true; // Special case: conflicts at the starting time ayni yon ise prob yok
								}
							}

						}

					}

					if (index1 > 0 && index2 > 0) {
						int prevLocation1 = jobTrajectory1.get(index1 - 1);
						int prevLocation2 = jobTrajectory2.get(index2 - 1);

						if (location1 == prevLocation2 && prevLocation1 == location2) {
							
							return true;
						}
					}

				}

			}

		}
		return false;
	}

	public static boolean checkConflicSourceTime(Node fromNode1, Node fromNode2, Node to1, Node to2, int timeInterval) {
		// check starting and finishing time range of jobs whether they are conflicting

		ArrayList<Integer> jobTrajectory1 = createJobTrajectory(fromNode1.job, timeInterval);
		ArrayList<Integer> jobTrajectory2 = createTravelTrajectory(fromNode2.locationId, to2.locationId);

		boolean checkTimeRange = (to1.time > fromNode2.time) && (to2.time >= fromNode1.time);

		if (!checkTimeRange) {
			return false;

		}

		if (checkTimeRange) {
			int conflictLower = Math.max(fromNode1.time, fromNode2.time);
			int conflictUpper = Math.min(to1.time, to2.time);
			for (int t = conflictLower; t <= conflictUpper; t++) {
				int index1 = t - fromNode1.time; // Adjusted index for jobTrajectory1
				int from2time = Math.abs(Math.abs(fromNode2.locationId - to2.locationId) - to2.time);
				int index2 = t - from2time;

				if (index1 >= 0 && index1 < jobTrajectory1.size() && index2 >= 0 && index2 < jobTrajectory2.size()) {

					int location1 = jobTrajectory1.get(index1);
					int location2 = jobTrajectory2.get(index2);

					if (location1 == location2) {
						return true; // Conflict found
					}

					if ((index1 == 0 && index2 != 0) || (index1 != 0 && index2 == 0)) {
						if (index1 == 0 && index2 != 0) {

							int prevLocation2 = jobTrajectory2.get(index2 - 1);
							if (location1 == prevLocation2) {
								return true; // Special case: conflicts at the starting time
							}

						}
						if (index1 != 0 && index2 == 0) {
							int prevLocation1 = jobTrajectory1.get(index1 - 1);
							if (prevLocation1 == location2) {
								return true; // Special case: conflicts at the starting time
							}

						}

					}

					if (index1 > 0 && index2 > 0) {
						int prevLocation1 = jobTrajectory1.get(index1 - 1);
						int prevLocation2 = jobTrajectory2.get(index2 - 1);

						if (location1 == prevLocation2 && prevLocation1 == location2) {
							return true; // Overlapping segments, conflict found
						}
					}

				}

			}

		}
		return false;
	}
	
	public static ArrayList<Integer> createGroundTrajectory(Node fromNode, Node to, int timeInterval){
		ArrayList<Integer> groundTrajectory = new ArrayList<Integer>();
		 if(fromNode.locationId == to.locationId) {
			 int idleTime = (to.time - fromNode.time) * timeInterval;
			 for (int i=0; i< idleTime; i++) {
				 groundTrajectory.add(to.locationId);			    	
			 }
			 return groundTrajectory;			 
		}

		    return null;
		
	}
	
	public static boolean checkConflicGroundTime(Node fromNode1, Node fromNode2, Node to1, Node to2, int timeInterval) {
		// check starting and finishing time range of jobs whether they are conflicting

		ArrayList<Integer> jobTrajectory1 = createJobTrajectory(fromNode1.job, timeInterval);
		ArrayList<Integer> jobTrajectory2 = createGroundTrajectory(fromNode2, to2, timeInterval);

		boolean checkTimeRange = (to1.time > fromNode2.time) && (to2.time >= fromNode1.time);

		if (!checkTimeRange) {
			return false;

		}

		if (checkTimeRange) {
			int conflictLower = Math.max(fromNode1.time, fromNode2.time) * timeInterval;
			int conflictUpper = Math.min(to1.time, to2.time) * timeInterval;
			for (int t = conflictLower; t <= conflictUpper; t++) {
				int index1 = t - fromNode1.time * timeInterval; // Adjusted index for jobTrajectory1
				int index2 = t - fromNode2.time * timeInterval;

				if (index1 >= 0 && index1 < jobTrajectory1.size() && index2 >= 0 && index2 < jobTrajectory2.size()) {

					int location1 = jobTrajectory1.get(index1);
					int location2 = jobTrajectory2.get(index2);

					if (location1 == location2) {
						return true; // Conflict found
					}

					if ((index1 == 0 && index2 != 0) || (index1 != 0 && index2 == 0)) {
						if (index1 == 0 && index2 != 0) {

							int prevLocation2 = jobTrajectory2.get(index2 - 1);
							if (location1 == prevLocation2) {	
								if((jobTrajectory1.get(index1) - jobTrajectory1.get(jobTrajectory1.size()-1) < 0 &&  jobTrajectory2.get(index2) - jobTrajectory2.get(jobTrajectory2.size()-1) > 0 )
										|| (jobTrajectory1.get(index1) - jobTrajectory1.get(jobTrajectory1.size()-1) > 0 &&  jobTrajectory2.get(index2) - jobTrajectory2.get(jobTrajectory2.size()-1) < 0 )) {
									return false; 
								}
								if((location1 - jobTrajectory1.get(index1 + 1)) >  (prevLocation2 - location2 )) {
									return false; 
								}
								else {
									return true; // Special case: conflicts at the starting time ayni yon ise prob yok
								}
							}

						}

					}

					if (index1 > 0 && index2 > 0) {
						int prevLocation1 = jobTrajectory1.get(index1 - 1);
						int prevLocation2 = jobTrajectory2.get(index2 - 1);

						if (location1 == prevLocation2 && prevLocation1 == location2) {
							
							return true;
						}
					}

				}

			}

		}
		return false;
	}
	
	public Integer conflictIndex(ArrayList<Integer> jobTrajectory1, ArrayList<Integer> jobTrajectory2) {
		for (int i = 0; i < Math.min(jobTrajectory1.size(), jobTrajectory2.size()); i++) {
			if (jobTrajectory1.get(i).equals(jobTrajectory2.get(i))) {
				return i;
			}
		}
		return null;
	}

	public boolean checkConflict(ArrayList<Integer> jobTrajectory1, ArrayList<Integer> jobTrajectory2) {
		for (int i = 0; i < Math.min(jobTrajectory1.size(), jobTrajectory2.size()); i++) {
			if (jobTrajectory1.get(i).equals(jobTrajectory2.get(i))) {
				return true;
			}
		}
		return false;
	}

	public static ArrayList<Integer> createTravelTrajectory(int loc1, int loc2) {
		ArrayList<Integer> travelTrajectory = new ArrayList<Integer>();

		if (loc1 > loc2) {
			for (int i = loc1; i >= loc2; i--) {
				travelTrajectory.add(i);
			}
			return travelTrajectory;
		}

		if (loc2 > loc1) {
			for (int i = loc1; i <= loc2; i++) {
				travelTrajectory.add(i);
			}
			return travelTrajectory;
		}

		if (loc1 == loc2) {
			travelTrajectory.add(loc1);
			return travelTrajectory;
		}

		return null;
	}

	
	public static ArrayList<Integer> createJobTrajectory(Job j, int timeInterval) {
		ArrayList<Integer> jobTrajectory = new ArrayList<Integer>();

		for (int i = 0; i <= j.duration; i++) {
			jobTrajectory.add(j.fromColumn);

		}

		if (j.fromColumn < j.toColumn) {
			for (int i = j.fromColumn + 1; i <= j.toColumn; i++) {
				jobTrajectory.add(i);
			}
		}

		if (j.fromColumn > j.toColumn) {
			for (int i = j.fromColumn - 1; i >= j.toColumn; i--) {
				jobTrajectory.add(i);
			}
		}

		for (int i = 0; i < j.duration2; i++) {
			jobTrajectory.add(j.toColumn);

		}
				   
		return jobTrajectory;
	}
	
	public static boolean checkWait(ArrayList<Integer> jobTrajectory, int timeInterval) {
		
		int waitTime = jobTrajectory.size() % timeInterval;
		if(waitTime != 0) {
			 Math.abs(waitTime - timeInterval);
		}
		
		if(waitTime > 0) {
			return true;
		} else {
			return false;
		}
		
	}
	
	public ArrayList<Integer> UpdateJobTrajectory(ArrayList<Integer> jobTrajectory, Job j, int timeInterval){
		int waitTime = jobTrajectory.size() % timeInterval;
		if(waitTime != 0) {
			 Math.abs(waitTime - timeInterval);
		}  
		for (int l = 0; l < waitTime; l++) {
			jobTrajectory.add(j.toColumn);
		}
		
		return jobTrajectory;
	}


	public ArrayList<Integer> createPushTrajectory(int loc1, Job j, int loc3) {

		ArrayList<Integer> pushTrajectory = new ArrayList<Integer>();

		if (loc1 > j.fromColumn) {
			for (int i = loc1; i >= j.fromColumn; i--) {
				pushTrajectory.add(i);
			}

			for (int i = 0; i <= j.duration; i++) {
				pushTrajectory.add(j.fromColumn);

			}

			if (j.fromColumn < j.toColumn) {
				for (int i = j.fromColumn + 1; i <= j.toColumn; i++) {
					pushTrajectory.add(i);
				}
			}

			if (j.fromColumn > j.toColumn) {
				for (int i = j.fromColumn - 1; i >= j.toColumn; i--) {
					pushTrajectory.add(i);
				}
			}

			for (int i = 0; i < j.duration2; i++) {
				pushTrajectory.add(j.toColumn);

			}

			return pushTrajectory;
		}

		if (j.fromColumn > loc1) {
			for (int i = loc1; i <= j.fromColumn; i++) { // Corrected loop condition
				pushTrajectory.add(i);
			}

			for (int i = 0; i <= j.duration; i++) {
				pushTrajectory.add(j.fromColumn);

			}

			if (j.fromColumn < j.toColumn) {
				for (int i = j.fromColumn + 1; i <= j.toColumn; i++) {
					pushTrajectory.add(i);
				}
			}

			if (j.fromColumn > j.toColumn) {
				for (int i = j.fromColumn - 1; i >= j.toColumn; i--) {
					pushTrajectory.add(i);
				}
			}

			for (int i = 0; i < j.duration2; i++) {
				pushTrajectory.add(j.toColumn);

			}

			return pushTrajectory;
		}

		return null;

	}


}
