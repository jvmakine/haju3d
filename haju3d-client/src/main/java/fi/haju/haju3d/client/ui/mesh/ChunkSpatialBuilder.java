package fi.haju.haju3d.client.ui.mesh;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.texture.Image;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.TextureArray;
import com.jme3.util.BufferUtils;
import fi.haju.haju3d.client.ui.ChunkRenderer;
import fi.haju.haju3d.client.ui.ChunkSpatial;
import fi.haju.haju3d.client.ui.mesh.MyMesh.MyFaceAndIndex;
import fi.haju.haju3d.client.ui.mesh.TileRenderPropertyProvider.TileProperties;
import fi.haju.haju3d.protocol.Vector3i;
import fi.haju.haju3d.protocol.world.Tile;
import fi.haju.haju3d.protocol.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

@Singleton
public class ChunkSpatialBuilder {
  public static final int SMOOTH_BUFFER = 2;
  private static final Logger LOGGER = LoggerFactory.getLogger(ChunkSpatialBuilder.class);
  private Material lowMaterial;
  private Material highMaterial;

  @Inject
  private ChunkRenderer chunkRenderer;

  public void init() {
    init(chunkRenderer.getAssetManager());
  }

  public void init(AssetManager assetManager) {
    Map<MyTexture, String> textureToFilename = new HashMap<>();
    textureToFilename.put(MyTexture.DIRT, "new-dirt.png");
    textureToFilename.put(MyTexture.GRASS, "new-grass.png");
    textureToFilename.put(MyTexture.GRASS2, "new-grass2.png");
    textureToFilename.put(MyTexture.ROCK, "new-rock.png");
    textureToFilename.put(MyTexture.ROCK2, "new-rock2.png");
    textureToFilename.put(MyTexture.BRICK, "new-brick.png");
    textureToFilename.put(MyTexture.WOOD1, "wood1.png");
    textureToFilename.put(MyTexture.WOOD2, "wood2.png");
    textureToFilename.put(MyTexture.COBBLESTONE1, "cobblestone1.png");

    List<Image> images = new ArrayList<Image>();
    for (MyTexture tex : MyTexture.values()) {
      String textureResource = "fi/haju/haju3d/client/textures/" + textureToFilename.get(tex);
      TextureKey key = new TextureKey(textureResource);
      key.setGenerateMips(true);
      images.add(assetManager.loadTexture(key).getImage());
    }
    TextureArray textures = new TextureArray(images);
    textures.setWrap(WrapMode.Clamp);
    //TODO setting MinFilter to use MipMap causes GLError GL_INVALID_ENUM! But not idea where exactly..
    textures.setMinFilter(MinFilter.BilinearNearestMipMap);
    textures.setAnisotropicFilter(4);

    this.lowMaterial = makeMaterial(assetManager, textures, "fi/haju/haju3d/client/shaders/Lighting.j3md");
    this.highMaterial = makeMaterial(assetManager, textures, "fi/haju/haju3d/client/shaders/Terrain.j3md");
  }

  private Material makeMaterial(AssetManager assetManager, TextureArray textures, String materialFile) {
    Material mat = new Material(assetManager, materialFile);
    mat.setBoolean("UseMaterialColors", true);
    mat.setTexture("DiffuseMap", textures);
    mat.setColor("Ambient", ColorRGBA.White);
    mat.setColor("Diffuse", ColorRGBA.White);
    return mat;
  }

  public void rebuildChunkSpatial(World world, ChunkSpatial chunkSpatial) {
    LOGGER.info("Updating chunk spatial at " + chunkSpatial.chunk.getPosition());
    MyMesh myMesh = makeCubeMesh(world, chunkSpatial.chunk.getPosition());
    chunkSpatial.cubes = makeCubeSpatial(myMesh);

    // common processing for non-cube meshes
    smoothMesh(myMesh);
    prepareMesh(myMesh);

    chunkSpatial.lowDetail = makeSpatial(true, myMesh);
    chunkSpatial.highDetail = makeSpatial(false, myMesh);
    LOGGER.info("Done");
  }

  public static void prepareMesh(MyMesh myMesh) {
    for (MyFace face : myMesh.faces) {
      face.normal = face.v2.v.subtract(face.v1.v).cross(face.v4.v.subtract(face.v1.v)).normalize();
    }
    for (Map.Entry<MyVertex, List<MyFaceAndIndex>> e : myMesh.vertexFaces.entrySet()) {
      Collections.sort(e.getValue(), new Comparator<MyFaceAndIndex>() {
        @Override
        public int compare(MyFaceAndIndex o1, MyFaceAndIndex o2) {
          return Integer.compare(o1.face.zIndex, o2.face.zIndex);
        }
      });
    }
    myMesh.calcVertexNormals();
  }

  private Spatial makeCubeSpatial(MyMesh myMesh) {
    // normals are not meaningful for cube spatial, just assign something
    for (MyFace face : myMesh.faces) {
      face.normal = Vector3f.UNIT_Z;
    }
    for (MyVertex v : myMesh.vertexFaces.keySet()) {
      myMesh.vertexToNormal.put(v, Vector3f.UNIT_Z);
    }
    Geometry geom = new Geometry("ColoredMesh", new SimpleMeshBuilder(myMesh).build());
    myMesh.vertexToNormal.clear();
    return geom;
  }

  public ChunkSpatial makeChunkSpatial(World world, Vector3i chunkIndex) {
    LOGGER.info("Making chunk spatial at " + chunkIndex);
    ChunkSpatial chunkSpatial = new ChunkSpatial();
    chunkSpatial.chunk = world.getChunk(chunkIndex);
    rebuildChunkSpatial(world, chunkSpatial);
    return chunkSpatial;
  }

  public Spatial makeSpatial(boolean useSimpleMesh, MyMesh myMesh) {
    Mesh m = useSimpleMesh ? new SimpleMeshBuilder(myMesh).build() : new NewMeshBuilder2(myMesh).build();
    final Geometry groundObject = new Geometry("ColoredMesh", m);
    groundObject.setMaterial(useSimpleMesh ? lowMaterial : highMaterial);
    groundObject.setShadowMode(ShadowMode.CastAndReceive);
    return groundObject;
  }


  public static class NewMeshBuilder2 {
    private List<MyFace> realFaces;
    private FloatBuffer vertexes;
    private FloatBuffer vertexNormals;
    private FloatBuffer textureUvs;
    private FloatBuffer textureUvs2;
    private FloatBuffer textureUvs3;
    private FloatBuffer textureUvs4;
    private FloatBuffer textureUvs5;
    private IntBuffer indexes;
    private FloatBuffer[] allUvs;

    private MyMesh mesh;
    private int i;
    private final List<Vector2f> centerQuad;
    private final Map<String, List<Vector2f>> uvMapping;

    public NewMeshBuilder2(MyMesh mesh) {
      List<MyFace> realFaces = mesh.getRealFaces();
      this.mesh = mesh;
      this.realFaces = realFaces;

      final int quadsPerFace = 1;
      this.vertexes = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3 * quadsPerFace);
      this.vertexNormals = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3 * quadsPerFace);
      this.textureUvs = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3 * quadsPerFace);
      this.textureUvs2 = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3 * quadsPerFace);
      this.textureUvs3 = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3 * quadsPerFace);
      this.textureUvs4 = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3 * quadsPerFace);
      this.textureUvs5 = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3 * quadsPerFace);
      this.indexes = BufferUtils.createIntBuffer(realFaces.size() * 6 * quadsPerFace);
      this.allUvs = new FloatBuffer[] {textureUvs, textureUvs2, textureUvs3, textureUvs4, textureUvs5};


      uvMapping = new HashMap<>();
      //vertex/quadrant order:
      //32
      //41

      centerQuad = makeQuad(0, 0);
      List<Vector2f> quad;

      {
        quad = centerQuad;
        String key = uvKey(centerQuad, quad);
        List<Vector2f> uvs = makeUvs(quad);
        uvMapping.put(key, uvs);
      }
      addUvMappings(makeQuad(1, 0));
      addUvMappings(makeQuad(0, 1));
      addUvMappings(makeQuad(0, -1));
      addUvMappings(makeQuad(-1, 0));
    }

    private void addUvMappings(List<Vector2f> quad) {
      for (int e = 0; e < 2; e++) {
        for (int i = 0; i < 4; i++) {
          rotateQuad(quad);
          String key = uvKey(centerQuad, quad);
          List<Vector2f> uvs = makeUvs(quad);
          if (uvMapping.containsKey(key)) {
            throw new IllegalStateException("key exists: " + key);
          }
          uvMapping.put(key, uvs);
        }
        flipQuad(quad);
      }
    }

    private String uvKey(List<Vector2f> centerQuad, List<Vector2f> quad) {
      String uvKey = "";
      for (int ci = 0; ci < centerQuad.size(); ci++) {
        boolean match = false;
        int qi = 0;
        for (; qi < quad.size(); qi++) {
          if (quad.get(qi).equals(centerQuad.get(ci))) {
            match = true;
            break;
          }
        }
        if (match) {
          uvKey += (ci + 1) + "=" + (qi + 1) + ",";
        }
      }
      return uvKey;
    }

    private List<Vector2f> makeUvs(List<Vector2f> quad) {
      List<Vector2f> result = new ArrayList<>();
      for (Vector2f q : makeQuad(0, 0)) {
        result.add(makeUv(q, quad));
      }
      return result;
    }

    private Vector2f makeUv(Vector2f q, List<Vector2f> quad) {
      //quad[0] == 0.75 , 0.25
      //quad[2] == 0.25 , 0.75
      //quad[3] == 0.25 , 0.25

      float a = q.x - quad.get(3).x;
      float e = q.y - quad.get(3).y;
      float c = quad.get(0).x - quad.get(3).x;
      float g = quad.get(0).y - quad.get(3).y;
      float d = quad.get(2).x - quad.get(3).x;
      float h = quad.get(2).y - quad.get(3).y;

      //solve a=x c + y d, e=x g + y h for x and y
      float x, y;
      x = (d * e - a * h) / (d * g - c * h);
      y = (c * e - a * g) / (c * h - d * g);
      return new Vector2f(
          0.25f + 0.5f * x,
          0.25f + 0.5f * y);
    }

    private void flipQuad(List<Vector2f> quad) {
      Collections.reverse(quad);
    }

    private void rotateQuad(List<Vector2f> quad) {
      quad.add(quad.get(0));
      quad.remove(0);
    }

    private List<Vector2f> makeQuad(int x, int y) {
      List<Vector2f> result = new ArrayList<>();
      result.add(new Vector2f(x + 1, y));
      result.add(new Vector2f(x + 1, y + 1));
      result.add(new Vector2f(x, y + 1));
      result.add(new Vector2f(x, y));
      return result;
    }


    private static class TexZUvs {
      boolean base;
      MyTexture texture;
      List<Vector2f> uvs;
      int zIndex;

      private TexZUvs(MyTexture texture, List<Vector2f> uvs, int zIndex, boolean base) {
        this(texture, uvs, zIndex);
        this.base = base;
      }

      private TexZUvs(MyTexture texture, List<Vector2f> uvs, int zIndex) {
        this.texture = texture;
        this.uvs = uvs;
        this.zIndex = zIndex;
      }
    }

    public Mesh build() {
      i = 0;
      for (MyFace face : realFaces) {
        face.calcCenter();

        Vector3f v1 = face.v1.v;
        Vector3f v2 = face.v2.v;
        Vector3f v3 = face.v3.v;
        Vector3f v4 = face.v4.v;

        Vector3f n1 = mesh.vertexToNormal.get(face.v1);
        Vector3f n2 = mesh.vertexToNormal.get(face.v2);
        Vector3f n3 = mesh.vertexToNormal.get(face.v3);
        Vector3f n4 = mesh.vertexToNormal.get(face.v4);

        putVector(vertexes, v1);
        putVector(vertexes, v2);
        putVector(vertexes, v3);
        putVector(vertexes, v4);

        putVector(vertexNormals, n1);
        putVector(vertexNormals, n2);
        putVector(vertexNormals, n3);
        putVector(vertexNormals, n4);

        List<TexZUvs> texZUvses = new ArrayList<>();
        texZUvses.add(new TexZUvs(face.texture, uvMapping.get("1=1,2=2,3=3,4=4,"), face.zIndex, true));

        {
          MyFace bf = findBuddyFace(face, face.v1, face.v2);
          if (bf != null) {
            String key = "1=" + findFaceVertexIndex(bf, face.v1) + ",2=" + findFaceVertexIndex(bf, face.v2) + ",";
            texZUvses.add(new TexZUvs(bf.texture, uvMapping.get(key), bf.zIndex));
          }
        }
        {
          MyFace bf = findBuddyFace(face, face.v2, face.v3);
          if (bf != null) {
            String key = "2=" + findFaceVertexIndex(bf, face.v2) + ",3=" + findFaceVertexIndex(bf, face.v3) + ",";
            texZUvses.add(new TexZUvs(bf.texture, uvMapping.get(key), bf.zIndex));
          }
        }
        {
          MyFace bf = findBuddyFace(face, face.v3, face.v4);
          if (bf != null) {
            String key = "3=" + findFaceVertexIndex(bf, face.v3) + ",4=" + findFaceVertexIndex(bf, face.v4) + ",";
            texZUvses.add(new TexZUvs(bf.texture, uvMapping.get(key), bf.zIndex));
          }
        }
        {
          MyFace bf = findBuddyFace(face, face.v4, face.v1);
          if (bf != null) {
            String key = "1=" + findFaceVertexIndex(bf, face.v1) + ",4=" + findFaceVertexIndex(bf, face.v4) + ",";
            texZUvses.add(new TexZUvs(bf.texture, uvMapping.get(key), bf.zIndex));
          }
        }
        Collections.sort(texZUvses, new Comparator<TexZUvs>() {
          @Override
          public int compare(TexZUvs o1, TexZUvs o2) {
            return Integer.compare(o1.zIndex, o2.zIndex);
          }
        });
        // remove all textures under base texture
        while (!texZUvses.get(0).base) {
          texZUvses.remove(0);
        }
        // remove top textures if there's too many
        while (texZUvses.size() > 5) {
          texZUvses.remove(1);
        }

        for (int i = 0; i < texZUvses.size(); i++) {
          FloatBuffer textures = allUvs[i];
          TexZUvs texZUvs = texZUvses.get(i);
          int ti = texZUvs.texture.ordinal();
          for (Vector2f uv : texZUvs.uvs) {
            textures.put(uv.x).put(uv.y).put(ti);
          }
        }

        for (int fi = texZUvses.size(); fi < 5; fi++) {
          FloatBuffer uvs = allUvs[fi];
          for (int e = 0; e < 4; e++) {
            uvs.put(0.0f).put(0.0f).put(0);
          }
        }

        indexes.put(i + 0).put(i + 1).put(i + 3);
        indexes.put(i + 1).put(i + 2).put(i + 3);
        i += 4;

        //vertex/quadrant order:
        //32
        //41
      }

      Mesh m = new Mesh();
      m.setBuffer(Type.Position, 3, vertexes);
      m.setBuffer(Type.Normal, 3, vertexNormals);
      m.setBuffer(Type.TexCoord, 3, textureUvs);
      m.setBuffer(Type.TexCoord2, 3, textureUvs2);
      m.setBuffer(Type.TexCoord3, 3, textureUvs3);
      m.setBuffer(Type.TexCoord4, 3, textureUvs4);
      m.setBuffer(Type.TexCoord5, 3, textureUvs5);
      m.setBuffer(Type.Index, 1, indexes);
      m.updateBound();
      return m;
    }

    private MyFace findBuddyFace(MyFace face, MyVertex v1, MyVertex v2) {
      Set<MyFace> potentialFaces = new HashSet<>();
      for (MyFaceAndIndex fi : mesh.vertexFaces.get(v1)) {
        if (fi.face != face) {
          potentialFaces.add(fi.face);
        }
      }
      for (MyFaceAndIndex fi : mesh.vertexFaces.get(v2)) {
        if (potentialFaces.contains(fi.face)) {
          return fi.face;
        }
      }
      return null;
    }

    private int findFaceVertexIndex(MyFace face, MyVertex v) {
      for (MyFaceAndIndex fi : mesh.vertexFaces.get(v)) {
        if (fi.face == face) {
          return fi.index;
        }
      }
      throw new IllegalStateException();
    }
  }

  public static class NewMeshBuilder {
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
      List<MyFace> realFaces = mesh.getRealFaces();
      this.mesh = mesh;
      this.realFaces = realFaces;

      final int quadsPerFace = 4;
      this.vertexes = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3 * quadsPerFace);
      this.vertexNormals = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3 * quadsPerFace);
      this.textureUvs = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3 * quadsPerFace);
      this.textureUvs2 = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3 * quadsPerFace);
      this.textureUvs3 = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3 * quadsPerFace);
      this.textureUvs4 = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3 * quadsPerFace);
      this.indexes = BufferUtils.createIntBuffer(realFaces.size() * 6 * quadsPerFace);
      this.allUvs = new FloatBuffer[] {textureUvs, textureUvs2, textureUvs3, textureUvs4};
    }

    public Mesh build() {
      i = 0;
      for (MyFace face : realFaces) {
        face.calcCenter();

        Vector3f v1 = face.v1.v;
        Vector3f v2 = face.v2.v;
        Vector3f v3 = face.v3.v;
        Vector3f v4 = face.v4.v;

        Vector3f n1 = mesh.vertexToNormal.get(face.v1);
        Vector3f n2 = mesh.vertexToNormal.get(face.v2);
        Vector3f n3 = mesh.vertexToNormal.get(face.v3);
        Vector3f n4 = mesh.vertexToNormal.get(face.v4);

        Vector3f v12 = v1.add(v2).mult(0.5f);
        Vector3f v23 = v2.add(v3).mult(0.5f);
        Vector3f v34 = v3.add(v4).mult(0.5f);
        Vector3f v41 = v4.add(v1).mult(0.5f);

        Vector3f n12 = n1.add(n2).normalizeLocal();
        Vector3f n23 = n2.add(n3).normalizeLocal();
        Vector3f n34 = n3.add(n4).normalizeLocal();
        Vector3f n41 = n4.add(n1).normalizeLocal();

        Vector3f vc = face.center;
        Vector3f nc = face.normal;

        //vertex/quadrant order:
        //32
        //41

        // v1 quadrant
        makeQuadrant(face,
            v1, v12, vc, v41,
            n1, n12, nc, n41,
            face.v1,
            0.5f, 0.5f, //current face
            0.5f, 0.0f, //below
            0.0f, 0.0f, //below right
            0.0f, 0.5f //right
        );

        // v2 quadrant
        makeQuadrant(face,
            v12, v2, v23, vc,
            n12, n2, n23, nc,
            face.v2,
            0.5f, 0.75f, //above
            0.5f, 0.25f, //current
            0.0f, 0.25f, //right
            0.0f, 0.75f //above right
        );

        // v3 quadrant
        makeQuadrant(face,
            vc, v23, v3, v34,
            nc, n23, n3, n34,
            face.v3,
            0.75f, 0.75f, //above left
            0.75f, 0.25f, //left
            0.25f, 0.25f, //current
            0.25f, 0.75f //above
        );

        // v4 quadrant
        makeQuadrant(face,
            v41, vc, v34, v4,
            n41, nc, n34, n4,
            face.v4,
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
        MyFace face,
        Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4,
        Vector3f n1, Vector3f n2, Vector3f n3, Vector3f n4,
        MyVertex vertex,
        float tu1, float tv1, float tu2, float tv2, float tu3, float tv3, float tu4, float tv4) {
      putVector(vertexes, v1);
      putVector(vertexes, v2);
      putVector(vertexes, v3);
      putVector(vertexes, v4);

      if (face.tile == Tile.BRICK) {
        putVector(vertexNormals, face.normal);
        putVector(vertexNormals, face.normal);
        putVector(vertexNormals, face.normal);
        putVector(vertexNormals, face.normal);
      } else {
        putVector(vertexNormals, n1);
        putVector(vertexNormals, n2);
        putVector(vertexNormals, n3);
        putVector(vertexNormals, n4);
      }

      List<MyFaceAndIndex> faces = new ArrayList<>(mesh.vertexFaces.get(vertex));
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

    private static void putUvs(FloatBuffer textureUvs, float u, float v, int ti) {
      textureUvs.put(u + 0.25f).put(v + 0.25f).put(ti);
      textureUvs.put(u + 0.25f).put(v + 0.0f).put(ti);
      textureUvs.put(u + 0.0f).put(v + 0.0f).put(ti);
      textureUvs.put(u + 0.0f).put(v + 0.25f).put(ti);
    }
  }

  public static class SimpleMeshBuilder {
    private MyMesh myMesh;

    public SimpleMeshBuilder(MyMesh myMesh) {
      this.myMesh = myMesh;
    }

    public Mesh build() {
      List<MyFace> realFaces = myMesh.getRealFaces();

      FloatBuffer vertexes = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3);
      FloatBuffer vertexNormals = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3);
      FloatBuffer textures = BufferUtils.createFloatBuffer(realFaces.size() * 4 * 3);
      IntBuffer indexes = BufferUtils.createIntBuffer(realFaces.size() * 6);

      int i = 0;
      for (MyFace face : realFaces) {
        putVector(vertexes, face.v1.v);
        putVector(vertexes, face.v2.v);
        putVector(vertexes, face.v3.v);
        putVector(vertexes, face.v4.v);

        if (face.tile == Tile.BRICK) {
          putVector(vertexNormals, face.normal);
          putVector(vertexNormals, face.normal);
          putVector(vertexNormals, face.normal);
          putVector(vertexNormals, face.normal);
        } else {
          putVector(vertexNormals, myMesh.vertexToNormal.get(face.v1));
          putVector(vertexNormals, myMesh.vertexToNormal.get(face.v2));
          putVector(vertexNormals, myMesh.vertexToNormal.get(face.v3));
          putVector(vertexNormals, myMesh.vertexToNormal.get(face.v4));
        }

        int ti = face.texture.ordinal();
        textures.put(0.25f).put(0.25f).put(ti);
        textures.put(0.25f).put(0.75f).put(ti);
        textures.put(0.75f).put(0.75f).put(ti);
        textures.put(0.75f).put(0.25f).put(ti);

        indexes.put(i + 0).put(i + 1).put(i + 3);
        indexes.put(i + 1).put(i + 2).put(i + 3);

        i += 4;
      }

      Mesh m = new Mesh();
      m.setBuffer(Type.Position, 3, vertexes);
      m.setBuffer(Type.Normal, 3, vertexNormals);
      m.setBuffer(Type.TexCoord, 3, textures);
      m.setBuffer(Type.Index, 1, indexes);
      m.updateBound();
      return m;
    }
  }

  private static void putVector(FloatBuffer vertexes, Vector3f v) {
    vertexes.put(v.x).put(v.y).put(v.z);
  }

  private static int getZIndex(int x, int y, int z, int edge) {
    return new Random(x + y * 133 + z * 23525 + edge * 1248234).nextInt();
  }

  public static MyMesh makeCubeMesh(World world, Vector3i chunkIndex) {
    synchronized (world) {
      MyMesh myMesh = new MyMesh();

      Vector3i w1o = world.getWorldPosition(chunkIndex);
      Vector3i w2o = world.getWorldPosition(chunkIndex.add(1, 1, 1));

      Vector3i w1 = w1o.add(-SMOOTH_BUFFER, -SMOOTH_BUFFER, -SMOOTH_BUFFER);
      Vector3i w2 = w2o.add(SMOOTH_BUFFER, SMOOTH_BUFFER, SMOOTH_BUFFER);

      for (int z = w1.z; z < w2.z; z++) {
        for (int y = w1.y; y < w2.y; y++) {
          for (int x = w1.x; x < w2.x; x++) {
            Tile tile = world.get(x, y, z);
            if (tile != Tile.AIR) {
              TileProperties properties = TileRenderPropertyProvider.getProperties(tile);
              boolean realTile =
                  x >= w1o.x && x < w2o.x &&
                      y >= w1o.y && y < w2o.y &&
                      z >= w1o.z && z < w2o.z;
              float color = world.getColor(x, y, z);
              if (world.get(x, y - 1, z) == Tile.AIR) {
                int seed = getZIndex(x, y, z, 0);
                myMesh.addFace(
                    new Vector3f(x, y, z),
                    new Vector3f(x + 1, y, z),
                    new Vector3f(x + 1, y, z + 1),
                    new Vector3f(x, y, z + 1),
                    properties.getSideTexture(seed), color,
                    realTile,
                    seed, tile);
              }
              if (world.get(x, y + 1, z) == Tile.AIR) {
                int seed = getZIndex(x, y, z, 1);
                myMesh.addFace(
                    new Vector3f(x, y + 1, z + 1),
                    new Vector3f(x + 1, y + 1, z + 1),
                    new Vector3f(x + 1, y + 1, z),
                    new Vector3f(x, y + 1, z),
                    properties.getTopTexture(seed), color,
                    realTile,
                    seed, tile);
              }
              if (world.get(x - 1, y, z) == Tile.AIR) {
                int seed = getZIndex(x, y, z, 2);
                myMesh.addFace(
                    new Vector3f(x, y, z + 1),
                    new Vector3f(x, y + 1, z + 1),
                    new Vector3f(x, y + 1, z),
                    new Vector3f(x, y, z),
                    properties.getSideTexture(seed), color,
                    realTile,
                    seed, tile);
              }
              if (world.get(x + 1, y, z) == Tile.AIR) {
                int seed = getZIndex(x, y, z, 3);
                myMesh.addFace(
                    new Vector3f(x + 1, y, z),
                    new Vector3f(x + 1, y + 1, z),
                    new Vector3f(x + 1, y + 1, z + 1),
                    new Vector3f(x + 1, y, z + 1),
                    properties.getSideTexture(seed), color,
                    realTile,
                    seed, tile);
              }
              if (world.get(x, y, z - 1) == Tile.AIR) {
                int seed = getZIndex(x, y, z, 4);
                myMesh.addFace(
                    new Vector3f(x, y, z),
                    new Vector3f(x, y + 1, z),
                    new Vector3f(x + 1, y + 1, z),
                    new Vector3f(x + 1, y, z),
                    properties.getSideTexture(seed), color,
                    realTile,
                    seed, tile);
              }
              if (world.get(x, y, z + 1) == Tile.AIR) {
                int seed = getZIndex(x, y, z, 5);
                myMesh.addFace(
                    new Vector3f(x + 1, y, z + 1),
                    new Vector3f(x + 1, y + 1, z + 1),
                    new Vector3f(x, y + 1, z + 1),
                    new Vector3f(x, y, z + 1),
                    properties.getSideTexture(seed), color,
                    realTile,
                    seed, tile);
              }
            }
          }
        }
      }
      return myMesh;
    }
  }

  private static class PositionChange {
    Vector3f oldPos;
    Vector3f newPos;

    private PositionChange(Vector3f oldPos, Vector3f newPos) {
      this.oldPos = oldPos;
      this.newPos = newPos;
    }
  }

  public static void smoothMesh(MyMesh myMesh) {
    for (int i = 0; i < SMOOTH_BUFFER; i++) {
      List<PositionChange> newPos = new ArrayList<>(myMesh.vertexFaces.size());
      for (MyFace f : myMesh.faces) {
        f.calcCenter();
      }
      for (Map.Entry<MyVertex, List<MyFaceAndIndex>> e : myMesh.vertexFaces.entrySet()) {
        MyVertex vertex = e.getKey();
        Vector3f sum = Vector3f.ZERO.clone();
        List<MyFaceAndIndex> faces = e.getValue();
        int maxSmooths = SMOOTH_BUFFER;
        for (MyFaceAndIndex f : faces) {
          sum.addLocal(f.face.center);
          maxSmooths = Math.min(TileRenderPropertyProvider.getProperties(f.face.tile).getMaxSmooths(), maxSmooths);
        }
        sum.divideLocal(faces.size());
        if (vertex.smooths < maxSmooths) {
          vertex.smooths++;
          newPos.add(new PositionChange(vertex.v, sum));
        }
      }
      for (PositionChange positionChange : newPos) {
        positionChange.oldPos.set(positionChange.newPos);
      }
    }
  }
}
