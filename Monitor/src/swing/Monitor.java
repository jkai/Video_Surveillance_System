package swing;

import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class Monitor extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel monitorPanel;

	public Monitor() {
		super("Video Surveillance System");
		initialize();
	}

	public JFrame getFrame() {
		return this;
	}

	public void nextImg() {
		monitorPanel.add(new ImageLoader("M:\\Lab5\\Monitor\\src\\resource\\pic2.jpg"));
		getFrame().pack();
		getFrame().repaint();
	}

	private void initialize() {
		getFrame().setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		getFrame().setIconImage(Toolkit.getDefaultToolkit().getImage(Gui.class.getResource("/resource/Icon.png")));
		getFrame().setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		getFrame().getContentPane().setLayout(null);

		monitorPanel = new JPanel();
		monitorPanel.setBounds(10, 11, 640, 480);
		monitorPanel.add(new ImageLoader("M:\\Lab5\\Monitor\\src\\resource\\pic1.jpg"));
		getFrame().getContentPane().add(monitorPanel);
		getFrame().pack();
	}

}
