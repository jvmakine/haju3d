package fi.haju.haju3d.client;

import com.google.inject.Singleton;
import fi.haju.haju3d.util.SettingsManager;

@Singleton
public class ClientSettings extends SettingsManager {

  private static final String SCREEN_WIDTH_KEY = "screenWidth";
  private static final String SCREEN_HEIGHT_KEY = "screenHeight";
  private static final String CHUNK_RENDER_DISTANCE_KEY = "chunkRenderDistance";

  private static final String CONFIG_FILE_NAME = "haju3d-client.properties";

  private int screenWidth;
  private int screenHeight;
  private int chunkRenderDistance;

  @Override
  protected void loadSettings() {
    screenWidth = loadInt(SCREEN_WIDTH_KEY, 800);
    screenHeight = loadInt(SCREEN_HEIGHT_KEY, 600);
    chunkRenderDistance = loadInt(CHUNK_RENDER_DISTANCE_KEY, 4);
  }

  public int getScreenWidth() {
    return screenWidth;
  }

  public int getScreenHeight() {
    return screenHeight;
  }

  public int getChunkRenderDistance() {
    return chunkRenderDistance;
  }

  @Override
  protected String getPropertiesFileName() {
    return CONFIG_FILE_NAME;
  }

}
