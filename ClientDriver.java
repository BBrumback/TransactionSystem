import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Connection;

public class ClientDriver {

	public static void main(String[] args) throws RemoteException, NotBoundException {
		String coordIp = "192.168.2.36";
		
		
		int serverPort = 2000;
		String ip; 
		Registry reg;
		Registry coordReg;
		String dbName = "jdbc:sqlite:transaction.db";
		String tName = "transactions";
		Connection db;
		Logger log;
		int TransactionCount = 0;
		Coordinator coord;
		
		
		coordReg = LocateRegistry.getRegistry(coordIp, serverPort);
		coord = (Coordinator)coordReg.lookup("Coordinator");
	}
}
