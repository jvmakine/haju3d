package fi.haju.haju3d.server.world;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import fi.haju.haju3d.protocol.Vector3i;
import fi.haju.haju3d.protocol.world.Chunk;
import fi.haju.haju3d.protocol.world.FloatArray3d;
import fi.haju.haju3d.protocol.world.Tile;
import fi.haju.haju3d.util.noise.InterpolationUtil;

public class PerlinNoiseWorldGenerator implements WorldGenerator {
  private int seed;
  private boolean fastMode;
  
  private Map<Vector3i, PerlinNoiseScales> perlinNoises = Maps.newHashMap();
  
  @Override
  public Chunk generateChunk(Vector3i position, int width, int height, int depth) {
    int realseed = seed ^ (position.x + position.y * 123 + position.z * 12347);
    if (position.y < 0) {
      return new Chunk(width, height, depth, realseed, position, Tile.GROUND);
    } else if (position.y > 0) {
      return new Chunk(width, height, depth, realseed, position, Tile.AIR);
    } else if (fastMode && !position.equals(new Vector3i())) {
      return new Chunk(width, height, depth, realseed, position, Tile.AIR);
    }
    Chunk chunk = new Chunk(width, height, depth, realseed, position);
    return makeChunk(chunk, realseed, position);
  }

  public void setFastMode(boolean fastMode) {
    this.fastMode = fastMode;
  }

  @Override
  public void setSeed(int seed) {
    this.seed = seed;
  }
  
  private static final class PerlinNoiseScales {
    public static List<Integer> SCALES = ImmutableList.of(2, 4, 8, 16, 32);
    
    private Map<Integer, FloatArray3d> noises = Maps.newHashMap();
    
    public PerlinNoiseScales(final Random random, int width, int height, int depth) {
      for(int scale : SCALES) {
        int nw = width / scale;
        int nh = height / scale;
        int nd = depth / scale;
        final float amp = (float)Math.pow(0.5f * scale * 1.0f, 1.0f);
        FloatArray3d noise = new FloatArray3d(nw, nh, nd, new FloatArray3d.Initializer() {
          @Override
          public float getValue(int x, int y, int z) {
            return (float)((random.nextDouble() - 0.5) * amp);
          }
        });
        noises.put(scale, noise);
      }
    }
    
    public FloatArray3d getNoise(int scale) {
      return noises.get(scale);
    }
    
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
  
  private PerlinNoiseScales getPerlinNoiseScale(Vector3i pos, Random random, int w, int h, int d) {
    if(perlinNoises.containsKey(pos)) {
      return perlinNoises.get(pos);
    }
    PerlinNoiseScales noises = new PerlinNoiseScales(random, w, h, d);
    perlinNoises.put(pos, noises);
    return noises;
  }
  
  private FloatArray3d make3dPerlinNoise(long seed, int w, int h, int d, Vector3i position) {
    Random random = new Random(seed);
    FloatArray3d data = new FloatArray3d(w, h, d);
    for (int scale : PerlinNoiseScales.SCALES) {
      FloatArray3d[] surroundingScales = new FloatArray3d[3*3*3];
      for(int x = 0; x < 2; ++x) {
        for(int y = 0; y < 2; ++y) {
          for(int z = 0; z < 2; ++z) {
            surroundingScales[x + y*2 + z*4] = getPerlinNoiseScale(position.add(x, y, z), random, w, h, d).getNoise(scale);
          }
        }
      }
      add3dNoise(random, data, scale, surroundingScales);
    }
    return data;
  }
    
  private static float getNoiseValueFromSet(int x, int y, int z, int nw, int nh, int nd, FloatArray3d[] scales) {
    boolean xOver = x >= nw;
    boolean yOver = y >= nh;
    boolean zOver = z >= nd;
    int i = xOver ? 1 : 0;
    int j = yOver ? 1 : 0;
    int k = zOver ? 1 : 0;
    return scales[i + j*2 + k*4].get(
        xOver ? x - nw : x,
        yOver ? y - nh : y,
        zOver ? z - nd : z);
  }
  
  private static void add3dNoise(final Random random, FloatArray3d data, int scale, FloatArray3d[] surroundingScales) {
    int w = data.getWidth();
    int h = data.getHeight();
    int d = data.getDepth();
       
    FloatArray3d centralNoise = surroundingScales[0];
    
    int nw = centralNoise.getWidth();
    int nh = centralNoise.getHeight();
    int nd = centralNoise.getDepth();
    
    for (int z = 0; z < d; z++) {
      float zt = (float) (z % scale) / scale;
      int zs = z / scale;
      for (int y = 0; y < h; y++) {
        float yt = (float) (y % scale) / scale;
        int ys = y / scale;
        for (int x = 0; x < w; x++) {
          float xt = (float) (x % scale) / scale;
          int xs = x / scale;
         
          float n1 = getNoiseValueFromSet(xs, ys, zs, nw, nh, nd, surroundingScales);
          float n2 = getNoiseValueFromSet(xs + 1, ys, zs, nw, nh, nd, surroundingScales);
          float n3 = getNoiseValueFromSet(xs, ys + 1, zs, nw, nh, nd, surroundingScales);
          float n4 = getNoiseValueFromSet(xs + 1, ys + 1, zs, nw, nh, nd, surroundingScales);

          float n5 = getNoiseValueFromSet(xs, ys, zs + 1, nw, nh, nd, surroundingScales);
          float n6 = getNoiseValueFromSet(xs + 1, ys, zs + 1, nw, nh, nd, surroundingScales);
          float n7 = getNoiseValueFromSet(xs, ys + 1, zs + 1, nw, nh, nd, surroundingScales);
          float n8 = getNoiseValueFromSet(xs + 1, ys + 1, zs + 1, nw, nh, nd, surroundingScales);

          data.add(x, y, z, InterpolationUtil.interpolateLinear3d(xt, yt, zt, n1, n2, n3, n4, n5, n6, n7, n8));
        }
      }
    }
  }
  
  private Chunk makeChunk(Chunk chunk, int seed, Vector3i position) {
    int w = chunk.getWidth();
    int h = chunk.getHeight();
    int d = chunk.getDepth();

    FloatArray3d noise = make3dPerlinNoise(seed, w, h, d, position);
    float thres = h / 3;
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        for (int z = 0; z < d; z++) {
          float v = Math.abs(y);
          if (y < h / 5) {
            v += InterpolationUtil.interpolateLinear(y / (float) (h / 5), -80, 0);
          }
          // create a platform at h/4:
          if (y < h / 4) {
            v -= 5;
          }
          v += noise.get(x, y, z) * 4;
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
    Random r = new Random(seed);
    // add a "building" in the chunk
    int midX = r.nextInt(w - 10);
    int midZ = r.nextInt(d - 10);
    int groundY = findGround(chunk, h, midX, midZ);
    if (groundY >= 0 && groundY < h - 10) {
      for (int x = 0; x < 10; x++) {
        for (int y = 0; y < 10; y++) {
          for (int z = 0; z < 5; z++) {
            chunk.set(x + midX, y + groundY, z + midZ, Tile.BRICK);
          }
        }
      }
    }
    
    // add trees
    for(int i = 0; i < 4; ++i) {
      int x = r.nextInt(w - 3);
      int z = r.nextInt(d - 3);
      int y = findGround(chunk, h, x, z);
      if (y >= 1 && y < h - 20 && chunk.get(x, y-1, z) == Tile.GROUND) {
        for(int k = r.nextInt(10) + 10; k >= 0; k--) {
          chunk.set(x, y + k, z, Tile.WOOD);
          chunk.set(x+1, y + k, z, Tile.WOOD);
          chunk.set(x+1, y + k, z+1, Tile.WOOD);
          chunk.set(x, y + k, z+1, Tile.WOOD);
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
