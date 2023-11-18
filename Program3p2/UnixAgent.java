import java.io.*;
import UWAgent.*;

public class UnixAgent extends UWAgent implements Serializable {
    private String destination = null;
    
    public AnAgent( String[] args ) {
	    // Should have at least 6 arguments 
        if(args.length < 6){
            System.out.println("Usage: java UnixClient P/C port 
            NumberOfServers ListOfServers NumberOfUnixCommands ListOfUnixCommands");
            System.exit(-1); 
        }

        // Get all the arguments from the command line
        String pOrC = args[0];                                              // P or C
        int port = Integer.parseInt(args[1]);                               // Port 
        int numServers = Integer.parseInt(args[2]);                         // Number of Servers 
        String[] serverList = Arrays.copyOfRange(args, 3, 3 + numServers);  // List of Servers
        int numCommands = Integer.parseInt(args[3 + numServers]);           // Number of Commands 
        String[] commands = Arrays.copyOfRange(args, 3 + numServers + 1, 
            3 + numServers + 1 + numCommands);
    

    
    System.out.println( "Injected" );
	
    destination = args[0];
    }

    public void init( ) {
	System.out.println( "I'll hop to " + destination );
	String[] args = new String[1];
	args[0] = "hello";
	hop( destination, "func", args );
    }

    public void func( String[] args ) {
	System.out.println( args[0] );
    }
}
