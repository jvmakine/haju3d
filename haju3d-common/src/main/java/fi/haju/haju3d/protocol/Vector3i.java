package fi.haju.haju3d.protocol;

import java.io.Serializable;

public class Vector3i implements Serializable {
  private static final long serialVersionUID = 1L;
  public int x;
  public int y;
  public int z;

  public Vector3i(int x, int y, int z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + x;
    result = prime * result + y;
    result = prime * result + z;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Vector3i other = (Vector3i) obj;
    if (x != other.x) {
      return false;
    }
    if (y != other.y) {
      return false;
    }
    if (z != other.z) {
      return false;
    }
    return true;
  }

  public Vector3i add(int x, int y, int z) {
    return new Vector3i(this.x + x, this.y + y, this.z + z);
  }
  
  public String toString() {
    return "[" + x + "," + y + "," + z + "]";
  }
  
}