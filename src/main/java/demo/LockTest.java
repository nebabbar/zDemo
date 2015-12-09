package demo;

public class LockTest {


 public static void main(String[] args) throws Exception {
     new LockTest().run();
}


public void run() throws Exception{
    Thread t1 = new Thread(new Process(1));
    Thread t2 = new Thread(new Process(2));
    Thread t3 = new Thread(new Process(3));
    Thread t4 = new Thread(new Process(4));

    t1.start();
    t2.start();
    t3.start();
    t4.start();
}


class Process implements Runnable{


   int id;

   public Process(int id ) {
        this.id = id;
   }
    public void run() {
        try {
            String resource= "resource";
            ResourceLock lock = new ResourceLock("127.0.0.1", 2181,resource);


            System.out.println(id + " Acquiring Lock" );
            lock.acquire();
            System.out.println(id + " gets Lock" );

            Thread.sleep(500);

            System.out.println(id + " Releasing Lock" );
            lock.release();
            System.out.println(id + " Released Lock" );
            lock.destroy();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
}