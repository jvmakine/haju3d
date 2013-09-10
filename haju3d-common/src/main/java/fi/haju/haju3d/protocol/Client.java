package fi.haju.haju3d.protocol;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Client extends Remote {
  void receiveServerMessage(String message) throws RemoteException;
}
