package ru.netology;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class Request implements RequestContext {

    final private String method;
    final private String path;
    final private String protocol;
    final private String body;
    final byte[] requestBytes;


    @Override
    public String toString() {
        return "Request{" +
                "method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", protocol='" + protocol + '\'' +
                ", body='" + body + '\'' +
                '}';
    }

    public Request(String method, String path, String protocol, String body, byte[] requestBytes) {
        this.method = method;
        this.path = path;
        this.protocol = protocol;
        this.body = body;
        this.requestBytes = requestBytes;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getBody() {
        return body;
    }

    public List<NameValuePair> getQueryParams() {
        List<NameValuePair> params = null;
        try {
            params = URLEncodedUtils.parse(new URI(this.path), StandardCharsets.UTF_8);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return params;
    }

    public Optional<NameValuePair> getQueryParam(String name) {
        return this.getQueryParams().stream()
                .filter(pair -> pair.getName().equals(name))
                .findAny();
    }

    public String getPathWithoutQueryParams() {
        if (this.path.contains("?")) {
            int delimiter = this.path.indexOf("?");
            return this.path.substring(0, delimiter);
        } else {
            return this.path;
        }
    }

    public List<NameValuePair> getPostParams() {
        List<NameValuePair> params = null;
        if (!(this.body == null)) {
            params = URLEncodedUtils.parse(this.body, StandardCharsets.UTF_8, '&');
        }
        return params;
    }

    public Optional<NameValuePair> getPostParam(String name) {
        return this.getPostParams().stream()
                .filter(pair -> pair.getName().equals(name))
                .findAny();
    }

    public void getParts() throws FileUploadException {
        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold(100);
        factory.setRepository(new File("temp"));

        ServletFileUpload upload = new ServletFileUpload(factory);
        //здесь как раз передается объект Request(это this) так как он имплементирует интерфейс RequestContext
        List<FileItem> items = upload.parseRequest(this);
        Iterator<FileItem> iter = items.iterator();
        while (iter.hasNext()) {
            FileItem item = iter.next();
            System.out.println(item);
        }
    }

    @Override
    public String getCharacterEncoding() {
        return "ISO-8859-1";
    }

    @Override
    public String getContentType() {
        return "multipart/form-data";
    }

    @Override
    public int getContentLength() {
        return requestBytes.length;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(requestBytes);
    }

}