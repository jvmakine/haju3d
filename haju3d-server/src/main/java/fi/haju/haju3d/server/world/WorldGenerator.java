package fi.haju.haju3d.server.world;

import fi.haju.haju3d.protocol.world.Chunk;

public interface WorldGenerator {
  Chunk generateChunk(int seed);
}
