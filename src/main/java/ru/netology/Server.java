package ru.netology;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static java.lang.System.out;

public class Server {

    private ServerSocket serverSocket = null;

    private final int port;

    final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html",
            "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");

    public Server(int port) {
        this.port = port;
    }

    //Выделить класс Server с методами для запуска и обработки конкретного подключения
    public void connect() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            this.serverSocket = serverSocket;
            while (true) {
                try (final Socket socket = serverSocket.accept()) {
                    ClientConnection connection = new ClientConnection(socket, validPaths);
                    new Thread(connection).start();
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
