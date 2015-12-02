package code;

public class MonitorTimer extends Thread {

	public MonitorTimer() {
		
	}

	public void run() {
		System.out.println("Timer started!");
		while (true) {
			try {
				Thread.sleep(2000);
				tick();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void tick() {
		System.out.println("Tick!");
		Start.getMonitor().togglePic();
	}
}
