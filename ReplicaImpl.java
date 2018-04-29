import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;


public class ReplicaImpl extends UnicastRemoteObject implements Replica{

	//This is the ip of the Coordinator change this if needed
	private final String coordIp = "192.168.2.36";
	
	
	private final int serverPort = 2000;
	private final String ip; 
	private final Registry reg;
	private final Registry coordReg;
	private final String dbName = "jdbc:sqlite:transaction.db";
	private final String tName = "transactions";
	Connection db;
	Logger log;
	int TransactionCount = 0;
	Coordinator coord;
	
	//Setting up timers to trigger when the replica should ask for the decision
	Timer timer;
	RequestDecision getDecision;
	
	public ReplicaImpl() throws SQLException, RemoteException, UnknownHostException, AlreadyBoundException, NotBoundException {
		super();
		
		//Find coordinator
		coordReg = LocateRegistry.getRegistry(coordIp, serverPort);
		coord = (Coordinator)coordReg.lookup("Coordinator");
		
		//register in RMI table
		LocateRegistry.createRegistry(serverPort);
		ip = InetAddress.getLocalHost().getHostAddress();
		reg = LocateRegistry.getRegistry(ip, serverPort);
		reg.bind("Replica", this);
		
		//Set up timer
		timer = new Timer();
		getDecision = new RequestDecision(this, coord);
		
		//Set up file reading
		log = new Logger();
		connectToDb();
	}
	
	/*
	 * This will connect to the database that is in place or create one if there isnt one there already
	 */
	private void connectToDb() throws SQLException {
        //check if the db is there. This will be used later.
		File dbFile = new File("State.db");
		boolean fileExists = dbFile.exists() && !dbFile.isDirectory();
		
		//Connect to the db        
		try {
			db = DriverManager.getConnection(dbName);
		} catch (SQLException e) {
		    System.out.println(e.getMessage());
		    //I might want to force stop here and say I cant go on
		}
		
		//if we had to create a new database file create the table as well.
		if(!fileExists) {
			String tableCreate = "CREATE TABLE IF NOT EXISTS "+ tName +" (\n"
	                + "	key varchar(255),\n"
	                + "	value varchar(255)\n"
	                + ");";
			Statement create = db.createStatement();
			create.execute(tableCreate);
		}
	}
	
	/*
	 * These are the basic commands that the coordinator will tell the replica. They are all wrapped in a transaction statement
	 * that locks the database to all writes until the transaction is resolved. If the transaction commits that is written to a log,
	 * the same is done for rollbacks.
	 */
	
	//Selects the value with the given string key input
	public String get(String key) {
		try {
			Statement select = db.createStatement();
			ResultSet results = select.executeQuery("SELECT value FROM " + tName + " WHERE key=\'" + key + "\';");
			return results.getString("value");
		} catch (SQLException e) {
			return "VALUE NOT FOUND OR ACCESSABLE FOR GIVEN KEY";
		}
		
	}
	
	/*
	 * This method adds a new key into the database if its not already inplace. If the key is already in place then the value is just
	 * updated.
	 */
	public boolean updateOrInsert(String key, String value) {
		begin();
		try {
			Statement count = db.createStatement();
			ResultSet results = count.executeQuery("SELECT COUNT(key) AS total FROM " + tName + " WHERE key = \'" + key + " \'");
			if(results.getInt("total") > 0) {
				PreparedStatement update = db.prepareStatement("UPDATE " + tName + " SET value = ? WHERE key = ?");
				update.setString(1, value);
				update.setString(2, key);
				update.executeUpdate();
			}else {
				
				PreparedStatement update = db.prepareStatement("INSERT INTO " + tName + "(key,value) VALUES(?,?)");
				update.setString(1, key);
				update.setString(2, value);
				update.executeUpdate();
			}
			//Make a request after 5 seconds and repeat every 5 seconds
			timer.schedule(getDecision, 5000, 5000);
		} catch (SQLException e) {
			//If I cant vote no and rollback
			rollback();
			return false;
		}
		return true;
		
	}
	
	//This deletes the key and associated value from the database
	public boolean del(String key) {
		begin();
		try {
			PreparedStatement delete = db.prepareStatement("DELETE FROM " + tName + " WHERE key = ?");
			delete.setString(1, key);
			delete.executeUpdate();
			//Make a request after 5 seconds and make a request every 5 seconds
			timer.schedule(getDecision, 5000, 5000);
		} catch (SQLException e) {
			//Vote no and rollback
			rollback();
			return false;
		}
		return true;
	}
	
	
	//This starts a transcation and locks the database. this is called at the start of inserts and deletes
	private void begin() {
		try {
			Statement select = db.createStatement();
			select.execute("BEGIN IMMEDIATE;");
			
		} catch (SQLException e) {
			//If this happesn I probably should fail all interactions.
			//maybe call rollback?
		}
	}
	
	
	/*
	 * These are commits and rollbacks for the coordinator to call after the votes have been counted.
	 */
	public boolean commit() {
		try {
			Statement select = db.createStatement();
			select.execute("COMMIT TRANSACTION;");
			log.writeLog("COMMIT");
			timer.cancel();
			timer.purge();
			return true;
			//Return back to the coordinator to say you committed
		} catch (SQLException e) {
			return false;
			//NOT GOOD IF I GET HERE.
		}
	}
	
	public boolean rollback() {
		try {
			Statement select = db.createStatement();
			select.execute("ROLLBACK TRANSACTION;");
			//TODO: add transaction id;
			log.writeLog("ROLLBACK");
			timer.cancel();
			timer.purge();
			return true;
		} catch (SQLException e) {
			return false;
			//THIS IS REAL BAD...
		}
	}
}

class RequestDecision extends TimerTask {
	ReplicaImpl rep;
	Coordinator coord;
	
	public RequestDecision(ReplicaImpl rep, Coordinator coord) {
		this.rep = rep;
		this.coord = coord;
	}
	
	@Override
	public void run() {
		if(coord.getDecision())
			rep.commit();
		else
			rep.rollback();
	}
}
