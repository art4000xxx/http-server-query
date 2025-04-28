package ru.netology.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);
    private final int port;
    private final ExecutorService threadPool;
    private final Map<String, Map<String, Handler>> handlers;

    public Server(int port) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(64);
        this.handlers = new HashMap<>();
    }

    public void addHandler(String method, String path, Handler handler) {
        handlers.computeIfAbsent(method, k -> new HashMap<>()).put(path, handler);
    }

    public void listen() {
        try (var serverSocket = new ServerSocket(port)) {
            logger.info("Server started on port {}", port);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    final var socket = serverSocket.accept();
                    threadPool.submit(() -> handleConnection(socket));
                } catch (IOException e) {
                    logger.error("Error accepting connection: {}", e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.error("Server error: {}", e.getMessage(), e);
        } finally {
            threadPool.shutdown();
        }
    }

    private void handleConnection(Socket socket) {
        try (
                var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                var out = new BufferedOutputStream(socket.getOutputStream())
        ) {
            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                sendErrorResponse(out, 400, "Bad Request");
                return;
            }

            String[] parts = requestLine.split(" ");
            if (parts.length != 3) {
                sendErrorResponse(out, 400, "Bad Request");
                return;
            }

            String method = parts[0];
            String fullPath = parts[1];

            Map<String, String> headers = new HashMap<>();
            String line;
            while (!(line = in.readLine()).isEmpty()) {
                int index = line.indexOf(":");
                if (index != -1) {
                    headers.put(line.substring(0, index).trim(), line.substring(index + 1).trim());
                }
            }

            StringBuilder body = new StringBuilder();
            if (headers.containsKey("Content-Length")) {
                int length = Integer.parseInt(headers.get("Content-Length"));
                char[] buffer = new char[length];
                int read = in.read(buffer, 0, length);
                if (read != -1) {
                    body.append(buffer, 0, read);
                }
            }

            Request request = new Request(method, fullPath, headers, body.toString());

            Map<String, Handler> methodHandlers = handlers.get(method);
            if (methodHandlers != null) {
                Handler handler = methodHandlers.get(request.getPath());
                if (handler != null) {
                    handler.handle(request, out);
                    out.flush();
                    return;
                }
            }

            sendErrorResponse(out, 404, "Not Found");
        } catch (IOException e) {
            logger.error("Error handling connection: {}", e.getMessage(), e);
            try {
                sendErrorResponse(new BufferedOutputStream(socket.getOutputStream()), 500, "Internal Server Error");
            } catch (IOException ex) {
                logger.error("Failed to send error response: {}", ex.getMessage());
            }
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                logger.error("Error closing socket: {}", e.getMessage());
            }
        }
    }

    private void sendErrorResponse(BufferedOutputStream out, int statusCode, String statusText) throws IOException {
        String response = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                "Content-Length: 0\r\n" +
                "Connection: close\r\n" +
                "\r\n";
        out.write(response.getBytes());
        out.flush();
    }
}