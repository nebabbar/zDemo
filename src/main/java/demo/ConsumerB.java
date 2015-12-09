package demo;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;

/**
 * Producer-Consumer queue
 */
public class ConsumerB extends SyncPrimitive {

    /**
     * Constructor of producer-consumer queue
     *
     * @param address
     * @param name
     */
    ConsumerB(String address, String name) {
        super(address);
        this.root = name;
        // Create ZK node name
        if (zk != null) {
            try {
                Stat s = zk.exists(root, false);
                if (s == null) {
                    zk.create(root, new byte[0], Ids.OPEN_ACL_UNSAFE,
                            CreateMode.PERSISTENT);
                }
            } catch (KeeperException e) {
                System.out
                        .println("Keeper exception when instantiating queue: "
                                + e.toString());
            } catch (InterruptedException e) {
                System.out.println("Interrupted exception");
            }
        }
    }

    /**
     * Add element to the queue.
     *
     * @param i
     * @return
     */

    /*boolean produce(int i) throws KeeperException, InterruptedException{
        ByteBuffer b = ByteBuffer.allocate(4);
        byte[] value;

        // Add child with value i
        b.putInt(i);
        value = b.array();
        zk.create(root + "/element", value, Ids.OPEN_ACL_UNSAFE,
                    CreateMode.PERSISTENT_SEQUENTIAL);

        return true;
    }*/


    /**
     * Remove first element from the queue.
     *
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    int consume() throws KeeperException, InterruptedException{
        int retvalue = -1;
        Stat stat = null;

        // Get the first element available
        while (true) {
            synchronized (mutex) {
                List<String> list = zk.getChildren(root, true);
                System.out.println("B size:" + list.size());
                if (list.size() == 0) {
                    System.out.println("Consumer B Going to wait");
                    mutex.wait();
                } else {
                    Integer min = new Integer(list.get(0).substring(7));
                    //System.out.println("min:" + min);
                    for(String s : list){
                        Integer tempValue = new Integer(s.substring(7));
                        //System.out.println("Temporary value: " + tempValue);
                        if(tempValue < min) min = tempValue;
                        
                    }
                    
                  //  byte[] b = zk.getData(root + "/element0000000012",
                    //                    false, stat);
                     byte[] b = zk.getData(root + "/element000000000" + min,
                                false, stat);
                  //  System.out.println("test:" + min);
                  
                     //zk.delete(root + "/element000000000" + min, 0);
                   
                     // zk.delete(root + "/element0000000012", 0);
                  //  ByteBuffer buffer = ByteBuffer.wrap(b);
                   // retvalue = buffer.getInt();
                     
                     Stat stat2 = zk.exists(root + "/element000000000" + min, true);
                     
                     byte[] data = {'t','e','s','t',' ','u','p'};
					zk.setData(root + "/element000000000" + min, data, stat2.getVersion());
                     
                    
                    StringBuilder str = new StringBuilder();
                    
                    for(int i=0;i<b.length;i++){
                    	
                    	str.append((char)b[i]);
                    }
                    
                    
                    
                    System.out.println("B znode value: " + str);
                    System.out.println("");
                    return retvalue;
                }
            }
        }
    }
}