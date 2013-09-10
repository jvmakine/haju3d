package fi.haju.haju3d.client;

import fi.haju.haju3d.protocol.Client;

public class ClientImpl implements Client {

  @Override
  public void receiveServerMessage(String message) {
    System.out.println(message);
  }

}
