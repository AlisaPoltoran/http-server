package ru.netology;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Server {

    private int port;

    private final List<String> validPaths;

    public Server(int port, List<String> validPaths) {
        this.port = port;
        this.validPaths = validPaths;
    }


    //Выделить класс Server с методами для запуска и обработки конкретного подключения
    public void connect() {
        try (var serverSocket = new ServerSocket(this.port)) {
            while (true) {
                final Socket socket = serverSocket.accept();
                ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(64);
                executor.submit(new ClientConnection(socket, validPaths));
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
