package fi.haju.haju3d.client;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Typically images have a black or white color when alpha = 0. This causes issues when mipmap images
 * are generated, because the black or white bleeds into the visible part of mipmap images. The intention
 * of this tool is to fix the color value of pixel whose alpha = 0 by taking the color value of the closest
 * pixel whose alpha != 0.
 */
public class FixTextureAlphaColor {

  private WritableRaster raster;
  private int width;
  private int height;
  private File texturePath;

  public FixTextureAlphaColor(File texturePath) {
    this.texturePath = texturePath;
    assert texturePath.exists();
  }

  public static void main(String[] args) throws IOException {
    new FixTextureAlphaColor(new File("src/main/resources/fi/haju/haju3d/client/textures")).runAll();
  }

  private static final class Front {
    int x;
    int y;

    private Front(int x, int y) {
      this.x = x;
      this.y = y;
    }
  }

  private Deque<Front> front = new ArrayDeque<Front>();

  public void runAll() throws IOException {
    for (File f : texturePath.listFiles()) {
      if (!f.getName().endsWith(".png")) {
        continue;
      }
      System.out.println("processing: " + f);
      run(f);
    }
  }

  private void run(File imageFile) throws IOException {
    BufferedImage image = ImageIO.read(imageFile);

    raster = image.getRaster();
    width = image.getWidth();
    height = image.getHeight();

    if (raster.getNumBands() == 3) {
      return;
    }

    if (!fixAlphaColor()) {
      return;
    }
    //showAlpha();

    ImageIO.write(image, "png", imageFile);
    //ImageIO.write(image, "png", new File("output.png"));
  }

  private void showAlpha() {
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        raster.setSample(x, y, 3, 255);
      }
    }
  }

  private boolean fixAlphaColor() {
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        if (goodPixel(x, y)) {
          increaseFront(x, y);
        }
      }
    }

    if (front.isEmpty()) {
      return false;
    }

    while (!front.isEmpty()) {
      Front f = front.removeFirst();
      increaseFront(f.x, f.y);
    }
    return true;
  }

  private void increaseFront(int x, int y) {
    int r = raster.getSample(x, y, 0);
    int g = raster.getSample(x, y, 1);
    int b = raster.getSample(x, y, 2);
    maybeAddFront(x - 1, y, r, g, b);
    maybeAddFront(x + 1, y, r, g, b);
    maybeAddFront(x, y - 1, r, g, b);
    maybeAddFront(x, y + 1, r, g, b);
  }

  private void maybeAddFront(int x, int y, int r, int g, int b) {
    if (badPixel(x, y)) {
      raster.setSample(x, y, 0, r);
      raster.setSample(x, y, 1, g);
      raster.setSample(x, y, 2, b);
      front.add(new Front(x, y));
    }
  }

  private boolean goodPixel(int x, int y) {
    return raster.getSample(x, y, 3) != 0;
  }

  private boolean badPixel(int x, int y) {
    return inside(x, y) && raster.getSample(x, y, 3) == 0 && (raster.getSample(x, y, 0) == 0 || raster.getSample(x, y, 0) == 255);
  }

  private boolean inside(int x, int y) {
    return x >= 0 && y >= 0 && x < width && y < height;
  }

}
