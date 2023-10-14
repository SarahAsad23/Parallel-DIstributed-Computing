/**
* ChatClient.java: realizes communication with other clients through a central chat server.
*/

import java.net.*; // for Socket
import java.io.*; // for IOException

public class ChatClient {
    private Socket socket;        //a socket connection to the chat server
    private InputStream rawIn;    //a input stream from the server 
    private DataInputStream in;   //a filtered input stream from the server 
    private DataOutputStream out; //a filtered output stream to the server
    private BufferedReader stdin; //a standard input 

    /*
     * Creates a socket, contacts the server with the given server ip name
     * and port then sents a given calling user name and goes into "while"
     * loop in which 
     * 
     * 1. forwards a message from the standard input to the server 
     * 2. forwards a messafe from the server to the standard output 
     * 
     * @param name the calling user name
     * @param server a server ip nam
     * @param port a server port
     */

     public ChatClient(String name, String server, int port){
        //create a socket, register, and listen to the server 
        try{
            //connect to the server 
            socket = new Socket(server, port); 
            rawIn = socket.getInputStream(); 
            //create input and output and the standard output stream 
            in = new DataInputStream (rawIn); 
            out = new DataOutputStream(scoket.getOutputStream()); 
            stdin = new BufferedReader(new InputStreamReader(system.in)); 
            //send the client name to the server 
            out.writeUTF(name); 

            while(true){
                //if the user types something from the keyboard, read it from
                //the standard input and simply forward it to the server 
                if(stdin.ready()){
                    String s = stdin.readLine(); 
                    //no more keyboard input: user typed ^d
                    if(s == null){
                        break;
                    }
                    out.writeUTF(s); 
                }

                //if the server gives me a message, read it from the server
                //and write id down to the standard input
                if(rawIn.available() > 0){
                    String s = in.readUTF(); 
                    system.out.println(s); 
                }
            }

            //close the connection 
            socket.close(); 
        }
        catch(Exception e){
            e.printStackTrace(); 
        }
     }

    /*
    * usage: java ChatClient <yourName> <ServerIpName> <port> 
    * 
    * @param args Recieves a client user name, a server IP name, 
    * and irs port in args[0], and args[2] respectively 
    */

    public static void main(String args[]){
    //check number of arguments 
    if(args.length != 3){
        system.err.println("Syntax: java ChatClient <yourNamr> <ServerIpName> <port>"); 
        system.exit(1); 
    }
    //convert args[2] into int that will be used a s port 
    int port = integer.parseInt(args[2]); 
    new ChatClient(args[0], args[1], port); 
    }
}
