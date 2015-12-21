package test;

//File Name GreetingClient.java

import java.awt.AWTException;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.Socket;

import javax.imageio.ImageIO;

public class TestClient {
	Image newimg;
	static BufferedImage bimg;
	byte[] bytes;

	public static void main(String[] args) {
		String serverName = "localhost";
		int port = 6066;
		try {
			Socket client = new Socket(serverName, port);
			Robot bot;
			bot = new Robot();
			bimg = bot.createScreenCapture(new Rectangle(13, 23, 640, 480));
			ImageIO.write(bimg, "JPG", client.getOutputStream());
			client.close();
		} catch (IOException | AWTException e) {
			e.printStackTrace();
		}
	}
}
