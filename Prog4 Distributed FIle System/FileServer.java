import jdk.jshell.spi.ExecutionControl;

import java.io.*;                  // IOException
import java.nio.file.Files;
import java.util.*;
import java.net.*;                 // InetAddress
import java.rmi.*;                 // Naming
import java.rmi.server.*;          // UnicastRemoteObject
import java.rmi.registry.*;        // rmiregistry
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileServer extends UnicastRemoteObject implements ServerInterface {

    private final Vector<CachedFileEntry> cache; // vector of cached entire - null when server started
    private int port;

    // CHANGE LOCATION 
    private final static String ramDiskFile = "/tmp/sasad23.txt"; // a cached file in /tmp: 

    public FileServer(int port)throws RemoteException{
        this.cache = new Vector<>(); 
        this.port = port;
    }

    public void shutdown()
    {
        System.out.println("Terminating");
    }

    public FileContents download(String client, String filename, String mode) throws RemoteException
    {
        FileContents f;
        CachedFileEntry file = findFile(filename);

        // Downloading a new file to a client
        // if the file does not exist, then cache it
        if(file == null){
            System.out.println("File does not exist in cache"); 
            var contents = getFileContent(filename);

            // if mode is w then we return an empty file
            if (contents.length == 0 && mode.equals("r")) {
                System.out.println("File Does not exist - Cannot be opened in read mode.");
                return null;
            }

            // first create a cached file entry ******* COME BACK AND ADD CONTENTS *******
            var newFile = new CachedFileEntry(filename, client, "Write_Shared", contents);
            newFile.setOwner(client);
            System.out.println("FIle " + filename + " has been added to the server cache");

            // add client to readers list
            newFile.addReaders(client);

            // then add the file to the cache
            this.cache.add(newFile);


            

            return new FileContents(contents);
        }
        // Downloading a cached file to a client
        else{ // file is already cached at the server
            System.out.println("File is already cached"); 
            if (file.getState().equalsIgnoreCase("Not_Shared")) {
                System.out.println("File " + filename + " is in state Not_shared"); 
                if (mode.equals("r")) { // read
                    System.out.println("Not_shared being accessed in r");
                    // add this client to the readers list
                    file.addReaders(client);
                    // file state changed to Read_shared
                    file.setState("Read_Shared");
                } else if (mode.equals("w")) { // write
                    System.out.println("Not_shared being accessed in w");
                    // register this client as the owner
                    System.out.println("Previous Owner: " + file.getOwner());
                    file.setOwner(client);
                    System.out.println("New Owner: " + file.getOwner()); 
                    // file state changed to Write_shared
                    file.setState("Write_Shared");
                } else {
                    throw new RemoteException();
                }
            }

            else if (file.getState().equalsIgnoreCase("Read_Shared")) {
                System.out.println("File " + filename + " is in state Read_shared"); 
                if (mode.equals("r")) { // read
                    System.out.println("Read_shared being accessed in r");
                    // add this client to the readers list
                    file.addReaders(client);
                } else if (mode.equals("w")) { // write
                    System.out.println("Read_shared being accessed in w");
                    // register this client as the owner 
                    file.setOwner(client);
                    // file state changed to Write_shared
                    file.setState("Write_Shared");
                } else {
                    throw new RemoteException();
                }
            } 

            else if (file.getState().equalsIgnoreCase("Write_Shared")) {
                System.out.println("File " + filename + " is in state Write_shared"); 
                if (mode.equals("r")) { // read
                    System.out.println("Write_shared being accessed in r");
                    // add this client to the readers list
                    file.addReaders(client);
                } else if (mode.equals("w")) { // write
                    System.out.println("Write_shared being accessed in w");
                    // file state goes to Ownership_Change
                    file.setState("Ownership_Change");
            
                    // Step 1: Reset the last Upload Client.
                    file.resetLastUploadClient();
            
                    // Step 2: Call WriteBack on the client.
                    requestWriteBack(file.getOwner());
                    System.out.println("Writeback() has been called5555");
            
                    // Step 3: Wait for the upload to complete.
                    file.waitForUploadClient();
            
                    return new FileContents(file.contents);
                } else {
                    throw new RemoteException();
                }
            }

            else if (file.getState().equalsIgnoreCase("Ownership_Change")) {
                System.out.println("File " + filename + " is in state Ownership_Change"); 
                if (mode.equals("r")) { // read
                    System.out.println("Ownership_Change being accessed in r");
                    // add client to readers list
                    file.addReaders(client);
                } else if (mode.equals("w")) {
                    System.out.println("Ownership_Change being accessed in w");
                    // COME BACK AND FIX
                }
            }

            f = new FileContents(file.getContents());
        }


        // return file contents object to client
        return f;
    }

    public boolean upload( String client, String filename, FileContents contents ) throws RemoteException{
        CachedFileEntry file = findFile(filename);

        System.out.printf("Upload called: Client=%s, File=%s\n", client, filename);

        // if we have found the file in the servers cache
        if(file != null){
            // and the file state is Ownership_Change OR Write_Shared
            if((file.getState().equalsIgnoreCase("Ownership_Change"))
                    || (file.getState().equalsIgnoreCase("Write_Shared"))){

                //update the file contents
                file.setContents(contents.get());

                // Inform all readers to invalidate the cache.
                invalidateClientCaches(file.getReaders());

                // Now note that upload has been completed.
                file.setLastUploadClient(client);
            }
            else{
                return false;
            }
        }

        return true;
    }

    private void requestWriteBack(String client) {
        try{
            var uri = "rmi://" + client + ":" + port + "/fileclient";
            System.out.printf("Requesting Write Back from Client: %s\n", uri);
            var clientFace = (ClientInterface)Naming.lookup( uri );
            clientFace.writeback();
            System.out.printf("Request completed for Write Back from Client: %s\n", uri);
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }

    private void invalidateClientCaches(Vector<String> clients) {
        // loop through the readers of this file and invalidate for each reader/client
        for (String client : clients) {
            try{
                System.out.printf("Invalidate Client: %s\n", client);
                var clientFace = (ClientInterface)Naming.lookup( "rmi://" + client + ":" + port + "/fileclient" );
                clientFace.invalidate();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private CachedFileEntry findFile(String filename){
        for(int i = 0; i < this.cache.size(); i++){
            var file = this.cache.elementAt(i);
            if(file.getName().equals(filename)){
                return file;
            }
        }

        return null;
    }

    private byte[] getFileContent(String fileName) throws RemoteException {
        var currentDirectory = Paths.get(System.getProperty("user.dir"));
        var filePath = currentDirectory.resolve(Paths.get(fileName));

        //System.out.println("Path: " + filePath); 

        if (!Files.exists(filePath)) {
            return new byte[0];
        }

        try {
            return Files.readAllBytes(filePath);
        } catch (Exception ex) {
            throw new RemoteException(ex.toString());
        }
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

    public static void main(String[] args){

        System.out.println("Server Starting");

        // check to see if we have correct number of args 
        if(args.length != 1){
            System.out.println("Usage: java FileServer port");
            System.exit(-1);
        }


        try{
            int port = Integer.parseInt(args[0]);

            System.out.println("Starting FileServer");
            FileServer server = new FileServer(port);

            // create a server object
            // start the registry
            System.out.println("Starting Registry");
            startRegistry(port);

            System.out.println("Starting Bind");
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
            input = scan.nextLine();

            // if input is exit or quit
            if(input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")){
                // before termination, server must write back all modified
                // file contents from its file cache to the local disk


                // exit 
                System.exit(0);
            }
             
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

        // The name of the last client that completed the upload.
        private String lastUploadClient;
    
    
        // construtor that creates a new file with the name, contents, ownder, readers, and state
        public CachedFileEntry(String name, String owner, String state, byte[] contents){
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
    
        public void addReaders(String client){ 
            if(!this.readers.equals(client)){
                this.readers.add(client); 
            }
        }

        public void resetLastUploadClient() {
            synchronized (this) {
                this.lastUploadClient = null;
                notifyAll();
            }
        }

        public void setLastUploadClient(String client) {
            System.out.printf("setLastUploadClient: %s\n", client);
            synchronized (this) {
                this.lastUploadClient = client;
                notifyAll();
            }
        }

        public void waitForUploadClient() {
            try {
                synchronized (this) {
                    while (this.lastUploadClient == null) {
                        System.out.printf("waitForUploadClient: %s\n", this.lastUploadClient);
                        wait();
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}


