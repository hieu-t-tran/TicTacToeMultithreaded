/**
 * Implementation of the player client for a tic-tac-toe multiplayer game.
 */
import java.io.*;
import java.net.*;

public class TicTacToeClient {
	
	// A BufferedReader that reads input from the user
	private BufferedReader inFromUser;
	
	// A Socket that connects to the game server
	private Socket clientSocket;

	// A DataOutputStream that sends output to server
	private DataOutputStream outToServer;

	// A BufferedReader that reads input from the server
	private BufferedReader inFromServer;
	
	/**
	 * Initialize the client with the required attributes
	 * 
	 * @param ipAddress		the IP address of the game server
	 * @param port			the port number of the game server
	 * @param inFromUser	reader to read input from user
	 * 
	 * @throws UnknownHostException if the server cannot be found
	 * @throws IOException if an I/O exception occurs with the socket
	 */
	public TicTacToeClient(String ipAddress, int port, BufferedReader inFromUser) throws UnknownHostException, IOException {
		clientSocket = new Socket(ipAddress, port);
		outToServer = new DataOutputStream(clientSocket.getOutputStream());
		inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));	
		this.inFromUser = inFromUser;
	}
	
	/**
	 * Play the game by exchanging input between the user and the server
	 * 
	 * @throws IOException if an I/O exception occurs with the socket
	 */
	public void play() throws IOException {
		String serverMsg = "";
		try {
			while ((serverMsg = inFromServer.readLine()) != null) {
				if (serverMsg.startsWith("Invalid position! Try again!") || 
						serverMsg.startsWith("Your turn to move")) {
					// If it is the current player's turn or the player has just made an invalid move, ask the user to make a move
					System.out.println("Please enter the position for your move:");
					
					String move = inFromUser.readLine();
					
					System.out.println("Move: " + move);
					
					outToServer.writeBytes(move + "\n");
	
				}
				else if (serverMsg.startsWith("Terminating connection")) {
					// If the server signals that the game has ended, stop receiving server input
					System.out.println(serverMsg);
					break;
				}
				else {
					// Any other message should simply be printed out to the user
					System.out.println(serverMsg);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// Clean up after the game has ended
			System.out.println("GAME ENDED!");
			clientSocket.close();
			inFromServer.close();
			outToServer.close();
		}
	}
	
	public static void main(String[] args) throws UnknownHostException, IOException {
		
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
		
		// Get the game server's information and initialize the client
		System.out.println("Please enter server's IP address: ");
		String serverIP = inFromUser.readLine();
		System.out.println("Please enter server's port number: ");
		int serverPort = Integer.parseInt(inFromUser.readLine());
		TicTacToeClient client = new TicTacToeClient(serverIP, serverPort, inFromUser);
		client.play();
	}
}