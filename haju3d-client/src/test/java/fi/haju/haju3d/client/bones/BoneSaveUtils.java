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
    public float[] attachPoint;
    public float[] freePoint;
    public float thickness = 1.0f;
    public int mirrorBoneIndex;
    public int parentBoneIndex;
    public String meshName;
  }

  public static List<MyBone> cloneBones(List<MyBone> bones) {
    return readBones(saveBones(bones));
  }

  public static String saveBones(List<MyBone> bones) {
    List<MyBoneStruct> boneStructs = new ArrayList<>();
    for (MyBone bone : bones) {
      MyBoneStruct bs = new MyBoneStruct();
      bs.attachPoint = bone.getAttachPoint().toArray(null);
      bs.freePoint = bone.getFreePoint().toArray(null);
      bs.thickness = bone.getThickness();
      bs.mirrorBoneIndex = getBoneIndex(bones, bone.getMirrorBone());
      bs.parentBoneIndex = getBoneIndex(bones, bone.getParentBone());
      bs.meshName = bone.getMeshName();
      boneStructs.add(bs);
    }
    try {
      return OBJECT_MAPPER.writeValueAsString(boneStructs);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static int getBoneIndex(List<MyBone> bones, MyBone bone) {
    if (bone != null) {
      return bones.indexOf(bone);
    } else {
      return -1;
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
          new Vector3f(bs.attachPoint[0], bs.attachPoint[1], bs.attachPoint[2]),
          new Vector3f(bs.freePoint[0], bs.freePoint[1], bs.freePoint[2]),
          bs.thickness,
          bs.meshName);
      bones.add(bone);
    }
    int i = 0;
    for (MyBoneStruct bs : boneStructs) {
      if (bs.mirrorBoneIndex >= 0) {
        bones.get(i).setMirrorBone(bones.get(bs.mirrorBoneIndex));
      }
      if (bs.parentBoneIndex >= 0) {
        bones.get(i).setParentBone(bones.get(bs.parentBoneIndex));
      }
      i++;
    }
    return bones;
  }

}
