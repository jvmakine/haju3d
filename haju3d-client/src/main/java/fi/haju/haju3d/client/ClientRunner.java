package fi.haju.haju3d.client;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import com.google.common.collect.Lists;
import com.jogamp.newt.event.KeyListener;

import fi.haju.haju3d.client.ui.ClientWindow;
import fi.haju.haju3d.client.ui.input.BasicOperationsKeyListener;
import fi.haju.haju3d.client.ui.render.Ortho2DViewportRenderer;
import fi.haju.haju3d.client.ui.render.TriangleRenderer;
import fi.haju.haju3d.protocol.Client;
import fi.haju.haju3d.protocol.Server;

/**
 * Class to start the client
 */
public class ClientRunner {

  public static void main(String[] args) {
    
    Client client = new ClientImpl();
    
    try {
      Client stub = (Client)UnicastRemoteObject.exportObject(client, 5251);
      Registry registry = LocateRegistry.getRegistry(5250);
      Server server = (Server)registry.lookup("haju3d_server");
      server.login(stub);
    } catch (RemoteException | NotBoundException e) {
      throw new RuntimeException(e);
    }
    
    ClientWindow window = new ClientWindow();
    BasicOperationsKeyListener basicOperationsKeyListener = new BasicOperationsKeyListener();
    
    basicOperationsKeyListener.setClientWindow(window);
    
    window.setGlListeners(Lists.newArrayList(
        new Ortho2DViewportRenderer(),
        new TriangleRenderer()
    ));
    window.setKeyListeners(Lists.<KeyListener>newArrayList(
        basicOperationsKeyListener
    ));
    
    window.run();
  }

}
