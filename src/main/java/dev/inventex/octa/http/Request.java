package dev.inventex.octa.http;

import dev.inventex.octa.concurrent.future.Future;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an abstract HTTP request handler, that can send data to the server and read its response.
 */
@RequiredArgsConstructor
@Getter
public class Request {
    /**
     * The endpoint url of the http request.
     */
    private final String endpoint;

    /**
     * The used method of the http request.
     */
    private final Method method;

    /**
     * The used headers of the http request.
     */
    private final Map<String, String> headers;

    /**
     * The user agent header of the http request.
     */
    private final String userAgent;

    /**
     * The content type of the http request.
     */
    private final String contentType;

    /**
     * The accepted response type of the http request.
     */
    private final String accept;

    /**
     * The indication, whether the request expects a response.
     */
    private final boolean input;

    /**
     * The indication, whether the request send data to the server
     */
    private final boolean output;

    /**
     * The used proxy of the http request.
     */
    private final Proxy proxy;

    /**
     * The concurrent connection handle of the http request.
     */
    private volatile HttpURLConnection connection;

    /**
     * The SSL certificate validators of the http request.
     */
    private List<TrustManager> trustManagers;

    /**
     * The secure socket protocol handler of the http request.
     */
    private SSLContext sslContext;

    @SneakyThrows
    public void send(byte[] data) {
        try (OutputStream stream = connection.getOutputStream()) {
            stream.write(data);
        }
    }


    /*
    public Builder sendString(String content) {
        return this;
    }

    public Builder sendForm(String form) {
        return this;
    }

    public Builder sendJson(String json) {
        return this;
    }

    public Builder sendJson(JsonObject json) {
        return this;
    }

    public Builder sendJson(JsonBuilder json) {
        return this;
    }

    public Builder sendJson(Object json) {
        return this;
    }
    */

    /**
     * Bootstrap the http connection with the server.
     */
    private HttpURLConnection bootstrap() {
        connection = createConnection();
        if (connection instanceof HttpsURLConnection)
            createSslContext();
        setHeaders();
        return connection;
    }

    public Future<HttpURLConnection> open() {
        return Future.completeAsync(this::bootstrap);
    }

    /**
     * Establish the connection to the http server.
     * @return new http connection
     */
    @SneakyThrows
    private HttpURLConnection createConnection() {
        URL url = new URL(endpoint);
        return (HttpURLConnection) url.openConnection(proxy);
    }

    @SneakyThrows
    private void setHeaders() {
        // set the default headers of the connection
        connection.setRequestMethod(method.name());
        connection.setRequestProperty("User-Agent", userAgent);
        connection.setRequestProperty("Content-Type", contentType);
        connection.setRequestProperty("Accept", accept);
        // set the custom headers of the connection
        for (Map.Entry<String, String> entry : headers.entrySet())
            connection.setRequestProperty(entry.getKey(), entry.getValue());
    }

    @SneakyThrows
    private void createSslContext() {
        sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustManagers.toArray(new TrustManager[0]), new SecureRandom());
        ((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());
    }

    @RequiredArgsConstructor
    public static class Builder {
        /**
         * The endpoint url of the http request.
         */
        private final String endpoint;

        /**
         * The used method of the http request.
         */
        private final Method method;

        /**
         * The used headers of the http request.
         */
        private final Map<String, String> headers = new HashMap<>();

        /**
         * The user agent header of the http request.
         */
        private String userAgent = "Mozilla/5.0 ( compatible ) ";

        /**
         * The content type of the http request.
         */
        private String contentType = "text/plain";

        /**
         * The accepted response type of the http request.
         */
        private String accept = "*/*";

        /**
         * The indication, whether the request expects a response.
         */
        private boolean input = true;

        /**
         * The indication, whether the request send data to the server.
         */
        private boolean output = false;

        /**
         * The used proxy of the http request.
         */
        private Proxy proxy = Proxy.NO_PROXY;

        /**
         * Set the used headers of the http request.
         * @param headers additional request headers
         * @return this
         */
        public Builder setHeaders(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }

        /**
         * Set a header of the http request.
         * @param header header key
         * @param value header value
         * @return this
         */
        public Builder setHeader(String header, String value) {
            headers.put(header, value);
            return this;
        }

        /**
         * Set the user agent header of the http request.
         * @param userAgent new user agent header
         * @return this
         */
        public Builder setUserAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        };

        /**
         * Set the content type of the http request.
         * @param contentType new content type header
         * @return this
         */
        public Builder setContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        /**
         * Set the accepted response type of the http request.
         * @param accept new accept type header
         * @return this
         */
        public Builder setAccept(String accept) {
            this.accept = accept;
            return this;
        }

        /**
         * Indicate, whether the request expects a response.
         * @param input new input state
         * @return this
         */
        public Builder setInput(boolean input) {
            this.input = input;
            return this;
        }

        /**
         * Indication, whether the request send data to the server.
         * @param output new output state
         * @return this
         */
        public Builder setOutput(boolean output) {
            this.output = output;
            return this;
        }

        /**
         * Set the used proxy of the http request.
         * @param proxy new request proxy
         * @return this
         */
        public Builder setProxy(Proxy proxy) {
            this.proxy = proxy;
            return this;
        }

        public Request build() {
            return new Request(endpoint, method, headers, userAgent, contentType, accept, input, output, proxy);
        }

        public static Builder get(String endpoint) {
            return new Builder(endpoint, Method.GET);
        }

        public static Builder post(String endpoint) {
            return new Builder(endpoint, Method.POST);
        }
    }
}
