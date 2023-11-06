import java.util.Date;
import mpi.*; 

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
		println( "usage: " + "java Heat2D size max_time heat_time interval" );
	    System.exit( -1 );
	}

	//initialize MPI 
	MPI.Init(args); 

	int myRank = MPI.COMM_WORLD.Rank(); //my rank 
	int mpiSize = MPI.COMM_WORLD.Size(); //number of members

	int size = Integer.parseInt( args[0] );
	int max_time = Integer.parseInt( args[1] );
	int heat_time = Integer.parseInt( args[2] );
	int interval  = Integer.parseInt( args[3] );
	double r = a * dt / ( dd * dd );

	//compute the number of strips we need to have 
	int stripe = size / mpiSize; 
	int remainder = size % mpiSize;
	
	//compute the beginning and end of each ranks stripe 
	int begin = (myRank < remainder) ? (stripe * myRank + myRank) : (stripe * myRank + remainder); 
	int end = begin + stripe + ((myRank < remainder) ? 1 : 0) - 1; 

	// create a 1D array representing 3d space 
	double[] z = new double[2 * size * size];

	// start a timer
	Date startTime = new Date( );
	
	// simulate heat diffusion
	for ( int t = 0; t < max_time; t++ ) {
	    int p = t % 2; // p = 0 or 1: indicates the phase

		// Calculate Eulers equation for local stripe 
	    
		for(int x = begin; x <= end; x++){
			if(x == 0 || x == size - 1) continue; 
			for(int y = 1; y < size - 1; y++){
				z[index(p, x, y, size)] = z[index(p, x, y, size)] + 
				r * ( z[index(p, x + 1, y, size)] - 2 * z[index(p, x, y, size)] + z[index(p, x - 1, y, size)] ) +
				r * ( z[index(p, x, y + 1, size)] - 2 * z[index(p, x, y, size)] + z[index(p, x, y - 1, size)] );
			}
		}

		//synchronize after computing local stripe - make sure each rank has finished their local computation 
		MPI.COMM_WORLD.Barrier(); 

		// to prevent deadlocks, we will make sure all even ranks first send   
		// and then recieve all odd ranks will first receive and then send

		// i am an even rank 
		if(myRank % 2 == 0){
			// send left and receive from right 
			if(myRank > 0){
				MPI.COMM_WORLD.send(z, index(p, begin, 0, size), size, MPI.DOUBLE, myRank - 1, 0);
				MPI.COMM_WORLD.Recv(z, index(p, begin - 1, 0, size), size, MPI.DOUBLE, myRank - 1, 0);
			}
			// send right and recieve from left 
			if(myRank < mpiSize - 1){
				MPI.COMM_WORLD.Send(z, index(p, end, 0, size), size, MPI.DOUBLE, myRank + 1, 0);
				MPI.COMM_WORLD.Recv(z, index(p, end + 1, 0, size), size, MPI.DOUBLE, myRank + 1, 0);

			}
		}
		// i am an odd rank 
		else{
			// receive from right and send to left 
			if(myRank < mpiSize - 1){
				MPI.COMM_WORLD.Recv(z, index(p, end + 1, 0, size), size, MPI.DOUBLE, myRank + 1, 0);
                MPI.COMM_WORLD.Send(z, index(p, end, 0, size), size, MPI.DOUBLE, myRank + 1, 0);
			}
			if(myRank > 0){
				MPI.COMM_WORLD.Recv(z, index(p, begin - 1, 0, size), size, MPI.DOUBLE, myRank - 1, 0);
                MPI.COMM_WORLD.Send(z, index(p, begin, 0, size), size, MPI.DOUBLE, myRank - 1, 0);
			}
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
	    
	    
	    
	} // end of simulation
	
	// finish the timer
	Date endTime = new Date( );
	System.out.println( "Elapsed time = " + ( endTime.getTime( ) - startTime.getTime( ) ) );	
	MPI.Finalize(); 
    }
}


