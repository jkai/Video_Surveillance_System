package code;

public class MonitorTimer extends Thread {

	public MonitorTimer() {

	}

	public void run() {
		System.out.println("Timer started!");
		while (true) {
			try {
				//Tick every 1s
				//Thread.sleep(1000);
				Thread.sleep(2000);
				tick();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void tick() {
		Start.getMonitor().fetchPic();
	}
}
