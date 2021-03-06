package ch.viascom.groundwork.foxhttp;

import ch.viascom.groundwork.foxhttp.authorization.FoxHttpAuthorization;
import ch.viascom.groundwork.foxhttp.authorization.FoxHttpAuthorizationContext;
import ch.viascom.groundwork.foxhttp.authorization.FoxHttpAuthorizationScope;
import ch.viascom.groundwork.foxhttp.body.request.FoxHttpRequestBody;
import ch.viascom.groundwork.foxhttp.body.request.FoxHttpRequestBodyContext;
import ch.viascom.groundwork.foxhttp.exception.FoxHttpException;
import ch.viascom.groundwork.foxhttp.exception.FoxHttpRequestException;
import ch.viascom.groundwork.foxhttp.header.FoxHttpHeader;
import ch.viascom.groundwork.foxhttp.header.HeaderEntry;
import ch.viascom.groundwork.foxhttp.interceptor.FoxHttpInterceptorExecutor;
import ch.viascom.groundwork.foxhttp.interceptor.request.context.FoxHttpRequestConnectionInterceptorContext;
import ch.viascom.groundwork.foxhttp.interceptor.request.context.FoxHttpRequestHeaderInterceptorContext;
import ch.viascom.groundwork.foxhttp.interceptor.request.context.FoxHttpRequestInterceptorContext;
import ch.viascom.groundwork.foxhttp.interceptor.response.context.FoxHttpResponseCodeInterceptorContext;
import ch.viascom.groundwork.foxhttp.interceptor.response.context.FoxHttpResponseInterceptorContext;
import ch.viascom.groundwork.foxhttp.log.FoxHttpLoggerLevel;
import ch.viascom.groundwork.foxhttp.placeholder.FoxHttpPlaceholderStrategy;
import ch.viascom.groundwork.foxhttp.query.FoxHttpRequestQuery;
import ch.viascom.groundwork.foxhttp.type.HeaderTypes;
import ch.viascom.groundwork.foxhttp.type.RequestType;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.HttpsURLConnection;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * @author patrick.boesch@viascom.ch
 */
public class FoxHttpRequest {

    @Getter
    private URL url;

    @Getter
    @Setter
    private FoxHttpAuthorizationScope authScope;

    @Getter
    @Setter
    private FoxHttpRequestQuery requestQuery = new FoxHttpRequestQuery();

    @Getter
    @Setter
    private FoxHttpRequestBody requestBody;

    @Getter
    @Setter
    private FoxHttpHeader requestHeader = new FoxHttpHeader();

    @Getter
    @Setter
    private RequestType requestType = RequestType.GET;

    @Getter
    @Setter
    private boolean skipResponseBody = false;

    @Getter
    @Setter
    private boolean followRedirect = true;

    @Getter
    private FoxHttpResponse foxHttpResponse;

    @Getter
    private FoxHttpClient foxHttpClient;

    @Getter(AccessLevel.PROTECTED)
    private URLConnection connection;

    @Getter
    @Setter
    private FoxHttpPlaceholderStrategy foxHttpPlaceholderStrategy;

    @Getter
    @Setter
    private boolean overrideLoggerEnabled;


    public FoxHttpRequest() throws FoxHttpRequestException {
        this(new FoxHttpClient());
    }

    public FoxHttpRequest(FoxHttpClient foxHttpClient) throws FoxHttpRequestException {
        setFoxHttpClient(foxHttpClient);
    }

    public void setFoxHttpClient(FoxHttpClient foxHttpClient) throws FoxHttpRequestException {
        if (foxHttpClient == null) {
            throw new FoxHttpRequestException("FoxHttpClient can not be null");
        }
        this.foxHttpClient = foxHttpClient;
        // Copy configuration from client to request
        try {
            this.setFoxHttpPlaceholderStrategy(this.foxHttpClient.getFoxHttpPlaceholderStrategy().getClass().newInstance());
            this.getFoxHttpPlaceholderStrategy().getPlaceholderMap().putAll(this.foxHttpClient.getFoxHttpPlaceholderStrategy().getPlaceholderMap());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new FoxHttpRequestException("Could not copy foxHttpPlaceholderStrategy from client to request: " + e.getMessage());
        }

        this.overrideLoggerEnabled = this.foxHttpClient.getFoxHttpLogger().isLoggingEnabled();
    }

    public void setUrl(String url) throws MalformedURLException, FoxHttpRequestException {
        String parsedURL = foxHttpPlaceholderStrategy.processPlaceholders(url, foxHttpClient);
        this.url = new URL(parsedURL);
    }

    /**
     * Activate defined Logger
     *
     * @param activate activate logger
     */
    public void activateFoxHttpLogger(boolean activate) {
        this.overrideLoggerEnabled = activate;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    /**
     * Execute a this request
     *
     * @return Response if this request
     */
    public FoxHttpResponse execute() throws FoxHttpException {
        return execute(foxHttpClient);
    }

    /**
     * Execute a this request
     *
     * @param foxHttpClient a specific client which will be used for this request
     * @return Response if this request
     */
    public FoxHttpResponse execute(FoxHttpClient foxHttpClient) throws FoxHttpException {
        verifyRequest();
        foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.INFO, "========= Request =========");
        foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.DEBUG, "setFoxHttpClient(" + foxHttpClient + ")");
        this.foxHttpClient = foxHttpClient;

        return executeHttp("https".equals(getUrl().getProtocol()));
    }


    private FoxHttpResponse executeHttp(boolean isHttps) throws FoxHttpException {
        try {
            //Execute interceptor
            foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.DEBUG, "executeRequestInterceptor()");
            FoxHttpInterceptorExecutor.executeRequestInterceptor(new FoxHttpRequestInterceptorContext(getUrl(), this, foxHttpClient));

            foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.DEBUG, "setCookieStore(" + foxHttpClient.getFoxHttpCookieStore() + ")");
            CookieHandler.setDefault((CookieManager) foxHttpClient.getFoxHttpCookieStore());

            // Create Scope
            setAuthScope(FoxHttpAuthorizationScope.create(getUrl().toString(), requestType));

            foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.DEBUG, "prepareQuery(" + getRequestQuery() + ")");
            prepareQuery();

            foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.DEBUG, "processPlaceholders()");
            String parsedURL = foxHttpPlaceholderStrategy.processPlaceholders(getUrl().toString(), foxHttpClient);
            url = new URL(parsedURL);

            checkPlaceholders();

            //Execute interceptor
            foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.DEBUG, "executeRequestConnectionInterceptor()");
            FoxHttpInterceptorExecutor.executeRequestConnectionInterceptor(new FoxHttpRequestConnectionInterceptorContext(getUrl(), this, foxHttpClient));

            //Create connection
            foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.INFO, "createConnection(" + getUrl() + ")");
            if (foxHttpClient.getFoxHttpProxyStrategy() == null) {
                connection = getUrl().openConnection();
            } else {
                foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.INFO, "useProxy(" + foxHttpClient.getFoxHttpProxyStrategy() + ")");
                connection = getUrl().openConnection(foxHttpClient.getFoxHttpProxyStrategy().getProxy(getUrl()));
                if (foxHttpClient.getFoxHttpProxyStrategy().hasProxyAuthorization(getUrl())) {
                    setHeaderIfNotExist(HeaderTypes.PROXY_AUTHORIZATION, foxHttpClient.getFoxHttpProxyStrategy().getProxyAuthorization(getUrl()), connection);
                }
            }

            foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.DEBUG, "setRequestMethod(" + requestType.toString() + ")");
            ((HttpURLConnection) connection).setRequestMethod(requestType.toString());

            //Set headers
            foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.DEBUG, "prepareHeader(" + getRequestHeader() + ")");
            prepareHeader();

            //Set User-Agent if not exist
            foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.DEBUG, "setUserAgentIfNotExist(" + foxHttpClient.getFoxHttpUserAgent() + ")");
            setHeaderIfNotExist(HeaderTypes.USER_AGENT, foxHttpClient.getFoxHttpUserAgent(), connection);

            connection.setUseCaches(false);
            connection.setDoInput(true);
            foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.DEBUG, "setDoOutput(" + doOutput() + ")");
            connection.setDoOutput(doOutput());
            foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.DEBUG, "setFollowRedirects(" + followRedirect + ")");
            ((HttpURLConnection) connection).setInstanceFollowRedirects(followRedirect);
            HttpURLConnection.setFollowRedirects(followRedirect);
            foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.DEBUG, "setFoxHttpTimeoutStrategy(" + foxHttpClient.getFoxHttpTimeoutStrategy() + ")");
            connection.setConnectTimeout(foxHttpClient.getFoxHttpTimeoutStrategy().getConnectionTimeout());
            connection.setReadTimeout(foxHttpClient.getFoxHttpTimeoutStrategy().getReadTimeout());

            if (isHttps) {
                if (foxHttpClient.getFoxHttpSSLTrustStrategy() != null) {
                    foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.DEBUG, "setSSLSocketFactory(" + foxHttpClient.getFoxHttpSSLTrustStrategy() + ")");
                    ((HttpsURLConnection) connection).setSSLSocketFactory(
                        foxHttpClient.getFoxHttpSSLTrustStrategy().getSSLSocketFactory((HttpsURLConnection) connection, foxHttpClient.getFoxHttpLogger()));
                }
                //TODO: re-enable getFoxHttpHostTrustStrategy()
                /*if (foxHttpClient.getFoxHttpHostTrustStrategy() != null) {
                    foxHttpClient.getFoxHttpLogger().log("setHostnameVerifier(" + foxHttpClient.getFoxHttpHostTrustStrategy() + ")");
                    ((HttpsURLConnection) connection).setHostnameVerifier(foxHttpClient.getFoxHttpHostTrustStrategy());
                }*/
            }

            //Process authorization strategy
            foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.DEBUG, "processAuthorizationStrategy(" + foxHttpClient.getFoxHttpAuthorizationStrategy() + ")");
            processAuthorizationStrategy();

            //Execute interceptor
            foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.DEBUG, "executeRequestHeaderInterceptor()");
            FoxHttpInterceptorExecutor.executeRequestHeaderInterceptor(new FoxHttpRequestHeaderInterceptorContext(connection, this, foxHttpClient));

            //Send request
            if (doOutput()) {
                //Add Content-Type header if not exist
                foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.DEBUG, "setContentTypeIfNotExist(" + requestBody.getOutputContentType().toString() + ")");
                setHeaderIfNotExist(HeaderTypes.CONTENT_TYPE, requestBody.getOutputContentType().toString(), connection);
                //Set request body
                foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.DEBUG, "setRequestBodyStream(" + getRequestBody() + ")");
                setRequestBodyStream();
            }

            foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.INFO, "sendRequest()");
            connection.connect();

            foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.INFO, "========= Response =========");

            int responseCode = ((HttpURLConnection) connection).getResponseCode();
            foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.INFO, "responseCode(" + responseCode + ")");

            //Execute interceptor
            foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.DEBUG, "executeResponseCodeInterceptor()");
            FoxHttpInterceptorExecutor.executeResponseCodeInterceptor(new FoxHttpResponseCodeInterceptorContext(responseCode, this, foxHttpClient));

            if (!skipResponseBody) {
                InputStream is;
                if (responseCode >= HttpURLConnection.HTTP_OK && responseCode < HttpURLConnection.HTTP_MULT_CHOICE) {
                    //On success response code
                    foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.DEBUG, "getResponseBody(success)");
                    is = connection.getInputStream();
                } else {
                    //On error response code
                    foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.DEBUG, "getResponseBody(error)");
                    is = ((HttpURLConnection) connection).getErrorStream();
                }

                foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.INFO, "createFoxHttpResponse()");
                foxHttpResponse = new FoxHttpResponse(is, this, responseCode, foxHttpClient);
            } else {
                foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.INFO, "createFoxHttpResponse()");
                foxHttpResponse = new FoxHttpResponse(null, this, responseCode, foxHttpClient);
                foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.INFO, "No body return because skipResponseBody is active!");
            }

            //Process response headers
            foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.DEBUG, "processResponseHeader()");
            processResponseHeader();

            //Execute interceptor
            foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.DEBUG, "executeResponseInterceptor()");
            FoxHttpInterceptorExecutor.executeResponseInterceptor(new FoxHttpResponseInterceptorContext(responseCode, foxHttpResponse, this, foxHttpClient));

            return foxHttpResponse;
        } catch (FoxHttpException e) {
            throw e;
        } catch (Exception e) {
            throw new FoxHttpRequestException(e);
        } finally {

            if (connection != null) {
                ((HttpURLConnection) connection).disconnect();
            }
        }
    }

    private void checkPlaceholders() throws FoxHttpRequestException {
        Pattern pattern = Pattern.compile(foxHttpPlaceholderStrategy.getPlaceholderMatchRegex());
        Matcher matcher = pattern.matcher(getUrl().toString());
        if (matcher.find()) {
            throw new FoxHttpRequestException("The url dose still contain placeholders after finishing processing all defined placeholders.\n-> " + getUrl().toString());
        }
    }

    private void prepareHeader() {
        for (HeaderEntry headerField : getRequestHeader()) {
            connection.addRequestProperty(headerField.getName(), headerField.getValue());
        }
    }

    private void setRequestBodyStream() throws FoxHttpException {
        requestBody.setBody(new FoxHttpRequestBodyContext(connection, this, foxHttpClient));
    }

    private void prepareQuery() throws FoxHttpRequestException, MalformedURLException {
        if (getRequestQuery() != null && getRequestQuery().hasQueryEntries()) {
            String query = getRequestQuery().getQueryString();
            url = new URL(getUrl().toString() + query);
        }
    }

    private void setHeaderIfNotExist(HeaderTypes type, String value, URLConnection connection) {
        if (connection.getRequestProperty(type.toString()) == null) {
            connection.setRequestProperty(type.toString(), value);
        }
    }

    private void processAuthorizationStrategy() throws FoxHttpRequestException {
        List<FoxHttpAuthorization> foxHttpAuthorizations = foxHttpClient.getFoxHttpAuthorizationStrategy()
                                                                        .getAuthorization(connection, authScope, foxHttpClient, foxHttpPlaceholderStrategy);
        FoxHttpAuthorizationContext authorizationContext = new FoxHttpAuthorizationContext(connection, this, foxHttpClient);
        for (FoxHttpAuthorization foxHttpAuthorization : foxHttpAuthorizations) {
            foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.DEBUG, "-> doAuthorization(" + foxHttpAuthorization + ")");
            foxHttpAuthorization.doAuthorization(authorizationContext, authScope);
        }
    }

    private void processResponseHeader() {
        FoxHttpHeader responseHeaders = new FoxHttpHeader();
        Map<String, List<String>> map = connection.getHeaderFields();

        map.entrySet().stream().filter(entry -> entry.getKey() != null).forEach(entry -> {
            responseHeaders.addHeader(entry.getKey(), entry.getValue().get(0));
            foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.DEBUG, "-> ResponseHeader(" + entry.getKey() + ":" + entry.getValue().get(0) + ")");
        });
        foxHttpResponse.setResponseHeaders(responseHeaders);
    }

    private boolean doOutput() {
        return requestBody != null && requestBody.hasBody();
    }

    private void verifyRequest() throws FoxHttpException {
        //Check for body in GET and DELETE
        if ((requestType == RequestType.GET || requestType == RequestType.DELETE) && (requestBody != null && requestBody.hasBody())) {
            throw new FoxHttpRequestException("Request type '" + requestType + "' does not allow a request body!");
        }

        //Check for empty URL
        if (getUrl() == null) {
            throw new FoxHttpRequestException("URL of the request ist not defined");
        }

        //Check for FoxHttpClient
        if (foxHttpClient == null) {
            throw new FoxHttpRequestException("FoxHttpClient of the request ist not defined");
        }
        foxHttpClient.getFoxHttpLogger().log(FoxHttpLoggerLevel.DEBUG, "verifyRequest()");
    }
}
