package fi.haju.haju3d.protocol.world;

import java.io.Serializable;

import com.jme3.math.Vector3f;

import fi.haju.haju3d.protocol.Vector3i;

public class TilePosition implements Serializable {
  private static final long serialVersionUID = 1L;
  
  private final Vector3i chunkPosition;
  private final Vector3i tileWithinChunk;
  
  public TilePosition(Vector3i chunkPosition, Vector3i tileWithinChunk) {
    this.chunkPosition = chunkPosition;
    this.tileWithinChunk = tileWithinChunk;
  }

  public Vector3i getChunkPosition() {
    return chunkPosition;
  }
  
  public Vector3i getTileWithinChunk() {
    return tileWithinChunk;
  }  
  
  public Vector3f getWorldPosition(float scale, int chunkSize) {
    return new Vector3f(
      (chunkPosition.x * chunkSize + tileWithinChunk.x) * scale + scale/2,
      (chunkPosition.y * chunkSize + tileWithinChunk.y) * scale + scale/2,
      (chunkPosition.z * chunkSize + tileWithinChunk.z) * scale + scale/2
    );
  }

  @Override
  public String toString() {
    return "Chunk: " + chunkPosition + ", tile: " + tileWithinChunk;
  }
  
}
