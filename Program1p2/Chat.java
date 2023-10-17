import java.net.*;       // ServerSocket, Socket
import java.io.*;        // InputStream, ObjectInputStream, ObjectOutputStream
import java.util.Vector; //Vector 

public class Chat {
    // Each element i of the follwoing arrays represent a chat member[i]
    private Socket[] sockets = null;             // connection to i
    private InputStream[] indata = null;         // used to check data from i
    private ObjectInputStream[] inputs = null;   // a message from i
    private ObjectOutputStream[] outputs = null; // a message to i

    private int[] vector = null;        //vector stamp 
    private Vector<int[]> vec_queue;    //maintains vector stamps from the others
    private Vector<String> msg_queue;   //maintain actual messagge from the others 
    private Vector<integer> src_queue;  //maintain source ID's
    
    
    /**
     * Is the main body of the Chat application. This constructor establishes
     * a socket to each remote chat member, broadcasts a local user's message
     * to all the remote chat members, and receive a message from each of them.
     *
     * @param port  IP port used to connect to a remote node as well as to
     *              accept a connection from a remote node.
     * @param rank  this local node's rank (one of 0 through to #members - 1)
     * @param hosts a list of all computing nodes that participate in chatting
     */

    public Chat(int port, int rank, String[] hosts) throws IOException {
        //print out my port, rank, and local hostname 
        System.out.println("port = " + port + ", rank = " + rank + ", localhost = " + hosts[rank]);

        //create the sockets, inputs, outputsm and vector arrays 
        sockets = new socket[hosts.length]; 
        indata = new InputStream[hosts.length];
        inputs = new ObjectInputStream[hosts.length]; 
        outputs = new ObjectOutputStream[hosts.length]; 
        vector = new int[hosts.length];

        //establish a complete network
        ServerSocket server = new ServerSocket(port); 
        for(int i = hosts.length - 1; i >= 0; i--){
            if(i > rank){
                Socket socket = server.accept(); 
                String src_host = socket.getInetAddress().getHostName(); 

                //find the sources host rank
                for(int j = 0; j < host.length; j++){
                    if(src_host.startsWith(hosts[j])){
                        //j is the source host's rank 
                        System.out.println("accepted from " + src_host);

                        //store the source host j's connection, input stream and object input/output streams
                        sockets[j] = socket; 
                        indata[j] = socket.getInputStream(); 
                        inputs[j] = new ObjectInputStream(indata[j]); 
                        outputs[j] = new ObjectOutputStream(socket.getOutputStream()); 
                    }
                }

                if(i < rank){
                    //establish a connection to others with lower rank 
                    socket[i] = new scoket(hosts[i], port); 
                    System.out.println("connected to" + hosts[i]); 

                    //store the destination host i's connection, input stream,
                    //and object input/output streams 
                    outputs[i] = new ObjectOutputStream(sockets[i]. getOutputStream()); 
                    indata[i] = sockets[i].getInputStream(); 
                    inputs[i] = new ObjectInputStream(indata[i]);  
                }
                
            }

        } 

        //create a keyboard stream 
        BufferedReader keyborad = new BufferedReader(new InputStreamReader(System.in)); 
        while(true){
            //read the message from the keyboard and broadcast it to all the others 
            if(keyboard.ready()){
                //since the keyboard is ready, read the line 
                String message = keyboard.readLine(); 
                if(message == null){
                    //keyboard closed by "^d"
                    break; //terminate the program 
                }

                vector[rank] += 1; 
                for(int i = 0; i < hosts.length; i++){
                    if(i != rank){
                        //send message to everyone but yourself
                        outputs[i].writeObject(vector); 
                        outputs[i].writeObject(message); 
                        outputs[i].flush(); 
                    }
                }

                //read the message from each of the chat memebers 
                for(int i = 0; i < hosts.length; i++){
                    //for intentional message misordering, slow down chat memeber #2
                    try{
                        if(rank == 2){
                            Thread.currentThread.sleep(5000); //sleep for 5 seconds 
                        }catch(InterruptedException e) {}

                        //check if chat member i has something 
                        if(i != rank && indata[i].available() > 0){
                            try{

                                int[] vec = (int[]) inputs.readObject(); //this will be a vector
                                String message = (String) inputs[i].readObject(); //actual message 

                                

                                System.out.println(hosts[i] + ": " + message);





                                
                                
                            } catch(ClassNotFoundException e) {}
                        }
                    }
                }
            }
        }
    }


    /**
     * Is the main function that verifies the correctness of its arguments and
     * starts the application.
     *
     * @param args receives <port> <ip1> <ip2> ... where port is an IP port
     *             to establish a TCP connection and ip1, ip2, .... are a
     *             list of all computing nodes that participate in a chat.
     */
    public static void main(String[] args) {
        // verify #args.
        if (args.length < 2) {
            System.err.println("Syntax: java Chat <port> <ip1> <ip2> ...");
            System.exit(-1);
        }

        // retrieve the port
        int port = 0;

        try{
            port = Integer.parseInt(args[0]); 
        }
        catch(NumberFormatException e){
            e.printStackTrace(); 
            System.exit(-1); 
        }
	
        if ( port <= 5000 ) {
            System.err.println("port should be 5001 or larger");
            System.exit(-1);
        }

        // retireve my local hostname
        String localhost = null;
        try {
            localhost = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        // store a list of computing nodes in hosts[] and check my rank
        int rank = -1;
        String[] hosts = new String[args.length - 1];
        for (int i = 0; i < args.length - 1; i++) {
            hosts[i] = args[i + 1];
            if (localhost.startsWith(hosts[i])) 
                // found myself in the i-th member of hosts
                rank = i;
        }

        // now start the Chat application
        try {
            new Chat(port, rank, hosts);
        } 
        catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
