
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A client that attempts to upload a file to a specified server
 * 
 * References:
 * http://stackoverflow.com/questions/23276407/how-to-read-from-files-with-files-lines-foreach
 *
 * @author Eli
 */
public class client {

    public static void main(String[] args) {

        // Formatting
        System.out.println("");

        // Clean Arguements
        if (args.length != 3) {
            System.out.println("Error: Expecting 3 arguments");
            System.out.println("    Example:  localhost 6003 file.txt");
            return;
        }

        // Negotiation port
        int port;

        // Try parsing port from arguments
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Error: Arguement should be of type integer");
            System.out.println("    Example:  localhost 6003 file.txt");
            return;
        }

        // Grab server name and file name from rest of arguments
        String server = args[0];
        String fileToCopy = args[2];

        // Connects to negotiation server port and get's a new port number for file transfer
        int transactionPort = GetTransactionPort(server, port);

        // If we where unable to grab the file transfer port, display error
        if (transactionPort == -1) {
            System.err.println("Error grabbing transaction port #. Client shutting down");
            return;
        }

        // Attempt to send the file specified in arguments to the port we
        // recieved from the negotiation process
        try {
            SendFile(server, transactionPort, fileToCopy);
        } catch (IOException ex) {
           System.err.println("Transfer Error: \n\t" + ex.getMessage());
           System.out.println("Transer terminated early");
        }

    }

    /**
     * Creates a TCP socket and attempts to connect to a server given
     * name and port for negotiating a port number for performing file transfer
     * 
     * @param server name such as 'localhost'
     * @param port the server is running on
     * @return port for file transfer
     */
    public static int GetTransactionPort(String server, int port) {
        try {

            Socket connection = new Socket(server, port);
            
            // Get the stream to write to our socket
            OutputStream output = connection.getOutputStream();
            
            // in stream
            Scanner s = new Scanner(connection.getInputStream());
            
            // Send the port we want to use for transaction
            output.write("123\n".getBytes("UTF-8"));

            // Read an integer from what was pushed over the wire
            int transactionPort = s.hasNext() ? s.nextInt() : -1;

            // Display port we recieved
            System.out.println("Random port: " + transactionPort + "\n");

            // clean up connections
            connection.close();

            return transactionPort;

        } catch (IOException ex) {
            Logger.getLogger(client.class.getName()).log(Level.SEVERE, null, ex);
            return -1;
        }

    }

    /**
     * Attempts to connect to the specified server name and port and send a
     * file encoded as ASCII.
     * 
     * @param server name for transfer
     * @param port for transfer
     * @param filename name of file to transfer
     * @return An error message if one occured
     * @throws IOException 
     */
    public static String SendFile(String server, int port, String filename) throws IOException {

        DatagramSocket socket;
        DatagramPacket inPacket = new DatagramPacket(new byte[4], 4);
        DatagramPacket outPacket = new DatagramPacket(new byte[4], 4);
        InetAddress address;
        BufferedReader br;
        FileReader fr;

        // Get IP server
        try {
            address = InetAddress.getByName(server);
        } catch (UnknownHostException ex) {
            return ex.getMessage();
        }

        // Setup packet route
        outPacket.setAddress(address);
        outPacket.setPort(port);

        // Initialize client socket
        socket = new DatagramSocket();

        // Open the file to send
        fr = new FileReader(client.class.getResource(filename).getPath());

        br = new BufferedReader(fr);

        int character;
        String currentPayload = "";

        // Continue reading until we get through file
        while (true) {
            
            // try reading a character in from the buffer.
            character = br.read();

            // If -1 we're end of file, else continue building payload
            if (character == -1) {
                break;
            } else {
                currentPayload += (char)character;
            }
            
            // Talk to peer if we've reached 4 characters
            if (currentPayload.length() == 4) {
                // Send data
                System.out.println(pushSocketData(socket, inPacket, outPacket, currentPayload));
                currentPayload = "";
                
            }

        }

        // Finnish up our last payload, adding \4 (End of Transmission) to 
        // signalfy we're done transfering
        currentPayload += "\4";
        System.out.println(pushSocketData(socket, inPacket, outPacket, currentPayload));

        socket.close();

        return null;

    }
    
    /**
     * Pushes data to the socket that's been opened up.
     * 
     * @param socket the socket we want to communicate with
     * @param inPacket the response will be stored in this packet
     * @param outPacket our message will be stored in this packet
     * @param payload The data we want to send over the socket
     * @return the response message from the socket
     * @throws IOException When the ack from the server is incorrect
     */
    private static String pushSocketData(DatagramSocket socket, DatagramPacket inPacket, DatagramPacket outPacket, String payload) throws IOException{
        
        // Send a packet
        outPacket.setData(payload.getBytes("US-ASCII"));
        socket.send(outPacket);

        // Get response
        socket.receive(inPacket);
        String response =  new String(inPacket.getData(), "US-ASCII");
        
        
        // Throw an error if the ack is illegal
        if(!response.equalsIgnoreCase(payload)){
            throw new IOException("Malformed ack from server");
        }
        
        // return correct response from server
        return response;
    }

    
}
