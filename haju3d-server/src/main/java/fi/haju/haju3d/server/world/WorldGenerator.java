package fi.haju.haju3d.server.world;

import fi.haju.haju3d.protocol.coordinate.ChunkPosition;
import fi.haju.haju3d.protocol.world.Chunk;

public interface WorldGenerator {
  void setSeed(int seed);
  Chunk generateChunk(ChunkPosition position, int sizeLog2);
}
