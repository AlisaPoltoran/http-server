package ru.netology;

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

    private Map<String, Map<String, Handler>> handlers;

    public ClientConnection(Socket socket, List<String> validPaths, Map<String, Map<String, Handler>> handlers) {
        this.socket = socket;
        this.validPaths = validPaths;
        this.handlers = handlers;
    }

    @Override
    public void run() {
        try (final BufferedInputStream in = new BufferedInputStream(this.socket.getInputStream());
             final BufferedOutputStream out = new BufferedOutputStream(this.socket.getOutputStream())) {

            Request request = parseRequest(in, out);

            handleRequest(request, out);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Request parseRequest(BufferedInputStream in, BufferedOutputStream out) throws IOException {

        in.mark(LIMIT);
        final byte[] buffer = new byte[LIMIT];
        final int read = in.read(buffer);

        final byte[] requestLineDelimiter = new byte[]{'\r', '\n'};
        final int requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1) {
            badRequestMessage(out);
            socket.close();
        }

        final String[] requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) {
            // just close socket
            badRequestMessage(out);
            socket.close();
        }

        final String method = requestLine[0];
        System.out.println("Метод: " + method);

        final String path = requestLine[1];
        if (!path.startsWith("/")) {
            badRequestMessage(out);
            socket.close();
        }
        System.out.println("Путь: " + path);

        String protocol = requestLine[2];
        System.out.println("Протокол: " + protocol);

        //ищем заголовки
        final byte[] headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final int headersStart = requestLineEnd + requestLineDelimiter.length;
        final int headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            badRequestMessage(out);
            socket.close();
        }

        in.reset();
        in.skip(headersStart);

        final byte[] headersBytes = in.readNBytes(headersEnd - headersStart);
        final List<String> headers = Arrays.asList(new String(headersBytes).split("\r\n"));
        String body = null;
        if (!method.equals("GET")) {
            in.skip(headersDelimiter.length);
            final Optional <String> contentLength = extractHeader(headers, "Content-Length");
            if(contentLength.isPresent()) {
                final int length = Integer.parseInt(contentLength.get());
                final var bodyBytes = in.readNBytes(length);
                body = new String(bodyBytes);
                System.out.println("This is a body: " + body);
            }
        }

        Request request = new Request(method, path, protocol, body);
        System.out.println("Метод getQueryParam по title: " + request.getQueryParam("title"));
        System.out.println("Метод getQueryParams: " + request.getQueryParams());
        System.out.println("this is a request: " + request);

        return request;

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

    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }
}