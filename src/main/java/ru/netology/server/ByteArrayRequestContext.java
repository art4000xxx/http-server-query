package ru.netology.server;

import org.apache.commons.fileupload.RequestContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class ByteArrayRequestContext implements RequestContext {
    private final byte[] rawBody;
    private final Map<String, String> headers;

    public ByteArrayRequestContext(byte[] rawBody, Map<String, String> headers) {
        this.rawBody = rawBody;
        this.headers = headers;
    }

    @Override
    public String getCharacterEncoding() {
        return "UTF-8";
    }

    @Override
    public String getContentType() {
        return headers.getOrDefault("Content-Type", "");
    }

    @Override
    public int getContentLength() {
        return rawBody.length;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(rawBody);
    }
}