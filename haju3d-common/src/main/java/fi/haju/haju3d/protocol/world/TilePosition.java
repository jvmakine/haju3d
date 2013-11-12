package fi.haju.haju3d.protocol.world;

import com.jme3.math.Vector3f;
import fi.haju.haju3d.protocol.coordinate.ChunkPosition;
import fi.haju.haju3d.protocol.coordinate.LocalTilePosition;

import java.io.Serializable;

public class TilePosition implements Serializable {
  private static final long serialVersionUID = 1L;

  private final ChunkPosition chunkPosition;
  private final LocalTilePosition tileWithinChunk;

  public TilePosition(ChunkPosition chunkPosition, LocalTilePosition tileWithinChunk) {
    this.chunkPosition = chunkPosition;
    this.tileWithinChunk = tileWithinChunk;
  }

  public ChunkPosition getChunkPosition() {
    return chunkPosition;
  }

  public LocalTilePosition getTileWithinChunk() {
    return tileWithinChunk;
  }

  public Vector3f getWorldPosition(float scale, int chunkSize) {
    return new Vector3f(
        (chunkPosition.x * chunkSize + tileWithinChunk.x) * scale + scale / 2,
        (chunkPosition.y * chunkSize + tileWithinChunk.y) * scale + scale / 2,
        (chunkPosition.z * chunkSize + tileWithinChunk.z) * scale + scale / 2
    );
  }

  public static TilePosition getTilePosition(float scale, int chunkSize, Vector3f position) {
    ChunkPosition chunkPos = new ChunkPosition(
        (int) Math.floor(position.x / chunkSize / scale),
        (int) Math.floor(position.y / chunkSize / scale),
        (int) Math.floor(position.z / chunkSize / scale));
    LocalTilePosition tilePos = new LocalTilePosition(
        (int) Math.floor(position.x / scale - chunkPos.x * chunkSize),
        (int) Math.floor(position.y / scale - chunkPos.y * chunkSize),
        (int) Math.floor(position.z / scale - chunkPos.z * chunkSize)
    );
    if (tilePos.x < 0) {
      chunkPos.x -= 1;
      tilePos.x += chunkSize - 1;
    }
    if (tilePos.y < 0) {
      chunkPos.y -= 1;
      tilePos.y += chunkSize - 1;
    }
    if (tilePos.z < 0) {
      chunkPos.z -= 1;
      tilePos.z += chunkSize - 1;
    }
    return new TilePosition(chunkPos, tilePos);
  }

  @Override
  public String toString() {
    return "Chunk: " + chunkPosition + ", tile: " + tileWithinChunk;
  }

}
