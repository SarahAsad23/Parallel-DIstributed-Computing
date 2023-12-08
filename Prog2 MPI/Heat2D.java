import java.util.Date;

public class Heat2D {
    private static double a = 1.0;  // heat speed
    private static double dt = 1.0; // time quantum
    private static double dd = 2.0; // change in system

	//function used compute index 
	public static int index (int p, int x, int y, int size){
		return p * size * size + x * size + y;
	}
    
    public static void main( String[] args ) {
	// verify arguments
	if ( args.length != 4 ) {
	    System.out.
		println( "usage: " + `"java Heat2D size max_time heat_time interval" );
	    System.exit( -1 );
	}

	int size = Integer.parseInt( args[0] );
	int max_time = Integer.parseInt( args[1] );
	int heat_time = Integer.parseInt( args[2] );
	int interval  = Integer.parseInt( args[3] );
	double r = a * dt / ( dd * dd );

	// create a space
	double[][][] z = new double[2][size][size];
	for ( int p = 0; p < 2; p++ ) 
	    for ( int x = 0; x < size; x++ )
		for ( int y = 0; y < size; y++ )
		    z[index(p, x, y, size)] = 0.0; // no heat or cold
	
	// start a timer
	Date startTime = new Date( );
	
	// simulate heat diffusion
	for ( int t = 0; t < max_time; t++ ) {
	    int p = t % 2; // p = 0 or 1: indicates the phase
	    
	    // two left-most and two right-most columns are identical
	    for ( int y = 0; y < size; y++ ) {
		z[index(p, 0, y, size)] = z[index(p, 1, y, size)];
		z[index(p, size - 1, y, size)] = z[index(p, size - 2, y, size)];
	    }
	    
	    // two upper and lower rows are identical
	    for ( int x = 0; x < size; x++ ) {
			z[index(p, x, 0, size)] = z[index(p, x, 1, size)];
		z[index(p, x, size - 1, size)] = z[index(p, x, size - 2, size)];
	    }
	    
	    // keep heating the bottom until t < heat_time
	    if ( t < heat_time ) {
		for ( int x = size /3; x < size / 3 * 2; x++ )
		z[index(p, x, 0, size)] = 19.0; // heat
	    }

	    // display intermediate results
	    if ( interval != 0 && 
		 ( t % interval == 0 || t == max_time - 1 ) ) {
		System.out.println( "time = " + t );
		for ( int y = 0; y < size; y++ ) {
		    for ( int x = 0; x < size; x++ )
			System.out.print( (int)( Math.floor(z[p][x][y] / 2) ) 
					  + " " );
		    System.out.println( );
		}
		System.out.println( );
	    }
	    
	    // perform forward Euler method
	    int p2 = (p + 1) % 2;
	    for ( int x = 1; x < size - 1; x++ )
		for ( int y = 1; y < size - 1; y++ )
		z[index(p2, x, y, size)] = z[index(p, x, y, size)] + 
			r * ( z[index(p, x + 1, y, size)] - 2 * z[index(p, x, y, size)] + z[index(p, x - 1, y, size)] ) +
			r * ( z[index(p, x, y + 1, size)] - 2 * z[index(p, x, y, size)] + z[index(p, x, y - 1, size)] );
	    
	} // end of simulation
	
	// finish the timer
	Date endTime = new Date( );
	System.out.println( "Elapsed time = " + 
			    ( endTime.getTime( ) - startTime.getTime( ) ) );
    }
}
