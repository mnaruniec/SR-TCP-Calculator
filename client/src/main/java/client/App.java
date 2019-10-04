package client;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class App {
    public static final int TIMEOUT_MS = 5000;
    public static final Charset charset =  StandardCharsets.UTF_8;
    public static final int BUFFER_SIZE = 20000;
    private static final byte[] buffer = new byte[BUFFER_SIZE];

    private static String host = "";
    private static int port = -1;

    public static void main(String[] args) throws IOException {
        parseArgs(args);
        String result = "";
        boolean timeout = false;

        try (
            InputStream cin = new BufferedInputStream(System.in);
            Socket socket = new Socket(host, port);
            DataOutputStream outStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            DataInputStream inStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()))
        ) {
            socket.setSoTimeout(TIMEOUT_MS);

            int read;
            int newline = -1;
            while (newline == -1 && (read = cin.read(buffer)) != -1) {
                newline = findNewline(read);
                if (newline != -1) {
                    read = newline;
                }
                if (read > 0) {
                    sendBuffer(outStream, read);
                }
            }
            sendTerminator(outStream);
            result = receiveResponse(inStream);
        } catch (SocketTimeoutException e) {
            timeout = true;
        }

        System.out.println(timeout ? "TIMEOUT" : result);
    }

    private static boolean parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch(args[i].trim()) {
                case "-p":
                    port = Integer.parseInt(args[++i].trim());
                    break;
                case "-a":
                    host = args[++i].trim();
                    break;
                default:
                    throw new IllegalArgumentException("Invalid CLI option");
            }
        }
        return port >= 0 && !host.isEmpty();
    }

    private static int findNewline(int read) {
        for (int i = 0; i < read; i++) {
            if (buffer[i] == '\n') {
                return i;
            }
        }
        return -1;
    }

    private static void sendBuffer(DataOutputStream outStream, int read) throws IOException {
        outStream.writeInt(read);
        outStream.write(buffer, 0, read);
    }

    private static void sendTerminator(DataOutputStream outStream) throws IOException {
        outStream.writeInt(-1);
        outStream.flush();
    }

    private static String receiveResponse(DataInputStream inStream) throws IOException {
        int len = inStream.readInt();
        byte[] inBuffer = new byte[len];
        int read = 0;
        while (read < len) {
            int nowRead = inStream.read(inBuffer, read, len - read);
            if (nowRead == -1) {
                throw new IOException("Connection finished before receiving full response.");
            }
            read += nowRead;
        }
        return new String(inBuffer, charset);
    }

}
