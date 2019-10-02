package server;

import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ConnectionWaiter {
    private BlockingQueue<Socket> slot = new LinkedBlockingQueue<>(1);

    public void put(Socket socket) throws InterruptedException {
        slot.put(socket);
    }

    public Socket take() throws InterruptedException {
        return slot.take();
    }
}
