package fi.haju.haju3d.server.world;

import fi.haju.haju3d.protocol.Vector3i;
import fi.haju.haju3d.protocol.world.Chunk;

public interface WorldGenerator {
  void setSeed(int seed);
  Chunk generateChunk(Vector3i position);
}
