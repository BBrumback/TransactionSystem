import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Random;

public class CoordinatorImpl extends UnicastRemoteObject implements Coordinator{
	
	//This is the list of replicas' IP addresses, feel free to change this.
	private final String[] ips = {"192.168.2.42", "192.168.2.33", "192.168.2.47", "192.168.2.31"};
	
	
	ArrayList<Replica> replicas = new ArrayList<Replica>();
	Logger log;
	private final int serverPort = 2000;
	private final String ip;
	private final Registry reg;
	private final Random rand;
	
	public CoordinatorImpl() throws RemoteException, UnknownHostException, AlreadyBoundException {
		
		//register in RMI table
		LocateRegistry.createRegistry(serverPort);
		ip = InetAddress.getLocalHost().getHostAddress();
		reg = LocateRegistry.getRegistry(ip, serverPort);
		reg.bind("Coordinator", this);
		
		//Set up logging
		log = new Logger();
		
		rand = new Random();
	}
	
	/*
	 * Client commands. These are the basic commands that the client can call to read or write
	 * to the database.
	 */
	
	//Gets a value from the database and returns it. 
	public String get(String key) {
		return replicas.get(rand.nextInt(replicas.size())).get(key);
	}
	
	//Inserts or updates a value from all replicas
	public void put(String key, String value) {
		boolean commit = true;
		for(Replica r: replicas) {
			commit = commit && r.updateOrInsert(key, value);
		}
		if(commit) {
			for(Replica r: replicas) {
				r.commit();
			}
			log.writeLog("COMMIT");
		}else {
			for(Replica r: replicas) {
				r.rollback();
			}
			log.writeLog("ROLLBACK");
		}
	}
	
	//Deletes an element from all replicas
	public void del(String key) {
		boolean commit = true;
		for(Replica r: replicas) {
			commit = commit && r.del(key);
		}
		if(commit) {
			for(Replica r: replicas) {
				r.commit();
			}
			log.writeLog("COMMIT");
		}else {
			for(Replica r: replicas) {
				r.rollback();
			}
			log.writeLog("ROLLBACK");
		}
	}
	
	//The replicas might ask if they never got communication about if they should commit or rollback
	public boolean getDecision() {
		String last = log.readLast();
		if(last.equals("COMMIT"))
			return true;
		else if(last.equals("ROLLBACK"))
			return false;
		else {
			System.out.println("I DIDNT KNOW WHAT TO DO WITH " + last);
			return false;
		}
	}
	
	public void findReplicas() throws RemoteException, NotBoundException {
		Registry repReg;
		for(String ip : ips) {
			repReg = LocateRegistry.getRegistry(ip, serverPort);
			replicas.add((Replica)repReg.lookup("Coordinator"));
		}
	}
}
