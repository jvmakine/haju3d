package fi.haju.haju3d.client.ui.mesh;

import com.jme3.math.Vector3f;
import fi.haju.haju3d.client.chunk.light.TileLight;
import fi.haju.haju3d.protocol.coordinate.Vector3i;
import fi.haju.haju3d.protocol.world.Tile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MyMesh {
  private Map<Vector3f, MyVertex> vectorToVertex = new HashMap<>();
  public Map<MyVertex, List<MyFaceAndIndex>> vertexFaces = new HashMap<>();
  public List<MyFace> faces = new ArrayList<>();
  public Map<MyVertex, Vector3f> vertexToNormal = new HashMap<>();
  public Map<MyVertex, Vector3f> vertexToLight = new HashMap<>();

  public static class MyFaceAndIndex {
    public MyFace face;
    public int index;
  }

  public void addFace(
      Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4,
      MyTexture texture, float color, boolean realTile, int zIndex,
      Tile tile, TileLight light, Vector3i worldPos) {
    MyFace face = new MyFace(
        getVertex(v1), getVertex(v2), getVertex(v3), getVertex(v4),
        texture, color, realTile, zIndex, tile, light.red, light.green, light.blue, worldPos);

    addVertexFace(face, face.v1, 1);
    addVertexFace(face, face.v2, 2);
    addVertexFace(face, face.v3, 3);
    addVertexFace(face, face.v4, 4);
    faces.add(face);
  }

  private MyVertex getVertex(Vector3f vect) {
    MyVertex v = vectorToVertex.get(vect);
    if (v == null) {
      v = new MyVertex(vect);
      vectorToVertex.put(vect, v);
    }
    return v;
  }

  private void addVertexFace(MyFace face, MyVertex vertex, int vertexIndex) {
    List<MyFaceAndIndex> faces = vertexFaces.get(vertex);
    if (faces == null) {
      faces = new ArrayList<>(5);
      vertexFaces.put(vertex, faces);
    }
    MyFaceAndIndex fi = new MyFaceAndIndex();
    fi.face = face;
    fi.index = vertexIndex;
    faces.add(fi);
  }

  public void calcVertexLights() {
    for (MyFace face : faces) {
      calcVertexLight(face.v1);
      calcVertexLight(face.v2);
      calcVertexLight(face.v3);
      calcVertexLight(face.v4);
    }
  }

  private void calcVertexLight(MyVertex v) {
    if (vertexToLight.containsKey(v)) {
      return;
    }
    float sumR = 0;
    float sumG = 0;
    float sumB = 0;
    for (MyFaceAndIndex f : vertexFaces.get(v)) {
      sumR += f.face.lightR;
      sumG += f.face.lightG;
      sumB += f.face.lightB;
    }
    sumR /= vertexFaces.get(v).size();
    sumG /= vertexFaces.get(v).size();
    sumB /= vertexFaces.get(v).size();
    vertexToLight.put(v, new Vector3f(
        sumR / (float) TileLight.MAX_DISTANCE,
        sumG / (float) TileLight.MAX_DISTANCE,
        sumB / (float) TileLight.MAX_DISTANCE));
  }

  public void calcVertexNormals() {
    for (MyFace face : faces) {
      calcVertexNormal(face.v1);
      calcVertexNormal(face.v2);
      calcVertexNormal(face.v3);
      calcVertexNormal(face.v4);
    }
  }

  private void calcVertexNormal(MyVertex v) {
    if (vertexToNormal.containsKey(v)) {
      return;
    }

    Vector3f sum = Vector3f.ZERO.clone();
    for (MyFaceAndIndex f : vertexFaces.get(v)) {
      sum.addLocal(f.face.normal);
    }
    sum.normalizeLocal();

    vertexToNormal.put(v, sum);
  }

  public List<MyFace> getRealFaces() {
    List<MyFace> realFaces = new ArrayList<>();
    for (MyFace face : faces) {
      if (face.realTile) {
        realFaces.add(face);
      }
    }
    return realFaces;
  }

}