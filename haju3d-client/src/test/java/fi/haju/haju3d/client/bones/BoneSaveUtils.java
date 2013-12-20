package fi.haju.haju3d.client.bones;

import com.jme3.math.Vector3f;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class BoneSaveUtils {
  private BoneSaveUtils() {
    assert false;
  }

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static class MyBoneStruct implements Serializable {
    public float[] start;
    public float[] end;
    public float thickness = 1.0f;
    public int mirrorBoneIndex;
    public String meshName;
  }

  public static String saveBones(List<MyBone> bones) {
    List<MyBoneStruct> boneStructs = new ArrayList<>();
    for (MyBone bone : bones) {
      MyBoneStruct bs = new MyBoneStruct();
      bs.start = bone.getStart().toArray(null);
      bs.end = bone.getEnd().toArray(null);
      bs.thickness = bone.getThickness();
      if (bone.getMirrorBone() != null) {
        bs.mirrorBoneIndex = bones.indexOf(bone.getMirrorBone());
      } else {
        bs.mirrorBoneIndex = -1;
      }
      bs.meshName = bone.getMeshName();
      boneStructs.add(bs);
    }
    try {
      return OBJECT_MAPPER.writeValueAsString(boneStructs);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static List<MyBone> readBones(String json) {
    List<MyBoneStruct> boneStructs;
    try {
      boneStructs = OBJECT_MAPPER.readValue(json, new TypeReference<List<MyBoneStruct>>() {
      });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    List<MyBone> bones = new ArrayList<>();
    for (MyBoneStruct bs : boneStructs) {
      MyBone bone = new MyBone(
          new Vector3f(bs.start[0], bs.start[1], bs.start[2]),
          new Vector3f(bs.end[0], bs.end[1], bs.end[2]),
          bs.thickness,
          bs.meshName);
      bones.add(bone);
    }
    int i = 0;
    for (MyBoneStruct bs : boneStructs) {
      if (bs.mirrorBoneIndex >= 0) {
        bones.get(i).setMirrorBone(bones.get(bs.mirrorBoneIndex));
      }
      i++;
    }
    return bones;
  }

}
