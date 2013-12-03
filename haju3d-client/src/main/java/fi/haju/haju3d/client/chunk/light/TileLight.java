package fi.haju.haju3d.client.chunk.light;

public final class TileLight {
  
  private static final int SUN_MASK   = 0b10000000000000;
  private static final int SOURCE_MASK = 0b1000000000000;
  
  public static final int MAX_DISTANCE = 15; 
  
  public int red;
  public int green;
  public int blue;
  public boolean source;
  public boolean inSun;
  
  public TileLight(short data) {
    red   = data & 0b1111;
    green = (data & 0b11110000) >> 4;
    blue  = (data & 0b111100000000) >> 8;
    source = (data & SOURCE_MASK) == 1;
    inSun = (data & SUN_MASK) == 1;
  }
  
  public TileLight() {
    red   = 0;
    green = 0;
    blue  = 0;
    source = false;
    inSun = false;
  }
  
  public TileLight(int red, int green, int blue, boolean source, boolean inSun) {
    this.red = red;
    this.green = green;
    this.blue = blue;
    this.source = source;
    this.inSun = inSun;
  }
  
  public short getData() {
    short data = (short)((red & 0b1111) | ((green & 0b1111) << 4) | ((blue & 0b1111) << 8));
    if(source) data |= SOURCE_MASK;
    if(inSun) data |= SUN_MASK;
    return data;
  }
  
  public TileLight getDimmer() {
    return new TileLight(
        (red > 0 ? red - 1 : 0), 
        (green > 0 ? green - 1 : 0),
        (blue > 0 ? blue - 1 : 0),
        false, false);
  }
  
  public TileLight combineBrightest(TileLight other) {
    return new TileLight(
        other.red > red ? other.red : red,
        other.green > green ? other.green : green,
        other.blue > blue ? other.blue : blue,
        false,
        false);
  }
  
  public boolean hasLight() {
    return red > 0 || green > 0 || blue > 0;
  }
  
  public boolean hasBrighter(TileLight light) {
    return light.red < red || light.green < green || light.blue < blue;
  }
  
}
