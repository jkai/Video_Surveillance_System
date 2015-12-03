package code;

import java.net.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.imageio.ImageIO;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.*;

public class TcpServer extends Thread {

	private ServerSocket serverSocket;
	private Socket server;

	public TcpServer(int port) throws IOException {
		serverSocket = new ServerSocket(port);
		serverSocket.setSoTimeout(180000);
		System.out.print("Server started!\n");
	}

	public void run() {
		while (true) {
			try {
				// Receive images
				server = serverSocket.accept();
				BufferedImage img = ImageIO.read(ImageIO.createImageInputStream(server.getInputStream()));

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

			} catch (SocketTimeoutException st) {
				System.out.println("Socket timed out!");
				break;
			} catch (IOException e) {
				e.printStackTrace();
				break;
			} catch (Exception ex) {
				System.out.println(ex);
			}
		}
	}

}
