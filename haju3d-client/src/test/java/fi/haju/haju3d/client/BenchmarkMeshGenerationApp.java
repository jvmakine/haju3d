package fi.haju.haju3d.client;

import fi.haju.haju3d.client.chunk.light.ChunkLightManager;
import fi.haju.haju3d.client.ui.mesh.ChunkSpatialBuilder;
import fi.haju.haju3d.client.ui.mesh.MyMesh;
import fi.haju.haju3d.protocol.coordinate.ChunkPosition;
import fi.haju.haju3d.protocol.world.Chunk;
import fi.haju.haju3d.protocol.world.FloatArray3d;
import fi.haju.haju3d.protocol.world.Tile;
import fi.haju.haju3d.protocol.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BenchmarkMeshGenerationApp {
  public static void main(String[] args) {
    //benchmarkPerformance();
    //benchmarkMemoryUse();
    //benchmarkBlurPerformance();
  }

  private static void benchmarkBlurPerformance() {
    World world = makeEmptyWorld();
    final ChunkPosition cp = new ChunkPosition(1, 1, 1);
    initSphereChunk(world.getChunk(cp));
    Chunk chunk = world.getChunk(cp);

    for (int i = 0; i < 10; i++) {
      long t0 = System.currentTimeMillis();
      blurChunk(chunk);
      long t1 = System.currentTimeMillis();
      System.out.println("tt = " + (t1 - t0));
    }


  }

  private static FloatArray3d blurChunk(Chunk chunk) {
    //22 ms to guassian blur 64^3 chunk
    //11 ms to do vertex blurring on sphere in 64^3 chunk
    FloatArray3d array = new FloatArray3d(chunk.getSize(), chunk.getSize(), chunk.getSize());
    FloatArray3d array2 = new FloatArray3d(chunk.getSize(), chunk.getSize(), chunk.getSize());
    for (int z = 0; z < chunk.getSize(); z++) {
      for (int y = 0; y < chunk.getSize(); y++) {
        for (int x = 0; x < chunk.getSize(); x++) {
          array.set(x, y, z, chunk.get(x, y, z) != Tile.AIR ? 1 : 0);
        }
      }
    }

    FloatArray3d out = array2;
    FloatArray3d in = array;

    for (int i = 0; i < 2; i++) {
      for (int z = 0; z < chunk.getSize(); z++) {
        for (int y = 0; y < chunk.getSize(); y++) {
          float accum = 0;
          for (int x = 0; x < chunk.getSize() - 3; x++) {
            out.set(x, y, z, accum);
            accum += in.get(x + 3, y, z);
            accum -= in.get(x, y, z);
          }
        }
      }
      FloatArray3d tmp = out;
      out = in;
      in = tmp;
    }

    for (int i = 0; i < 2; i++) {
      for (int z = 0; z < chunk.getSize(); z++) {
        for (int x = 0; x < chunk.getSize(); x++) {
          float accum = 0;
          for (int y = 0; y < chunk.getSize() - 3; y++) {
            out.set(x, y, z, accum);
            accum += in.get(x, y + 3, z);
            accum -= in.get(x, y, z);
          }
        }
      }
      FloatArray3d tmp = out;
      out = in;
      in = tmp;
    }

    for (int i = 0; i < 2; i++) {
      for (int y = 0; y < chunk.getSize(); y++) {
        for (int x = 0; x < chunk.getSize(); x++) {
          float accum = 0;
          for (int z = 0; z < chunk.getSize() - 3; z++) {
            out.set(x, y, z, accum);
            accum += in.get(x, y, z + 3);
            accum -= in.get(x, y, z);
          }
        }
      }
      FloatArray3d tmp = out;
      out = in;
      in = tmp;
    }

    return array;
  }

  private static void benchmarkMemoryUse() {
    World world = makeEmptyWorld();

    final ChunkPosition cp = new ChunkPosition(1, 1, 1);
    initRandomChunk(world.getChunk(cp));
    //initSphereChunk(world.getChunk(cp));

    long used1 = getUsedMemory();
    System.out.println("used1 = " + (used1 / 1e6));

    //176 MB per MyMesh
    //399304 faces
    //399304*3*FLOAT=vertex info
    //399304*4*INT=index info
    //92 MB for buffer data!
    //232 bytes per face
    //32 bytes per face just to save vertex indexes + 1 vertex per face
    /*
      FloatBuffer vertexes = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3);
      FloatBuffer colors = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 4);
      FloatBuffer vertexNormals = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3);
      FloatBuffer textures = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3);
      IntBuffer indexes = BufferUtils.createIntBuffer(realFaces.size() * 6);
     */
    List<MyMesh> myMeshList = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      MyMesh cubeMesh = ChunkSpatialBuilder.makeCubeMesh(world, cp, new ChunkLightManager());
      ChunkSpatialBuilder.smoothMesh(cubeMesh);
      ChunkSpatialBuilder.prepareMesh(cubeMesh);
      myMeshList.add(cubeMesh);
      System.out.println("cubeMesh.getRealFaces() = " + cubeMesh.getRealFaces().size());

      long used2 = getUsedMemory();
      System.out.println("used = " + ((used2 - used1) / 1e6));
    }

  }

  private static long getUsedMemory() {
    System.gc();
    Runtime runtime = Runtime.getRuntime();
    return runtime.totalMemory() - runtime.freeMemory();
  }

  private static void benchmarkPerformance() {
    World world = makeEmptyWorld();

    final ChunkPosition cp = new ChunkPosition(1, 1, 1);
    //initRandomChunk(world.getChunk(cp));
    initSphereChunk(world.getChunk(cp));

    for (int i = 0; i < 10; i++) {
      System.gc();

      long t0 = System.currentTimeMillis();
      MyMesh cubeMesh = ChunkSpatialBuilder.makeCubeMesh(world, cp, new ChunkLightManager());
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
      System.out.println("Total time: " + (t4 - t0));
    }
  }

  private static World makeEmptyWorld() {
    World world = new World();

    for (int x = 0; x < 3; x++) {
      for (int y = 0; y < 3; y++) {
        for (int z = 0; z < 3; z++) {
          ChunkPosition chunkPosition = new ChunkPosition(x, y, z);
          int chunkSize = world.getChunkCoordinateSystem().getChunkSize();
          world.setChunk(chunkPosition, new Chunk(chunkSize, 0, chunkPosition));
        }
      }
    }
    return world;
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

  private static void initSphereChunk(Chunk chunk) {
    final int sz = chunk.getSize();
    chunk.set(new Chunk.GetValue() {
      @Override
      public Tile getValue(int x, int y, int z) {
        int xd = x - sz / 2;
        int yd = y - sz / 2;
        int zd = z - sz / 2;
        int bsz = sz / 2;
        return xd * xd + yd * yd + zd * zd < bsz * bsz ? Tile.GROUND : Tile.AIR;
      }
    });
  }
}
