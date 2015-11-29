package swing;

import java.awt.EventQueue;

public class Start {

	public static void main(String args[]) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Monitor monitor = new Monitor();
					monitor.setBounds(100, 100, 676, 582);
					monitor.setVisible(true);

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

}
