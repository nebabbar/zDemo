package demo;


import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
//import org.wso2.zookeeper.sample.leader.coordination.Exception;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;

public class SampleResourceLock2 {

  private final Object lock = new Object();

  private String myNode;
  private String myZNode = null;
  private int myId;

  private ZooKeeper zooKeeper = null;

  private String address;
  private int port;
  private String resource;
  private int taskId;

  public static String RESOURCE_LOCK_PARENT ="/sample_resource_lock_parent_2";

  public static String NODE_SEPARATOR = "/";


  public static String RESOURCE_LOCK_NODE = "/sample_resource_lock_node_2";

  /**
   * Creates a Distributed Lock for a resource
   *
   * @param address  zookeeper instance host name
   * @param port     zookeeper instance port
   * @param resource resource name
 * @param req 
   */
  public SampleResourceLock2(String address, int port, String resource, int taskId) {

      this.address = address;
      this.port = port;
      this.resource = resource;
      this.taskId = taskId;

  }


  /**
   * Acquire the lock for the resource. This will get blocked till the it get
   * the lock for the resource
 * @return 
   *
   */
  public int acquire() throws InterruptedException, Exception {
      
	  int result = -1;
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
          result = proceed();


      } catch (Exception e) {
          throw new Exception("Error Acquiring Lock ", e);
      }
      return result;

  }


  /**
   * Release the Lock for the resource
 * @param result 
   *
   * @throws Exception
   */
  public void release(int result) throws Exception {

      try {
    	  if(result==1){
    		  deleteNode();
    	  }
    	  else if(result==0){
    		  String failText = "fail-" + taskId;
    		  byte[] failTextByte = failText.getBytes();
    		  updateNode(failTextByte);
    		  if(taskId == 103){
    			  deleteNode();    //last task's (email) node gets left over in case of failure. so deleting it
    		  }
    	  }else{
    		  
    	  }
      } catch (Exception e) {
          throw new Exception("Error while releasing lock", e);
      }

  }


  private void updateNode(byte[] failTextByte) throws KeeperException, InterruptedException {
	  if (zooKeeper != null) {
          String path = RESOURCE_LOCK_PARENT + "_" + resource + NODE_SEPARATOR + myZNode;
          //zooKeeper.delete(path, -1);
         // Stat stat = zooKeeper.exists(path, false);  
    	  zooKeeper.setData(path, failTextByte, -1);
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
              ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);
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


  private int proceed() throws InterruptedException, KeeperException {
	  
	  int result = -1;     // success (1) or failure(0) or skippedtask_because_of_previousfailure (-1)
	  
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
              System.out.println("Call to the "+Thread.currentThread().getName()+" API.(returns 1 for success and "
              		+ "0 for failure)");
              SampleAPIs sampleapi = new SampleAPIs();
              result = sampleapi.sample(taskId);
             // result = 1;  //or 0 for failure
              break;
          } else {
        	 // final int isfailed;
              int myLockHolder = myId-1;
              final String prevPath = RESOURCE_LOCK_PARENT +
                      "_" + resource + NODE_SEPARATOR +
                      nodeIdMap.get(myLockHolder);
              final String currPath = RESOURCE_LOCK_PARENT +
                      "_" + resource + NODE_SEPARATOR +
                      nodeIdMap.get(myLockHolder+1);
              System.out.println(currPath);
              Stat stat = zooKeeper.exists(prevPath,
                      new Watcher() {

                          public void process(WatchedEvent watchedEvent) {
                              if (Event.EventType.NodeDeleted == watchedEvent.getType()) {
                                  System.out.println("Locked Release Detected.. Trying to acquire lock again..");
                                  lock.release();

                              } else if(Event.EventType.NodeDataChanged == watchedEvent.getType()){
                            	  try {
                            		  System.out.println("data changed");
									byte[] b = zooKeeper.getData(prevPath, false, null);
									zooKeeper.delete(prevPath, -1);
									if(taskId != 103)
									zooKeeper.setData(currPath, b, -1);
									lock.release();
									//isfailed = 1;
								} catch (KeeperException e) {
									e.printStackTrace();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
                              }
                          }
                      });
              
              System.out.println("stat:" + stat);

              if (stat == null) {
                  System.out.println("Locked Release Detected...... Trying to acquire lock again..");
                  continue;
              }

              lock.acquire();
              
              byte[] tmp = zooKeeper.getData(currPath, false, null);
              
              String str = new String(tmp);
              
             /* StringBuilder strb = new StringBuilder();
              for(int i=0;i<tmp.length;i++){
              	strb.append((char)tmp[i]);
              }
              String str = strb.toString();*/
              
              if(str.contains("fail")){
            	  break;
              }
              
          }


      }

      return result;
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
