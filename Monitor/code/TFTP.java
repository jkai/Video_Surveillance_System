package code;

import java.net.*;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.io.*;

public class TFTP {
	public static final int BUF_SIZE = 100;
	public static final int TFTP_PADDING = 0;
	public static final int OP_CODE_SIZE = 2;
	public static final int BLOCK_NUMBER_SIZE = 2;
	public static final int MAX_DATA_SIZE = 512;
	public static final int MAX_PACKET_SIZE = 516;
	public static final int READ_OP_CODE = 1;
	public static final int WRITE_OP_CODE = 2;
	public static final int DATA_OP_CODE = 3;
	public static final int ACK_OP_CODE = 4;
	public static final int ERROR_OP_CODE = 5;
	public static final int ERROR_CODE_SIZE = 2;
	public static final int ERROR_CODE_NOT_DEFINED = 0;
	public static final int ERROR_CODE_FILE_NOT_FOUND = 1;
	public static final int ERROR_CODE_ACCESS_VIOLATION = 2;
	public static final int ERROR_CODE_DISK_FULL = 3;
	public static final int ERROR_CODE_ILLEGAL_TFTP_OPERATION = 4;
	public static final int ERROR_CODE_UNKNOWN_TID = 5;
	public static final int ERROR_CODE_NO_SUCH_USER = 7;
	public static final int MIN_PORT = 1;
	public static final int MAX_PORT = 65535;
	public static final int MAX_ERROR_CODE = 7;
	public static final int MAX_OP_CODE = 5;
	public static final int MAX_BLOCK_NUMBER = 65535;
	public static final String MODE_NETASCII = "netascii";
	public static final String MODE_OCTET = "octet";
	public static int VERBOSITY = 1;

	/**
	 * Forms a DatagramPacket with an empty data buffer large enough to hold the maximum
	 * packet size
	 *
	 * @return DatagramPacket with an empty data buffer of size MAX_PACKET_SIZE + 1
	 */
	public static DatagramPacket formPacket() {
		byte[] data = new byte[MAX_PACKET_SIZE + 1];
		return new DatagramPacket(data, data.length);
	}

	/**
	 * Forms a DatagramPacket with the byte[] data passed in
	 *
	 * @param addr InetAddress of packet destination
	 * @param port Port number of packet destination
	 * @param data byte[] to use as the packet payload
	 * 
	 * @return Datagram packet for specified address and port with given request
	 */
	public static DatagramPacket formPacket(InetAddress addr, int port, byte[] data) {
		return new DatagramPacket(data, data.length, addr, port);
	}
	
	/**
	 * Forms a DatagramPacket using Request r with information about request type
	 * (read, write, or test), filename, and mode (ascii, octet, etc.).
	 *
	 * @param addr InetAddress of packet destination
	 * @param port Port number of packet destination
	 * @param r Request contains request type (READ or WRITE), filename, and mode
	 *
	 * @return Datagram packet for specified address and port with given request
	 */
	public static DatagramPacket formRQPacket(InetAddress addr, int port, Request r) {
		if (!isValidPort(port)) throw new IllegalArgumentException();
		int currentIndex;
		// Create byte array for packet
		byte[] buf = new byte[BUF_SIZE];
		// First element will always be 0
		buf[0] = TFTP_PADDING;
		switch (r.getType()) {
			case READ:
				buf[1] = 1;
				break;
			case WRITE:
				buf[1] = 2;
				break;
			default:
				buf[1] = TFTP_PADDING;
				break;
		}

		// Add filename to packet data
		byte[] fbytes = r.getFileName().getBytes();
		System.arraycopy(fbytes,0,buf,OP_CODE_SIZE,fbytes.length);

		// Add 0 byte padding
		currentIndex = fbytes.length + OP_CODE_SIZE;
		buf[currentIndex] = TFTP_PADDING;
		currentIndex++;

		// Add mode to packet data
		byte[] mbytes = r.getMode().getBytes();
		System.arraycopy(mbytes,0,buf,currentIndex,mbytes.length);

		// Add terminating 0 byte
		currentIndex = currentIndex + mbytes.length;
		buf[currentIndex] = TFTP_PADDING;

		// Truncate trailing zeros by copyings to a new array
		byte[] data = new byte[currentIndex + 1];
		System.arraycopy(buf,0,data,0,currentIndex+1);

		return new DatagramPacket(data,currentIndex+1, addr, port);
	}

	/**
	 * Forms a DatagramPacket using OPERATION,  with information about request type
	 * FILENAME, and FILENAME
	 * (read, write, or test), filename, and mode (ascii, octet, etc.).
	 *
	 * @param addr InetAddress of packet destination
	 * @param port Port number of packet destination
	 * @param operation String containing request type read or write
	 * @param fileName String containing file name
	 * @param mode String containing  the mode of transfer
	 *
	 * @return DatagramPacket for specified address and port with given request
	 */
	public static DatagramPacket formRQPacket(InetAddress addr, int port, String operation, String fileName, String mode) {
		Request.Type operationType;
		if (operation.equals("r"))
		{
			operationType = Request.Type.READ;
		}
		else if (operation.equals("w"))
		{
			operationType = Request.Type.WRITE;
		}
		else
		{
			throw new UnsupportedOperationException();
		}
		Request r = new Request(operationType, fileName, mode);
		return formRQPacket(addr, port, r);
	}

	/**
	 * Given a filename, returns a queue of datagram packets for that
	 * file in 512 byte blocks.
	 *
	 * @param addr InetAddress of packet destination
	 * @param port Port number of packet destination
	 * @param filePath path of file to read
	 *
	 * @return A queue of DATA packets formed from the file specified in 512-byte chunks
	 * @throws FileNotFoundException 
	 */
	public static Queue<DatagramPacket> formDATAPackets(InetAddress addr, int port, String filePath) throws FileNotFoundException {
		if (!isValidPort(port)) throw new IllegalArgumentException();
		if (filePath.isEmpty()) throw new IllegalArgumentException();
		Queue<DatagramPacket> packetQueue = new ArrayDeque<DatagramPacket>();
		try {
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(filePath));
			byte[] data = new byte[MAX_DATA_SIZE];
			int blockNumber = 1;
			int n, lastn;
			lastn = -1;

			//Read the file in 512 byte chunks and add to packet queue 
			while ((n = in.read(data)) != -1) {
				byte[] buf = new byte[n];
				System.arraycopy(data,0,buf,0,n);
				packetQueue.add(formDATAPacket(addr, port, blockNumber, buf));
				blockNumber = (blockNumber + 1) % (MAX_BLOCK_NUMBER + 1);
				lastn = n;
			}
			// Close stream
			in.close();
			// If the file is a multiple of 512, add a 0-byte data packet
			if (lastn == MAX_DATA_SIZE) {
				packetQueue.add(formDATAPacket(addr, port, blockNumber, new byte[0]));
			} else if (packetQueue.isEmpty()) {
				packetQueue.add(formDATAPacket(addr, port, blockNumber, new byte[0]));
			}
		} catch (FileNotFoundException e) {
			throw e;
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		return packetQueue;
	}

	/**
	 * Append a TFTP DATA packet's data to the given array byte.
	 *
	 * @param dataPacket A TFTP DATA packet
	 * @param fileBytes Byte array to append to
	 *
	 * @return New byte array with data appended
	 */
	public static byte[] appendData(DatagramPacket dataPacket, byte[] fileBytes) {
		byte[] data = getData(dataPacket);
		byte[] newFileBytes = new byte[fileBytes.length + data.length];
		// Copy old file array into new array
		System.arraycopy(fileBytes,0,newFileBytes,0,fileBytes.length);
		// Append data to array
		System.arraycopy(data,0,newFileBytes,fileBytes.length,data.length);
		return newFileBytes;
	}

	/**
	 * Write an array of bytes to a file
	 *
	 * @param filePath Name of file (including directory) to write to
	 * @param fileBytes Array of bytes to write
	 * 
	 * @return true if write successful, false if disk is full
	 */
	public static boolean writeBytesToFile(String filePath, byte[] fileBytes) {
		if (filePath.isEmpty()) throw new IllegalArgumentException();
		BufferedOutputStream out;
		try {
			out = new BufferedOutputStream(new FileOutputStream(filePath));
			out.write(fileBytes);
			out.close();
		} catch(IOException e) {
			if (e.getMessage().equals("No space left on device")) {
				return false;
			} else {
				e.printStackTrace();
				System.exit(1);
			}
		}
		return true;
	}

	/**
	 * Returns the OP code of a datagram packet as an integer.
	 *
	 * @param packet A TFTP DatagramPacket
	 *
	 * @return The OP code of the TFTP packet
	 */
	public static int getOpCode(DatagramPacket packet) {
		byte[] opCodeBytes = new byte[OP_CODE_SIZE];
		byte[] data = packet.getData();
		System.arraycopy(data,0,opCodeBytes,0,OP_CODE_SIZE);
		int opCode = toUnsignedInt(opCodeBytes[1]);
		//if (!isValidOpCode(opCode))  throw new IllegalArgumentException("DatagramPacket is not a valid TFTP packet.");
		return opCode;
	}

	public static int getModeIndex(DatagramPacket packet)
	{
		String[] errorMessage = new String[1];
		if (verifyRequestPacket(packet, errorMessage)) {
			int zeroCount = 0;
			int position = 0;
			while(zeroCount != 2 && position < packet.getLength()) {
				if (packet.getData()[position] == 0)
					zeroCount++;
				position++;
			}
			return position;
		}
		else
			return 0;
	}
	
	
	/**
	 * Checks the validity of the op code opCode supplied
	 *
	 * @param opCode int representing the op code to be tested
	 *
	 * @return boolean telling if the op code is valid
	 */
	public static boolean isValidOpCode(int opCode) {
		return opCode >= 1 && opCode <= 5;
	}

	/**
	 * Given a DATA or ACK datagram packet, returns the block number as an int.
	 *
	 * @param packet A TFTP DATA or ACK packet
	 *
	 * @return The block number of the ACK or DATA packet
	 */
	public static int getBlockNumber(DatagramPacket packet) {
		// Check that packet is either DATA or ACK
		int opCode = getOpCode(packet);
		boolean isDATA = opCode == DATA_OP_CODE;
		boolean isACK = opCode == ACK_OP_CODE;

		// If isn't DATA or ACK, throw an exception
		if (!(isDATA || isACK)) throw new IllegalArgumentException("Cannot get block number of packet that is not DATA or ACK.");

		// Get the block number as a byte array
		byte[] blockNumberBytes = new byte[BLOCK_NUMBER_SIZE];
		System.arraycopy(packet.getData(),OP_CODE_SIZE,blockNumberBytes,0,BLOCK_NUMBER_SIZE);

		// Check that the block number is valid
		int blockNumber = bytesToBlockNumber(blockNumberBytes);
		if (!isValidBlockNumber(blockNumber)) throw new IllegalArgumentException("Block number out of range.");

		return blockNumber;
	}

	/**
	 * Checks the validity of the block number blockNumber supplied
	 *
	 * @param opCode int representing the block number to be tested
	 *
	 * @return boolean telling if the block number is valid
	 */
	public static boolean isValidBlockNumber(int blockNumber) {
		return blockNumber>=0 && blockNumber<=MAX_BLOCK_NUMBER;
	}

	/**
	 * Given a TFTP DATA packet, returns the data portion of the TFTP packet 
	 * as a byte array.
	 *
	 * @param packet A TFTP DATA packet
	 *
	 * @return The data portion of a DATA packet as a byte array
	 */
	public static byte[] getData(DatagramPacket packet) {
		// Check that packet is DATA
		int opCode = getOpCode(packet);
		boolean isDATA = opCode == DATA_OP_CODE;

		// If packet isn't DATA, throw exception
		if (!isDATA) throw new IllegalArgumentException();

		// Check that the block number is valid
		if (!isValidBlockNumber(getBlockNumber(packet))) throw new IllegalArgumentException();

		int dataLen = packet.getLength() - OP_CODE_SIZE - BLOCK_NUMBER_SIZE;
		int dataStart = OP_CODE_SIZE + BLOCK_NUMBER_SIZE;
		byte[] data = new byte[dataLen];
		System.arraycopy(packet.getData(),dataStart,data,0,dataLen);

		return data;
	}

	/**
	 * Gets the error message from the ERROR PACKET
	 *
	 * @param packet A TFTP ERROR packet
	 *
	 * @return The error message from the error packet
	 */
	public static String getErrorMessage(DatagramPacket packet) {
		//If packet isn't an error, throw exception
		if(getOpCode(packet) != ERROR_OP_CODE) throw new IllegalArgumentException();

		int msgLen = packet.getLength() - OP_CODE_SIZE - BLOCK_NUMBER_SIZE - 1;
		byte[] errorMsgBytes =  new byte [msgLen];
		System.arraycopy(packet.getData(),OP_CODE_SIZE+ERROR_CODE_SIZE,errorMsgBytes,0,msgLen);

		String errorMsg = new String(errorMsgBytes);

		return errorMsg;
	}

	/**
	 * Gets the error code from the ERROR PACKET
	 *
	 * @param packet A TFTP ERROR packet
	 *
	 * @return The error code from the error packet
	 */
	public static int getErrorCode(DatagramPacket packet) {

		//If packet isn't an error, throw exception
		if(getOpCode(packet) != ERROR_OP_CODE) throw new IllegalArgumentException();

		byte[] errorCodeBytes = new byte[ERROR_CODE_SIZE];
		System.arraycopy(packet.getData(),OP_CODE_SIZE,errorCodeBytes,0,ERROR_CODE_SIZE);

		// Check that packet has a valid error code
		int errorCode = bytesToBlockNumber(errorCodeBytes);
		if (!isValidErrorCode(errorCode)) throw new IllegalArgumentException();

		return errorCode;
	}

	/**
	 * Checks the validity of the error code errorCode supplied
	 *
	 * @param opCode int representing the error code to be tested
	 *
	 * @return boolean telling if the error code is valid
	 */
	public static boolean isValidErrorCode(int errorCode) {
		return errorCode>=0 && errorCode<=MAX_ERROR_CODE;
	}

	/**
	 * Give a block number and a byte array of data, creates a datagram packet for the
	 * given IP address and port.
	 *
	 * @param addr InetAddress of DATA packet destination
	 * @param port Port number of DATA packet destination
	 * @param blockNumber The block number of the DATA packet
	 * @param data The byte array holding the data
	 *
	 * @return The respective DATA packet formed with given inputs.
	 */
	public static DatagramPacket formDATAPacket(InetAddress addr, int port, int blockNumber, byte[] data) {
		// 4+data.length because 2 bytes for op code and 2 bytes for blockNumber
		byte[] buf = new byte[OP_CODE_SIZE + BLOCK_NUMBER_SIZE +data.length];

		// Op code
		buf[0] = 0;
		buf[1] = 3;

		// Block number
		try {
			byte[] blockNumberBytes = blockNumberToBytes(blockNumber);
			System.arraycopy(blockNumberBytes,0,buf,OP_CODE_SIZE,BLOCK_NUMBER_SIZE);
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		// Data
		int startIndex = OP_CODE_SIZE + BLOCK_NUMBER_SIZE;
		System.arraycopy(data,0,buf,startIndex,data.length);

		return new DatagramPacket(buf,buf.length,addr,port);
	}

	/**
	 * Converts an integer to a 2-byte byte array.
	 *
	 * @param blockNumber Integer to be converted to a 2-byte byte array
	 *
	 * @return 2-byte representation of given block number
	 */
	public static byte[] blockNumberToBytes(int blockNumber) {
		if (blockNumber<0 || blockNumber>66535) throw new IllegalArgumentException();

		byte[] blockNumberBytes = new byte[2];
		blockNumberBytes[0] = (byte) (blockNumber / 256);
		blockNumberBytes[1] = (byte) (blockNumber % 256);

		return blockNumberBytes;
	}

	/**
	 * Converts a 2-byte byte array to an integer.
	 *
	 * @param bytes 2-byte byte array holding the block number
	 *
	 * @return Integer representation of given byte array
	 */
	public static int bytesToBlockNumber(byte[] bytes) {
		if (bytes.length != 2) throw new IllegalArgumentException();
		int msb = toUnsignedInt(bytes[0]);
		int lsb = toUnsignedInt(bytes[1]);
		return msb*256 + lsb;
	}

	/**
	 * Forms a ACK packet for the given IP address, port and block number
	 *
	 * @param addr InetAddress of ACK packet destination
	 * @param port Port number of ACK packet destination
	 * @param blockNumber Block number of the ACK packet
	 *
	 * @return ACK packet formed with given inputs
	 */
	public static DatagramPacket formACKPacket(InetAddress addr, int port, int blockNumber) {
		byte[] buf = new byte[OP_CODE_SIZE + BLOCK_NUMBER_SIZE];

		// Op code
		buf[0] = 0;
		buf[1] = 4;

		// Block number
		try {
			byte[] blockNumberBytes = blockNumberToBytes(blockNumber);
			System.arraycopy(blockNumberBytes,0,buf,OP_CODE_SIZE,BLOCK_NUMBER_SIZE);
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		return new DatagramPacket(buf,buf.length,addr,port);
	}

	/**
	 * Forms a ERROR packet given the IP address, port, and details of the error.
	 * 
	 * @param addr IP address of destination of the packet to be sent
	 * @param port Port number of destination of the packet to be sent
	 * @param errorCode Code representation of the error
	 * @param errMsg Detailed message of the error
	 * 
	 * @return ERROR packet formed with given inputs
	 */
	public static DatagramPacket formERRORPacket(InetAddress addr, int port, int errorCode, String errMsg) {
		///////////////////////////////
		// 4+data.length because 2 bytes for op code and 2 bytes for blockNumber
		byte[] sbytes = errMsg.getBytes();
		byte[] buf = new byte[OP_CODE_SIZE + BLOCK_NUMBER_SIZE + sbytes.length + 1];

		// Op code
		buf[0] = 0;
		buf[1] = ERROR_OP_CODE;

		// Block number
		// Block number
		try {
			byte[] blockNumberBytes = blockNumberToBytes(errorCode);
			System.arraycopy(blockNumberBytes,0,buf,OP_CODE_SIZE,BLOCK_NUMBER_SIZE);
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		// Data
		int startIndex = OP_CODE_SIZE + BLOCK_NUMBER_SIZE;
		System.arraycopy(sbytes, 0, buf, startIndex, sbytes.length);

		buf[OP_CODE_SIZE + BLOCK_NUMBER_SIZE + sbytes.length] = 0;

		return new DatagramPacket(buf, buf.length, addr, port);	
	}

	/**
	 * Verify validity of REQUEST PACKET and populates errorMessage[0] is an error occurs
	 * 
	 * @param packet TFTP REQUEST packet
	 * @param errorMessage String[] which is populated with error message if invalid (index 0)
	 * 
	 * @return Returns true if DATA packet matches TFTP specifications
	 */
	public static boolean verifyRequestPacket(DatagramPacket packet, String[] errorMessage)
	{
		// Stores the data that we are checking against
		byte data[] = packet.getData();
		int dataLength = data.length;
		CheckedOffset offset = new CheckedOffset(dataLength);

		try
		{
			// Check if the data packet is a valid size
			if (dataLength > MAX_PACKET_SIZE)
			{
				errorMessage[0] = "Packet too large";
				return false;
			}

			// Check if first byte is 0
			if (data[offset.getOffset()] != 0)
			{
				errorMessage[0] = "First byte is not 0";
				return false;
			}
			offset.incrementOffset(1);

			// Check if next byte is READ_OP_CODE or WRITE_OP_CODE
			if (data[offset.getOffset()] != READ_OP_CODE && data[offset.getOffset()] != WRITE_OP_CODE)
			{
				errorMessage[0] = "Invalid op code";
				return false;
			}
			offset.incrementOffset(1);

			// Check for text data (file name)
			int fileNameStartOffset = offset.getOffset();
			while (data[offset.getOffset()] != 0)
			{
				offset.incrementOffset(1);
			}

			// Calculates the fileName length using the offset
			int fileNameLength = offset.getOffset() - fileNameStartOffset;

			// Immediately return false if the length of the fileName is 0
			if (fileNameLength == 0)
			{
				errorMessage[0] = "Missing file name";
				return false;
			}

			// Check if next byte is 0
			if (data[offset.getOffset()] != 0)
			{
				errorMessage[0] = "Missing 0 byte separating file name and mode";
				return false;
			}
			offset.incrementOffset(1);

			// Check for "netascii" or "octet" (any case)
			int modeStartOffset = offset.getOffset();
			try
			{
				while (data[offset.getOffset()] != 0)
				{
					offset.incrementOffset(1);
				}
			}
			catch (Exception e)
			{
				errorMessage[0] = "Missing final 0 byte";
				return false;
			}

			// Calculates the mode length using the offset
			int modeLength = offset.getOffset() - modeStartOffset;

			// Immediately return false if the length of the mode is not the correct length
			if ((modeLength != MODE_NETASCII.length()) && (modeLength != MODE_OCTET.length()))
			{
				errorMessage[0] = "Invalid mode";
				return false;
			}

			// Constructs the mode as a string
			byte[] modeBytes = new byte[modeLength];
			System.arraycopy(data, modeStartOffset, modeBytes, 0, modeLength);
			String mode = new String(modeBytes);

			// Converts the mode to lower case and check for match
			String modeLower = mode.toLowerCase();
			if (!modeLower.equals(MODE_NETASCII) && !modeLower.equals(MODE_OCTET))
			{
				errorMessage[0] = "Invalid mode";
				return false;
			}

			// Check if next byte is 0
			if (data[offset.getOffset()] != 0)
			{
				errorMessage[0] = "Missing final 0 byte";
				return false;
			}
			offset.incrementOffset(1);

			// Check if the final position of the offset is at the last data byte
			if (offset.getOffset() != dataLength)
			{
				errorMessage[0] = "No termination after final 0 byte";
				return false;
			}

			return true;
		}
		catch (IndexOutOfBoundsException e)
		{
			errorMessage[0] = "Something is wrong with the packet";
			return false;
		}
	}

	/**
	 * Verify validity of DATA PACKET and populates errorMessage[0] is an error occurs
	 * 
	 * @param packet TFTP DATA packet
	 * @param errorMessage String[] which is populated with error message if invalid (index 0)
	 * 
	 * @return Returns true if DATA packet matches TFTP specifications
	 */
	public static boolean verifyDataPacket(DatagramPacket packet, int blockNumber, String[] errorMessage)
	{
		assert((blockNumber >= 0) && (blockNumber <= MAX_BLOCK_NUMBER));

		// Stores the data that we are checking against
		byte data[] = packet.getData();
		int dataLength = data.length;
		CheckedOffset offset = new CheckedOffset(dataLength);

		try
		{
			// Check if the data packet is a valid size
			if (dataLength > MAX_PACKET_SIZE)
			{
				errorMessage[0] = "Packet too large";
				return false;
			}

			// Check if first byte is 0
			if (data[offset.getOffset()] != 0)
			{
				errorMessage[0] = "First byte is not 0";
				return false;
			}
			offset.incrementOffset(1);

			// Check if next byte is OPCODE_DATA
			if (data[offset.getOffset()] != DATA_OP_CODE)
			{
				errorMessage[0] = "Invalid op code";
				return false;
			}
			offset.incrementOffset(1);

			// Saves the next two bytes
			byte msb_blockNumber = data[offset.getOffset()];
			offset.incrementOffset(1);
			byte lsb_blockNumber = data[offset.getOffset()];
			offset.incrementOffset(1);

			// Converts the saved two bytes into an int
			int parsedBlockNumber = toUnsignedInt(msb_blockNumber)*256 +
									toUnsignedInt(lsb_blockNumber);

			// Check if block number is larger than the currently 
			if (parsedBlockNumber > blockNumber)
			{
				errorMessage[0] = "Invalid block number";
				return false;
			}

			return true;
		}
		catch (IndexOutOfBoundsException e)
		{
			errorMessage[0] = "Something is wrong with the packet";
			return false;
		}
	}

	/**
	 * Verify validity of ACK PACKET and populates errorMessage[0] is an error occurs
	 * 
	 * @param packet TFTP ACK packet
	 * @param errorMessage String[] which is populated with error message if invalid (index 0)
	 * 
	 * @return Returns true if ACK packet matches TFTP specifications
	 */
	public static boolean verifyAckPacket(DatagramPacket packet, int blockNumber, String[] errorMessage)
	{
		assert((blockNumber >= 0) && (blockNumber <= MAX_BLOCK_NUMBER));

		// Stores the data that we are checking against
		byte data[] = packet.getData();
		int dataLength = data.length;
		CheckedOffset offset = new CheckedOffset(dataLength);

		try
		{
			// Check if the ack packet is a valid size
			if (dataLength != 4)
			{
				if (dataLength > 4)
				{
					errorMessage[0] = "Packet too large";
				}
				else
				{
					errorMessage[0] = "Packet too small";
				}
				return false;
			}

			// Check if first byte is 0
			if (data[offset.getOffset()] != 0)
			{
				errorMessage[0] = "First byte is not 0";
				return false;
			}
			offset.incrementOffset(1);

			// Check if next byte is OPCODE_ACK
			if (data[offset.getOffset()] != ACK_OP_CODE)
			{
				errorMessage[0] = "Invalid op code";
				return false;
			}
			offset.incrementOffset(1);

			// Saves the next two bytes
			byte msb_blockNumber = data[offset.getOffset()];
			offset.incrementOffset(1);
			byte lsb_blockNumber = data[offset.getOffset()];
			offset.incrementOffset(1);

			// Converts the saved two bytes into an int
			int parsedBlockNumber = toUnsignedInt(msb_blockNumber)*256 +
									toUnsignedInt(lsb_blockNumber);

			// Check if block number is greater than the number currently being sent (if smaller it is dealt with as a delay)
			if (parsedBlockNumber > blockNumber)
			{
				errorMessage[0] = "Invalid block number";
				return false;
			}

			return true;
		}
		catch (IndexOutOfBoundsException e)
		{
			errorMessage[0] = "Something is wrong with the packet";
			return false;
		}
	}

	/**
	 * Verify validity of ERROR PACKET and populates errorMessage[0] is an error occurs
	 * 
	 * @param packet TFTP ERROR packet
	 * @param errorMessage String[] which is populated with error message if invalid (index 0)
	 * 
	 * @return Returns true if ERROR packet matches TFTP specifications
	 */
	public static boolean verifyErrorPacket(DatagramPacket packet, String[] errorMessage)
	{
		// Stores the data that we are checking against
		byte data[] = packet.getData();
		int dataLength = data.length;
		CheckedOffset offset = new CheckedOffset(dataLength);

		try
		{
			// Check if first byte is 0
			if (data[offset.getOffset()] != 0)
			{
				errorMessage[0] = "First byte is not 0";
				return false;
			}
			offset.incrementOffset(1);

			// Check if next byte is OPCODE_ERROR
			if (data[offset.getOffset()] != ERROR_OP_CODE)
			{
				errorMessage[0] = "Invalid op code";
				return false;
			}
			offset.incrementOffset(1);

			// Check if next byte is 0
			if (data[offset.getOffset()] != 0)
			{
				errorMessage[0] = "Missing 0 byte separating op code and error code";
				return false;
			}
			offset.incrementOffset(1);

			// Check if next byte is a valid error code
			if ((data[offset.getOffset()] < ERROR_CODE_NOT_DEFINED) ||
				(data[offset.getOffset()] > ERROR_CODE_NO_SUCH_USER))
			{
				errorMessage[0] = "Invalid error code";
				return false;
			}
			offset.incrementOffset(1);

			// Check for text data (message)
			try
			{
				while (data[offset.getOffset()] != 0)
				{
					offset.incrementOffset(1);
				}
			}
			catch (IndexOutOfBoundsException e)
			{
				errorMessage[0] = "Missing final 0 byte";
				return false;
			}

			// Check if last byte is 0
			if (data[offset.getOffset()] != 0)
			{
				errorMessage[0] = "Missing final 0 byte";
				return false;
			}
			offset.incrementOffset(1);

			// Check if the final position of the offset is at the last data byte
			if (offset.getOffset() != dataLength)
			{
				errorMessage[0] = "No termination after final 0 byte";
				return false;
			}

			return true;
		}
		catch (IndexOutOfBoundsException e)
		{
			errorMessage[0] = "Something is wrong with the packet";
			return false;
		}
	}
	
	/**
	 * Verify the order of PACKET
	 * 
	 * @param packet TFTP ACK or DATA packet
	 * @param blockNumber int which indicates block number of PACKET
	 * 
	 * @return true if the block number of the packet is the current block number expected in the transfer
	 */
	public static boolean checkPacketInOrder(DatagramPacket packet, int blockNumber)
	{
		// Stores the data that we are checking against
		byte data[] = packet.getData();
		
		// Saves the two block number bytes
		byte msb_blockNumber = data[2];
		byte lsb_blockNumber = data[3];
		
		int parsedBlockNumber = toUnsignedInt(msb_blockNumber)*256 +
				toUnsignedInt(lsb_blockNumber);
		
		if(parsedBlockNumber == blockNumber)
		{
			return true;
		}
		else
			return false;
	}

	/**
	* Parse a given DatagramPacket p to see if it is valid. A valid packet must begin
	* with [0,1] or [0,2], followed by an arbitrary number of bytes representing the 
	* filename, followed by a 0 byte, followed by an arbitrary number of bytes representing
	* the mode, followed by a terminating 0 byte.
	* If the packet is valid, a request with the respective request type, filename, and mode
	* is created. Otherwise, an exception is thrown and the server quits.
	*
	* @param p Datagram packet to be parsed. Must either be a RRQ or WRQ packet.
	*
	* @return Request of the packet.
	*/
	public static Request parseRQ(DatagramPacket p) {
		Request.Type t = Request.Type.TEST;
		String f, m;
		int currentIndex = 0;
		int opCode = getOpCode(p);

		// Get number of bytes used by packet data
		int len = p.getData().length;
		// Make copy of data bytes to parse
		byte[] buf = new byte[len];
		System.arraycopy(p.getData(),0,buf,0,len);

		// Check second byte for read or write
		switch (opCode) {
			case 1:
				t = Request.Type.READ;
				break;
			case 2:
				t = Request.Type.WRITE;
				break;
			default:
				return null;
				//break;
		}

		// Get filename
		currentIndex = 2;
		if (currentIndex >= len) throw new IllegalArgumentException();
		// Create an array of bytes to hold filename byte data
		byte[] fbytes = new byte[len];
		// Loop through array until 0 byte is found or out of bound occurs
		while (buf[currentIndex] != TFTP_PADDING) {
			currentIndex++;
			if (currentIndex >= len) throw new IllegalArgumentException();
		}
		int filenameLength = currentIndex - 2;
		System.arraycopy(buf,2,fbytes,0,filenameLength);
		f = new String(fbytes).trim();

		// Check for 0 byte padding between filename and mode
		if (buf[currentIndex] != TFTP_PADDING) throw new IllegalArgumentException();

		// Get mode
		currentIndex++;
		if (currentIndex >= len) throw new IllegalArgumentException();
		int modeStartIndex = currentIndex;
		byte[] mbytes = new byte[len];
		// Loop through array until 0 byte is found or out of bound occurs
		while (buf[currentIndex] != TFTP_PADDING) {
			currentIndex++;
			if (currentIndex >= len) return null;
		}
		int modeLength = currentIndex - modeStartIndex;
		System.arraycopy(buf,modeStartIndex,mbytes,0,modeLength);
		m = new String(mbytes).trim();

		return new Request(t, f, m);
	}

	/**
	 * Overwrites the method toUnsignedInt in Class Byte since toUnsignedInt is
	 * only supported in JavaSE v1.8.
	 * 
	 * @param myByte Byte representation of the number.
	 * 
	 * @return Integer representation of the number given.
	 */
	public static int toUnsignedInt(byte myByte) {
		return (int)(myByte & 0xFF);
	}

	/**
	 * Checks if file supplied by FILEPATH exists
	 * 
	 * @param filePath String containing the path of the file
	 * 
	 * @return true if file exists
	 */
	public static boolean fileExists(String filePath) {
		File file = new File(filePath);
		return file.exists();
	}
	
	/**
	 * Checks if file supplied by FILEPATH is a directory
	 * 
	 * @param filePath String containing the path of the file
	 * 
	 * @return true if file is a directory
	 */
	public static boolean isDirectory(String filePath) {
		File file = new File(filePath);
		return file.isDirectory();
	}
	
	/**
	 * Checks if file supplied by FILEPATH exists
	 * 
	 * @param filePath String containing the path of the file
	 * 
	 * @return true if file exists
	 */
	public static boolean isPathless(String filePath)
	{
		Path path = Paths.get(filePath);
		// The file is pathless is the file name is the same as the file path
		return path.getFileName().toString().equals(filePath);
	}
	
	/**
	 * Gets the free space on the file system of FILEPATH
	 * 
	 * @param filePath String containing the path of the file
	 * 
	 * @return long indicating the number of free space (bytes) on file system
	 */
	public static long getFreeSpaceOnFileSystem(String filePath) {
		File file = new File(filePath);
		return file.getFreeSpace();
	}
	
	/**
	 * Checks if file supplied by FILEPATH is readable
	 * 
	 * @param filePath String containing the path of the file
	 * 
	 * @return true if file readable
	 */
	public static boolean isReadable(String filePath) {
		File file = new File(filePath);

		if (!file.canRead()) {
			return false;
		}

		try {
			FileReader fileReader = new FileReader(file.getAbsolutePath());
			fileReader.read();
			fileReader.close();
		} catch (Exception e) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Checks if file supplied by FILEPATH is writable
	 * 
	 * @param filePath String containing the path of the file
	 * 
	 * @return true if file writable
	 */
	public static boolean isWritable(String filePath) {
		File file = new File(filePath);
		return file.canWrite();
	}

	/**
	 * Deletes file
	 * 
	 * @param filePath String containing the path of the file
	 * 
	 * @return true is delete successful
	 */
	public static boolean delete(String filePath) {
		File file = new File(filePath);
		Path path = file.toPath();
		
		try {
			Files.delete(path);
		} catch (NoSuchFileException e) {
			System.out.println("No such file or directory");
			return false;
		} catch (DirectoryNotEmptyException e) {
			System.out.println("Directory not empty");
			return false;
		} catch (IOException e) {
			System.out.println("Something went wrong here");
			return false;
		}
		
		return true;
	}

	/**
	 * Checks if the port supplied is valid
	 * 
	 * @param port int containing the pot number
	 * 
	 * @return true if port is a valid port
	 */
	public static boolean isValidPort(int port) { 
		return port >= MIN_PORT && port <= MAX_PORT;
	}

	/**
	 * Truncates data buffer to fit data length of received packet
	 * 
	 * @param packet A TFTP DatagramPacket
	 */
	public static void shrinkData(DatagramPacket packet) {
		int dataLength = packet.getLength();
		byte data[] = new byte[dataLength];
		System.arraycopy(packet.getData(), 0, data, 0, dataLength);
		packet.setData(data);
	}

	/**
	 * Outputs contents of DatagramPacket based on the VERBOSITY level
	 * 
	 * @param packet A TFTP DatagramPacket
	 */
	public static void printPacket(DatagramPacket packet) {
				int operation = getOpCode(packet);
		if (VERBOSITY == 1)
		{
			switch(operation)
			{
				case READ_OP_CODE:
				case WRITE_OP_CODE:
					Request request = parseRQ(packet);
					if (request == null) {
						System.out.println("Could not print malformed packet.");
						return;
					}
					try { System.out.println(opCodeToString(operation) + " Request packet for file: " + parseRQ(packet).getFileName() + "\n"); }
					catch (ArrayIndexOutOfBoundsException e) { System.out.println("Unknown Read/Write request packet received\n"); }
					break;
				case DATA_OP_CODE:
				case ACK_OP_CODE:
					try { System.out.println(opCodeToString(operation) + " packet for block#: " + getBlockNumber(packet) + "\n"); }
					catch (ArrayIndexOutOfBoundsException e) { System.out.println("Unknown DATA/ACK packet received\n"); }
					break;
				case ERROR_OP_CODE:
					try { System.out.println(opCodeToString(operation) + " packet with message: " + getErrorMessage(packet) + "\n"); }
					catch (ArrayIndexOutOfBoundsException e) { System.out.println("Unknown ERROR packet received\n"); }
					break;
				default:
					//throw new UnsupportedOperationException();
					break;
			}
			return;
		}
		if (VERBOSITY >= 2)
		{
			System.out.println("===== Packet Info =====");
			System.out.println("Port = " + packet.getPort());
			try { System.out.println("Type = " + opCodeToString(operation)); }
			catch (UnsupportedOperationException e) { System.out.println("Type = Unknown"); }
			switch(operation)
			{
				case READ_OP_CODE:
				case WRITE_OP_CODE:
					Request request = parseRQ(packet);
					if (request == null) {
						System.out.println("Could not print malformed packet.");
						return;
					}
					try { System.out.println("File name = " + parseRQ(packet).getFileName()); }
					catch (ArrayIndexOutOfBoundsException e) { System.out.println("File name = Unknown"); }
					try { System.out.println("Mode = " + parseRQ(packet).getMode()); }
					catch (ArrayIndexOutOfBoundsException e) { System.out.println("Mode = Unknown"); }
					break;
				case DATA_OP_CODE:
				case ACK_OP_CODE:
					System.out.println("Block # = " + getBlockNumber(packet));
					break;
				case ERROR_OP_CODE:
					try { System.out.println("Error message = " + getErrorMessage(packet)); }
					catch (ArrayIndexOutOfBoundsException e) { System.out.println("Error message = Unknown"); }
					break;
				default:
					//throw new UnsupportedOperationException();
					break;
			}
		}
		if (VERBOSITY >= 3)
		{
			//System.out.println("String = " + packet.getData().toString());
			System.out.println("Data = " + Arrays.toString(packet.getData()));
		}
		System.out.println("");
	}

	/**
	 * Converts the OPERATION to its string representation
	 * 
	 * @param operation int containing the operation
	 * 
	 * @return String containing the string representation of the operation
	 */
	public static String opCodeToString(int operation) {
		switch(operation) {
			case READ_OP_CODE:
				return "Read";
			case WRITE_OP_CODE:
				return "Write";
			case DATA_OP_CODE:
				return "Data";
			case ACK_OP_CODE:
				return "ACK";
			case ERROR_OP_CODE:
				return "ERROR";
			default:
				throw new UnsupportedOperationException();
		}
	}

	/**
	 * Wrapper for array offset that throws an exception if it goes beyond an upper bound
	 * 
	 * @author Team 4
	 */
	private static class CheckedOffset
	{
		private int offset;
		private int upperBound;

		public CheckedOffset(int upperBound)
		{
			assert(upperBound > 0);
			offset = 0;
			this.upperBound = upperBound;
		}

		public int getOffset()
		{
			return offset;
		}

		public void incrementOffset(int increment)
		{
			assert(increment >= 0);
			offset += increment;
			if (offset > upperBound)
			{
				throw new IndexOutOfBoundsException();
			}
		}
	}

	/**
	 * Returns true if packet1 has the same data as packet2 and false otherwise.
	 *
	 * @param packet1 A DatagramPacket
	 * @param packet2 A DatagramPacket
	 *
	 * @return True is packet1 has the same data as packet2 and false otherwise.
	 */
	public static boolean hasSameData(DatagramPacket packet1, DatagramPacket packet2) {
		byte[] packet1Data = packet1.getData();
		byte[] packet2Data = packet2.getData();
		return Arrays.equals(packet1Data, packet2Data);
	}
}
