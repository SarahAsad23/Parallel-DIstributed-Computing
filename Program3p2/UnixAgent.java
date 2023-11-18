/*
* Sarah Asad 
* CSS 434: Parallel & Distributed Computing 
*/

import java.io.*;
import UWAgent.*;

public class UnixAgent extends UWAgent implements Serializable {
    
    // get the localhost so we know where to hop back 
    // when we are done executing all commands 
    String localHost = InetAddress.getLocalHost(); 
    String me = localHost.getHostAddress();   

    // this is where the return value will be stored
    Vector<String> output = new Vector<String>( );  

    // keep track of the number of hops so we know when to print 
    int numHops = 0; 
    
    public UnixAgent( String[] args ) {
	    
        // Should have at least 6 arguments 
        if(args.length < 5){
            System.out.println("Usage: java UnixClient P/C port 
            NumberOfServers ListOfServers NumberOfUnixCommands ListOfUnixCommands");
            System.exit(-1); 
        }

        // Get all the arguments from the command line
        String pOrC = args[0];                                              // P or C    
        int numServers = Integer.parseInt(args[1]);                         // Number of Servers 
        String[] serverList = Arrays.copyOfRange(args, 2, 2 + numServers);  // List of Servers
        int numCommands = Integer.parseInt(args[2 + numServers]);           // Number of Commands 
        String[] commands = Arrays.copyOfRange(args, 2 + numServers + 1,    // List of Commands
            2 + numServers + 1 + numCommands);
    
        System.out.println( "Injected" );
	}

    public void init( ) {
        if(numHops == serverList.size()){
            hop(me, "printOut", null)
        }

        for(String s : serverList){
            System.out.println( "I'll hop to " + s );
            hop(s, "runCommands", null)
            numHops++;
        }
    }

    // print out the results
    public void printOut(){
        // print the contents of the array
        if(porC = "P"){
            for(String s : output){
                System.out.println(s);
            }
        }
        // print the size of the array 
        else if(PorC == "C"){
            System.out.println(output.size());
        }
    }

    // run the command on each server 
    public void runCommands(){
        for(String s : commands){
            try{
                // execute the command 
			    Process process = Runtime.getRuntime().exec(c);
                // store the result
                InputStream input = process.getInputStream();
			    BufferedReader bufferedInput = new BufferedReader(new InputStreamReader(input));
			    while ((line = bufferedInput.readLine()) != null) {
			        output.addElement(line);
			    }
            }
            catch(IOException e){
                e.printStackTrace(); 
                System.exit(-1); 
            } 
        }
    }
}
