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
        System.out.print("hop"); 
        hop( "cssmpi2h", "step", null );
    }

    public void step(){
        System.out.print("step"); 
        hop( "cssmpi3h", "jump", null );
    }

    public void jump(){
        System.out.print("jump");
    }
}
