package ru.netology.server;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Request {
    private final String method;
    private final String path;
    private final Map<String, String> headers;
    private final String body;
    private final Map<String, List<String>> queryParams;
    private final Map<String, List<String>> postParams;
    private final Map<String, List<FileItem>> parts;

    public Request(String method, String path, Map<String, String> headers, String body, byte[] rawBody) {
        this.method = method;
        this.path = path.contains("?") ? path.substring(0, path.indexOf("?")) : path;
        this.headers = Collections.unmodifiableMap(new HashMap<>(headers));
        this.body = body;
        this.queryParams = parseQueryParams(path);
        this.postParams = parsePostParams(body, headers);
        this.parts = parseMultipart(rawBody, headers);
    }

    private Map<String, List<String>> parseQueryParams(String path) {
        if (!path.contains("?")) {
            return Collections.emptyMap();
        }
        String query = path.substring(path.indexOf("?") + 1);
        List<NameValuePair> pairs = URLEncodedUtils.parse(query, StandardCharsets.UTF_8);
        return pairs.stream()
                .collect(Collectors.groupingBy(
                        NameValuePair::getName,
                        Collectors.mapping(NameValuePair::getValue, Collectors.toList())
                ));
    }

    private Map<String, List<String>> parsePostParams(String body, Map<String, String> headers) {
        if (body.isEmpty() || !headers.getOrDefault("Content-Type", "").contains("application/x-www-form-urlencoded")) {
            return Collections.emptyMap();
        }
        List<NameValuePair> pairs = URLEncodedUtils.parse(body, StandardCharsets.UTF_8);
        return pairs.stream()
                .collect(Collectors.groupingBy(
                        NameValuePair::getName,
                        Collectors.mapping(NameValuePair::getValue, Collectors.toList())
                ));
    }

    private Map<String, List<FileItem>> parseMultipart(byte[] rawBody, Map<String, String> headers) {
        if (!headers.getOrDefault("Content-Type", "").startsWith("multipart/form-data")) {
            return Collections.emptyMap();
        }

        try {
            DiskFileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);
            List<FileItem> items = upload.parseRequest(new ByteArrayRequestContext(rawBody, headers));
            return items.stream()
                    .collect(Collectors.groupingBy(
                            FileItem::getFieldName,
                            Collectors.toList()
                    ));
        } catch (FileUploadException e) {
            return Collections.emptyMap();
        }
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public String getQueryParam(String name) {
        List<String> values = queryParams.get(name);
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }

    public Map<String, List<String>> getQueryParams() {
        return Collections.unmodifiableMap(queryParams);
    }

    public String getPostParam(String name) {
        List<String> values = postParams.get(name);
        return (values != null && !values.isEmpty()) ? values.get(0) : null;
    }

    public Map<String, List<String>> getPostParams() {
        return Collections.unmodifiableMap(postParams);
    }

    public FileItem getPart(String name) {
        List<FileItem> items = parts.get(name);
        return (items != null && !items.isEmpty()) ? items.get(0) : null;
    }

    public Map<String, List<FileItem>> getParts() {
        return Collections.unmodifiableMap(parts);
    }
}