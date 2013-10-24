package fi.haju.haju3d.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public abstract class SettingsManager {
  
  protected final Properties properties = new Properties();

  protected abstract void loadSettings();
  protected abstract String getPropertiesFileName();

  protected void load() {
    try {
      properties.load(new FileInputStream(getPropertiesFileName()));
    } catch (FileNotFoundException e) {
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected void save() {
    try {
      properties.store(new FileOutputStream(getPropertiesFileName()), null);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  protected int loadInt(String key, int defval) {
    if (!properties.containsKey(key)) {
      properties.setProperty(key, Integer.toString(defval));
    }
    return Integer.parseInt(properties.getProperty(key));
  }
  
  protected String loadString(String key, String defval) {
    if (!properties.containsKey(key)) {
      properties.setProperty(key, defval);
    }
    return properties.getProperty(key);
  }
  
  public void init() {
    load();
    loadSettings();
    // Save missing properties as defaults
    save();
  }
  
}
