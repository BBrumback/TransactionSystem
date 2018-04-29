import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Logger {

	FileWriter log;
	
	public Logger() {
		try {
			log = new FileWriter("log.txt",true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void writeLog(String input) {
		try {
			log.write(input);
			log.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public String readLast() {
		Scanner scan;
		String last = "NO FILE FOUND";
		try {
			scan = new Scanner(new File("log.txt"));
			while(scan.hasNextLine()) {
				last = scan.nextLine();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		return last;
	}
}
