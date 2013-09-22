package fi.haju.haju3d.client.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import fi.haju.haju3d.client.ChunkProvider;
import fi.haju.haju3d.client.ui.mesh.ChunkSpatialBuilder;
import fi.haju.haju3d.protocol.Vector3i;
import fi.haju.haju3d.protocol.world.Chunk;
import fi.haju.haju3d.protocol.world.World;

public class WorldBuilder {
  private World world;
  private ChunkProvider chunkProvider;
  private ChunkSpatialBuilder builder;
  private Map<Vector3i, ChunkSpatial> chunkSpatials = new ConcurrentHashMap<>();
  
  private AtomicBoolean running = new AtomicBoolean(false);
  private Object lock = new Object();
  private transient Vector3i position;
  
  public WorldBuilder(World world, ChunkProvider chunkProvider, ChunkSpatialBuilder builder) {
    this.world = world;
    this.chunkProvider = chunkProvider;
    this.builder = builder;
  }
  
  private Runnable runnable = new Runnable() {
    @Override
    public void run() {
      while (running.get()) {
//      synchronized (lock) {
//        try {
//          lock.wait();
//        } catch (InterruptedException e) {
//          throw new RuntimeException(e);
//        }
//      }
      if (position != null) {
        makeChunkNearPosition(position);
      }
      removeFarChunks(position);
    }
    }
  };
  
  private void removeFarChunks(Vector3i centerChunkIndex) {
    final int maxDistance = 3;
    Set<Vector3i> remove = new HashSet<>();
    for (Map.Entry<Vector3i, ChunkSpatial> c : chunkSpatials.entrySet()) {
      Vector3i v = c.getKey();
      if (Math.abs(centerChunkIndex.x - v.x) > maxDistance
          || Math.abs(centerChunkIndex.y - v.y) > maxDistance
          || Math.abs(centerChunkIndex.z - v.z) > maxDistance) {
        remove.add(v);
      }
    }
    for (Vector3i r : remove) {
      chunkSpatials.remove(r);
    }
  }

  private void makeChunkNearPosition(Vector3i centerChunkIndex) {
    List<Vector3i> indexes = new ArrayList<>();
    indexes.add(centerChunkIndex);
    indexes.addAll(centerChunkIndex.getSurroundingPositions(1, 1, 1));
    indexes.addAll(centerChunkIndex.getSurroundingPositions(2, 2, 2));
    for (Vector3i i : indexes) {
      if (chunkSpatials.containsKey(i)) {
        continue;
      }
      makeChunkAt(i);
      break;
    }
  }

  private void makeChunkAt(Vector3i chunkIndex) {
    // need 3x3 chunks around meshing area so that mesh borders can be handled correctly
    List<Chunk> chunks = chunkProvider.getChunks(chunkIndex.getSurroundingPositions());
    for (Chunk c : chunks) {
      world.setChunk(c.getPosition(), c);
    }
    chunkSpatials.put(chunkIndex, builder.makeChunkSpatial(world, chunkIndex));
  }

  public void setPosition(Vector3i position) {
    this.position = position;
    synchronized (lock) {
      lock.notify();
    }
  }
  
  public void start() {
    if (!running.get()) {
      running.set(true);
      new Thread(runnable).start();
    }
  }
  
  public void stop() {
    running.set(false);
    synchronized (lock) {
      lock.notify();
    }
  }

  public ChunkSpatial getChunkSpatial(Vector3i pos) {
    return chunkSpatials.get(pos);
  }


}
