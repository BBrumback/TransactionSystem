import java.rmi.Remote;

public interface Replica extends Remote{

	public String get(String key);
	
	public boolean updateOrInsert(String key, String value);
	
	public boolean del(String key);
	
	public boolean commit();
	
	public boolean rollback();
}
