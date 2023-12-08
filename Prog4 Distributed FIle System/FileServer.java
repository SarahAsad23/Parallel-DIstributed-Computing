import java.io.*;                  // IOException
import java.net.*;                 // InetAddress
import java.rmi.*;                 // Naming
import java.rmi.server.*;          // UnicastRemoteObject
import java.rmi.registry.*;        // rmiregistry

public class FileServer extends UnicastRemoteObject implements ServerInterface {

    private Vector<CachedFileEntry> cache; // vector of cached entire - null when server started
    private ClientInterface clientFace = null;  // a DFS client interface
    int port; 

    // CHANGE LOCATION 
    private final static String ramDiskFile = "/tmp/sasad23.txt"; // a cached file in /tmp: 

    public FileServer()throws RemoteException{
        cache = new Vector<>(); 
    }

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
            FileServer server = new FileServer(); 
            port = Integer.parseInt(args[0]);
            // start the registry 
            startRegistry(port);
	        Naming.rebind("rmi://localhost:" + port + "/fileserver", server);
        } catch(Exception e){
            e.printStackTrace();
            System.exit(1);
	    }

        System.out.println("Connected");

        Scanner scan = new Scanner(System.in); 
        String input = ""; 

        // the server needs to be terminated using quit or exit
        while(true){
            // get the user input 
            System.out.println("Enter 'Quit' or 'Exit' to Terminate the Server: "); 
            input = scan.getNextLine();

            // if input is exit or quit
            if(input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")){
                // before termination, server must write back all modified
                // file contents from its file cache to the local disk
                // COME BACK AND FINISH  

                
                // exit 
                System.exit(0);
            }
             
        }
    }

    public FileContents download(String client, String filename, String mode) throws RemoteException{

        FileContents f; 
        
        // scan the list of files 
        for(int i = 0; i < cache.size(); i++){
            // Downloading a new file to a client 
            // if the file does not exist, then cache it 
            if(!cache[i].getName().equalsIgnoreCase(filename)){
                // SOMEHOW HAVE TO CHECK IF CONTENTS ARE ON DISK?

                // first create a cached file entry ******* COME BACK AND ADD CONTENTS *******
                CachedFileEntry file = new CachedFileEntry(filename, client, "Not_Shared", contents); 
                // add client to readers list 
                cache[i].addReaders(client); 
                // then add the file to the cache 
                files.add(file); 
                
                // if mode is w then we return an empty file 
                if(mode.equals("w")){
                    return new FileContents(new byte[0]);
                }
                else{ // mode is read and file does not exist 
                    return null; 
                }
            }
            // Downloading a cached file to a client
            else{ // file is already cached at the server 
                
                if(cache[i].getState().equals("Not_Shared")){
                    if(mode.equals("r")){ // read
                        // add this client to the readers list 
                        cache[i].addReaders(client); 
                        // file state changed to Read_shared 
                        cache[i].setState("Read_Shared"); 
                    }
                    else{ //write 
                        // register this client as the owner 
                        cache[i].setOwner(client); 
                        // file state changed to Write_shared 
                        cache[i].setState("Write_Shared");
                    }
                }

                else if(cache[i].getState().equals("Read_Shared")){
                    if(mode.equals("r")){ // read 
                        // add this clients to readers list  
                        cache[i].addReaders(client);
                    }
                    else{ // write 
                        // register this client as the owner 
                        cache[i].setOwner(client);
                        // file state changed to Write_shared 
                        cache[i].setState("Write_Shared");
                    }
                }
                
                else if(cache[i].getState().equals("Write_Shared")){
                    if(mode.equals("r")){ // read 
                        // add client to readers list 
                        cache[i].addReaders(client); 
                    }
                    else{ // write 
                        // file state goes to Ownership_Change 
                        cache[i].setState("Ownership_Change"); 

                        // server calls the current owners writeback fucntion
                        // server must suspend download() until the owners client 
                        // calls the servers upload() to write back the latest contents 
                        synchronized(client){
                            try{
                                // make the RMI call to the clients writeback function 
                                FileContents contents = clientFace.writeback();

                                // wait until the client is done writing back contents 
                                client.wait(); 
                            } 
                            catch(Exception e){
                                e.printStackTrace(); 
                            }
                        }

                        return contents; 
                    }
                }
                
                else if(cache[i].getState().equals("Ownership_Change")){
                    if(mode.equals("r")){ // read 
                        // add client to readers list 
                        cache[i].addReaders(client); 
                    }
                    else{ // write 
                        // COME BACK AND FIX 
                    }
                }

                f = new FileContents(cache[i].getContents());
            }
        }
        
        // return file contents object to client 
        return f; 
    }

    public boolean upload( String client, String filename, FileContents contents ) throws RemoteException{
        for(int i = 0; i < cache.size(); i++){
            // if we have found the file in the servers cache
            if(cache[i].getName().equalsIgnoreCase(filename)){
                // and the file state is Ownership_Change OR Write_Shared
                if((cache[i].getState().equalsIgnoreCase("Ownership_Change")) 
                || (cache[i].getState().equalsIgnoreCase("Write_Shared"))){

                    //update the file contents
                    cache[i].setContents(contents.get()); 

                    // get the readers list 
                    Vector<String> readers = cache[i].getReaders();
                    // loop through the readers of this file and invalidate for each reader/client
                    for (String reader : readers) {
                        try{
                            clientFace = (ClientInterface)Naming.lookup( "rmi://" + reader + ":" + port + "/fileclient" );
                            clientFace.invalidate(); 
                        }
                        catch (Exception e){
                            e.printStackTrace(); 
                        }
                    }
                }
                else{
                    return false; 
                }
            }
        }

        return true; 
    }
}

private class CachedFileEntry {

    private String name; // a cached file name 
    // list of IP names each identifying a different 
    // DFS client who is sharing this file for read
    private Vector<String> readers; 
    private String owner; // IP name of client who owns file for modification 
    // indicates the state: Not_Shared, Read_Shared, Write_Shares, Ownership_Change
    private String state; 
    private byte[] contents; // stores the file contents


    // construtor that creates a new file with the name, contents, ownder, readers, and state
    public CachedFileEntry(String name, String owner, String state, byte[] contents, ){
        this.name = name; 
        this.readers = new Vector<>();
        this.owner = owner; 
        this.state = state; 
        this.contents = contents; 
    }

    // Getters
    public String getName() { return name; }
    public Vector<String> getReaders() { return readers; }
    public String getOwner() { return owner; }
    public String getState() { return state; }
    public byte[] getContents() { return contents; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setOwner(String owner) { this.owner = owner; }
    public void setState(String state) { this.state = state; }
    public void setContents(byte[] contents) { this.contents = contents; }

    public void addReaders(String clinet){ 
        if(!this.reader.equals(client)){
            this.readers.add(client); 
        }
    }
}

public class FileContents implements Serializable { 
    
    private byte[] contents; // file contents 
    
    public FileContents( byte[] contents ) { this.contents = contents; }
    public byte[] get() { return contents; }
    
    public void print() throws IOException{ 
        System.out.println( "FileContents = " + contents );
    } 
}
