package fi.haju.haju3d.client.ui.mesh;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import fi.haju.haju3d.protocol.world.Tile;

public class TileRenderPropertyProvider {

  public static class TileProperties {
    private final int maxSmooths;

    public TileProperties(int maxSmooths) {
      this.maxSmooths = maxSmooths;
    }

    public int getMaxSmooths() {
      return maxSmooths;
    }
    
  }
  
  private static final Map<Tile, TileProperties> properties = ImmutableMap.<Tile, TileProperties>builder()
      .put(Tile.BRICK, new TileProperties(0))
      .put(Tile.GROUND, new TileProperties(3))
      .put(Tile.ROCK, new TileProperties(3))
      .build();
  
  public static TileProperties getProperties(Tile tile) {
    return properties.get(tile);
  }
  
}
