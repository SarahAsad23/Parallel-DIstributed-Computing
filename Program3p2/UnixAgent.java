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
    Vector<String> output = new Vector<String>();  

    // keep track of the number of hops so we know when to print 
    int numHops = 0; 

    String me = ""; // origin 

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
	}

    public void init(){
        
        System.out.println("in init()");

        // get the localhost so we know where to hop back 
        // when we are done executing all commands 
        try{
            me = InetAddress.getLocalHost().getHostAddress();
        } catch(UnknownHostException e){}

        // start the timer
        //Date startTime = new Date();

        // hop to the first server in the list 
        System.out.println("I'll hop to " + serverList[0]);
        numHops++; 
        hop(serverList[0], "runCommands", commands);
            
        //end the timer 
        //Date endTime = new Date();
        // print elapsed time 
        //System.out.println( "Elapsed time = " + ( endTime.getTime() - startTime.getTime()));
    }

    // print out the results
    public void printOut(String[] arr){
        
        System.out.println("printOut getting called: [" + pOrC + "]");
        // print the contents of the array
        if(pOrC.equals("P")){
            for(String s : arr){
                //System.out.println("I AM P");
                System.out.println(s);
                //System.out.println("i am done printing P");
            }
        }
        // print the size of the array 
        else if(pOrC.equals("C")){
            //System.out.println("I AM C"); 
            System.out.println(arr.length);
            //System.out.println("i am done printing C");
        }
    }

    // run the command on each server 
    public void runCommands(String[] commands){
        String line;

        try{
            for(String s : commands){
                System.out.println("Running Command " + s);
                // execute the command 
                Process process = Runtime.getRuntime().exec(s);
                // store the result
                InputStream input = process.getInputStream();
                BufferedReader bufferedInput = new BufferedReader(new InputStreamReader(input));
                while ((line = bufferedInput.readLine()) != null) {
                    System.out.println(line); 
                    output.addElement(line);
                }
            }
        }
        catch(IOException e){
            e.printStackTrace(); 
            System.exit(-1); 
        } 

        System.out.println("Server List Length: " + serverList.length + " NumHops: " + numHops); 

        if(numHops != serverList.length){
            System.out.println("I'll hop to " + serverList[numHops]); 
            numHops++;
            int index = numHops - 1; 
            hop(serverList[index], "runCommands", commands);
            
            
        }
        else if(numHops == serverList.length){
            System.out.println("I'll hop back to origin " + me);
            String[] outputArray = output.toArray(new String[output.size()]);
            hop(me, "printOut", outputArray);
        }
    }
}
