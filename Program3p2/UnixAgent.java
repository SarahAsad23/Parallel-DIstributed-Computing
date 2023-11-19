/*
* Sarah Asad 
* CSS 434: Parallel & Distributed Computing 
*/

import java.io.*;
import UWAgent.*;
import java.util.Vector;
import java.util.Arrays;
import java.util.Date;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class UnixAgent extends UWAgent implements Serializable {
    
    // this is where the return value will be stored
    Vector<String> output = new Vector<String>( );  

    // keep track of the number of hops so we know when to print 
    int numHops = 0; 

    // args
    String pOrC;
    String[] serverList;
    String[] commands;
    
    public UnixAgent( String[] args ) {
	    
        // Should have at least 6 arguments 
        if(args.length < 5){
            System.out.println("Usage: java UnixClient P/C port NumberOfServers ListOfServers NumberOfUnixCommands ListOfUnixCommands");
            System.exit(-1); 
        }

        // Get all the arguments from the command line
        pOrC = args[0];                                            // P or C    
        int numServers = Integer.parseInt(args[1]);                // Number of Servers 
        serverList = Arrays.copyOfRange(args, 2, 2 + numServers);  // List of Servers
        int numCommands = Integer.parseInt(args[2 + numServers]);  // Number of Commands 
        commands = Arrays.copyOfRange(args, 2 + numServers + 1,    // List of Commands
            2 + numServers + 1 + numCommands);
    
        System.out.println( "Injected" );

        System.out.println(pOrC);
        System.out.println(numServers);
        for(String s: serverList){
            System.out.println(s);
        }
        System.out.println(numCommands);
        for(String s : commands){
            System.out.println(s);
        }
	}

    public void init(){
        
        System.out.println("in init()");

        // get the localhost so we know where to hop back 
        // when we are done executing all commands 
        String me = ""; 

        try{
            me = InetAddress.getLocalHost().getHostAddress();
        } catch(UnknownHostException e){}

        System.out.println("i am: " + me);

        
        for(String s : serverList){
            System.out.println("I'll hop to " + s);
            hop(s, "runCommands", gson.toJson(output));
            numHops++;
        }

        if(numHops == serverList.length){
            System.out.println("i am now printing: " + me); 
            hop(me, "printOut", gson.toJson(output));
        }
    }

    // print out the results
    public void printOut(){
        
        System.out.println("printOut getting called: [" + pOrC + "]");
        // print the contents of the array
        if(pOrC.equals("P")){
            for(String s : output){
                System.out.println(s);
            }
        }
        // print the size of the array 
        else if(pOrC.equals("C")){
            System.out.println(output.size());
        }
    }

    // run the command on each server 
    public void runCommands(){
        String line;

        try{
            for(String s : commands){
                System.out.println("RunCommand: " + s);
            // execute the command 
            Process process = Runtime.getRuntime().exec(s);
            // store the result
            InputStream input = process.getInputStream();
            BufferedReader bufferedInput = new BufferedReader(new InputStreamReader(input));
                while ((line = bufferedInput.readLine()) != null) {
                    output.addElement(line);
                }
            }
        }
        catch(IOException e){
            e.printStackTrace(); 
            System.exit(-1); 
        } 
    }
}
