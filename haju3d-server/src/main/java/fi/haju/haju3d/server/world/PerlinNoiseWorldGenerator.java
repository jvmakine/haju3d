package fi.haju.haju3d.server.world;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import fi.haju.haju3d.protocol.Vector3i;
import fi.haju.haju3d.protocol.world.Chunk;
import fi.haju.haju3d.protocol.world.FloatArray3d;
import fi.haju.haju3d.protocol.world.Tile;
import fi.haju.haju3d.util.noise.InterpolationUtil;

public class PerlinNoiseWorldGenerator implements WorldGenerator {
  private int seed;
  private boolean fastMode;
  
  @Override
  public Chunk generateChunk(Vector3i position, int width, int height, int depth) {
    int realseed = seed ^ (position.x + position.y * 123 + position.z * 12347);
    Chunk chunk = new Chunk(width, height, depth, realseed, position);
    if (position.y < 0) {
      chunk.fill(Tile.GROUND);
      return chunk;
    } else if (position.y > 0) {
      return chunk;
    } else if (fastMode && !position.equals(new Vector3i())) {
      return chunk;
    }
    return makeChunk(chunk, realseed, position);
  }

  public void setFastMode(boolean fastMode) {
    this.fastMode = fastMode;
  }

  @Override
  public void setSeed(int seed) {
    this.seed = seed;
  }
  
  private static final class FloodFill {
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
      if (!orig.isInside(n.x, n.y, n.z) || orig.get(n.x, n.y, n.z) == Tile.AIR) {
        return;
      }
      ground.set(n.x, n.y, n.z, orig.get(n.x, n.y, n.z));
      ground.setColor(n.x, n.y, n.z, orig.getColor(n.x, n.y, n.z));
      visited.add(n);
      front.add(n);
    }
  }
  
  private static Chunk filterFloaters(Chunk chunk) {
    Chunk ground = new Chunk(chunk.getWidth(), chunk.getHeight(), chunk.getDepth(), chunk.getSeed(), chunk.getPosition());
    new FloodFill(ground, chunk).fill();
    return ground;
  }
  
  private static FloatArray3d make3dPerlinNoise(long seed, int w, int h, int d) {
    Random random = new Random(seed);
    FloatArray3d data = new FloatArray3d(w, h, d);
    for (int scale = 4; scale != 128; scale *= 2) {
      add3dNoise(random, data, scale, (float)Math.pow(0.5f * scale * 1.0f, 1.0f));
    }
    return data;
  }
    
  private static void add3dNoise(Random random, FloatArray3d data, int scale, float amp) {
    int w = data.getWidth();
    int h = data.getHeight();
    int d = data.getDepth();
    
    int nw = w / scale + 2;
    int nh = h / scale + 2;
    int nd = d / scale + 2;
    int n = nw * nh * nd;
    float noise[] = new float[n];
    for (int i = 0; i < n; i++) {
      noise[i] = (float) (random.nextDouble() - 0.5) * amp;
    }

    int nwh = nw * nh;

    for (int z = 0; z < d; z++) {
      float zt = (float) (z % scale) / scale;
      int zs = z / scale;
      for (int y = 0; y < h; y++) {
        float yt = (float) (y % scale) / scale;
        int ys = y / scale;
        for (int x = 0; x < w; x++) {
          float xt = (float) (x % scale) / scale;
          int xs = x / scale;

          float n1 = noise[xs + ys * nw + zs * nwh];
          float n2 = noise[(xs + 1) + ys * nw + zs * nwh];
          float n3 = noise[xs + (ys + 1) * nw + zs * nwh];
          float n4 = noise[(xs + 1) + (ys + 1) * nw + zs * nwh];

          float n5 = noise[xs + ys * nw + (zs + 1) * nwh];
          float n6 = noise[(xs + 1) + ys * nw + (zs + 1) * nwh];
          float n7 = noise[xs + (ys + 1) * nw + (zs + 1) * nwh];
          float n8 = noise[(xs + 1) + (ys + 1) * nw + (zs + 1) * nwh];

          data.add(x, y, z, InterpolationUtil.interpolateLinear3d(xt, yt, zt, n1, n2, n3, n4, n5, n6, n7, n8));
        }
      }
    }
  }
  
  private Chunk makeChunk(Chunk chunk, int seed, Vector3i position) {
    int w = chunk.getWidth();
    int h = chunk.getHeight();
    int d = chunk.getDepth();

    FloatArray3d noise = make3dPerlinNoise(seed, w, h, d);
    float thres = h / 3;
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        for (int z = 0; z < d; z++) {
          float v = Math.abs(y);
          if (y < h / 5) {
            v += InterpolationUtil.interpolateLinear(y / (h / 5), -10, 0);
          }
          v += noise.get(x, y, z) * 3;
          Tile terrain = noise.get(x, h - 1 - y, z) < 0 ? Tile.GROUND : Tile.ROCK;
          chunk.set(x, y, z, v < thres ? terrain : Tile.AIR);
          float col = 0.75f + noise.get(w - 1 - x, y, z) * 0.1f;
          if (col < 0.5f) {
            col = 0.5f;
          } else if (col > 1.0f) {
            col = 1.0f;
          }
          chunk.setColor(x, y, z, col);
        }
      }
    }
    
    // add a "building" in the middle of chunk
    int midX = w / 2;
    int midZ = d / 2;
    int groundY = findGround(chunk, h, midX, midZ);
    if (groundY >= 0 && groundY < h - 10) {
      for (int x = 0; x < 10; x++) {
        for (int y = 0; y < 10; y++) {
          for (int z = 0; z < 10; z++) {
            chunk.set(x + midX, y + groundY, z + midZ, Tile.BRICK);
          }
        }
      }
    }
    
    if (fastMode) {
      return chunk;
    } else {
      return filterFloaters(chunk);
    }
  }

  private int findGround(Chunk chunk, int h, int midX, int midZ) {
    for (int y = 0; y < h; y++) {
      int testY = h - 1 - y;
      if (chunk.get(midX, testY, midZ) != Tile.AIR) {
        return testY;
      }
    }
    return -1;
  }

}
