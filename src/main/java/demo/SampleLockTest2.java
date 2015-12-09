package demo;

public class SampleLockTest2 {


 public static void main(String[] args) throws Exception {
     new SampleLockTest2().run();
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
            SampleResourceLock2 lock = new SampleResourceLock2("127.0.0.1", 2181,resource,id);


            System.out.println(task +" ("+id+")" + " Acquiring Lock" );
            int result = lock.acquire();
            if(result==1){
            	System.out.println("success in " + id + ":" +task);
            }else if(result==0){
            	System.out.println("failure in " + id +":" +task);
            }else{
            	System.out.println("Failure carry forward through " +id +":"+task );
            }
            //System.out.println(task +" ("+id+")" + " gets Lock" );

            Thread.sleep(500);

            System.out.println(task +" ("+id+")" + " Releasing Lock" );
            lock.release(result);
            System.out.println(task +" ("+id+")" + " Released Lock" );
            lock.destroy();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
}