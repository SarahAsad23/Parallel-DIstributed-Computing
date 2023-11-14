import java.io.*;
import UWAgent.*;

public class MyAgent extends UWAgent implements Serializable {
    private String destination = null;
   
    public MyAgent( String[] args ) {
        System.out.println( "Injected" );
        destination = args[0];
    }

    public MyAgent( ) { 
        System.out.println( "Injected" );
        destination = "localhost";
    }

    public void init( ) {
        System.out.println( "I'll hop to " + destination );
        String[] args = new String[1];
        args[0] = "hello";
        System.out.print("hop"); 
        hop( "cssmpi2h", "step", args );
    }

    public void step(){
        System.out.print("step"); 
        hop( "cssmpi3h", "jump", args );
    }

    public void jump(){
        System.out.print("jump");
    }

    public void func( String[] args ) {
	    System.out.println( args[0] );
    }
}
