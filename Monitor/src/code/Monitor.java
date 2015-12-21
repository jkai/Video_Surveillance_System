package code;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class Monitor extends JFrame {

	private static final long serialVersionUID = 1L;

	private JPanel monitorPanel = new JPanel();
	private JButton btnStartServer = new JButton("Start Server");
	private JButton btnStartTimer = new JButton("Start Timer");
	
	public Monitor() {
		super("Video Surveillance System");
		setSize(260, 220);
		this.getContentPane().setBackground(Color.WHITE);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new FlowLayout());
		add(monitorPanel);
		add(btnStartServer);
		add(btnStartTimer);
		setVisible(true);
	}

	public void initial() {
		monitorPanel.setBounds(50, 20, 160, 120);
		monitorPanel.setBackground(Color.WHITE);	
		monitorPanel.add(new ImageLoader("resources/Ready.jpg"));
		
		// Start button
		btnStartServer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				startServer();
			}
		});
		// Next button
		btnStartTimer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				startTimer();
			}
		});
		this.revalidate();
	}

	public void fetchPic() {
		if (!Start.getImgList().isEmpty()) {
			monitorPanel.remove(0);
			monitorPanel.add(new ImageLoader(Start.getImgList().getFirst()));
			Start.getImgList().removeFirst();
			this.revalidate();
		} else {
			System.out.print("No images in the buffer!\n");
		}
	}

	private void startServer() {
		Start.getServer().start();
	}

	private void startTimer() {
		Start.getTimer().start();
	}

}
