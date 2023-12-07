import java.io.*;                  // IOException
import java.net.*;                 // InetAddress
import java.rmi.*;                 // Naming
import java.rmi.server.*;          // UnicastRemoteObject
import java.rmi.registry.*;        // rmiregistry

public class FileServer extends UnicastRemoteObject implements ServerInterface {

    // server maintains a list of cached entries 
    // list of files is null when server is booted up
    Vector<CachedFileEntry> files = null; 

    /*
     * Starts an RMI registry in background, which relieves a user from
     * manually starting the registry and thus prevents them from
     * forgetting its termination upon a logout.
     */
    private static void startRegistry(int port) throws RemoteException{
        try{
            Registry registry = LocateRegistry.getRegistry(port); 
            registry.list();
        }
        catch(RemoteException e){
            Registry registry = LocateRegistry.createRegistry(port); 
        }
    }

    public static void main(String args[]){
        // check to see if we have correct number of args 
        if(args.length != 1){
            System.out.println("Usage: java FileServer port");
            System.exit(-1);
        }

        try{
            // create a server object 
            FileServer server = new FileServer(); // FIX THIS
            // start the registry 
            startRegistry(Integer.parseInt(args[0]));
	        Naming.rebind("rmi://localhost:" + args[0] + "/fileserver", server);
        } catch(Exception e){
            e.printStackTrace( );
            System.exit( 1 );
	    }

        System.out.println("Connected");

        Scanner scan = new Scanner(System.in); 
        String input = ""; 

        // the server needs to be terminated using quit or exit
        while(true){
            // get the user input 
            System.out.println("Enter 'Quit' or 'Exit' to Terminate the Server: "); 
            input = scan.gerNextLine();

            // if input is exit or quit
            if(input.equalsIgnoreCase("exit") || input.equalsIgnoreCase('quit')){
                // before termination, server must write back all modified
                // file contents from its file cache to the local disk
                // COME BACK AND FINISH 
                
                // exit 
                System.exit(0);
            }
             
        }
    }


    public FileContents download(String client, String filename, String mode) throws RemoteException{
        
        // scan the list of files 
        for(int i = 0; i < files.size(); i++){
            // Downloading a new file to a client 
            // if the file does not exist, then cache it 
            if(!files[i].equals(filename)){
                files.add(filename, client, mode, ); //FIX THIS!!!!!!
            }
            // Downloading a cached file to a client 
            else{
                if()

            }
            
            
        }
    }
}


class CachedFileEntry {

    private String name; // a cached file name 
    // list of IP names each identifying a different 
    // DFS client who is sharing this file for read
    private Vector<String> readers; 
    private String owner; // IP name of client who owns file for modification 
    // indicates the state: not_shared, read_shared, write_shares, ownership_change
    private String state; 
    private byte[] contents; // stores the file contents


    // construtor that creates a new file with the name, contents, ownder, readers, and state
    public CachedFileEntry(String name, String owner, String state, byte[] contents){
        this.name = name; 
        this.readers = new Vector<>();
        this.owner = owner; 
        this.state = state; 
        this.contents = contents; 
    }

    String getName(String fname){
        return fname.name; 
    }

    void setName(string newName){
        name = newName;
    }

    String getOwnder(){
        return owner; 
    }

    void setOwner(string oName){
        owner = oName; 
    }


}

public class FileContents implements Serializable { 
    
    private byte[] contents; // file contents 
    
    public FileContents( byte[] contents ) {
        this.contents = contents; 
    }
    
    public void print() throws IOException{ 
        System.out.println( "FileContents = " + contents );
    }

    public byte[] get() {
        return contents; 
    }
}
