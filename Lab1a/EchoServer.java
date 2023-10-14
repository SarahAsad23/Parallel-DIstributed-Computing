import java.net.*;
import java.io.*;

public class EchoServer {
    public static void main( String args[] ) {
		// validate the argument
		if ( args.length != 1 ) {
			System.err.println( "usage: java TcpServer port" );
				return;
		}

		try {	
			// create a server socket
			ServerSocket server = new ServerSocket(Integer.parseInt(args[0]));
			while(true) {
				// accept a new connection from a new client
				Socket client = server.accept();

				// read a message
				InputStream is = client.getInputStream();
				byte[] data = new byte[1024];
				int message = is.read(data);

				// print it
				System.out.println(new String(data, 0, message));

				// just echo it back to the client
				OutputStream os = client.getOutputStream();
				os.write(data, 0, message); 
				os.flush(); 

				// close the connection
				client.close();
			
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}
