package tsnSolution;

import java.util.ArrayList;

import data.Job;

public class Node {

	public int locationId;
	public Integer time;
	public String name; 
	private ArrayList<Node> outgoing;
	public double reward;
	public Job job;

	public Node(int locationId, Integer time, String name, ArrayList<Node> outgoing, double reward, Job job) {
		this.locationId = locationId;
		this.time = time;
		this.name = name;
		this.outgoing = new ArrayList<>();
	}

	public void addOutgoing(Node toNode) {
		if (toNode != null) {
			this.outgoing.add(toNode);
		}
	}

	public ArrayList<Node> getOutgoing() {
		return this.outgoing;
	}

	public double getReward() {
		return reward;
	}

	public void setReward(double reward) {
		this.reward = reward;
	}

	public void setJob(Job job) {
		this.job = job;
	}

	public void addGroundArc(Node toNode) {
		if (toNode != null) {
			Node newOutgoing = new Node(toNode.locationId, toNode.time, "GroundArc", new ArrayList<>(), 0, toNode.job);
			newOutgoing.setJob(toNode.job);
			this.outgoing.add(newOutgoing);
		}
	}

	public void addSinkArc(Node fromNode, Node sink, Job myJob) {
		Node newOutgoing = new Node(sink.locationId, sink.time, sink.name, null, myJob.priority, null);
		this.outgoing.add(newOutgoing);
	}

	public void addTravelArc(Double priority, Node toNode) {
		if (toNode != null) {
			Node newOutgoing = new Node(toNode.locationId, toNode.time, toNode.name, null, priority, toNode.job);
			newOutgoing.setJob(toNode.job);
			this.outgoing.add(newOutgoing);
		}

	}

	public void addPushArc(Double priority, Node toNode) {
		if (toNode != null) {

			Node newOutgoing = new Node(toNode.locationId, toNode.time, "Push", null, priority, toNode.job);
			newOutgoing.setJob(toNode.job);
			this.outgoing.add(newOutgoing);
		}

	}
}
