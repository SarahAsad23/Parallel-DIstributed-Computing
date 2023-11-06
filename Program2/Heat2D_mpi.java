import java.util.Date;
import mpi.*; 

public class Heat2D_mpi {
    private static double a = 1.0;  // heat speed
    private static double dt = 1.0; // time quantum
    private static double dd = 2.0; // change in system

	//function used compute index 
	public static int index (int p, int x, int y, int size){
		return p * size * size + x * size + y;
	}

	public static void display(int rank, String op, int toRank) {
		System.out.println("Rank " + rank + ": " + op + " => " + toRank);
	}

	//add throws MPIException or else lots of errors 
    public static void main( String[] args ) throws MPIException{
	
	// verify arguments
	if ( args.length != 4 ) {
	    System.out.
		println( "usage: " + "java Heat2D_mpi size max_time heat_time interval" );
	    System.exit( -1 );
	}

	System.out.println("Starting.....................");

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

	System.out.println("Slice Range: myRank=" + myRank + ", begin=" + begin + ", end=" + end); 

	// create a 1D array representing 3d space 
	double[] z = new double[2 * size * size];

	// start a timer
	Date startTime = new Date( );
	
	// simulate heat diffusion
	for ( int t = 0; t < max_time; t++ ) {
	    int p = t % 2; // p = 0 or 1: indicates the phase


	    // two left-most and two right-most columns are identical
		// Only do it if the myRank is zero and you should be working on this slice.
		if (begin == 0) {
			for ( int y = 0; y < size; y++ ) {
				z[index(p, 0, y, size)] = z[index(p, 1, y, size)];
			}
		}

		if (begin <= (size - 2) && end <= size) {
			for ( int y = 0; y < size; y++ ) {
				z[index(p, size - 1, y, size)] = z[index(p, size - 2, y, size)];
			}
		}

	    // two upper and lower rows are identical
		// Only do it for the part of the row that this slice should operate on.
	    for ( int x = begin; x < end; x++ ) {
			z[index(p, x, 0, size)] = z[index(p, x, 1, size)];
			z[index(p, x, size - 1, size)] = z[index(p, x, size - 2, size)];
	    }

	    // keep heating the bottom until t < heat_time
		// Only do it for the assigned slice.
	    if ( t < heat_time ) {
			int startX = Math.max(size / 3, begin);
			int endX = Math.min(2 * size / 3, end);
			for ( int x = startX; x < endX; x++ ) {
				z[index(p, x, 0, size)] = 19.0; // heat
			}
		}

		// Calculate Eulers equation for local stripe 
		int p2 = (p + 1) % 2;
		for(int x = begin + 1; x < end - 1; x++){
			for(int y = 1; y < size - 1; y++){
				z[index(p2, x, y, size)] = z[index(p, x, y, size)] + 
				r * ( z[index(p, x + 1, y, size)] - 2 * z[index(p, x, y, size)] + z[index(p, x - 1, y, size)] ) +
				r * ( z[index(p, x, y + 1, size)] - 2 * z[index(p, x, y, size)] + z[index(p, x, y - 1, size)] );
			}
		}

		//synchronize after computing local stripe - make sure each rank has finished their local computation 
		MPI.COMM_WORLD.Barrier(); 

		// to prevent deadlocks, we will make sure all even ranks first send   
		// and then recieve all odd ranks will first receive and then send

		// i am an even rank 
		int slice_size = (end - begin) * size;
		int slice_width = (end - begin);

	if (myRank % 2 == 0) {
			// Compute the size of the slice.
			//System.out.println("slice_size=" + slice_size + ", slice_width=" + slice_width);

			// Sending to the worker on the left (if not the first one).
			if (myRank > 0) {
				//display(myRank, "Send", myRank - 1);
				MPI.COMM_WORLD.Send(z, index(p, begin, 0, size), slice_size, MPI.DOUBLE, myRank - 1, 0);
			}

			if (myRank < mpiSize - 1) {
				// Sending to the worker on the right (if not the last one).
				//display(myRank, "Send", myRank + 1);
				MPI.COMM_WORLD.Send(z, index(p, begin, 0, size), slice_size, MPI.DOUBLE, myRank + 1, 0);

				// Receive from right (if not the last one.)
				//display(myRank, "Recv", myRank + 1);
				MPI.COMM_WORLD.Recv(z, index(p, end + 1, 0, size), slice_size, MPI.DOUBLE, myRank + 1, 0);
			}

			if (myRank > 0) {
				//display(myRank, "Recv", myRank - 1);
				MPI.COMM_WORLD.Recv(z, index(p, begin - slice_width, 0, size), slice_size, MPI.DOUBLE, myRank - 1, 0);
			}
		}
		// i am an odd rank
		else {

			if (myRank < mpiSize - 1) {
				// Receive from the worker on the right.
				//display(myRank, "Recv", myRank + 1);
				MPI.COMM_WORLD.Recv(z, index(p, end + 1, 0, size), slice_size, MPI.DOUBLE, myRank + 1, 0);
			}

			if (myRank > 0) {
				// Recive from left.
				//display(myRank, "Recv", myRank - 1);
				MPI.COMM_WORLD.Recv(z, index(p, begin - slice_width, 0, size), slice_size, MPI.DOUBLE, myRank - 1, 0);

				// Send to the left.
				//display(myRank, "Send", myRank - 1);
				MPI.COMM_WORLD.Send(z, index(p, begin, 0, size), slice_size, MPI.DOUBLE, myRank - 1, 0);
			}

			if (myRank < mpiSize - 2) {
				// Send to the right.
				//display(myRank, "Send", myRank + 1);
				MPI.COMM_WORLD.Send(z, index(p, begin, 0, size), slice_size, MPI.DOUBLE, myRank + 1, 0);
			}
		}		
	

	// display intermediate results
	//System.out.println("rank = " + myRank + ", Displaying Results v2");

	if (myRank == 0) {
		for ( int y = 0; y < size; y++ ) {
			for ( int x = 0; x < size; x++ ) {
				System.out.print((int)( Math.floor(z[index(p, x, y, size)] / 2) ) + " " );
				//System.out.print(z[index(0, x, y, size)] + " " );
				//System.out.print( (int)( Math.floor(z[p][x][y] / 2) ) 		+ " " );
	  		}	
			
			System.out.println( );
		}
		System.out.println( );
	}

		//System.out.println("rank = " + myRank + ", time = " + t );
	} // end of simulation
	

	// finish the timer
	Date endTime = new Date( );
	System.out.println( "Elapsed time = " + ( endTime.getTime( ) - startTime.getTime( ) ) );	
	MPI.Finalize(); 
    }
}


