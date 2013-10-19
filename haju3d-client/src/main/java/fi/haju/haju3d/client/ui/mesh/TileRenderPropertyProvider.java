package fi.haju.haju3d.client.ui.mesh;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import fi.haju.haju3d.protocol.world.Tile;

import java.util.List;
import java.util.Map;

public class TileRenderPropertyProvider {

  private static final Map<Tile, TileProperties> properties = ImmutableMap.<Tile, TileProperties>builder()
      .put(Tile.BRICK, new TileProperties(
          0,
          Lists.newArrayList(MyTexture.BRICK),
          Lists.newArrayList(MyTexture.BRICK)))
      .put(Tile.GROUND, new TileProperties(
          3,
          Lists.newArrayList(MyTexture.GRASS, MyTexture.GRASS2),
          Lists.newArrayList(MyTexture.DIRT)))
      .put(Tile.ROCK, new TileProperties(
          3,
          Lists.newArrayList(MyTexture.ROCK, MyTexture.ROCK2),
          Lists.newArrayList(MyTexture.ROCK, MyTexture.ROCK2)))
      .put(Tile.WOOD, new TileProperties(
          2,
          Lists.newArrayList(MyTexture.WOOD1, MyTexture.WOOD2),
          Lists.newArrayList(MyTexture.WOOD1, MyTexture.WOOD2)))
      .put(Tile.COBBLESTONE, new TileProperties(
          1,
          Lists.newArrayList(MyTexture.COBBLESTONE1),
          Lists.newArrayList(MyTexture.COBBLESTONE1)))
      .build();

  public static TileProperties getProperties(Tile tile) {
    return properties.get(tile);
  }

  public static class TileProperties {
    private final int maxSmooths;
    private final List<MyTexture> topTextures;
    private final List<MyTexture> sideTextures;

    public TileProperties(int maxSmooths, List<MyTexture> topTextures, List<MyTexture> sideTextures) {
      this.maxSmooths = maxSmooths;
      this.topTextures = topTextures;
      this.sideTextures = sideTextures;
    }

    public int getMaxSmooths() {
      return maxSmooths;
    }

    public MyTexture getTopTexture(int seed) {
      return topTextures.get(Math.abs(seed) % topTextures.size());
    }

    public MyTexture getSideTexture(int seed) {
      return sideTextures.get(Math.abs(seed) % sideTextures.size());
    }

  }

}
