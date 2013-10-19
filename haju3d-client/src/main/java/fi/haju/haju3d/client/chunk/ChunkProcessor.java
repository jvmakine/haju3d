package fi.haju.haju3d.client.chunk;

import fi.haju.haju3d.protocol.world.Chunk;

import java.util.List;

public interface ChunkProcessor {
  void chunksLoaded(List<Chunk> chunks);
}
