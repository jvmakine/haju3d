package fi.haju.haju3d.server;

import java.rmi.RemoteException;
import java.util.List;

import com.google.common.collect.Lists;

import fi.haju.haju3d.protocol.Client;
import fi.haju.haju3d.protocol.Server;

public class ServerImpl implements Server {

  List<Client> loggedInClients = Lists.newArrayList(); 
  
  @Override
  public synchronized void login(Client client) {
    loggedInClients.add(client);
  }

  @Override
  public synchronized void logout(Client client) {
    loggedInClients.remove(client);
  }
  
  public synchronized void messageToClients() throws RemoteException {
    for (Client client : loggedInClients) {
      client.receiveServerMessage("Hello World");      
    }    
  }

}
