package ru.netology;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Server {

    private int port;

    private final List<String> validPaths;

    private final Map<String, Map<String, Handler>> handlers = new ConcurrentHashMap<>();

    public Server(int port, List<String> validPaths) {
        this.port = port;
        this.validPaths = validPaths;
    }

    public void addHandler(String method, String path, Handler handler) {
        Map<String, Handler> pathAndHandler = handlers.computeIfAbsent(method, k -> new ConcurrentHashMap<>());
        pathAndHandler.put(path, handler);
    }

    public void connect() {
        try (var serverSocket = new ServerSocket(this.port)) {
            while (true) {
                final Socket socket = serverSocket.accept();
                ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(64);
                executor.submit(new ClientConnection(socket, validPaths, handlers));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
