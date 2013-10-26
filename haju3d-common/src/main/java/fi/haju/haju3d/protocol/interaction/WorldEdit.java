package fi.haju.haju3d.protocol.interaction;

import fi.haju.haju3d.protocol.world.Tile;
import fi.haju.haju3d.protocol.world.TilePosition;

import java.io.Serializable;

public class WorldEdit implements Serializable {

  private static final long serialVersionUID = 1L;

  private final TilePosition position;
  private final Tile newTile;

  public WorldEdit(TilePosition position, Tile newTile) {
    this.position = position;
    this.newTile = newTile;
  }

  public TilePosition getPosition() {
    return position;
  }

  public Tile getNewTile() {
    return newTile;
  }
}
