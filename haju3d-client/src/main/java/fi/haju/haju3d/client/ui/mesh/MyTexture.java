package fi.haju.haju3d.client.ui.mesh;


public enum MyTexture {
  DIRT("new-dirt.png"),
  GRASS("new-grass.png"), GRASS2("new-grass2.png"),
  ROCK("new-rock.png"), ROCK2("new-rock2.png"),
  BRICK("new-brick.png"),
  WOOD1("wood1.png"), WOOD2("wood2.png"),
  COBBLESTONE1("cobblestone1.png"), COBBLESTONE2("cobblestone2.png"),
  SNOW1("snow1.png"), SNOW2("snow2.png");

  private final String texturefileName;

  private MyTexture(String filename) {
    texturefileName = filename;
  }

  public String getTexturefileName() {
    return texturefileName;
  }

}
