package server;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;

public class WorkerThread implements Runnable {
    public static final Charset charset = StandardCharsets.UTF_8;
    public static final int BUFFER_SIZE = 20000;
    private byte[] buffer = new byte[BUFFER_SIZE];

    private int id;
    private BlockingQueue<ConnectionWaiter> queue;
    private ConnectionWaiter waiter = new ConnectionWaiter();

    public WorkerThread(BlockingQueue<ConnectionWaiter> queue, int id) {
        this.queue = queue;
        this.id = id;
    }

    @Override
    public void run() {
        this.log("Commencing work.");
        Socket socket = null;
        try {
            while (true) {
                socket = null;
                queue.put(waiter);
                this.log("Ready for next request.");
                socket = waiter.take();
                this.log("Received new connection.");
                this.handleConnection(socket);
            }
        } catch (InterruptedException e) {
            this.log("Interrupted, finishing work.");
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            this.log("IOException occurred.");
            e.printStackTrace();
        } catch (Exception e) {
            this.log("Unknown exception occurred.");
            e.printStackTrace();
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                this.log("IOException occurred when closing a socket.");
                e.printStackTrace();
            }
        }
    }

    private void handleConnection(Socket socket) throws IOException {
        WorkerComputation computation = new WorkerComputation();
        try (
            DataInputStream inStream =
                    new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream outStream =
                    new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        ) {
            boolean gotNewline = false;
            while (!gotNewline) {
                int read = inStream.read(buffer, 0, BUFFER_SIZE);
                if (read < 0) {
                    this.log("Connection lost.");
                    return;
                }
                if (read == 0) {
                    continue;
                }
                if (buffer[read - 1] == '\n') {
                    gotNewline = true;
                    read--;
                }
                computation.processInput(buffer, read);
            }

            String result = computation.getResult();
            sendResult(outStream, result);
        }
    }

    private void sendResult(DataOutputStream outStream, String result) throws IOException {
        byte[] bytes = result.getBytes(charset);
        outStream.writeInt(bytes.length);
        outStream.write(bytes);
        outStream.flush();
    }

    private void log(String msg) {
        System.out.println("Worker " + this.id + ": " + msg);
    }
}
