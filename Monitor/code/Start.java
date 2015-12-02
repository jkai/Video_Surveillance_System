package code;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import javax.imageio.ImageIO;

public class Start {

	public static LinkedList<BufferedImage> ImgList = new LinkedList<BufferedImage>();
	public static Monitor monitor;
	public static TcpServer server;
	public static MonitorTimer timer;

	private static BufferedImage temp;

	public static void main(String[] args) {

		// Temp add img to list for test
		try {
			temp = ImageIO.read(new File("M:\\Lab5\\Monitor\\src\\resource\\pic1.jpg"));
			ImgList.add(temp);
			temp = ImageIO.read(new File("M:\\Lab5\\Monitor\\src\\resource\\pic2.jpg"));
			ImgList.add(temp);

		} catch (IOException e) {
			System.out.print("Image reading error!\n");
		}

		// TCP server initial
		try {
			server = new TcpServer(6066);

		} catch (IOException e) {
			e.printStackTrace();
		}

		// Start Timer
		timer = new MonitorTimer();

		// Start Monitor
		monitor = new Monitor();
		monitor.initial();

	}

	public static LinkedList<BufferedImage> getImgList() {
		return Start.ImgList;
	}

	public static TcpServer getServer() {
		return Start.server;
	}

	public static Monitor getMonitor() {
		return Start.monitor;
	}

	public static MonitorTimer getTimer() {
		return Start.timer;
	}

}
