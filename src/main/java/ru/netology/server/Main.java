package ru.netology.server;

import java.io.BufferedOutputStream;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        Server server = new Server(9999);

        server.addHandler("GET", "/messages", (request, responseStream) -> {
            StringBuilder responseBody = new StringBuilder();
            responseBody.append("Query Params: ").append(request.getQueryParams()).append("\n");
            String lastParam = request.getQueryParam("last");
            if (lastParam != null) {
                responseBody.append("Last Param: ").append(lastParam);
            } else {
                responseBody.append("No 'last' param provided");
            }

            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Length: " + responseBody.length() + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n" +
                    responseBody;
            responseStream.write(response.getBytes());
            responseStream.flush();
        });

        server.addHandler("POST", "/messages", (request, responseStream) -> {
            StringBuilder responseBody = new StringBuilder();
            responseBody.append("Post Params: ").append(request.getPostParams()).append("\n");
            String nameParam = request.getPostParam("name");
            if (nameParam != null) {
                responseBody.append("Name Param: ").append(nameParam);
            } else {
                responseBody.append("No 'name' param provided");
            }

            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Length: " + responseBody.length() + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n" +
                    responseBody;
            responseStream.write(response.getBytes());
            responseStream.flush();
        });

        server.listen();
    }
}