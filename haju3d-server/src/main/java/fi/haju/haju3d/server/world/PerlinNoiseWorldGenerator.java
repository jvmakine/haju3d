package fi.haju.haju3d.server.world;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fi.haju.haju3d.protocol.Vector3i;
import fi.haju.haju3d.protocol.world.Chunk;
import fi.haju.haju3d.protocol.world.Tile;
import fi.haju.haju3d.util.noise.PerlinNoiseUtil;

public class PerlinNoiseWorldGenerator implements WorldGenerator {

  private static final int WIDTH = 120;
  private static final int HEIGHT = 120;
  private static final int DEPTH = 120;
  
  @Override
  public Chunk generateChunk(int seed) {
    return makeChunk(seed);
  }
  
  private static class FloodFill {
    private List<Vector3i> front = new ArrayList<>();
    private Set<Vector3i> visited = new HashSet<>();
    private Chunk ground;
    private Chunk orig;

    public FloodFill(Chunk ground, Chunk orig) {
      this.ground = ground;
      this.orig = orig;
    }

    public void fill() {
      test(new Vector3i(0, 0, 0));
      while (!front.isEmpty()) {
        Vector3i v = front.remove(front.size() - 1);
        test(v.add(1, 0, 0));
        test(v.add(-1, 0, 0));
        test(v.add(0, 1, 0));
        test(v.add(0, -1, 0));
        test(v.add(0, 0, 1));
        test(v.add(0, 0, -1));
      }
    }

    private void test(Vector3i n) {
      if (visited.contains(n)) {
        return;
      }
      if (orig.get(n.x, n.y, n.z) == Tile.AIR) {
        return;
      }
      ground.set(n.x, n.y, n.z, Tile.GROUND);
      visited.add(n);
      front.add(n);
    }
  }
  
  private static Chunk filterFloaters(Chunk chunk) {
    Chunk ground = new Chunk(chunk.getWidth(), chunk.getHeight(), chunk.getDepth(), chunk.getSeed());
    new FloodFill(ground, chunk).fill();
    return ground;
  }
  
  private Chunk makeChunk(int seed) {
    Chunk chunk = new Chunk(WIDTH, HEIGHT, DEPTH, seed);
    int w = chunk.getWidth();
    int h = chunk.getHeight();
    int d = chunk.getDepth();

    float[] noise = PerlinNoiseUtil.make3dPerlinNoise(seed, w, h, d);
    float thres = h / 3;
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        for (int z = 0; z < d; z++) {
          float v = Math.abs(y);
          if (y < h / 5) {
            v += PerlinNoiseUtil.interpolateLinear(y / (h / 5), -10, 0);
          }
          v += noise[x + y * w + z * w * h] * 3;
          chunk.set(x, y, z, v < thres ? Tile.GROUND : Tile.AIR);
        }
      }
    }
    return filterFloaters(chunk);
  }

}
