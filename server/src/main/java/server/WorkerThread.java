package server;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class WorkerThread implements Runnable {
    private int id;
    private BlockingQueue<ConnectionWaiter> queue;
    private ConnectionWaiter waiter = new ConnectionWaiter();

    public WorkerThread(BlockingQueue<ConnectionWaiter> queue, int id) {
        this.queue = queue;
        this.id = id;
    }

    @Override
    public void run() {
        try {
            while (true) {
                queue.put(waiter);
                Socket socket = waiter.take();
                this.handleConnection(socket);
                socket.close();
            }
        } catch (InterruptedException e) {
            System.out.println("Worker " + this.id + " interrupted, finishing work.");
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.out.println("IOException in Worker " + id);
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void handleConnection(Socket socket) {

    }
}
