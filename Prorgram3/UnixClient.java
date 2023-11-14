/*
* Sarah Asad 
* CSS 434: Parallel & Distributed Computing 
* unixClient.java
*/

import java.rmi.*;
import java.util.Date;

public static void main (String args[]) {
    //java UnixClient P/C port NumberOfServers ListOfServers 
    //NumberOfUnixCommands ListOfUnixCommands
    
    if(args.length != 6){
        System.out.println("Usage: java UnixClient P/C port NumberOfServers 
            ListOfServers NumberOfUnixCommands ListOfUnixCommands")
        System.exit(-1); 
    }

    String pOrC = args[0];                                             // P or C
    int port = Integer.parseInt(args[1]);                              // Port 
    int numServers = Integer.parseInt(args[2]);                        // Number of Servers 
    String[] serverList = Arrays.copyOfRange(args, 3, 3 + numServers); // List of Servers
    int numCommands = Integer.parseInt(3 + numServers + 1);            // Number of Commands 
    String[] commands = Arrays.copyOfRange(args, 3 + numServers + 1, 
        3 + numServers + 1 + numCommands);                             // List of Commands 

    //start the timer 
    Date startTime = new Date();

    for(String s : serverList){
        try{
            //look for the server instance client wants to access
            ServerInterface server = (ServerInterface)
                Naming.lookup("rmi://" + s + ":" + port + "/UnixServer");
        } catch (Exception e){
            e.printStackTrace(); 
            System.exit(-1); 
        }

        // then we want to call server execute function 
        // this is where the return value will be stored 
        Vector<String> returnValue = new Vector<>(); 

        try {
            returnValue = server.execute();
        } catch (Exception e) {}
    }
    
    //end the timer 
    Date endTime = new Date();

    //print results
    System.out.println( "Elapsed time = " + ( endTime.getTime() - startTime.getTime()));
}