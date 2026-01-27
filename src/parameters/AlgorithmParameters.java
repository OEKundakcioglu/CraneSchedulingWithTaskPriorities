package parameters;

public class AlgorithmParameters {

	// ALGORITHM PARAMETERS
	public enum caseSet {
		Exact, Greedy, GreedyConflict, Stage, SimpleGreedy, StageConflict, ADP,
		 ADP_RDP, TS, GreedyNetwork, TSBounding
	}

	// Stage coefficient
	public double[] stageCoeff = { 0.2, 0.4, 0.6, 0.8 };


	public enum tsCaseSet {
		ConflictFree, BaseModel, ExtendedModel, HeuristicsNetwork
	}

	public AlgorithmParameters() {
	}

}
   