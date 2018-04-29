import java.rmi.Remote;

public interface Coordinator extends Remote{

	public String get(String key);
	
	public void put(String key, String value);
	
	public void del(String key);
	
	public boolean getDecision();
}
