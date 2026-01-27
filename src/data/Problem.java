package data;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Random;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Problem {

	public int numberOfTimePoints;
	public int numberOfColumns;
	public int numberOfJobs;
	public Job[] myJobs;
	private Random random;
	public int seednumber;
	public Crane[] crane;
	public double speedInColumnsPerTimeUnit;
	public int craneDuration;
	public String duration;
	public int totalSpentTime;
	public int timeLimit;
	public double coeff;
	public int detailedPrint;
	public int timeInterval;
	private boolean useJsonData;

	public Problem(int detailedPrint, int noTasks, int columns, int seed, int numberofCranes,
			int speedTimeUnitsforOneColumnTravel, String duration, int timeLimit, double coeff, int timeInterval, boolean useJsonData) {
		
		this.useJsonData = useJsonData;
		this.timeInterval = timeInterval;
		this.speedInColumnsPerTimeUnit = speedTimeUnitsforOneColumnTravel;
		this.timeLimit = timeLimit;  
		this.numberOfJobs = noTasks;
		this.numberOfColumns = columns;
		
		if (useJsonData) {
            try {
                // Assuming JSON file paths are predefined or passed as arguments
                loadFromJson("job_s_4_12_d_s_c_25.json", "crane_s_4_12_d_s_col_25.json"); // "job_s_55_10_d_s_c_25.json", "crane_s_55_10_d_s_col_25.json"
                
                for (Job j : this.myJobs) {
        			this.totalSpentTime += (j.duration + j.duration2)
        					+ (Math.abs(j.fromColumn - j.toColumn) / this.speedInColumnsPerTimeUnit);
        		}
        		this.numberOfTimePoints = this.totalSpentTime / 2 + 1;
        		
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
        	
        	
    		this.seednumber = seed;    		
    		this.crane = new Crane[numberofCranes]; 
    		this.random = new Random(seednumber);
    		this.duration = duration;
    		this.coeff = coeff;
    		this.detailedPrint = detailedPrint;
    		

    		if (this.duration == "s") {
    			this.craneDuration = random.nextInt(3) + 1; // (1-3)
    		}
    		if (this.duration == "l") {
    			this.craneDuration = random.nextInt(3) + 3; // (3-5)

    		}

    		for (int i = 0; i < numberofCranes; i++) {
    			int columnLocation = 0;
    			int rowLocation = 0;
    			if (i == 1) {
    				columnLocation = this.numberOfColumns;
    				rowLocation = 0;
    			}
    			this.crane[i] = new Crane(i + 1, columnLocation, rowLocation);
    		}

    		this.myJobs = new Job[this.numberOfJobs];
    		for (int i = 0; i < this.numberOfJobs; i++) {
    			this.myJobs[i] = new Job(i + 1, this.random, this.numberOfTimePoints, this.numberOfColumns,
    					this.numberOfColumns, this.craneDuration, this.speedInColumnsPerTimeUnit);

    		}

    		for (Job j : this.myJobs) {
    			this.totalSpentTime += (j.duration + j.duration2)
    					+ (Math.abs(j.fromColumn - j.toColumn) / this.speedInColumnsPerTimeUnit);
    		}
    		this.numberOfTimePoints = this.totalSpentTime / 2 + 1;

    		double a = 0.0001;
    		double b = 0.0001;
    		double c = 0.0001;
    		double d = 0.0001;
    		for (Job j : this.myJobs) {
    			if (j.priority == 1) {
    				j.priority += a;
    				a += 0.0001;
    			}
    			if (j.priority == 2) {
    				j.priority += b;
    				b += 0.0001;
    			}
    			if (j.priority == 3) {
    				j.priority += c;
    				c += 0.0001;
    			}
    			if (j.priority == 4) {
    				j.priority += d;
    				d += 0.0001;
    			}
    		}

    		String jobResult = new Gson().toJson(this.myJobs);
    		String craneResult = new Gson().toJson(crane);
    		String logPath = System.getProperty("user.dir");
    		File directory = new File(logPath);
    		if (!directory.exists()) {
    			directory.mkdir();
    		}
    		String pathCrane = logPath + File.separator + "crane" + "_s_" + this.seednumber + "_" + this.numberOfJobs
    				+ "_d_" + this.duration + "_col_" + this.numberOfColumns + ".json";
    		String pathJob = logPath + File.separator + "job" + "_s_" + this.seednumber + "_" + this.numberOfJobs + "_d_"
    				+ this.duration + "_c_" + this.numberOfColumns + ".json";
    		try {
    			if (new File(pathJob).exists() && new File(pathCrane).exists()) {
    				new File(pathJob).delete();
    				new File(pathCrane).delete();
    			}

    			FileWriter jobFileWriter = new FileWriter(pathJob);
    			FileWriter craneFileWriter = new FileWriter(pathCrane);

    			try {
    				jobFileWriter.write(jobResult);
    				craneFileWriter.write(craneResult);
    			} catch (IOException e) {
    				e.printStackTrace();
    			} finally {
    				jobFileWriter.close();
    				craneFileWriter.close();
    			}
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
        }
		
		

	}
	
	private void loadFromJson(String jobJsonFilePath, String craneJsonFilePath) throws IOException {
        this.myJobs = loadJobsFromJson(jobJsonFilePath);
        this.crane = loadCranesFromJson(craneJsonFilePath);
    }
	
	private Job[] loadJobsFromJson(String filePath) throws IOException {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(filePath)) {
            Type jobListType = new TypeToken<Job[]>() {}.getType();
            return gson.fromJson(reader, jobListType);
        }
    }
	
	private Crane[] loadCranesFromJson(String filePath) throws IOException {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(filePath)) {
            Type craneListType = new TypeToken<Crane[]>() {}.getType();
            return gson.fromJson(reader, craneListType);
        }
    }

	
	

}
