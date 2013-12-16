package fi.haju.haju3d.server.world.utils;

import java.util.Map;
import java.util.Random;

import com.google.common.collect.Maps;

import fi.haju.haju3d.protocol.coordinate.Vector3i;
import fi.haju.haju3d.protocol.world.FloatArray3d;
import fi.haju.haju3d.util.noise.InterpolationUtil;

public final class PerlinNoiseGenerator {
  private static final float LEVEL_AMPLITUDE_MULTIPLIER = 5.0f;
  private final int numberOfLevels;
  private final int baseMapSize;
  private final Random random;
  private final NoiseLevel[] levels; 
  
  private final static class NoiseLevel {
    public final int size;
    public final float amplitude;
    public final Map<Vector3i, FloatArray3d> data = Maps.newHashMap();
    public final Random random;
    
    public NoiseLevel(int size, float amplitude, Random random) {
      this.size = size;
      this.amplitude = amplitude;
      this.random = random;
    }
    
    /**
     * Speed optimizer, refetch array from map only when needed
     */
    private final class DataAccessor {
      private int lastx = Integer.MIN_VALUE;
      private int lasty = Integer.MIN_VALUE;
      private int lastz = Integer.MIN_VALUE;
      private FloatArray3d array = null;
      
      public float getValueAt(int x, int y, int z) {
        // efficient integer division with rounding towards -inf
        int gx = x >= 0 ? x/size : ~(~x / size);
        int gy = y >= 0 ? y/size : ~(~y / size);
        int gz = z >= 0 ? z/size : ~(~z / size);
        if(gx != lastx || gy != lasty || gz != lastz) {
          Vector3i pos = new Vector3i(gx, gy, gz);
          makeIfDoesNotExist(pos);
          array = data.get(pos); 
          lastx = gx;
          lasty = gy;
          lastz = gz;
        }
        return amplitude * array.get(x - gx*size, y - gy*size, z - gz*size);
      }
      
    }
    
    public float getValueAt(float x, float y, float z) {
      int xi = (int)Math.floor(x);
      int yi = (int)Math.floor(y);
      int zi = (int)Math.floor(z);
      DataAccessor accessor = new DataAccessor();
      return InterpolationUtil.interpolateLinear3d(x - xi, y - yi, z - zi,
          accessor.getValueAt(xi, yi, zi),
          accessor.getValueAt(xi + 1, yi, zi),
          accessor.getValueAt(xi, yi + 1, zi),
          accessor.getValueAt(xi + 1, yi + 1, zi),
          accessor.getValueAt(xi, yi, zi + 1),
          accessor.getValueAt(xi + 1, yi, zi + 1),
          accessor.getValueAt(xi, yi + 1, zi + 1),
          accessor.getValueAt(xi + 1, yi + 1, zi + 1)
      );
    }
    
    private void makeIfDoesNotExist(Vector3i pos) {
      if(data.containsKey(pos)) return;
      data.put(pos, new FloatArray3d(size, size, size, random));
    }
    
  }
  
  public PerlinNoiseGenerator(int levels, int baseMapSize, Random random) {
    this.numberOfLevels = levels;
    this.baseMapSize = baseMapSize;
    this.random = random;
    this.levels = new NoiseLevel[levels];
  }
  
  public float getValueAt(int x, int y, int z) {
    float value = 0.0f;
    int size = baseMapSize;
    for(int level = 1; level <= numberOfLevels; ++level) {
      NoiseLevel noise = levels[level-1];
      if(noise == null) {
        noise = new NoiseLevel(16, level*LEVEL_AMPLITUDE_MULTIPLIER, random);
        levels[level-1] = noise;
      }
      value += noise.getValueAt(x/(float)size, y/(float)size, z/(float)size);
      size *= 2;
    }
    return value;
  }
  
  public float getMaxValue() {
    return (float)Math.pow(LEVEL_AMPLITUDE_MULTIPLIER, numberOfLevels);
  }
  
  public float getMinValue() {
    return getMaxValue() * -1;
  }
  
}
