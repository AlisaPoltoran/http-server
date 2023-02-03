package ru.netology;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class Request {

    final private String method;
    final private String path;
    final private String protocol;
    final private String body;


    @Override
    public String toString() {
        return "Request{" +
                "method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", protocol='" + protocol + '\'' +
                ", body='" + body + '\'' +
                '}';
    }

    public Request(String method, String path, String protocol, String body) {
        this.method = method;
        this.path = path;
        this.protocol = protocol;
        this.body = body;
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


}