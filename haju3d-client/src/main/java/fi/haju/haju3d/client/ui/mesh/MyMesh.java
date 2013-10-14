package fi.haju.haju3d.client.ui.mesh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jme3.math.Vector3f;

import fi.haju.haju3d.protocol.world.Tile;

public class MyMesh {
  private Map<Vector3f, MyVertex> vectorToVertex = new HashMap<>();
  public Map<MyVertex, List<MyFaceAndIndex>> vertexFaces = new HashMap<>();
  public List<MyFace> faces = new ArrayList<>();
  
  public MyMesh() {
  }
  
  private MyMesh(Map<Vector3f, MyVertex> vectorToVertex, Map<MyVertex, List<MyFaceAndIndex>> vertexFaces, List<MyFace> faces) {
    this.vectorToVertex = vectorToVertex;
    this.vertexFaces = vertexFaces;
    this.faces = faces;
  }

  public static class MyFaceAndIndex {
    public MyFace face;
    public int index;
  }

  public void addFace(
      Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4,
      MyTexture texture, float color, boolean realTile, int zIndex,
      Tile tile) {
    MyFace face = new MyFace(getVertex(v1), getVertex(v2), getVertex(v3), getVertex(v4), texture, color, realTile, zIndex, tile);

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
  
  public MyMesh clone() {
    return new MyMesh(Maps.newHashMap(vectorToVertex), Maps.newHashMap(vertexFaces), Lists.newArrayList(faces));
  }
  
}