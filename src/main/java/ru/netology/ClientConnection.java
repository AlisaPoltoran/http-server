package ru.netology;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class ClientConnection implements Runnable {

    private final Socket socket;

    private final List<String> validPaths;

    // TODO согласно условиям задачи, handlers должны храниться только в классе server, но я не могу придумать,
    //  как их тогда использовать в классе ClientConnection, кроме как передать через конструктор.
    //  Можете подсказать, пожалуйста?

    private Map<String, Map<String, Handler>> handlers;

    public ClientConnection(Socket socket, List<String> validPaths, Map<String, Map<String, Handler>> handlers) {
        this.socket = socket;
        this.validPaths = validPaths;
        this.handlers = handlers;
    }

    @Override
    public void run() {
        try (final BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
             final BufferedOutputStream out = new BufferedOutputStream(this.socket.getOutputStream());) {

            Request request = parseRequest(in, out);

            handleRequest(request, out);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Request parseRequest(BufferedReader in, BufferedOutputStream out) throws IOException {
        final String requestLine = in.readLine();
        final String[] parts = requestLine.split(" ");
        if (parts.length != 3) {
            // just close socket
            badRequestMessage(out);
            socket.close();
        }

        String method = parts[0];
        String path = parts[1];
        String protocol = parts[2];
        String body = "There is no body in this request";
        if (parts.length > 3) {
            body = parts[3];
        }

        return new Request(method, path, protocol, body);
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

            if (!values.containsKey(request.getPath())) {
                notFoundMessage(out);
                return;
            }

            Handler handler = values.get(request.getPath());
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

