package fi.haju.haju3d.client;

import fi.haju.haju3d.client.chunk.light.ChunkLightingManager;
import fi.haju.haju3d.client.ui.mesh.ChunkSpatialBuilder;
import fi.haju.haju3d.client.ui.mesh.MyMesh;
import fi.haju.haju3d.protocol.coordinate.ChunkPosition;
import fi.haju.haju3d.protocol.world.Chunk;
import fi.haju.haju3d.protocol.world.Tile;
import fi.haju.haju3d.protocol.world.World;

import java.util.Random;

public class BenchmarkMeshGenerationApp {
  public static void main(String[] args) {
    World world = new World();

    for (int x = 0; x < 3; x++) {
      for (int y = 0; y < 3; y++) {
        for (int z = 0; z < 3; z++) {
          ChunkPosition chunkIndex = new ChunkPosition(x, y, z);
          Chunk chunk = new Chunk(world.getChunkSize(), world.getChunkSize(), world.getChunkSize(), 0, chunkIndex);
          world.setChunk(chunkIndex, chunk);
        }
      }
    }

    initRandomChunk(world.getChunk(new ChunkPosition(1, 1, 1)));

    for (int i = 0; i < 10; i++) {
      System.gc();

      long t0 = System.currentTimeMillis();
      MyMesh cubeMesh = ChunkSpatialBuilder.makeCubeMesh(world, new ChunkPosition(1, 1, 1), new ChunkLightingManager());
      long t1 = System.currentTimeMillis();
      ChunkSpatialBuilder.smoothMesh(cubeMesh);
      ChunkSpatialBuilder.prepareMesh(cubeMesh);
      long t2 = System.currentTimeMillis();
      new ChunkSpatialBuilder.SimpleMeshBuilder(cubeMesh).build();
      long t3 = System.currentTimeMillis();
      new ChunkSpatialBuilder.NewMeshBuilder2(cubeMesh).build();
      long t4 = System.currentTimeMillis();

      System.out.println("Mesh time: " + (t1 - t0));
      System.out.println("Prepare time: " + (t2 - t1));
      System.out.println("Spatial time: " + (t3 - t2));
      System.out.println("Spatial2 time: " + (t4 - t3));
    }
  }

  private static void initRandomChunk(Chunk chunk) {
    final Random random = new Random(0L);
    chunk.set(new Chunk.GetValue() {
      @Override
      public Tile getValue(int x, int y, int z) {
        return random.nextBoolean() ? Tile.GROUND : Tile.AIR;
      }
    });
  }
}
