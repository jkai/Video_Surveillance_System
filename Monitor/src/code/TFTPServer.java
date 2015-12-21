package code;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.imageio.ImageIO;

public class TFTPServer extends Thread {
	private static int RECEIVE_PORT = 69;
	private static int TIMEOUT = 2000; 	//Maximum time to wait for response before timeout and re-send packet: 2 seconds (2000ms)
	private static int RESEND_LIMIT = 3; //Maximum number of times to try re-send packet without response: 3
	private DatagramSocket receiveSocket;
	private boolean verbose = true;
	
	public TFTPServer() throws IOException {
		try {
			receiveSocket = new DatagramSocket(RECEIVE_PORT);
			receiveSocket.setSoTimeout(5000);
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void run() {
		System.out.println("Server started.");

		while (true) {
			// Form packet for reception
			DatagramPacket packet = TFTP.formPacket();

			// Receive packet
			try {
				//if (verbose) System.out.println("Waiting for request from client...");
				receiveSocket.receive(packet);
				TFTP.shrinkData(packet);
				System.out.println("A request was received.");
			} catch(Exception e) {
				if (e instanceof InterruptedIOException) {
					//System.out.println("Socket timeout.");
					continue;
				} else {
					e.printStackTrace();
					System.exit(1);
				}
			}

			// Start a handler to connect with client
			handleConnection(packet);
		}
	}
	
	public void handleConnection(DatagramPacket packet) {
		InetAddress replyAddr = packet.getAddress();
		int TID = packet.getPort();
		DatagramPacket initialPacket = packet;
		DatagramSocket socket = null;
		
		try {
			socket = new DatagramSocket();

			socket.setSoTimeout(TIMEOUT);	
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		
		String[] errorMessage = new String[1];
		// Check that packet is a valid RRQ/WRQ 
		if (!TFTP.verifyRequestPacket(initialPacket, errorMessage))
		{
			DatagramPacket errorPacket = TFTP.formERRORPacket(
					replyAddr,
					TID,
					TFTP.ERROR_CODE_ILLEGAL_TFTP_OPERATION,
					errorMessage[0]);

			try {
				socket.send(errorPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}

			System.out.println("Sent ERROR packet with ERROR code " + TFTP.ERROR_CODE_ILLEGAL_TFTP_OPERATION + ": Request packet malformed. Aborting transfer...\n");

			socket.close();
			return;
		}
		Request r = TFTP.parseRQ(initialPacket);

		switch (r.getType()) {
		case READ:
			// unsupported
			break;
		case WRITE:
			handleWrite(r, replyAddr, TID, socket);
			break;
		default: break;
		}
		
	}

	private void handleWrite(Request r, InetAddress replyAddr, int TID, DatagramSocket socket) {
		try {
			String fileName = r.getFileName();
			int currentBlockNumber = 1;
			DatagramPacket receivePacket;
			byte[] fileBytes = new byte[0];

			boolean packetInOrder;

			// Form and send ACK0
			DatagramPacket ackPacket = TFTP.formACKPacket(replyAddr, TID, 0);
			if (verbose) System.out.println("Sending ACK 0.");
			socket.send(ackPacket);

			// Flag set when transfer is finished
			boolean transferComplete = false;

			do {
				// Wait for a DATA packet
				if (verbose) System.out.println("Waiting for DATA from client...");
				receivePacket = TFTP.formPacket();

				for(int i = 0; i<RESEND_LIMIT+1; i++) {
					try {
						socket.receive(receivePacket);
						break;		//If packet successfully received, leave loop
					} catch(SocketTimeoutException e) {
						//if re-send attempt limit reached, 'give up' and cancel transfer
						if(i == RESEND_LIMIT) {
							System.out.println("No response from client after " + RESEND_LIMIT + " attempts. Try again later.");
							socket.close();
							return;
						}
					}
				}


				TFTP.shrinkData(receivePacket);

				InetAddress packetAddress = receivePacket.getAddress();
				int packetPort = receivePacket.getPort();
				if (!(packetAddress.equals(replyAddr) && (packetPort == TID))) {
					// Creates an "unknown TID" error packet
					DatagramPacket errorPacket = TFTP.formERRORPacket(
							packetAddress,
							packetPort,
							TFTP.ERROR_CODE_UNKNOWN_TID,
							"The address and port of the packet does not match the TID of the ongoing transfer.");

					// Sends error packet
					socket.send(errorPacket);

					// Echo error message
					if (verbose) System.out.println("Sent ERROR packet with ERROR code " + TFTP.getErrorCode(errorPacket) + ": Received packet from an unknown host. Discarding packet and continuing transfer...\n");
					continue;
				}

				// This block is entered if the packet received is not a valid DATA packet
				String[] errorMessage = new String[1];
				if (!TFTP.verifyDataPacket(receivePacket, currentBlockNumber, errorMessage)) {
					// If an ERROR packet is received instead of the expected DATA packet, delete the file
					// and abort the transfer
					String[] errorMessage2 = new String[1];
					if (TFTP.verifyErrorPacket(receivePacket, errorMessage2)) {
						if (verbose) System.out.println("Received ERROR packet with ERROR code " + TFTP.getErrorCode(receivePacket) + ": " + TFTP.getErrorMessage(receivePacket) + ". Aborting transfer...\n");
						return;
					}
					// If the received packet is not a DATA or an ERROR packet, then send an illegal TFTP
					// operation ERROR packet and abort the transfer
					else {
						// Creates an "illegal TFTP operation" error packet
						DatagramPacket errorPacket = TFTP.formERRORPacket(
								replyAddr,
								TID,
								TFTP.ERROR_CODE_ILLEGAL_TFTP_OPERATION,
								fileName + " could not be transferred because of the following error: " + errorMessage[0] + " (server expected a DATA packet with block#: " + currentBlockNumber + ")");

						// Sends error packet
						socket.send(errorPacket);

						// Echo error message
						if (verbose) System.out.println("Sent ERROR packet with ERROR code " + TFTP.getErrorCode(errorPacket) + ": Illegal TFTP Operation. Aborting transfer...\n");
						return;
					}
				}

				// Transfer is complete if data block is less than MAX_DATA_SIZE
				if (receivePacket.getLength() < TFTP.MAX_PACKET_SIZE) {
					transferComplete = true;
				}

				// Echo successful data receive
				if (verbose) System.out.println("DATA " + TFTP.getBlockNumber(receivePacket) + " received.");
				// Newline
				if (verbose) System.out.println();

				packetInOrder = TFTP.checkPacketInOrder(receivePacket, currentBlockNumber);

				//If the packet was the correct next sequential packet in the transfer (not delayed/duplicated)
				if(packetInOrder){
					// Write the data packet to file
					fileBytes = TFTP.appendData(receivePacket, fileBytes);
				}

				// Form a ACK packet to respond with
				ackPacket = TFTP.formACKPacket(replyAddr, TID, TFTP.getBlockNumber(receivePacket));
				if (verbose) System.out.println("Sending ACK " + TFTP.getBlockNumber(ackPacket) + ".");
				socket.send(ackPacket);

				//Increment next block number expected only if the last packet received was the correct sequentially expected one 
				if(packetInOrder){
					currentBlockNumber = (currentBlockNumber + 1) % (TFTP.MAX_BLOCK_NUMBER + 1);
				}

			} while (!transferComplete);

			// Create image
			BufferedImage img = ImageIO.read(new ByteArrayInputStream(fileBytes));

			// Add time to the image
			DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			Calendar cal = Calendar.getInstance();
			Graphics g = img.getGraphics();
			g.setFont(g.getFont().deriveFont(15f));
			g.setColor(Color.RED);
			g.drawString(dateFormat.format(cal.getTime()), 30, 30);
			g.dispose();

			// Add img to the shared list
			Start.getImgList().add(img);
			System.out.print("Added one img to list!\n");
		} catch(Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	/*private void handleWrite(Request r, InetAddress replyAddr, int TID, DatagramSocket socket) {
		try {
			String fileName = r.getFileName();
			String directory = "C:/temp/server/";
			String filePath = directory + fileName;
			int currentBlockNumber = 1;
			DatagramPacket receivePacket;
			byte[] fileBytes = new byte[0];

			boolean packetInOrder;

			// There is an error if the file exists and it not writable
			if (TFTP.fileExists(filePath) && !TFTP.isWritable(filePath)) {
				// Creates a "access violation" error packet
				DatagramPacket errorPacket = TFTP.formERRORPacket(
						replyAddr,
						TID,
						TFTP.ERROR_CODE_ACCESS_VIOLATION,
						"You do not have write access to the file \"" + fileName + "\".");

				// Sends error packet
				socket.send(errorPacket);

				// Echo error message
				if (verbose) System.out.println("Sent ERROR packet with ERROR code " + TFTP.ERROR_CODE_ACCESS_VIOLATION + ": File access violation. Aborting transfer...\n");

				// Closes socket and aborts thread
				socket.close();
				return;
			}

			// Form and send ACK0
			DatagramPacket ackPacket = TFTP.formACKPacket(replyAddr, TID, 0);
			if (verbose) System.out.println("Sending ACK 0.");
			socket.send(ackPacket);

			// Flag set when transfer is finished
			boolean transferComplete = false;

			do {
				// Wait for a DATA packet
				if (verbose) System.out.println("Waiting for DATA from client...");
				receivePacket = TFTP.formPacket();

				for(int i = 0; i<RESEND_LIMIT+1; i++) {
					try {
						socket.receive(receivePacket);
						break;		//If packet successfully received, leave loop
					} catch(SocketTimeoutException e) {
						//if re-send attempt limit reached, 'give up' and cancel transfer
						if(i == RESEND_LIMIT) {
							System.out.println("No response from client after " + RESEND_LIMIT + " attempts. Try again later.");
							socket.close();
							return;
						}
					}
				}


				TFTP.shrinkData(receivePacket);

				InetAddress packetAddress = receivePacket.getAddress();
				int packetPort = receivePacket.getPort();
				if (!(packetAddress.equals(replyAddr) && (packetPort == TID))) {
					// Creates an "unknown TID" error packet
					DatagramPacket errorPacket = TFTP.formERRORPacket(
							packetAddress,
							packetPort,
							TFTP.ERROR_CODE_UNKNOWN_TID,
							"The address and port of the packet does not match the TID of the ongoing transfer.");

					// Sends error packet
					socket.send(errorPacket);

					// Echo error message
					if (verbose) System.out.println("Sent ERROR packet with ERROR code " + TFTP.getErrorCode(errorPacket) + ": Received packet from an unknown host. Discarding packet and continuing transfer...\n");
					continue;
				}

				// This block is entered if the packet received is not a valid DATA packet
				String[] errorMessage = new String[1];
				if (!TFTP.verifyDataPacket(receivePacket, currentBlockNumber, errorMessage)) {
					// If an ERROR packet is received instead of the expected DATA packet, delete the file
					// and abort the transfer
					String[] errorMessage2 = new String[1];
					if (TFTP.verifyErrorPacket(receivePacket, errorMessage2)) {
						if (verbose) System.out.println("Received ERROR packet with ERROR code " + TFTP.getErrorCode(receivePacket) + ": " + TFTP.getErrorMessage(receivePacket) + ". Aborting transfer...\n");
						return;
					}
					// If the received packet is not a DATA or an ERROR packet, then send an illegal TFTP
					// operation ERROR packet and abort the transfer
					else {
						// Creates an "illegal TFTP operation" error packet
						DatagramPacket errorPacket = TFTP.formERRORPacket(
								replyAddr,
								TID,
								TFTP.ERROR_CODE_ILLEGAL_TFTP_OPERATION,
								fileName + " could not be transferred because of the following error: " + errorMessage[0] + " (server expected a DATA packet with block#: " + currentBlockNumber + ")");

						// Sends error packet
						socket.send(errorPacket);

						// Echo error message
						if (verbose) System.out.println("Sent ERROR packet with ERROR code " + TFTP.getErrorCode(errorPacket) + ": Illegal TFTP Operation. Aborting transfer...\n");
						return;
					}
				}

				// Transfer is complete if data block is less than MAX_DATA_SIZE
				if (receivePacket.getLength() < TFTP.MAX_PACKET_SIZE) {
					transferComplete = true;
				}

				// Echo successful data receive
				if (verbose) System.out.println("DATA " + TFTP.getBlockNumber(receivePacket) + " received.");
				// Newline
				if (verbose) System.out.println();

				packetInOrder = TFTP.checkPacketInOrder(receivePacket, currentBlockNumber);

				//If the packet was the correct next sequential packet in the transfer (not delayed/duplicated)
				if(packetInOrder){

					// Write the data packet to file
					fileBytes = TFTP.appendData(receivePacket, fileBytes);
					if ((fileBytes.length*TFTP.MAX_DATA_SIZE) > TFTP.getFreeSpaceOnFileSystem(directory)) {
						// Creates a "file not found" error packet
						DatagramPacket errorPacket = TFTP.formERRORPacket(
								replyAddr,
								TID,
								TFTP.ERROR_CODE_DISK_FULL,
								"\"" + r.getFileName() + "\" could not be transferred because disk is full.");

						// Sends error packet
						try {
							socket.send(errorPacket);
						} catch (Exception e) {
						}

						// Echo error message
						if (verbose) System.out.println("Sent ERROR packet with ERROR code " + TFTP.ERROR_CODE_DISK_FULL + ": Disk full. Aborting transfer...\n");

						// Closes socket and aborts thread
						socket.close();
						return;
					}
				}

				// Form a ACK packet to respond with
				ackPacket = TFTP.formACKPacket(replyAddr, TID, TFTP.getBlockNumber(receivePacket));
				if (verbose) System.out.println("Sending ACK " + TFTP.getBlockNumber(ackPacket) + ".");
				socket.send(ackPacket);

				//Increment next block number expected only if the last packet received was the correct sequentially expected one 
				if(packetInOrder){
					currentBlockNumber = (currentBlockNumber + 1) % (TFTP.MAX_BLOCK_NUMBER + 1);
				}

			} while (!transferComplete);

			// Write data to file
			TFTP.writeBytesToFile(directory + r.getFileName(), fileBytes);
			System.out.println("\nWrite complete.\n");
		} catch(Exception e) {
			System.out.println(e.getMessage());
		}
	}*/

	/*public static void main(String[] args) throws IOException {
		TFTPServer server = new TFTPServer();
		server.run();
	}*/

}
