package fi.haju.haju3d.client;

public class Timer {
  static long t0 = System.currentTimeMillis();
  public static void printTime(String text) {
    System.out.println("time taken " + text + ":" + (System.currentTimeMillis() - t0));
  }
}
