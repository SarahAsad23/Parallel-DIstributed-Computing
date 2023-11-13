/*
* Sarah Asad 
* CSS 434: Parallel & Distributed Computing 
* unixClient.java
*/

import java.rmi.*;

public static void main (String args[]) {
    try{
        //look for the server instance client wants to access
        ServerInterface server = ( ServerInterface )
        Naming.lookup( “rmi://serverIp:serverPort/symbolic_name” );
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