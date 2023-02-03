package ru.netology;

import javax.swing.text.html.Option;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

public class ClientConnection implements Runnable {

    final int LIMIT = 4_096;

    private final Socket socket;

    private final List<String> validPaths;

    private final Map<String, Map<String, Handler>> handlers;

    public ClientConnection(Socket socket, List<String> validPaths, Map<String, Map<String, Handler>> handlers) {
        this.socket = socket;
        this.validPaths = validPaths;
        this.handlers = handlers;
    }

    @Override
    public void run() {
        try (final BufferedInputStream in = new BufferedInputStream(this.socket.getInputStream());
             final BufferedOutputStream out = new BufferedOutputStream(this.socket.getOutputStream())) {

            Optional<Request> optionalRequest = new RequestParser().parseRequest(in);
            if(optionalRequest.isEmpty()){
                badRequestMessage(out);
                return;
            }

            Request request = optionalRequest.get();

            System.out.println("Метод getQueryParam по title: " + request.getQueryParam("title"));
            System.out.println("Метод getQueryParams: " + request.getQueryParams());
            System.out.println("this is a request: " + request);
            System.out.println("Метод getPostParams: " + request.getPostParams());
            System.out.println("Метод getPostParam по title: " + request.getPostParam("m"));

            handleRequest(request, out);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleRequest(Request request, BufferedOutputStream out) {
        try {

            if (this.validPaths.contains(request.getPath())) {
                final Path filePath = Path.of(".", "public", request.getPath());
                final var mimeType = Files.probeContentType(filePath);
                final var length = Files.size(filePath);
                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                Files.copy(filePath, out);
                out.flush();
                return;
            }

            if (!handlers.containsKey(request.getMethod())) {
                notFoundMessage(out);
                return;
            }

            Map<String, Handler> values = handlers.get(request.getMethod());

            if (!values.containsKey(request.getPathWithoutQueryParams())) {
                notFoundMessage(out);
                return;
            }

            Handler handler = values.get(request.getPathWithoutQueryParams());
            handler.handle(request, out);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void badRequestMessage(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    public void notFoundMessage(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }
}