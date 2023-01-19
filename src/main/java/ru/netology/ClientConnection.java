package ru.netology;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class ClientConnection extends Thread {

    private final Socket socket;

    private final List<String> validPaths;

    public ClientConnection(Socket socket, List<String> validPaths) {
        this.socket = socket;
        this.validPaths = validPaths;
    }

    @Override
    public void run() {
        try (final BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
             final BufferedOutputStream out = new BufferedOutputStream(this.socket.getOutputStream());) {

            final String url = this.getRequestUrl(in);
            System.out.println(url);


            if (!this.validPaths.contains(url)) {
                out.write((
                        "HTTP/1.1 404 Not Found\r\n" +
                                "Content-Length: 0\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.flush();
                currentThread().interrupt();

            } else {

                final Path filePath = Path.of(".", "public", url);
                System.out.println(filePath);
                final var mimeType = Files.probeContentType(filePath);
                System.out.println(mimeType);
                final var length = Files.size(filePath);
                System.out.println(length);

                // special case for classic
                if (url.equals("/classic.html")) {
                    final var template = Files.readString(filePath);
                    final var content = template.replace(
                            "{time}",
                            LocalDateTime.now().toString()
                    ).getBytes();
                    out.write((
                            "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: " + mimeType + "\r\n" +
                                    "Content-Length: " + content.length + "\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    out.write(content);
                    out.flush();
                }

                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mimeType + "\r\n" +
                                "Content-Length: " + length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                Files.copy(filePath, out);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getRequestUrl(BufferedReader bufferedReader) throws IOException {
        final var requestLine = bufferedReader.readLine();
        final var parts = requestLine.split(" ");
        if (parts.length != 3) {
            // just close socket
            socket.close();
            return "NO REQUEST";
        } else {
            return parts[1];
        }
    }
}

