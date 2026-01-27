package parameters;

public class ProblemParameters {
	// parameters for problem setting, random generation
	public int speedTimeUnitsforOneColumnTravel = 1;
	public int numberOfTimePoints = 30;
	public int[] numberOfJobs = { 12, 25, 12, 20, 14, 30, 10 }; // 30, 25, 16, 14, 12, 10
	public int[] numberOfColumns = { 25, 50 };
	public int[] seedNo = { 2, 4, 5, 10, 13, 15, 20, 87, 65, 55 };
	public int numberOfCranes = 2;
	public String[] duration = { "s" }; // if short (1-3), elif long (3-5)
	public int[] numberOfJobList;
	public int timeLimit = 3600 ; // 360000000
}
 