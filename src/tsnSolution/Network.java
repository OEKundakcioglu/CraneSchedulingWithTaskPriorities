package tsnSolution;

import java.util.ArrayList;
import java.util.HashMap;
import data.Crane;
import data.Job;
import data.Problem;

public class Network {

	public Problem problem;
	public int timeInterval;
	public ArrayList<Node> taskNodes;
	public Node source1;
	public Node source2;
	public Node sink;
	public Enum tsCase;
	public HashMap<Job, Node[][]> network; // all jobs are keys, Node[2][T] in size..
	public int spaceSize;

	public Network(Problem problem, Enum tsCase) {
		this.problem = problem;
		this.tsCase = tsCase;

		sink = createSinkNode();
		network = createTSNode(problem.timeInterval, sink);
		source1 = createSourceNode(problem.crane[0]);
		source2 = createSourceNode(problem.crane[1]);

	}

	public Node createSourceNode(Crane crane) {
		ArrayList<Node> outgoing = new ArrayList<Node>();
		Node source = new Node(crane.initColumn, 0, "SourceNode" + crane.id, outgoing, 0, null);

		for (Job myJob : problem.myJobs) {
			Node[] myNodes = network.get(myJob)[0];
			for (int t = 0; t < myNodes.length; t++) {
				if (myNodes[t] != null) {
					int travel = (int) Math.ceil((double)(Math.abs(myJob.fromColumn - crane.initColumn)) / problem.timeInterval);
					myNodes[t].job = myJob;
					if ((t  <= myNodes.length) && (t  < this.spaceSize) && (travel <= t)) {
						if (myNodes[t ] != null) {
							source.addOutgoing(myNodes[t]);
						}
					}
				}
			}
		}
		return source;
	}

	// Create task nodes using all locations and time points using time interval
	// parameter
	public HashMap<Job, Node[][]> createTSNode(int timeInterval, Node sink) {
		HashMap<Job, Node[][]> network = new HashMap<Job, Node[][]>();
		this.spaceSize = (int) Math.ceil(problem.numberOfTimePoints / timeInterval) + 1;
		for (Job j : problem.myJobs) {
			Node[][] TSNode = new Node[2][spaceSize]; // Node[2][T] in size..
			int firstTravel = Math.min(Math.abs(problem.crane[0].initColumn - j.fromColumn),
					Math.abs(problem.crane[1].initColumn - j.fromColumn));
			firstTravel = (int)Math.ceil((double)firstTravel / timeInterval);
			for (int i = firstTravel; i < spaceSize; i ++) {
				int travelTime = Math.abs(j.fromColumn - j.toColumn) + j.duration + j.duration2;
				travelTime = (int) Math.ceil((double)travelTime / timeInterval);
				if (i + travelTime < this.spaceSize) { 
					ArrayList<Node> outgoing = new ArrayList<Node>();
					
					Node outgoingNode = new Node(j.toColumn, i + travelTime, "outgoing", outgoing, 0, j);

					outgoingNode.setReward(j.prio);

					Node taskNode = new Node(j.fromColumn, i, "TaskNode", outgoing, 0, j);
					taskNode.setJob(j);
					TSNode[0][i] = taskNode;
					TSNode[1][i + travelTime] = outgoingNode;
				}
				
			}

			network.put(j, TSNode);
		}

		calculateTaskTimes(network, timeInterval);
		createTravelArcs(network, timeInterval);
		if (this.tsCase.toString().equals("ExtendedModel")) {
			createPushArcs(network, timeInterval);
		}
		createGroundArcs(network, timeInterval);
		createTasktoSinkArcs(network, sink);

		return network;
	} 

	public Node createSinkNode() {

		Node sink = new Node(problem.numberOfColumns + 1, 9999, "SinkNode", null, 0, null);
		return sink;
	}

	public void calculateTaskTimes(HashMap<Job, Node[][]> network, int timeInterval) {
		for (Job job : problem.myJobs) {
			Node[] fromNodes = network.get(job)[0];
			Node[] toNodes = network.get(job)[1];
			// [task origin at time 0 ] [task destination at time 0]
			// ...
			// [task origin at time T ] [task destination at time T]
			int totalTime = timeForNumberOfColumns(job.duration + job.duration2
					+ Math.abs(job.fromColumn - job.toColumn), timeInterval);


			createParallelArcs(fromNodes, toNodes, totalTime);
			setTaskNodeJob(fromNodes, toNodes, totalTime, job, timeInterval);
		}
	}

	private void setTaskNodeJob(Node[] fromNodes, Node[] toNodes, int totalTime, Job job, int timeInterval2) {
		for (int t = 0; t < fromNodes.length; t++) {
			if ((t + totalTime <= toNodes.length) && (t + totalTime <= problem.numberOfTimePoints)) {
				if (fromNodes[t] != null) {
					ArrayList<Node> outgoings = fromNodes[t].getOutgoing();
					for (Node n : outgoings) {
						n.setJob(job);
					}
				}
			}
		}
	}

	private void createParallelArcs(Node[] fromNodes, Node[] toNodes, int totalTime) {
		for (int t = 0; t < fromNodes.length; t++) {
			// t -> t+totalTime?
			if ((t + totalTime <= toNodes.length) && (t + totalTime < this.spaceSize)) {
				if (fromNodes[t] != null)
					fromNodes[t].addOutgoing(toNodes[t + totalTime]);
			}
		}
	}

	private void createParallelTravelArcs(Node[] fromNodes, Node[] toNodes, int totalTime, double priority,
			int totalTimeOverall, int timeInterval) {
		for (int t = 0; t < fromNodes.length; t++) {
			// t -> t+totalTime?
			if ((t + totalTimeOverall <= toNodes.length) && (t + totalTimeOverall < this.spaceSize)) {
				if (fromNodes[t] != null)
					fromNodes[t].addTravelArc(priority, toNodes[t + totalTime]);
			}
		}
	}

	private void createParallelPushArcs(Node[] fromNodes, Node[] toNodes, int totalTime, double priority,
			int timeInterval) {
		for (int t = 0; t < fromNodes.length; t++) {
			// t -> t+totalTime?
			if ((t + totalTime <= toNodes.length) && (t + totalTime < this.spaceSize)) {
				if (fromNodes[t] != null)
					fromNodes[t].addPushArc(priority, toNodes[t + totalTime]);
			}
		}
	}

	private int timeForNumberOfColumns(int abs, int timeInterval) {
		// travel speed to be added
		abs = (int) Math.ceil((double)abs / timeInterval);
		return abs;
	}

	public void createTravelArcs(HashMap<Job, Node[][]> network, int timeInterval) {
		for (Job jobFinished : problem.myJobs) {
			for (Job jobToStart : problem.myJobs) {
				if (jobToStart.getJobID() != jobFinished.getJobID()) {
					Node[] fromNodes = network.get(jobFinished)[1];
					Node[] toNodes = network.get(jobToStart)[0];

					int totalTimeTravel = timeForNumberOfColumns(
							Math.abs(jobFinished.toColumn - jobToStart.fromColumn), timeInterval);
					int totalTimeOverall = timeForNumberOfColumns(
							Math.abs(jobFinished.toColumn - jobToStart.fromColumn) + jobToStart.duration
									+ Math.abs(jobToStart.toColumn - jobToStart.fromColumn) + jobToStart.duration2, timeInterval);
					createParallelTravelArcs(fromNodes, toNodes, totalTimeTravel, jobFinished.prio, totalTimeOverall,
							timeInterval);
				}
			}
		}
	}

	public void createGroundArcs(HashMap<Job, Node[][]> network, int timeInterval) {
		for (Job myJob : problem.myJobs) {
			for (int column = 0; column < 2; column++) {
				Node[] myNodes = network.get(myJob)[column];
				for (int row = 0; row < myNodes.length - 1; row++) {
					if (myNodes != null && myNodes[row] != null && myNodes[row + 1] != null
							&& row + 1 < myNodes.length - 1) {
						myNodes[row].addGroundArc(myNodes[row + 1]);
					}
				}
			}
		}
	}

	public void createTasktoSinkArcs(HashMap<Job, Node[][]> network, Node sink) {
		for (Job myJob : problem.myJobs) {
			Node[] toNodes = network.get(myJob)[1];
			for (int t = 0; t < toNodes.length; t++) {
				if (toNodes != null && toNodes[t] != null) {
					toNodes[t].addSinkArc(toNodes[t], sink, myJob);
				}
			}
		}
	}

	public void createPushArcs(HashMap<Job, Node[][]> network, int timeInterval) {
		for (Job jobFinished : problem.myJobs) {
			for (Job jobToStartFinished : problem.myJobs) {
				if (jobToStartFinished.getJobID() != jobFinished.getJobID() &&  jobFinished.toColumn != jobToStartFinished.toColumn ) {
					Node[] fromNodes = network.get(jobFinished)[1];
					Node[] toNodes = network.get(jobToStartFinished)[1];

					int totalTime = timeForNumberOfColumns(
							Math.abs(jobFinished.toColumn - jobToStartFinished.toColumn), timeInterval);
					createParallelPushArcs(fromNodes, toNodes, totalTime, jobFinished.prio, timeInterval);
				}
			}
		}
	}
  

	
}
