package demo;

public class SampleLockTest {


 public static void main(String[] args) throws Exception {
     new SampleLockTest().run();
}


public void run() throws Exception{
    Thread t1 = new Thread(new Process(101,"Payments"),"Payments");
    Thread t2 = new Thread(new Process(102,"Fulfillment"),"Fulfillment");
    Thread t3 = new Thread(new Process(103,"Email"),"Customer Interaction");
    //Thread t4 = new Thread(new Process(4));
    //Thread t5 = new Thread(new Process(1), "Payment");

    t1.start();
    t2.start();
    t3.start();
    //t4.start();
}


class Process implements Runnable{


   int id;
   String task;

   public Process(int id, String task ) {
        this.id = id;
        this.task = task;
   }
    public void run() {
        try {
            String resource= "resource";
            SampleResourceLock lock = new SampleResourceLock("127.0.0.1", 2181,resource,id);


            System.out.println(task +" ("+id+")" + " Acquiring Lock" );
            lock.acquire();
            //System.out.println(task +" ("+id+")" + " gets Lock" );

            Thread.sleep(500);

            System.out.println(task +" ("+id+")" + " Releasing Lock" );
            lock.release();
            System.out.println(task +" ("+id+")" + " Released Lock" );
            lock.destroy();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
}