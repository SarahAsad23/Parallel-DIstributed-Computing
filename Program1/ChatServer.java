/**
 * Sarah Asad 
 * CSS 434: Parallel and Distibuted Computing
 * 
 * ChatServer.java: receives message from client and broadcasts that message
 * to all exisiting and available server connections
*/

import java.net.*; // for Socket and ServerSocket
import java.io.*; // for IOException
import java.util.*; // for Vector

public class ChatServer{
    //a list of existing client connection  
    List<socket> connections = new ArrayList<>(); 

    /*
     * Creates a server socket with a given port and goes into an 
     * infinite loop where: 
     * 
     * 1. accepts a new connection if there is one 
     * 2. adds this connection into a list of existing connections 
     * 3. for each connection, reads a new message and write it to all exisiting connections
     * 4. deletes the connection if it is already disconnected 
     * 
     * @param port: an IP port 
     */
    
    public ChatServer(int port){
        try{
            //create a server socket 
            ServerSocket server = new ServerSocket(port); 
            //will be blocked for 500ms upon accept 
            server.setSoTimeout(500); 
            
            while(true){
                //accept a new connection 
                Socket clientSocket = server.accept(); 
                //if this connection is not null 
                if(clientSocket != null){
                    //add the new connection into the list of existing connections
                    connection.add(clientSocket); 
                }
                 
                //for each connection, read a new message and write it to all existing connections 
                for(int i = 0; i < connections.length; i++){
                    // read a new message if exist 
                    // make sure that this read won't be blocked.
                    String msg = connections[i].readMessage(); 
                    // if you got a message, write it to all connections.
                    for(int j = 0; l < connections.length; j++){
                        if(j != i){
                            connections[j].writeMessage(msg); 
                        }
                    }
                    
                    //close and delete this connection if the client disconnected it
                    if(msg == null){
                        connections[i].close();
                        connections.remove(i); 
                        i--;  
                    }
                    
                }
            }
        }
        catch(IOException e){
            e.printStackTrace(); 
        }
    }

    /*
     * Usage: javaa CharServer <port> 
     * @params args: a string array where args[0] includes port 
     */

     public static void main(String args[]){
        //check if args[0] has port 
        if(args.length != 1){
            system.err.println("Syntax: java ChatServer <port>"); 
            system.exit(1); 
        }

        //start a chat server 
        new CharServer(integer.parseInt(args[0])); 
     }

     /*
      * Represents a connection fron a different chat client 
      */

      private class Connection{
        private Socket socket;          //a socket of this connection 
        private InputStream rawIn;      //a byte-stream input from client 
        private OutputStream rawOut;    //a byte-stream output to client 
        private DataInputStream in;     //a filterd input from client 
        private DataOutputStream out;   //a filtered output to client 
        private String name;            //a client name 
        private boolean alive;          //indicates if the connection is alive

        /*
         * creates a new connection with a givn socket 
         * 
         * @param client: a cocket representing a new chat client 
         */

        public Connection(Socket client){
            socket = client; 
           
            try{
                //from socket, initializes rawIn, rawOut, in, and out 
                rawIn = client.getInputStream(); 
                rawOut = client.getOutputStream(); 
                in = new DataInputStream(rawIn);
                out = new DataOutputStream(rawOut); 

                //the first message is the client name in unicode format 
                //upon successful initialization, alive should be true 
                alive = true; 
            }
            catch(IOException e){
                alive = false; 
            }
        }

        /*
         * reads a new message in unicode format and returnd it with this clients name
         * 
         * @return: a unicode message eith the clients name 
         */

        public String readMessage(){
            String s = "";
            try{
                //the message is available and there are still bytes to read 
                if(in.available() > 0){
                    //read the message
                    s = in.readUTF(); 
                    return s; 
                }
            }
            catch(IOException e){
                e.printStackTrace(); 
            }

            //otherwise, skip reading
            return null;  
             
        }

        /*
         * writes a given message through this clients socket 
         * 
         * @param message: a string to wrote to the client 
         */

        public void writeMessage(String message){
            //write a message 
            try{
                out.writeUTF(message); 
                //use flush() to send it immediately 
                out.flush(); 
            }
            //if an exception occurs, you can identify that this connection was gone 
            catch(IOException e){
                e.printStackTrace(); 
                clientSocket.close(); 
            }
        }

        /*
         * checks if the connection is still alive 
         */

         public boolean isAlive(){
            //if the connection is broken, return false
            if(clientSocket.isConnected()){
                return true; 
            }
            return false; 
         }
    }
}



