/*
* Sarah Asad 
* CSS 434: Parallel & Distributed Computing 
*/

import java.rmi.*;
import java.util.Date;
import java.util.Vector;
import java.util.Arrays;

public class UnixClient{

    public static void main(String args[]) {
        
        // Should have at least 6 arguments 
        if(args.length < 6){
            System.out.println("Usage: java UnixClient P/C port 
            NumberOfServers ListOfServers NumberOfUnixCommands ListOfUnixCommands");
            System.exit(-1); 
        }

        String pOrC = args[0];                                              // P or C
        int port = Integer.parseInt(args[1]);                               // Port 
        int numServers = Integer.parseInt(args[2]);                         // Number of Servers 
        String[] serverList = Arrays.copyOfRange(args, 3, 3 + numServers);  // List of Servers
        int numCommands = Integer.parseInt(args[3 + numServers]);           // Number of Commands 
        String[] commands = Arrays.copyOfRange(args, 3 + numServers + 1, 
            3 + numServers + 1 + numCommands);                              // List of Commands
     
        // start the timer 
        Date startTime = new Date();

        // this is where the return value will be stored
        Vector<String> returnValue = new Vector<String>();

        ServerInterface server = null;

        for(String s : serverList){
            try{
                // look for the server instance client wants to access
                server = (ServerInterface) Naming.lookup("rmi://" + s + ":" + port + "/unixserver");
            } catch (Exception e){
                e.printStackTrace(); 
                System.exit(-1); 
            }

            // iterate through commands and run at each server 
            for(String c : commands){
                try {
                    System.out.println(s + " command: " + c); 
                    
                    // call server execute function
                    returnValue = server.execute(c);

                    // print results 
                    if(pOrC.equals("P")){
                        for(String t : returnValue){
                            System.out.println(t); 
                        }
                    }
                    else if(pOrC.equals("C")){
                        System.out.println(returnValue.size()); 
                    }

               } catch (Exception e) {}
            }
        }

        // end the timer 
        Date endTime = new Date();

        // print elapsed time 
        System.out.println( "Elapsed time = " + ( endTime.getTime() - startTime.getTime()));
    }
}

