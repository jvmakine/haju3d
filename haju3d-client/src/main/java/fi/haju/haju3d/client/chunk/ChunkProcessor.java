package fi.haju.haju3d.client.chunk;

import java.util.List;

import fi.haju.haju3d.protocol.world.Chunk;

public interface ChunkProcessor {
  void chunksLoaded(List<Chunk> chunks);
}
