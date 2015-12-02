package code;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class ImageLoader extends Component {

	private static final long serialVersionUID = 1L;
	private BufferedImage img;

	public ImageLoader(String path) {
		try {
			img = ImageIO.read(new File(path));
		} catch (IOException e) {
			System.out.print("Image reading error!\n");
		}
	}

	public ImageLoader(BufferedImage img) {
		this.img = img;
	}

	public void paint(Graphics g) {
		g.drawImage(img, 0, 0, null);
	}

	public Dimension getPreferredSize() {
		return new Dimension(640, 480);
	}

}
