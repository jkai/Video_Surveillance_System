package code;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedList;

public class Start {

	public static LinkedList<BufferedImage> ImgList = new LinkedList<BufferedImage>();
	public static Monitor monitor;
	public static TFTPServer server;
	public static MonitorTimer timer;

	public static void main(String[] args) {

		// TFTP server initial
		try {
			server = new TFTPServer();

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

	public static TFTPServer getServer() {
		return Start.server;
	}

	public static Monitor getMonitor() {
		return Start.monitor;
	}

	public static MonitorTimer getTimer() {
		return Start.timer;
	}

}
