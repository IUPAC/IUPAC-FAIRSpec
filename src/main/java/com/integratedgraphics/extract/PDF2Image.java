package com.integratedgraphics.extract;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import javax.imageio.ImageIO;

/**
 * Just a test to see what we can find in a Bruker PDF document.
 * Answer was just the Bruker logo!
 * 
 * @author hansonr
 *
 */
public class PDF2Image {

	private InputStream is;

	public PDF2Image(InputStream is) {
		this.is = is;
	}

	public BufferedImage getImage() {
		DataInputStream dis = new DataInputStream(is);
		BufferedImage img = getNextImage(dis);
		File out = new File("c:/temp/img.png");
		try {
			ImageIO.write(img, "PNG", out);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return img;
	}

	private BufferedImage getNextImage(DataInputStream dis) {
		// this turned out just to be the Bruker logo!
		String s = null;
		try {
			while ((s = dis.readLine()) != null) {
				if (s.endsWith("stream")) {
					int len = getParam(s, "Length");
					int width = getParam(s, "Width");
					int height = getParam(s, "Height");
					int w3 = width * 3;
					byte[] b = new byte[len];
					dis.read(b);
					Inflater inf = new Inflater();
					inf.setInput(b);
					BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
					byte[] imgbuf = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
					for (int pt = 0; !inf.finished(); pt += w3) {
						inf.inflate(imgbuf, pt, w3);
					}					
					return img;
				}
			}
		} catch (NumberFormatException | IOException | DataFormatException e) {
			e.printStackTrace();
			System.out.println("PDF stream not found");
			return null;
		}
		return null;
	}

	private int getParam(String s, String key) {
		int i1 = s.indexOf(key + " ") + key.length() + 1;
		int i2 = s.indexOf("/", i1);
		return Integer.parseInt(s.substring(i1, i2));
	}

	public static void main(String[] args) {
		try {
			new PDF2Image(new FileInputStream("c:/temp/test.pdf")).getImage();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
	}
}