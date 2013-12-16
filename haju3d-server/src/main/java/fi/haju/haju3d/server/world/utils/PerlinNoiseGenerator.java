package fi.haju.haju3d.server.world.utils;

import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.text.StyledEditorKit.ForegroundAction;

import com.google.common.collect.Lists;
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
  
  public float getMinValue(Vector3i corner, int edge) {
    List<Vector3i> corners = getCorners(corner, edge);
    float sum = 0;
    Vector3i localPos = null;
    int size = baseMapSize;
    for(int i = 1; i <= numberOfLevels; ++i) {
      float min = Float.MAX_VALUE;
      NoiseLevel level = levels[i-1];
      if(level == null) {
        level = new NoiseLevel(16, i*LEVEL_AMPLITUDE_MULTIPLIER, random);
        levels[i-1] = level;
      }
      for(Vector3i v : corners) {
        int gx = v.x >= 0 ? v.x/size : ~(~v.x / size);
        int gy = v.y >= 0 ? v.y/size : ~(~v.y / size);
        int gz = v.z >= 0 ? v.z/size : ~(~v.z / size);
        Vector3i lp = new Vector3i(gx, gy, gz);
        if(localPos == null) {
          localPos = lp;
        }
        if(!lp.equals(localPos)) {
          min = -LEVEL_AMPLITUDE_MULTIPLIER*i;
          break;
        }
        min = Math.min(min, level.getValueAt(v.x/(float)size, v.y/(float)size, v.z/(float)size));
      }
      sum += min;
      size *= 2;
    }
    return sum;
  }
  
  public float getMaxValue(Vector3i corner, int edge) {
    List<Vector3i> corners = getCorners(corner, edge);
    float sum = 0;
    Vector3i localPos = null;
    int size = baseMapSize;
    for(int i = 1; i <= numberOfLevels; ++i) {
      float max = Float.MIN_VALUE;
      NoiseLevel level = levels[i-1];
      if(level == null) {
        level = new NoiseLevel(16, i*LEVEL_AMPLITUDE_MULTIPLIER, random);
        levels[i-1] = level;
      }
      for(Vector3i v : corners) {
        int gx = v.x >= 0 ? v.x/size : ~(~v.x / size);
        int gy = v.y >= 0 ? v.y/size : ~(~v.y / size);
        int gz = v.z >= 0 ? v.z/size : ~(~v.z / size);
        Vector3i lp = new Vector3i(gx, gy, gz);
        if(localPos == null) {
          localPos = lp;
        }
        if(!lp.equals(localPos)) {
          max = LEVEL_AMPLITUDE_MULTIPLIER*i;
          break;
        }
        max = Math.max(max, level.getValueAt(v.x/(float)size, v.y/(float)size, v.z/(float)size));
      }
      sum += max;
      size *= 2;
    }
    return sum;
  }

  private List<Vector3i> getCorners(Vector3i corner, int edge) {
    List<Vector3i> corners = Lists.newArrayList(
        corner, corner.add(0,edge,0), corner.add(edge,edge,0), corner.add(edge,0,0),
        corner.add(0,0,edge), corner.add(edge,0,edge), corner.add(0,edge,edge), corner.add(edge,edge,edge)
    );
    return corners;
  }
  
}
