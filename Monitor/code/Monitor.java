package code;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class Monitor extends JFrame {

	private static final long serialVersionUID = 1L;
	private boolean flag = true;

	private JPanel monitorPanel = new JPanel();
	JButton btnStart = new JButton("Start");
	JButton btnNext = new JButton("Next");

	public Monitor() {
		super("Video Surveillance System");
		setSize(720, 640);
		this.getContentPane().setBackground(Color.WHITE);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setIconImage(Toolkit.getDefaultToolkit().getImage(Monitor.class.getResource("/resource/Icon.png")));
		setLayout(new FlowLayout());
		add(monitorPanel);
		add(btnStart);
		add(btnNext);
		setVisible(true);
	}

	public void initial() {
		monitorPanel.setBounds(10, 10, 640, 480);
		monitorPanel.setBackground(Color.WHITE);
		monitorPanel.add(new ImageLoader("M:\\Lab5\\Monitor\\src\\resource\\Ready.jpg"));
		// Start button
		btnStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				startMonitor();
			}
		});
		// Next button
		btnNext.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				nextPic();
			}
		});
		this.revalidate();
	}

	private void nextPic() {
		if (flag) {
			monitorPanel.remove(0);
			monitorPanel.add(new ImageLoader("M:\\Lab5\\Monitor\\src\\resource\\pic1.jpg"));
			this.revalidate();
			flag = !flag;
		} else {
			monitorPanel.remove(0);
			monitorPanel.add(new ImageLoader("M:\\Lab5\\Monitor\\src\\resource\\pic2.jpg"));
			this.revalidate();
			flag = !flag;
		}

	}

	private void startMonitor() {
		monitorPanel.remove(0);
		monitorPanel.add(new ImageLoader("M:\\Lab5\\Monitor\\src\\resource\\pic2.jpg"));
		this.revalidate();
	}

}
