package fi.haju.haju3d.client.chunk;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import fi.haju.haju3d.client.connection.ServerConnector;
import fi.haju.haju3d.protocol.Vector3i;
import fi.haju.haju3d.protocol.world.Chunk;

public class ChunkProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(ChunkProvider.class);
    
  private final ServerConnector server;
  private Map<Vector3i, Chunk> chunkCache = new ConcurrentHashMap<Vector3i, Chunk>();

  @Inject
  public ChunkProvider(ServerConnector server) {
    this.server = server;
  }

  public List<Chunk> getChunks(List<Vector3i> positions) {
    Collection<Vector3i> newPositions = Collections2.filter(positions, new Predicate<Vector3i>() {
      @Override
      public boolean apply(Vector3i v) {
        return !chunkCache.containsKey(v);
      }
    });
    if(!newPositions.isEmpty()) {
      LOGGER.info("Requested " + newPositions);
      List<Chunk> chunks = server.getChunks(Lists.newArrayList(newPositions));
      for (Chunk c : chunks) {
        chunkCache.put(c.getPosition(), c);
      }
      return chunks;
    }
    return Lists.newArrayList();
  }
  
}
