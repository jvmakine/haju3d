package fi.haju.haju3d.protocol;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Server extends Remote {
  void login(Client client) throws RemoteException;
  void logout(Client client) throws RemoteException;
}
