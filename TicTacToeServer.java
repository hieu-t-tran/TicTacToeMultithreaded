/**
 * Implementation of the multithreaded server for a tic-tac-toe multiplayer game.
 */
import java.io.*;
import java.net.*;

public class TicTacToeServer {

	public static void main(String[] args) throws IOException {
		
		int port = 6789; // a default port number
		ServerSocket serverSocket = new ServerSocket(port);
		
		while (true) {
			System.out.println("Waiting for incoming connection...");
			
			// Initialize the arrays holding the sockets, input and output streams
			Socket[] playerSocket = new Socket[2];
			BufferedReader[] fromPlayer = new BufferedReader[2];
			DataOutputStream[] toPlayer = new DataOutputStream[2];
			
			// Handle the first player's connection
			playerSocket[0] = serverSocket.accept();
			fromPlayer[0] = new BufferedReader(new InputStreamReader(playerSocket[0].getInputStream()));
			toPlayer[0] = new DataOutputStream(playerSocket[0].getOutputStream());
			System.out.println("A player connected. Waiting for second connection...");
			toPlayer[0].writeBytes("Waiting for another player to connect...\n");
			
			// Handle the second player's connection
			playerSocket[1] = serverSocket.accept();
			fromPlayer[1] = new BufferedReader(new InputStreamReader(playerSocket[1].getInputStream()));
			toPlayer[1] = new DataOutputStream(playerSocket[1].getOutputStream());
			
			// Construct a new game instance between these two players
			GameInstance gameInstance = new GameInstance(playerSocket, fromPlayer, toPlayer);
			
			// Create a new thread for this game instance and start the thread
			Thread thread = new Thread(gameInstance);
			thread.start();
		}
	}
}

/**
 * Representation of a game instance to be run in a thread.
 */
final class GameInstance implements Runnable {
	
	Socket[] playerSocket;			// the sockets of the two players
	BufferedReader[] fromPlayer;	// the input readers for the two players
	DataOutputStream[] toPlayer;	// the output writers for the two players
	GameLogic game;					// the current game associated with this instance
	
	/**
	 * Constructor to initialize all fields for a new game instance.
	 * 
	 * @param playerSocket	the sockets of the two players
	 * @param fromPlayer	the input readers for the two players
	 * @param toPlayer		the output writers for the two players
	 */
	public GameInstance(Socket[] playerSocket, BufferedReader[] fromPlayer, DataOutputStream[] toPlayer) {
		this.playerSocket = playerSocket;
		this.fromPlayer = fromPlayer;
		this.toPlayer = toPlayer;
		game = new GameLogic();
	}
	
	/**
	 * Implement the run() method of the Runnable interface.
	 */
	public void run() {
		try {
			playGame();
		} catch (Exception e) {
			System.out.println(e);
		}
	}
	
	/**
	 * Handle all events during the game.
	 * 
	 * @throws IOException if an I/O error occurs with the sockets
	 */
	private void playGame() throws IOException {
		try {
			// System.out.println("Game start");
			
			// Send messages to both players to indicate their symbols
			toPlayer[0].writeBytes("You are player X\n");
			toPlayer[1].writeBytes("You are player O\n");
			
			int turnPlayer = 0;
			
			toPlayer[1].writeBytes("Waiting for opponent's move...\n");
			
			// Hold the value associated with the winning player, or a BLANK value if no player has won yet.
			int playerWon = game.BLANK;
			
			// Indicate if the game has ended in a draw
			boolean draw = false;
			
			while (true) {
				// System.out.println("Player " + turnPlayer + "'s turn to move");
				
				// Send messages to the current turn's player about the current board
				toPlayer[turnPlayer].writeBytes("Current board:\n");
				toPlayer[turnPlayer].writeBytes(game.toString());
				toPlayer[turnPlayer].writeBytes("Your turn to move!\n");
				
				// Read the input from the current turn's player
				int[] playerInput = readAndValidatePlayerMove(turnPlayer);
				
				// Make the move from the input received
				game.makePlayerMove(turnPlayer, playerInput[0], playerInput[1]);
				
				// System.out.println("Player " + turnPlayer + " marked on (" + playerInput[0] + "," + playerInput[1] + ")");
				
				// Check to see if the game has ended
				playerWon = game.isWinningBoard();
				draw = game.boardFilled();
				
				if (playerWon == game.BLANK && !draw) {
					// If the game has not ended, send messages about the current board after the move is made
					toPlayer[turnPlayer].writeBytes("Current board:\n");
					toPlayer[turnPlayer].writeBytes(game.toString());
					toPlayer[turnPlayer].writeBytes("Waiting for opponent's move...\n");
					
					// Switch to the other player's turn
					turnPlayer = switchTurn(turnPlayer);
				} else {
					break;
				}
			}
			
			// Send messages to both players indicating the winner of the game and the final board
			sendEndGameMessages(playerWon);
			
			// System.out.println("Terminating all connections...");
			
			// Terminate connections with both players
			for (int i=0; i<2; i++) {
				toPlayer[i].writeBytes("Terminating connection...\n");
				playerSocket[i].close();
			}
		} catch (Exception e) {
			// If an error occurs during the game, terminate connections with both players
			
			// System.out.println("A player disconnected! Terminating all connections...");
			
			// Send a message to the active player to indicate that the other player has left
			for (int i=0; i<2; i++) {
				try {
					toPlayer[i].writeBytes("The other player has left! Terminating connection...\n");
				} catch (Exception exc) {
				}
			}
			
			// Terminate connections with both players
			for (int i=0; i<2; i++) {
				playerSocket[i].close();
			}
		}
	}
	
	/**
	 * Switch turns between the two players.
	 * 
	 * @param turnPlayer	the player with the current turn
	 * 
	 * @return the next player's turn
	 */
	private int switchTurn(int turnPlayer) {
		if (turnPlayer == 0) {
			return 1;
		} else if (turnPlayer == 1) {
			return 0;
		} else {
			return -1;
		}
	}
	
	/**
	 * Read and validate the player's move location.
	 * 
	 * @param turnPlayer	the index of the current turn's player
	 * 
	 * @return an array with 2 int values indicating the location of the player's move
	 * 
	 * @throws IOException if an I/O error occurs with the sockets
	 */
	private int[] readAndValidatePlayerMove(int turnPlayer) throws IOException {
		int[] playerInput = new int[2];
		while (true) {
			try {
				// Read the player's input and return the move location if it is valid
				String[] messageReceived = fromPlayer[turnPlayer].readLine().split(" ");
				playerInput[0] = Integer.parseInt(messageReceived[0]);
				playerInput[1] = Integer.parseInt(messageReceived[1]);
				if (game.isValidMove(playerInput[0], playerInput[1])) {
					return playerInput;
				} else {
					// If the location is not valid, throw an exception to switch to the catch block
					throw new IllegalArgumentException();
				}
			} catch (Exception e) {
				// Send a message to the player and go back to the try block to wait for new input
				toPlayer[turnPlayer].writeBytes("Invalid position! Try again!\n");
				
				// System.out.println("Player sent invalid position");
			}
		}
	}
	
	/**
	 * Send messages to both players indicating the winner of the game and the final board.
	 * 
	 * @param playerWon		the winning player, or the BLANK value if the game ended in a draw
	 * 
	 * @throws IOException if an I/O error occurs with the sockets
	 */
	private void sendEndGameMessages(int playerWon) throws IOException {
		// Send information about the result of the game
		if (playerWon == game.X_PLAYER) {
			toPlayer[0].writeBytes("YOU WON!\n");
			toPlayer[1].writeBytes("YOU LOST!\n");
			
			// System.out.println("X player won!");
		} else if (playerWon == game.O_PLAYER) {
			toPlayer[1].writeBytes("YOU WON!\n");
			toPlayer[0].writeBytes("YOU LOST!\n");
			
			// System.out.println("O player won!");
		} else {
			for (int i=0; i<2; i++) {
				toPlayer[i].writeBytes("IT'S A DRAW!\n");
			}
			// System.out.println("The game ended in a draw!");
		}
		
		// Send information about the final board
		for (int i=0; i<2; i++) {
			toPlayer[i].writeBytes("Final board:\n");
			toPlayer[i].writeBytes(game.toString());
		}
	}
	
	/**
	 * Representation of the game logic for the tic-tac-toe game.
	 */
	private class GameLogic {
		
		final int X_PLAYER = 0;		// value associated with the X player
		final int O_PLAYER = 1;		// value associated with the O player
		final int BLANK = -1;		// value associated with a blank cell
		
		int[][] board;
		
		public GameLogic() {
			board = new int[3][3];
			for (int i=0; i<3; i++) {
				for (int j=0; j<3; j++) {
					board[i][j] = BLANK;
				}
			}
		}
		
		/**
		 * Determine if the move is valid.
		 * 
		 * @param row	the row position of the move
		 * @param col	the col position of the move
		 * 
		 * @return true if the move is valid, and false otherwise
		 */
		public boolean isValidMove(int row, int col) {
			if (row < 0 || row > 2 || col < 0 || col > 2 || board[row][col] != BLANK) {
				return false;
			} else {
				return true;
			}
		}
		
		/**
		 * Make a move on the board.
		 * 
		 * @param player	the player making the move
		 * @param row		the row position of the move
		 * @param col		the col position of the move
		 */
		public void makePlayerMove(int player, int row, int col) {
			if (isValidMove(row, col) && player >= X_PLAYER && player <= O_PLAYER) {
				board[row][col] = player;
			}
		}
		
		/**
		 * Determine if the current board is a winning board.
		 * 
		 * @return the value associated with the winning player, or the BLANK value otherwise
		 */
		public int isWinningBoard() {
			if (board[0][0] != BLANK && board[1][1] == board[0][0] && board[2][2] == board[0][0]) {
				return board[0][0];
				
			} else if (board[0][2] != BLANK && board[1][1] == board[0][2] && board[2][0] == board[0][2]) {
				return board[0][2];
				
			} else {
				for (int i=0; i<3; i++) {
					if (board[0][i] != BLANK && board[1][i] == board[0][i] && board[2][i] == board[0][i]) {
						return board[0][i];
					}
				}
				
				for (int i=0; i<3; i++) {
					if (board[i][0] != BLANK && board[i][1] == board[i][0] && board[i][2] == board[i][0]) {
						return board[i][0];
					}
				}
				
				return BLANK;
			}
		}
		
		/**
		 * Determine if the current board is already filled up.
		 * 
		 * @return true if the board is already filled up, and false otherwise
		 */
		public boolean boardFilled() {
			for (int i=0; i<3; i++) {
				for (int j=0; j<3; j++) {
					if (board[i][j] == BLANK) {
						return false;
					}
				}
			}
			return true;
		}
	
		/**
		 * Convert the current board into a String.
		 * 
		 * @return a String representing the current board
		 */
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (int i=0; i<3; i++) {
				for (int j=0; j<3; j++) {
					int cur = board[i][j];
					if (cur == X_PLAYER) {
						sb.append("X");
					} else if (cur == O_PLAYER) {
						sb.append("O");
					} else {
						sb.append(" ");
					}
					if (j < 2) {
						sb.append("|");
					} else {
						sb.append("\n");
					}
				}
			}
			return sb.toString();
		}
	}
}