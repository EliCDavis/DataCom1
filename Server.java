
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * References: https://systembash.com/a-simple-java-tcp-server-and-tcp-client/
 * http://stackoverflow.com/questions/4069028/write-string-to-output-stream
 * http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string
 *
 * @author Eli
 */
public class Server {
    
    static int transactionServerPort = 3000;
    
    public static void main(String[] args) {

        // Formatting
        System.out.println("");
        
        if (args.length != 1) {
            System.out.println("Error: Expecting port # as only arguemnet");
            System.out.println("    Example:  java Server 2000");
            return;
        }
        
        int port;
        
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Error: Arguement should be of type integer");
            System.out.println("    Example:  java Server 2000");
            return;
        }
        
        String negotiationErrorMessage = StartNegotiationServer(port);
        
        if (negotiationErrorMessage != null && negotiationErrorMessage.length() != 0) {
            System.err.println("Negotiation Error: \n\t" + negotiationErrorMessage);
            return;
        }
        
        String transferErrorMessage = StartTransferServer(transactionServerPort);
        
        if (transferErrorMessage != null && transferErrorMessage.length() != 0) {
            System.err.println("Transfer Error: \n\t" + transferErrorMessage);
            return;
        }
        
        System.out.println("Assignment Complete!");
        
    }
    
    public static String StartNegotiationServer(int port) {
        
        if (port < 1024 || port > 65535) {
            return "Invalid port #.  Must be between 1024 and 65535 (inclusive)";
        }
        
        ServerSocket server;
        
        try {
            
            server = new ServerSocket(port);
            System.out.println("TCP Negotiation Server started on port " + port + "\n");
            
            Socket listening = server.accept();

            // Get the stream to write to our socket
            OutputStream output = listening.getOutputStream();

            // Listen for input
            Scanner s = new Scanner(listening.getInputStream());
            
            System.out.println("Waiting for data...");
            
            String input = s.hasNext() ? s.next() : "";
            
            System.out.println("Recieved over socket: " + input);

            // If we recieve something from our socket
            if (input.equals("123")) {

                // Get a port currently available
                int portForTransaction = GetFreePortForTransaction();

                // Send the port we want to use for transaction
                output.write(String.valueOf(portForTransaction).getBytes(Charset.forName("UTF-8")));
                
                transactionServerPort = portForTransaction;
                
                System.out.println("Negotiation detected. Selected the following random port " + portForTransaction);
                
            }
            
            listening.close();
            
        } catch (IOException e) {
            return e.getMessage();
        }
        
        return "";
        
    }
    
    public static String StartTransferServer(int port) {
        
        DatagramSocket socket;
        DatagramPacket inPacket = new DatagramPacket(new byte[4], 4);
        DatagramPacket outPacket = new DatagramPacket(new byte[4], 4);

        // Initialize the socket
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException ex) {
            return ex.getMessage();
        }
        
        System.out.println("UDP Transfer Server started on port " + port + "\n");
        
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
            
            System.out.print("Payload: " + payload);

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
        
        return null;
    }
    
    public static int GetFreePortForTransaction() {
        return 3000;
    }
    
}
