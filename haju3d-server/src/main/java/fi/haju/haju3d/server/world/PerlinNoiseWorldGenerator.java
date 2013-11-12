package fi.haju.haju3d.server.world;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import fi.haju.haju3d.protocol.coordinate.ChunkPosition;
import fi.haju.haju3d.protocol.coordinate.LocalTilePosition;
import fi.haju.haju3d.protocol.world.Chunk;
import fi.haju.haju3d.protocol.world.FloatArray3d;
import fi.haju.haju3d.protocol.world.Tile;
import fi.haju.haju3d.server.world.utils.FloodFiller;
import fi.haju.haju3d.server.world.utils.WorldGenerationUtils;
import fi.haju.haju3d.util.noise.InterpolationUtil;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class PerlinNoiseWorldGenerator implements WorldGenerator {
  private int seed;

  private Map<ChunkPosition, PerlinNoiseScales> perlinNoises = Maps.newHashMap();

  @Override
  public Chunk generateChunk(ChunkPosition position, int size) {
    int realseed = seed ^ (position.x + position.y * 123 + position.z * 12347);
    if (position.y < 0) {
      return new Chunk(size, realseed, position, Tile.GROUND);
    } else if (position.y > 0) {
      return new Chunk(size, realseed, position, Tile.AIR);
    }
    Chunk chunk = new Chunk(size, realseed, position);
    return makeChunk(chunk, realseed, position);
  }

  @Override
  public void setSeed(int seed) {
    this.seed = seed;
  }

  private static final class PerlinNoiseScales {
    public static List<Integer> SCALES = ImmutableList.of(2, 4, 8, 16, 32);

    private Map<Integer, FloatArray3d> noises = Maps.newHashMap();

    public PerlinNoiseScales(final Random random, int width, int height, int depth) {
      for (int scale : SCALES) {
        int nw = width / scale;
        int nh = height / scale;
        int nd = depth / scale;
        final float amp = (float) Math.pow(0.5f * scale * 1.0f, 1.0f);
        FloatArray3d noise = new FloatArray3d(nw, nh, nd, new FloatArray3d.Initializer() {
          @Override
          public float getValue(int x, int y, int z) {
            return (float) ((random.nextDouble() - 0.5) * amp);
          }
        });
        noises.put(scale, noise);
      }
    }

    public FloatArray3d getNoise(int scale) {
      return noises.get(scale);
    }

  }

  private static Chunk filterFloaters(Chunk chunk) {
    Chunk ground = new Chunk(chunk.getSize(), chunk.getSeed(), chunk.getPosition());
    new FloodFiller(ground, chunk).fill();
    return ground;
  }

  private PerlinNoiseScales getPerlinNoiseScale(ChunkPosition pos, Random random, int w, int h, int d) {
    if (perlinNoises.containsKey(pos)) {
      return perlinNoises.get(pos);
    }
    PerlinNoiseScales noises = new PerlinNoiseScales(random, w, h, d);
    perlinNoises.put(pos, noises);
    return noises;
  }

  private FloatArray3d make3dPerlinNoise(long seed, int w, int h, int d, ChunkPosition position) {
    Random random = new Random(seed);
    FloatArray3d data = new FloatArray3d(w, h, d);
    for (int scale : PerlinNoiseScales.SCALES) {
      FloatArray3d[] surroundingScales = new FloatArray3d[3 * 3 * 3];
      for (int x = 0; x < 2; ++x) {
        for (int y = 0; y < 2; ++y) {
          for (int z = 0; z < 2; ++z) {
            surroundingScales[x + y * 2 + z * 4] = getPerlinNoiseScale(position.add(x, y, z), random, w, h, d).getNoise(scale);
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
    return scales[i + j * 2 + k * 4].get(
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

  private Chunk makeChunk(Chunk chunk, int seed, ChunkPosition position) {
    int size = chunk.getSize();

    FloatArray3d noise = make3dPerlinNoise(seed, size, size, size, position);
    float thres = size / 3;
    for (int x = 0; x < size; x++) {
      for (int y = 0; y < size; y++) {
        for (int z = 0; z < size; z++) {
          float v = Math.abs(y);
          if (y < size / 5) {
            v += InterpolationUtil.interpolateLinear(y / (float) (size / 5), -80, 0);
          }
          // create a platform at h/4:
          if (y < size / 4) {
            v -= 5;
          }
          v += noise.get(x, y, z) * 4;
          Tile terrain = noise.get(x, size - 1 - y, z) < 0 ? Tile.GROUND : Tile.ROCK;
          chunk.set(x, y, z, v < thres ? terrain : Tile.AIR);
          float col = 0.75f + noise.get(size - 1 - x, y, z) * 0.1f;
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
    int midX = r.nextInt(size - 10);
    int midZ = r.nextInt(size - 10);
    int groundY = WorldGenerationUtils.findGround(chunk, size, midX, midZ);
    if (groundY >= 0 && groundY < size - 10) {
      for (int x = 0; x < 10; x++) {
        for (int y = 0; y < 10; y++) {
          for (int z = 0; z < 5; z++) {
            chunk.set(x + midX, y + groundY, z + midZ, Tile.BRICK);
          }
        }
      }
    }

    // add trees
    generateTrees(chunk, r);
    return filterFloaters(chunk);
  }

  private void generateTrees(Chunk chunk, Random r) {
    int size = chunk.getSize();
    for (int i = 0; i < 4; ++i) {
      int x = r.nextInt(size - 3);
      int z = r.nextInt(size - 3);
      int y = WorldGenerationUtils.findGround(chunk, size, x, z);
      if (y >= 1 && y < size - 20 && chunk.get(x, y - 1, z) == Tile.GROUND) {
        WorldGenerationUtils.makeTreeAt(chunk, r, new LocalTilePosition(x, z, y));
      }
    }
  }

}
