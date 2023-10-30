import mpi.*;

public class MyProgram {
    private final static int aSize = 100; // the size of dArray
    private final static int master = 0;  // the master rank
    private final static int tag = 0;     // Send/Recv's tag is always 0.

    public static void main(String[] args) throws MPIException {
        // Start the MPI library.
        MPI.Init(args);

        // Compute my own stripe
        int stripe = aSize / MPI.COMM_WORLD.Size(); // each portion of the array
        double[] dArray = null;

        if (MPI.COMM_WORLD.Rank() == 0) { // master
            // Initialize dArray[100].
            dArray = new double[aSize];
            for (int i = 0; i < aSize; i++)
                dArray[i] = i;

            for (int rank = 1; rank < MPI.COMM_WORLD.Size(); rank++) {
                // Send a portion of dArray[100] to each slave
                // Stripe * rank is the offset of what portion to send to each rank - where we start sending
                MPI.COMM_WORLD.Send(dArray, stripe * rank, stripe, MPI.DOUBLE, rank, tag);
            }
        } else { // slaves: rank 1 to rank n - 1
            // Allocate dArray[stripe].
            dArray = new double[stripe];

            // Receive a portion of dArray[100] from the master
            MPI.COMM_WORLD.Recv(dArray, 0, stripe, MPI.DOUBLE, master, tag);
        }

        // Compute the square root of each array element
        for (int i = 0; i < stripe; i++)
            dArray[i] = Math.sqrt(dArray[i]);

        if (MPI.COMM_WORLD.Rank() == 0) { // master
            for (int rank = 1; rank < MPI.COMM_WORLD.Size(); rank++) {
                // Receive answers from each slave
                MPI.COMM_WORLD.Recv(dArray, stripe * rank, stripe, MPI.DOUBLE, rank, tag);
            }

            // Print out the results
            for (int i = 0; i < aSize; i++)
                System.out.println("dArray[" + i + "] = " + dArray[i]);
        } else { // slaves: rank 1 to rank n - 1
            // Send the results back to the master
            MPI.COMM_WORLD.Send(dArray, 0, stripe, MPI.DOUBLE, master, tag);
        }

        // Terminate the MPI library.
        MPI.Finalize();
    }
}
