package fi.haju.haju3d.client;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

import fi.haju.haju3d.protocol.Server;
import fi.haju.haju3d.protocol.Vector3i;
import fi.haju.haju3d.protocol.world.Chunk;

public class ChunkProvider {
  private static final Logger LOGGER = LoggerFactory.getLogger(ChunkProvider.class);
    
  private final Server server;
  private Map<Vector3i, Chunk> chunkCache = new ConcurrentHashMap<Vector3i, Chunk>();
  private Set<List<Vector3i>> requests = Collections.newSetFromMap(new ConcurrentHashMap<List<Vector3i>, Boolean>());
  
  public ChunkProvider(Server server) {
    this.server = server;
  }
  
  public void requestChunks(final List<Vector3i> positions, final ChunkProcessor processor) {
    LOGGER.info("Requested " + positions);
    
    requests.add(positions);
    new Thread(new Runnable() {
      @Override
      public void run() {
        List<Chunk> chunks = new ArrayList<>();
        chunks.addAll(getChunks(positions));
        processor.chunksLoaded(chunks);
        requests.remove(positions);
      }
    }).start();
  }
  
  public boolean hasChunk(Vector3i position) {
    if(requests.contains(position)) {
      return true;
    }
    return chunkCache.containsKey(position);
  }
  
  private List<Chunk> getChunks(List<Vector3i> positions) {
    Collection<Vector3i> newPositions = Collections2.filter(positions, new Predicate<Vector3i>() {
      public boolean apply(Vector3i v) {
        return !chunkCache.containsKey(v);
      }
    });
    if(!newPositions.isEmpty()) {
      try {
        return server.getChunks(Lists.newArrayList(newPositions));
      } catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }
    return Lists.newArrayList();
  }
  
}
