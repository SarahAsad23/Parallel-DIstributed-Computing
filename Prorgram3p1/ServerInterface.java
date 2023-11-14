import java.rmi.*;
import java.util.*;

public interface ServerInterface extends Remote {
    public Vector execute( String command ) throws RemoteException;
}
