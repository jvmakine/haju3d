package fi.haju.haju3d.client;

import fi.haju.haju3d.protocol.world.Chunk;

public interface ChunkProcessor {
  void chunkLoaded(Chunk chunk);
}
