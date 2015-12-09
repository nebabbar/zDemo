package demo;


import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
//import org.wso2.zookeeper.sample.leader.coordination.Exception;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;

public class SampleResourceLock {

  private final Object lock = new Object();

  private String myNode;
  private String myZNode = null;
  private int myId;

  private ZooKeeper zooKeeper = null;

  private String address;
  private int port;
  private String resource;
  private int taskId;

  public static String RESOURCE_LOCK_PARENT ="/sample_resource_lock_parent";

  public static String NODE_SEPARATOR = "/";


  public static String RESOURCE_LOCK_NODE = "/sample_resource_lock_node";

  /**
   * Creates a Distributed Lock for a resource
   *
   * @param address  zookeeper instance host name
   * @param port     zookeeper instance port
   * @param resource resource name
 * @param req 
   */
  public SampleResourceLock(String address, int port, String resource, int taskId) {

      this.address = address;
      this.port = port;
      this.resource = resource;
      this.taskId = taskId;

  }


  /**
   * Acquire the lock for the resource. This will get blocked till the it get
   * the lock for the resource
   *
   */
  public void acquire() throws InterruptedException, Exception {
      try {

          if (zooKeeper == null) {
              synchronized (lock) {
                  if (zooKeeper == null) {
                      System.out.println("Starting Zookeeper agent for host : " + address + " port : " + port);
                      zooKeeper = new ZooKeeper(address, port, null);
                      System.out.println("ZooKeeper agent started successfully and connected to  " + address + ":"
                              + port);
                      try {
                          if (zooKeeper.exists(RESOURCE_LOCK_PARENT + "_" + resource,false) == null) {
                              zooKeeper.create(RESOURCE_LOCK_PARENT + "_" + resource,
                                      new byte[0],
                                      ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                          }

                      } catch (Exception e) {
                          String msg = "Error while creating Queue worker coordination parent at " +
                                  RESOURCE_LOCK_PARENT + "_" + resource;
                          e.printStackTrace();
                          throw new Exception(msg, e);
                      }
                  }
              }
          }
          createNode();
          proceed();


      } catch (Exception e) {
          throw new Exception("Error Acquiring Lock ", e);
      }


  }


  /**
   * Release the Lock for the resource
   *
   * @throws Exception
   */
  public void release() throws Exception {

      try {
          deleteNode();
      } catch (Exception e) {
          throw new Exception("Error while releasing lock", e);
      }

  }


  private void createNode() throws InterruptedException, KeeperException {


      final String nodeName = RESOURCE_LOCK_NODE + taskId +
              (UUID.randomUUID()).toString().replace("-", "_");
      //System.out.println("nodename:" + nodeName);
      this.myNode = nodeName.replace("/", "");
      String path = RESOURCE_LOCK_PARENT
              + "_" + resource + nodeName;
      zooKeeper.create(path, new byte[0],
              ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
     // System.out.println("created");

  }


  private void deleteNode() throws InterruptedException, KeeperException {
      if (zooKeeper != null) {
          String path = RESOURCE_LOCK_PARENT + "_" + resource +
                  NODE_SEPARATOR + myZNode;
          zooKeeper.delete(path, -1);
      }
  }


  private List<String> getChildren() throws InterruptedException, KeeperException {

      return zooKeeper.getChildren(RESOURCE_LOCK_PARENT + "_" + resource, false);
  }


  private boolean proceed() throws InterruptedException, KeeperException {

      while (true) {
          final Semaphore lock = new Semaphore(1);
          lock.acquire();
          List<String> childNodes = getChildren();
          HashMap<Integer, String> nodeIdMap = new HashMap<Integer, String>();

          String selectedNode = null;
          int currentMin = Integer.MAX_VALUE;
          for (String child : childNodes) {
        	  
              String id = child.substring(RESOURCE_LOCK_NODE.length()-1,RESOURCE_LOCK_NODE.length()+2);
              int seqNumber = Integer.parseInt(id);
              if (child.contains(myNode)) {
                  myId = seqNumber;
                  myZNode = child;
              }
              //System.out.println("myID:" + myId+"-"+seqNumber + ",myznode:"+myZNode+"-"+child) ;
              nodeIdMap.put(seqNumber, child);
              if (seqNumber < currentMin) {
                  selectedNode = child;
                  currentMin = seqNumber;
              }
          }

          assert selectedNode != null;
          //System.out.println("selected node:" + selectedNode);
          if (selectedNode.contains(myNode)) {
              System.out.println("Lock acquired..");
              System.out.println("Call to the "+Thread.currentThread().getName()+" API.");
              break;
          } else {
              int myLockHolder = --myId;
              Stat stat = zooKeeper.exists(RESOURCE_LOCK_PARENT +
                      "_" + resource + NODE_SEPARATOR +
                      nodeIdMap.get(myLockHolder),
                      new Watcher() {

                          public void process(WatchedEvent watchedEvent) {
                              if (Event.EventType.NodeDeleted == watchedEvent.getType()) {
                                 // System.out.println("Locked Release Detected.. Trying to acquire lock again..");
                                  lock.release();

                              }
                          }
                      });

              if (stat == null) {
                  System.out.println("Locked Release Detected.. Trying to acquire lock again..");
                  continue;
              }

              lock.acquire();
          }


      }

      return true;
  }

  /**
   * Cleanup allocated Zookeeper Resources for this Lock
   */
  public void destroy() throws Exception {

      try {
          zooKeeper.close();
      } catch (InterruptedException e) {
          throw new Exception("Error while releasing the Queue Lock ", e);
      } finally {
          zooKeeper = null;
      }
  }

}
