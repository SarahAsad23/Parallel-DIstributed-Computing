/*
* Sarah Asad 
* CSS 434: Parallel & Distributed Computing 
*/


import java.util.*;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;

public class UnixClient extends UnicastRemoteObject {
    private boolean print = false; // whether or not we should print contents 
    private int count = 0; //store the size 
    private Vector<String> all = new Vector<>(); // all values are stored here

    // coppied from UnixServer to see if P (otherwise C)
    UnixClient(String print) throws RemoteException {
        this.print = print.startsWith("P");
    }

    public static void main(String[] args) throws RemoteException {
        // Should have at least 6 arguments
        if (args.length < 6) {
            System.out.println("Usage: java UnixClient P/C port NumberOfServers ListOfServers NumberOfUnixCommands ListOfUnixCommands");
            System.exit(-1);
        }

        String pOrC = args[0];                                              // P or C
        int port = Integer.parseInt(args[1]);                               // Port 
        int numServers = Integer.parseInt(args[2]);                         // Number of Servers 
        String[] serverList = Arrays.copyOfRange(args, 3, 3 + numServers);  // List of Servers
        int numCommands = Integer.parseInt(args[3 + numServers]);           // Number of Commands 
        String[] commands = Arrays.copyOfRange(args, 3 + numServers + 1, 
            3 + numServers + 1 + numCommands);                              // List of Commands

        UnixClient myClient = new UnixClient(pOrC); // initialize client 

        Date startTime = new Date(); // start the timer 

        ServerInterface[] servers = new ServerInterface[numServers]; 

        // establish a connection to each server 
        for (int i = 0; i < numServers; i++) { 
            try {
                // look for the server instance client wants to access
                servers[i] = (ServerInterface) Naming.lookup("rmi://" + serverList[i] + ":" + port + "/unixserver");
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }

        // execute the commands 
        myClient.executeCommands(servers, commands);

        //if P then we print the contents of the vector 
        if (myClient.print) {
            for (String s : myClient.all) {
                System.out.println(s);
            }
        }
        // if C then we print the size of the vector 
        else{
            System.out.println("Count: " + myClient.count);
        }

        Date endTime = new Date(); // end the timer 
        // compute and print the elapsed time
        System.out.println("Elapsed time = " + (endTime.getTime() - startTime.getTime()));

        System.exit(0); // force exit otherwise stays stuck 
    }

    // Executes all commands at each server 
    private void executeCommands(ServerInterface[] servers, String[] commands) {
        // this is where the return value will be stored
        Vector<String> returnValue;

        // iterate through server list 
        for (ServerInterface server : servers) {
            // and commands 
            for (String command : commands) {
                try {
                    // call server execute function
                    returnValue = server.execute(command);
                    //if P 
                    if (print) {
                        // then add all the values to all vector for later printing 
                        all.add("running command " + command);
                        all.addAll(returnValue);
                    }
                    // compute the size 
                    count += returnValue.size();
                } catch (Exception e) {
                    e.printStackTrace(); 
                }
            }
            all.add(""); // separates different server outputs
        }
    }
}
