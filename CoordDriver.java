import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;

public class CoordDriver {

	public static void main(String[] args) {
		
		try {
			CoordinatorImpl coord = new CoordinatorImpl();
			//This keeps the server running until you want to end the program by typing exit
			Scanner scan = new Scanner(System.in);
			String command = scan.nextLine();
			while(!command.equals("exit")) {
				if(command.equals("find")) {
					try {
						coord.findReplicas();
					} catch (RemoteException | NotBoundException e) {
						// TODO Auto-generated catch block
						System.out.println("COULDNT FIND REPLICAS");
						e.printStackTrace();
					}
				}
			}
			scan.close();
		} catch (RemoteException | UnknownHostException | AlreadyBoundException e) {
			e.printStackTrace();
		}
		
		
		
	}
}