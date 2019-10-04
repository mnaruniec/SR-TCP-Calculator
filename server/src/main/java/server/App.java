package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

public class App {
    public static long TERMINATION_TIMEOUT_MS = 1000;

    private static int port = -1;
    private static int workersNum = -1;
    private static Runnable[] workers;
    private static BlockingQueue<ConnectionWaiter> waiterQueue;
    private static ExecutorService executor;

    public static void main(String[] args) throws IOException {
        if (!parseArgs(args)) {
            System.out.println("Wrong CLI argument combination.");
            System.exit(1);
        }

        createWorkers();

        Socket socket = null;
        try (
                ServerSocket serverSocket = new ServerSocket(port);
        ) {
            while (!Thread.currentThread().isInterrupted()) {
                socket = serverSocket.accept();
                assignWorker(socket);
            }
        } catch(InterruptedException e) {
            if (socket != null) {
                socket.close();
            }
        } finally {
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(TERMINATION_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    System.exit(1);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

    }

    private static boolean parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch(args[i].trim()) {
                case "-p":
                    port = Integer.parseInt(args[++i].trim());
                    break;
                case "-t":
                    workersNum = Integer.parseInt(args[++i].trim());
                    break;
                default:
                    throw new IllegalArgumentException("Invalid CLI option");
            }
        }
        return port >= 0 && workersNum >= 1;
    }

    private static void createWorkers() {
        waiterQueue = new LinkedBlockingQueue<>(workersNum);
        workers = new Runnable[workersNum];
        executor = Executors.newFixedThreadPool(workersNum);

        for (int i = 0; i < workersNum; i++) {
            workers[i] = new WorkerThread(waiterQueue, i);
            executor.submit(workers[i]);
        }
    }

    private static void assignWorker(Socket socket) throws InterruptedException {
        ConnectionWaiter waiter = waiterQueue.take();
        waiter.put(socket);
    }
}
