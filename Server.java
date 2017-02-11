
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Scanner;

/**
 * A simple socketed server implementation that uses TCP for port negotiation
 * that is then used for UDP file transfer.  Files are written to output.txt
 * 
 * References: https://systembash.com/a-simple-java-tcp-server-and-tcp-client/
 * http://stackoverflow.com/questions/4069028/write-string-to-output-stream
 * http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string
 *
 * @author Eli
 */
public class server {

    public static void main(String[] args) {

        // Make sure port is passed in
        if (args.length != 1) {
            System.out.println("Error: Expecting port # as only arguemnet");
            System.out.println("    Example:  java Server 2000");
            return;
        }

        // Port for negotiation
        int port;

        // Try parsing integer out of arguemnt
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Error: Arguement should be of type integer");
            System.out.println("    Example:  java Server 2000");
            return;
        }

        // A socket for file transfer
        DatagramSocket socketForTransfer = null;
        
        // Listen Negotiation
        try {
             socketForTransfer = StartNegotiationServer(port);
        } catch (IOException ex) {
            System.err.println("Negotiation Error: \n\t" + ex.getMessage());
        }

        // Start server for file transfer and catch errors
        String transferErrorMessage = StartTransferServer(socketForTransfer);

        // Print out error message if one exists
        if (transferErrorMessage != null && transferErrorMessage.length() != 0) {
            System.err.println("Transfer Error: \n\t" + transferErrorMessage);
        }

    }

    /**
     * Starts a TCP socket server for clients to connect to.  Returns a UDP socket
     * that clients will later talk with.
     * 
     * @param port number for negotiation
     * @return a socket for file transfer
     * @throws IOException 
     */
    public static DatagramSocket StartNegotiationServer(int port) throws IOException {

        // Make sure port is valid
        if (port < 1024 || port > 65535) {
            throw new IOException("Invalid port #.  Must be between 1024 and 65535 (inclusive)");
        }

        // Start the server
        ServerSocket server = new ServerSocket(port);
        
        // Listen for data on the wire
        Socket listening = server.accept();

        // Get the stream to write to our socket
        OutputStream output = listening.getOutputStream();

        // Listen for input
        Scanner s = new Scanner(listening.getInputStream());

        // Grab a message
        String input = s.hasNext() ? s.next() : "";

        // Display message
        System.out.println(input+"\n");

        DatagramSocket portForTransaction = null;
        
        // If we recieve start code from our socket
        if (input.equals("123")) {

            // Get a port currently available
            portForTransaction = GetRandomSocketForTransaction();

            
            // Send the port we want to use for transaction
            output.write(String.valueOf(portForTransaction.getLocalPort()).getBytes("UTF-8"));

            System.out.println("Negotiation detected. Selected the following random port " + portForTransaction.getLocalPort() + "\n");

        }

        listening.close();

       return portForTransaction;

    }

    /**
     * Takes a UDP socket and begins listening for file transfer
     * 
     * @param socket
     * @return errors that occur in the process
     */
    public static String StartTransferServer(DatagramSocket socket) {

        DatagramPacket inPacket = new DatagramPacket(new byte[4], 4);
        DatagramPacket outPacket = new DatagramPacket(new byte[4], 4);
        PrintWriter writer;
        
        // Initialize file we're going to be writing too.
        try {
            writer = new PrintWriter(new File("./output.txt"));
        } catch (IOException ex) {
            return ex.getMessage();
        }

        // Stay in this state till someone sends an EOT character
        while (true) {

            // Grab a packet of data
            try {
                socket.receive(inPacket);
            } catch (IOException ex) {
                return ex.getMessage();
            }

            String payload;

            // Grab payload
            try {
                payload = new String(inPacket.getData(), "US-ASCII");
            } catch (UnsupportedEncodingException ex) {
                return ex.getMessage();
            }

            // Write payload to output
            writer.write(payload.split("\4")[0]);
            
            // Create new payload
            try {
                outPacket.setData(payload.toUpperCase().getBytes("US-ASCII"));
            } catch (UnsupportedEncodingException ex) {
                return ex.getMessage();
            }

            // Set routing for return to sender
            outPacket.setSocketAddress(inPacket.getSocketAddress());

            // Send payload
            try {
                socket.send(outPacket);
            } catch (IOException ex) {
                return ex.getMessage();
            }

            // End if found END OF TRANSMISSION
            if (payload.contains("\4")) {
                break;
            }

        }
        
        // Close everything up
        socket.close();
        writer.close();

        return null;
    }

    /**
     * Creates a socket between 1024 and 65535
     * 
     * @return a UDP Socket with a random port 
     */
    public static DatagramSocket GetRandomSocketForTransaction() {
        // Look for a port until we find one
        while(true){
            try {
                // Try creating a socket between 1024 and 65535
                return new DatagramSocket(1024 + (int)(Math.random() * 64511));
            } catch (SocketException ex) {
                
            }
        }
    }

}
