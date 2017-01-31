
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Eli
 */
public class Client {

    public static void main(String[] args) {

        // Formatting
        System.out.println("");

        if (args.length != 3) {
            System.out.println("Error: Expecting 3 arguments");
            System.out.println("    Example:  localhost 6003 file.txt");
            return;
        }

        int port;

        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Error: Arguement should be of type integer");
            System.out.println("    Example:  localhost 6003 file.txt");
            return;
        }

        String server = args[0];
        String fileToCopy = args[2];

        int transactionPort = GetTransactionPort(server, port);

        if (transactionPort == -1) {
            System.err.println("Error grabbing transaction port #. Client shutting down");
            return;
        }

        String errorMessage = SendFile(server, transactionPort, fileToCopy);

        if (errorMessage == null) {
            System.out.println("File succesfully uploaded to server");
        } else {
            System.err.println("Transfer Error: \n\t" + errorMessage);
        }

    }

    public static int GetTransactionPort(String server, int port) {
        try {

            Socket connection = new Socket(server, port);

            // Get the stream to write to our socket
            OutputStream output = connection.getOutputStream();

            // in stream
            Scanner s = new Scanner(connection.getInputStream());

            // Send the port we want to use for transaction
            output.write("123\n".getBytes(Charset.forName("UTF-8")));

            System.out.println("Sent 123");

            int transactionPort = s.hasNext() ? s.nextInt() : -1;

            System.out.println("Recieved over socket: " + transactionPort);

            connection.close();

            return transactionPort;

        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            return -1;
        }

    }

    public static String SendFile(String server, int port, String filename) {

        DatagramSocket socket;
        DatagramPacket inPacket = new DatagramPacket(new byte[4], 4);
        DatagramPacket outPacket = new DatagramPacket(new byte[4], 4);
        InetAddress address;
        
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
        try {
            socket = new DatagramSocket();
        } catch (SocketException ex) {
            return ex.getMessage();
        }
        
        for (int i = 0; i < 32; i ++) {
            
            // Create packet data
            try {
                outPacket.setData( ( i == 31? "BBB\4":i+"aa"+i).getBytes("US-ASCII") );
            } catch (UnsupportedEncodingException ex) {
                return ex.getMessage();
            }
            
            // Send packet data
            try {
                socket.send(outPacket);
            } catch (IOException ex) {
                return ex.getMessage();
            }
            
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
            
            System.out.print(payload);
            
        }
        
        
        return null;
        
    }

}
