module CraneScheduling {
	opens data;
	opens parameters;
	opens heuristics;
	opens tsnSolution;
	opens DP;
	requires java.base;
	requires java.desktop;
	requires com.google.gson;
	requires com.google.common;
	requires gurobi;
	requires java.logging;
	

}