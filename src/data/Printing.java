package data;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class Printing {

	public static void writeCsv(String result) {
		try {
			FileWriter writer = new FileWriter("results_afterinforms_oneHour.csv", true);
			writer.write(String.join(",", result));
			writer.write(System.lineSeparator());
			writer.close();
		} catch (IOException e) {
			
		}
	}

}
