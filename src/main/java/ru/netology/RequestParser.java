package ru.netology;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class RequestParser {
    final int LIMIT = 4_096;

    public Optional<Request> parseRequest(BufferedInputStream in) throws IOException {

        in.mark(LIMIT);
        final byte[] buffer = new byte[LIMIT];
        final int read = in.read(buffer);

        final byte[] requestLineDelimiter = new byte[]{'\r', '\n'};
        final int requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1) {
            return Optional.empty();
        }

        final String[] requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) {
            return Optional.empty();
        }

        final String method = requestLine[0];
        System.out.println("Метод: " + method);

        final String path = requestLine[1];
        if (!path.startsWith("/")) {
            return Optional.empty();
        }
        System.out.println("Путь: " + path);

        String protocol = requestLine[2];
        System.out.println("Протокол: " + protocol);

        //ищем заголовки
        final byte[] headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        final int headersStart = requestLineEnd + requestLineDelimiter.length;
        final int headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            return Optional.empty();
        }

        in.reset();
        in.skip(headersStart);

        final byte[] headersBytes = in.readNBytes(headersEnd - headersStart);
        final List<String> headers = Arrays.asList(new String(headersBytes).split("\r\n"));
        String body = null;
        if (!method.equals("GET")) {
            in.skip(headersDelimiter.length);
            final Optional<String> contentLength = extractHeader(headers, "Content-Length");
            if (contentLength.isPresent()) {
                final int length = Integer.parseInt(contentLength.get());
                final var bodyBytes = in.readNBytes(length);
                body = new String(bodyBytes);
                System.out.println("This is a body: " + body);
            }
        }

        return Optional.of(new Request(method, path, protocol, body));
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
