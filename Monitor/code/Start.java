package code;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedList;

public class Start {

	public static LinkedList<BufferedImage> ImgList = new LinkedList<BufferedImage>();
	public static Monitor monitor;
	public static TcpServer server;
	public static MonitorTimer timer;

	public static void main(String[] args) {

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
