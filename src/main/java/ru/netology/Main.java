package ru.netology;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {

        final var validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html",
                "/styles.css", "/app.js", "/links.html", "/forms.html", "/events.html", "/events.js");
        final int port = 9999;

        Server server = new Server(port, validPaths);

        server.addHandler("GET", "/classic.html", (request, out) -> {
            final var template = Files.readString(Path.of(".", "public", request.getPath()));
            final var content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();

            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + Files.probeContentType(Path.of(".", "public",
                            request.getPath())) + "\r\n" +
                            "Content-Length: " + content.length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.write(content);
            out.flush();
        });


        server.addHandler("POST", "/messages", ((request, out) -> {
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
        }));

        server.addHandler("GET", "/messages", (((request, out) -> {
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
        })));

        server.addHandler("GET", "/", ((((request, out) -> {
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
        }))));

        server.connect();

    }
}