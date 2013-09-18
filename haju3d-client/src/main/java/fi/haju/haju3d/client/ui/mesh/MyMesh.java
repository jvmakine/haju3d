package fi.haju.haju3d.client.ui.mesh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jme3.math.Vector3f;

class MyMesh {
  private Map<Vector3f, MyVertex> vectorToVertex = new HashMap<>();
  Map<MyVertex, List<MyFace>> vertexFaces = new HashMap<>();
  List<MyFace> faces = new ArrayList<>();

  public void addFace(Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4, MyTexture texture, float color) {
    MyFace face = new MyFace(getVertex(v1), getVertex(v2), getVertex(v3), getVertex(v4), texture, color);

    addVertexFace(face, face.v1);
    addVertexFace(face, face.v2);
    addVertexFace(face, face.v3);
    addVertexFace(face, face.v4);
    faces.add(face);
  }

  private MyVertex getVertex(Vector3f v1) {
    MyVertex v = vectorToVertex.get(v1);
    if (v == null) {
      v = new MyVertex(v1);
      vectorToVertex.put(v1, v);
    }
    return v;
  }

  private void addVertexFace(MyFace face, MyVertex v1) {
    List<MyFace> faces = vertexFaces.get(v1);
    if (faces == null) {
      faces = new ArrayList<>();
      vertexFaces.put(v1, faces);
    }
    faces.add(face);
  }
}