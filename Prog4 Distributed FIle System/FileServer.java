/*
 * Sarah Asad 
 * CSS 434: Parallel & Distributed Computing 
 * 
 * Prog 4: Distributed File System 
 */

import jdk.jshell.spi.ExecutionControl;
import java.io.*;                  // IOException
import java.nio.file.Files;        // Files
import java.util.*;                // Vector
import java.net.*;                 // InetAddress
import java.rmi.*;                 // Naming
import java.rmi.server.*;          // UnicastRemoteObject
import java.rmi.registry.*;        // rmiregistry
import java.nio.file.Path;         // File path 
import java.nio.file.Paths;        // Files path 

public class FileServer extends UnicastRemoteObject implements ServerInterface {

    private final Vector<CachedFileEntry> cache; // vector of cached entire - null when server started
    private int port;
    
    
    // constructor which creates a FileServer object
    public FileServer(int port)throws RemoteException{
        this.cache = new Vector<>(); 
        this.port = port;
    }

    /*
     * Called by the client to download contents of a requested file. Download is handled differently
     * based on the mode the client wants to download the file (r/w) as well as what the current file 
     * state is (Not_Shared, Read_Shared, Write_Shared, Ownership_Chnage)
     * 
     * Parameters: client: the IP name of the client 
     *             file name: the name of the file the client wants 
     *             mode: the mode (read/write) the client wants to access the file
     *  
     * Returns: a FileContents object that holds a byte array which are the contents of the file 
     *  
     */
    public FileContents download(String client, String filename, String mode) throws RemoteException
    {
        FileContents f;
        
        // check to see if file exists in the cache 
        CachedFileEntry file = findFile(filename);

        // Downloading a new file to a client
        // if the file does not exist, then cache it
        if(file == null){
            System.out.println("File does not exist in cache"); 
            // get the contents of the file 
            var contents = getFileContent(filename);

            // if mode is r then return null - cannot read file that DNE
            if (contents.length == 0 && mode.equals("r")) {
                System.out.println("File Does not exist - Cannot be opened in read mode.");
                return null;
            }

            // first create a cached file entry - with default state as Write_Shared
            var newFile = new CachedFileEntry(filename, client, "Write_Shared", contents);

            System.out.println("FIle " + filename + " has been added to the server cache");

            // add client to readers list
            newFile.addReaders(client);

            // then add the file to the cache
            this.cache.add(newFile);

            //return the contents to the client 
            return new FileContents(contents);
        }
        // Downloading a cached file to a client
        else{ 
            // file is already cached at the server
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
            
                    // Step 1: Reset the last Upload Client
                    file.resetLastUploadClient();
            
                    // Step 2: Call WriteBack on the client
                    requestWriteBack(file.getOwner());
                    System.out.println("Writeback() has been called");
            
                    // Step 3: Wait for the upload to complete
                    file.waitForUploadClient();

                    // step 4: set the new owner 
                    file.setOwner(client);
                    
                    // return the new file contents 
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
                    System.out.println("Ownership_Change being accessed in w, waiting for Write_Shared state");
                    
                    // wait for client to upload 
                    file.waitForState("Write_Shared");
                }
            }

            // setting the contents  
            f = new FileContents(file.getContents());
        }


        // return file contents object to client
        return f;
    }

    /*
     * this function is called by the client once the server asks the client to writeback in which the 
     * client sets the new/updates contents of th requested file. upload() then sets the new contents 
     * in the server cache and invalidates all readers of the specified file. 
     * 
     * Parameters: client: the IP name of the client 
     *             file name: the name of the file that is being updated 
     *             contents: the updated contents of the file
     * 
     * Returns: boolean: whether or not the contents were successfully updated and all readers invalidated
     *   
     */

    public boolean upload( String client, String filename, FileContents contents ) throws RemoteException{
        // check to see if file exists in cache 
        CachedFileEntry file = findFile(filename);

        System.out.printf("Upload called: Client=%s, File=%s\n", client, filename);

        // if we have found the file in the servers cache
        if(file != null){
            // and the file state is Ownership_Change OR Write_Shared
            if((file.getState().equalsIgnoreCase("Ownership_Change"))
                    || (file.getState().equalsIgnoreCase("Write_Shared"))){
                

                // set new state if currently Ownership_Change
                if(file.getState().equalsIgnoreCase("Ownership_Change")){
                    // change state to Write_Shared
                    file.setState("Write_Shared");
                }
                // set new state if currently Write_Shared
                else if(file.getState().equalsIgnoreCase("Write_Shared")){
                    // change state to Not_Shared
                    file.setState("Not_Shared"); 
                }

                // update the file contents with the new contents 
                file.setContents(contents.get());

                // Inform all readers to invalidate the cache
                invalidateClientCaches(file.getReaders());

                // Now note that upload has been completed
                file.setLastUploadClient(client);
            }
            else{
                // if in states Not_Shared or Read_Shared, return false
                return false;
            }
        }

        return true;
    }

    /*
     * This is where the clients writeback() is called. we first use 
     * naming.lookup to find each client and then use the client 
     * interface to call writeback.
     * 
     * Parameters: client: the client that we are requesting the writeback from 
     */
    private void requestWriteBack(String client) {
        try{
            var uri = "rmi://" + client + ":" + port + "/fileclient";
            System.out.printf("Requesting Write Back from Client: %s\n", uri);
            // look up each client 
            var clientFace = (ClientInterface)Naming.lookup( uri );
            clientFace.writeback(); // call their writeback()
            System.out.printf("Request completed for Write Back from Client: %s\n", uri);
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }

    /*
     * This is where invalidate is called. it iterates through the clients list, 
     * uses naming.lookup to find each client, and invalidates their file.
     * 
     * Parameters: a vector of client IP's  
     * 
     */
    private void invalidateClientCaches(Vector<String> clients) {
        // loop through the readers of this file and invalidate for each reader/client
        for (String client : clients) {
            try{
                System.out.printf("Invalidate Client: %s\n", client);
                // find each client and establish a connection
                var clientFace = (ClientInterface)Naming.lookup( "rmi://" + client + ":" + port + "/fileclient" );
                clientFace.invalidate(); // call the clients invalidate function 
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    /*
     * checks to see if a given file is in the server cache. 
     * 
     * Parameters: a string filename which is the file we are looking for 
     * 
     * Returns: a CachedFileEntry associated to the file if found, otherwise null 
     */
    private CachedFileEntry findFile(String filename){
        for(int i = 0; i < this.cache.size(); i++){
            var file = this.cache.elementAt(i);
            if(file.getName().equals(filename)){
                return file;
            }
        }

        return null;
    }

    /*
     * This method retrieves the contents of a given file from the current 
     * directory. 
     * 
     * Parameters: string filename which is the file we are looking for 
     * 
     * Returns: a byte array of the contents 
     */
    private byte[] getFileContent(String fileName) throws RemoteException {
        // get the current directory 
        var currentDirectory = Paths.get(System.getProperty("user.dir"));
        // get the filepath of the given file 
        var filePath = currentDirectory.resolve(Paths.get(fileName));

        System.out.println("Path: " + filePath); 

        // if the file doesnt exist then return an empty byte array  
        if (!Files.exists(filePath)) {
            return new byte[0];
        }

        try {
            // otherwise return the contents of the file 
            return Files.readAllBytes(filePath);
        } catch (Exception ex) {
            throw new RemoteException(ex.toString());
        }
    }

    /*
     * This method is called right before the server is terminated. it iterates through
     * the cache and writes all the files back to the current directory.
     */
    private void writeFileToDirectory(){
        // get the current directory 
        var currentDirectory = Paths.get(System.getProperty("user.dir"));
        // iterate through the server cache 
        for(int i = 0; i < cache.size(); i++){
            String filename = cache.get(i).getName(); //filename 
            System.out.println("Filename " + filename);
            // get the file path for the file 
            var filePath = currentDirectory.resolve(Paths.get(filename));
            System.out.println("FilePath: " + filePath); 
            // use FileOutputStream to write the contents to the file 
            try(FileOutputStream fs = new FileOutputStream(filePath.toFile())){
                fs.write(cache.get(i).getContents()); 
                System.out.println("File " + filename + " written to the current directory successfully"); 

            } catch(IOException e){
                e.printStackTrace(); 
            }
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

            // create a server object
            FileServer server = new FileServer(port);
            
            // start the registry
            System.out.println("Starting Registry");
            startRegistry(port);

            System.out.println("Starting Bind");
	        Naming.rebind("rmi://localhost:" + port + "/fileserver", server);

            System.out.println("Connected");

            Scanner scan = new Scanner(System.in); //used to get the input for quit/exit
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
                    server.writeFileToDirectory();

                    System.out.println("Terminating"); 

                    // exit 
                    System.exit(0);
                }
                
            }
        } 
        catch(Exception e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    /*
     * This class is used to store a file in the server cache. 
     */
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
        public void setContents(byte[] contents) { this.contents = contents; }

        public void setState(String state) {
            synchronized(this) { 
                this.state = state; 
                notifyAll(); //when state has changed 
            }
        }
        
        // wait for the state to go back to write_shared so that execution can contine 
        // when we are currently in ownership change 
        public void waitForState(String expectedState) {
            try {
                synchronized (this) {
                    while (!this.state.equalsIgnoreCase(expectedState)) {
                        System.out.printf("waitForState: CurrentState=%s\n", this.state);
                        wait();
                    }
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }

        // if the reader is not already in the readers list, add it 
        public void addReaders(String client){ 
            if(!this.readers.equals(client)){
                this.readers.add(client); 
            }
        }

        //sets last upload client to null and wakes up the waiting threads 
        public void resetLastUploadClient() {
            synchronized (this) {
                this.lastUploadClient = null;
                notifyAll();
            }
        }

        // updates lastUploadClient with the current client and wakes up waiting threads
        public void setLastUploadClient(String client) {
            System.out.printf("setLastUploadClient: %s\n", client);
            synchronized (this) {
                this.lastUploadClient = client;
                notifyAll();
            }
        }

        //  waits until lastUploadClient is not null and then continues execution
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


