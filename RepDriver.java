import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Scanner;

public class RepDriver {

	public static void main(String[] args) {
		try {
			Replica rep = new ReplicaImpl();
		} catch (RemoteException | UnknownHostException | SQLException | AlreadyBoundException | NotBoundException e) {
			e.printStackTrace();
		}
		
		
		//This keeps the server running until you want to end the program by typing exit
		Scanner scan = new Scanner(System.in);
		String command = scan.nextLine();
		while(!command.equals("exit"));
		scan.close();
	}
}
