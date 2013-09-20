package fi.haju.haju3d.client;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.system.AppSettings;
import com.jme3.texture.Image;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.TextureArray;
import com.jme3.util.BufferUtils;

import fi.haju.haju3d.client.ui.mesh.MyFace;
import fi.haju.haju3d.client.ui.mesh.MyMesh;
import fi.haju.haju3d.client.ui.mesh.MyMesh.MyFaceAndIndex;
import fi.haju.haju3d.client.ui.mesh.MyTexture;
import fi.haju.haju3d.client.ui.mesh.MyVertex;

public class ShaderTestingApp extends SimpleApplication {
  public static void main(String[] args) {
    ShaderTestingApp app = new ShaderTestingApp();
    AppSettings settings = new AppSettings(true);
    settings.setResolution(1024, 768);
    settings.setVSync(false);
    settings.setAudioRenderer(null);
    settings.setFullscreen(false);
    app.setSettings(settings);
    app.setShowSettings(false);
    app.start();
  }

  private TextureArray textures;
  
  private void loadTextures() {
    Map<MyTexture, String> textureToFilename = new HashMap<>();
//    textureToFilename.put(MyTexture.DIRT, "mc-dirt.png");
//    textureToFilename.put(MyTexture.GRASS, "mc-grass.png");
//    textureToFilename.put(MyTexture.ROCK, "mc-rock.png");
//    textureToFilename.put(MyTexture.BRICK, "mc-brick.png");
    textureToFilename.put(MyTexture.DIRT, "new-dirt.png");
    textureToFilename.put(MyTexture.GRASS, "new-grass.png");
    textureToFilename.put(MyTexture.ROCK, "new-rock.png");
    textureToFilename.put(MyTexture.BRICK, "new-rock.png");
    
    List<Image> images = new ArrayList<Image>();
    for (MyTexture tex : MyTexture.values()) {
      String textureResource = "fi/haju/haju3d/client/textures/" + textureToFilename.get(tex);
      TextureKey key = new TextureKey(textureResource);
      key.setGenerateMips(true);
      images.add(assetManager.loadTexture(key).getImage());
    }
    textures = new TextureArray(images);
    textures.setWrap(WrapMode.Clamp);
    textures.setMinFilter(MinFilter.BilinearNearestMipMap);
    textures.setAnisotropicFilter(4);
  }
  
  private static class NewMeshBuilder {
    private List<MyFace> realFaces;
    private FloatBuffer vertexes;
    private FloatBuffer vertexNormals;
    private FloatBuffer textureUvs;
    private FloatBuffer textureUvs2;
    private FloatBuffer textureUvs3;
    private FloatBuffer textureUvs4;
    private IntBuffer indexes;
    private FloatBuffer[] allUvs;
    private MyMesh mesh;
    private int i;

    public NewMeshBuilder(MyMesh mesh) {
      this.mesh = mesh;
      this.realFaces = mesh.faces;
      int mul = 4;
      this.vertexes = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3 * mul);
      this.vertexNormals = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3 * mul);
      this.textureUvs = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3 * mul);
      this.textureUvs2 = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3 * mul);
      this.textureUvs3 = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3 * mul);
      this.textureUvs4 = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3 * mul);
      this.indexes = BufferUtils.createIntBuffer(realFaces.size() * 6 * mul);
      this.allUvs = new FloatBuffer[] {textureUvs, textureUvs2, textureUvs3, textureUvs4};
      for (Map.Entry<MyVertex, List<MyFaceAndIndex>> e : mesh.vertexFaces.entrySet()) {
        Collections.sort(e.getValue(), new Comparator<MyFaceAndIndex>() {
          @Override
          public int compare(MyFaceAndIndex o1, MyFaceAndIndex o2) {
            return Integer.compare(o1.face.zIndex, o2.face.zIndex);
          }
        });
      }
    }

    public Mesh build() {
      i = 0;
      for (MyFace face : realFaces) {
        face.normal = new Vector3f(0, 0, -1);
        face.calcCenter();
        
        Vector3f v1 = face.v1.v;
        Vector3f v2 = face.v2.v;
        Vector3f v3 = face.v3.v;
        Vector3f v4 = face.v4.v;
        
        Vector3f v12 = v1.add(v2).mult(0.5f);
        Vector3f v23 = v2.add(v3).mult(0.5f);
        Vector3f v34 = v3.add(v4).mult(0.5f);
        Vector3f v41 = v4.add(v1).mult(0.5f);
        
        Vector3f vc = face.center;

        // v1 quadrant
        makeQuadrant(face, v1, v12, vc, v41, face.v1,
            0.5f, 0.5f, //current face
            0.5f, 0.0f, //below
            0.0f, 0.0f, //below right
            0.0f, 0.5f //right
            );

        // v2 quadrant
        makeQuadrant(face, v12, v2, v23, vc, face.v2,
            0.5f, 0.75f, //above
            0.5f, 0.25f, //current
            0.0f, 0.25f, //right
            0.0f, 0.75f //above right
            );
        
        // v3 quadrant
        makeQuadrant(face, vc, v23, v3, v34, face.v3,
            0.75f, 0.75f, //above left
            0.75f, 0.25f, //left
            0.25f, 0.25f, //current
            0.25f, 0.75f //above
            );

        // v4 quadrant
        makeQuadrant(face, v41, vc, v34, v4, face.v4,
            0.75f, 0.5f, //left
            0.75f, 0.0f, //below left
            0.25f, 0.0f, //below
            0.25f, 0.5f //current
            );
      }

      Mesh m = new Mesh();
      m.setBuffer(Type.Position, 3, vertexes);
      m.setBuffer(Type.Normal, 3, vertexNormals);
      m.setBuffer(Type.TexCoord, 3, textureUvs);
      m.setBuffer(Type.TexCoord2, 3, textureUvs2);
      m.setBuffer(Type.TexCoord3, 3, textureUvs3);
      m.setBuffer(Type.TexCoord4, 3, textureUvs4);
      m.setBuffer(Type.Index, 1, indexes);
      m.updateBound();
      return m;
    }

    private void makeQuadrant(
        MyFace face, Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4,
        MyVertex vert,
        float tu1, float tv1, float tu2, float tv2, float tu3, float tv3, float tu4, float tv4) {
      putVector(vertexes, v1);
      putVector(vertexes, v2);
      putVector(vertexes, v3);
      putVector(vertexes, v4);
  
      putVector(vertexNormals, face.normal);
      putVector(vertexNormals, face.normal);
      putVector(vertexNormals, face.normal);
      putVector(vertexNormals, face.normal);
      
      List<MyFaceAndIndex> faces = new ArrayList<>(mesh.vertexFaces.get(vert));
      while (faces.size() > 4) {
        // remove faces with lowest zindex, *except* the current face which should cover all of the poly
        if (faces.get(0).face == face) {
          faces.remove(1);
        } else {
          faces.remove(0);
        }
      }
      
      for (int fi = 0; fi < faces.size(); fi++) {
        MyFaceAndIndex fi1 = faces.get(fi);
        FloatBuffer uvs = allUvs[fi];
        int ti = fi1.face.texture.ordinal();
        if (fi1.index == 1) {
          putUvs(uvs, tu1, tv1, ti);
        } else if (fi1.index == 2) {
          putUvs(uvs, tu2, tv2, ti);
        } else if (fi1.index == 3) {
          putUvs(uvs, tu3, tv3, ti);
        } else if (fi1.index == 4) {
          putUvs(uvs, tu4, tv4, ti);
        }
      }
      // for unused textures, put all vertexes at 0.0f
      for (int fi = faces.size(); fi < 4; fi++) {
        FloatBuffer uvs = allUvs[fi];
        for (int e = 0; e < 4; e++) {
          uvs.put(0.0f).put(0.0f).put(0);
        }
      }
      
      indexes.put(i + 0).put(i + 1).put(i + 3);
      indexes.put(i + 1).put(i + 2).put(i + 3);
      i += 4;
    }
  }

  @Override
  public void simpleInitApp() {
    loadTextures();
    
    int height = 10;
    int width = 10;
    
    MyMesh mesh = new MyMesh();
    Random r = new Random(0L);
    
    int z = 0;
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        int ti = r.nextInt(999) % MyTexture.values().length;
        MyTexture texture = MyTexture.values()[ti];
        int zindex = r.nextInt(10000);
        // grass always on top
        if (texture == MyTexture.GRASS) {
          zindex += 10000;
        }
        mesh.addFace(
            new Vector3f(x + 1, y, z),
            new Vector3f(x + 1, y + 1, z),
            new Vector3f(x, y + 1, z),
            new Vector3f(x, y, z),
            texture,
            1.0f, true,
            zindex);
      }
    }
    
    Mesh m = new NewMeshBuilder(mesh).build();
    
    /*
    // simple faces
    int i = 0;
    for (MyFace face : realFaces) {
      face.normal = new Vector3f(0, 0, -1);
      face.calcCenter();
      
      Vector3f v1 = face.v1.v;
      Vector3f v2 = face.v2.v;
      Vector3f v3 = face.v3.v;
      Vector3f v4 = face.v4.v;
      
      // full face quadrant
      putVector(vertexes, v1);
      putVector(vertexes, v2);
      putVector(vertexes, v3);
      putVector(vertexes, v4);

      putVector(vertexNormals, face.normal);
      putVector(vertexNormals, face.normal);
      putVector(vertexNormals, face.normal);
      putVector(vertexNormals, face.normal);
      
      FloatBuffer uvs = allUvs[0];
      int ti = face.texture.ordinal();
      uvs.put(0.75f).put(0.75f).put(ti);
      uvs.put(0.75f).put(0.25f).put(ti);
      uvs.put(0.25f).put(0.25f).put(ti);
      uvs.put(0.25f).put(0.75f).put(ti);
      
      indexes.put(i + 0).put(i + 1).put(i + 3);
      indexes.put(i + 1).put(i + 2).put(i + 3);

      i += 4;
    }
    */
    

    //unshaded: 5500  FPS (limited?)
    //lightning, simple texture: 3900 FPS
    //lightning, texture array: 3700 FPS
    //lightning, texture array with 2d UVs and modf: 3700 FPS
    //lightning, texture array with 2d UVs and modf, vertex lightning: 5100 FPS
    //lightning, texture array with 3d UVs, 4-way blending: 2900 FPS
    //lightning, texture array with 3d UVs, 4-way blending, doubled polys: 2800 FPS
    //final: 2600 FPS
    Geometry obj = new Geometry("obj", m);
//    Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    //Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
    Material mat = new Material(assetManager, "fi/haju/haju3d/client/shaders/Terrain.j3md");
    mat.setBoolean("UseMaterialColors", true);
    mat.setColor("Diffuse", ColorRGBA.White);
    //mat.setTexture("DiffuseMap", assetManager.loadTexture("fi/haju/haju3d/client/textures/rock.png"));
    mat.setTexture("DiffuseMap", textures);
    obj.setMaterial(mat);
    float scale = 1.0f;
    obj.setLocalScale(scale);
    obj.setLocalTranslation(-width * 0.5f * scale, -height * 0.5f * scale, 0);
    rootNode.attachChild(obj);
    
    DirectionalLight light = new DirectionalLight();
    Vector3f lightDir = new Vector3f(1, 2, 3);
    light.setDirection(lightDir.normalizeLocal());
    light.setColor(new ColorRGBA(1f, 1f, 1f, 1f).mult(1.0f));
    rootNode.addLight(light);
    
  }
  
  private static void putUvs(FloatBuffer textureUvs, float u, float v, int ti) {
    textureUvs.put(u + 0.25f).put(v + 0.25f).put(ti);
    textureUvs.put(u + 0.25f).put(v + 0.0f).put(ti);
    textureUvs.put(u + 0.0f).put(v + 0.0f).put(ti);
    textureUvs.put(u + 0.0f).put(v + 0.25f).put(ti);
    
  }

  private static void putVector(FloatBuffer vertexes, Vector3f v) {
    vertexes.put(v.x).put(v.y).put(v.z);
  }

}
