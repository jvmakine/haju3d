package fi.haju.haju3d.protocol;

import java.rmi.Remote;
import java.rmi.RemoteException;

import fi.haju.haju3d.protocol.world.Chunk;

public interface Server extends Remote {
  void login(Client client) throws RemoteException;
  void logout(Client client) throws RemoteException;
  Chunk getChunk(Vector3i position) throws RemoteException;
}
