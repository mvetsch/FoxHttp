package ch.viascom.groundwork.foxhttp;

import ch.viascom.groundwork.foxhttp.authorization.*;
import ch.viascom.groundwork.foxhttp.body.request.RequestStringBody;
import ch.viascom.groundwork.foxhttp.builder.FoxHttpClientBuilder;
import ch.viascom.groundwork.foxhttp.builder.FoxHttpRequestBuilder;
import ch.viascom.groundwork.foxhttp.cookie.DefaultCookieStore;
import ch.viascom.groundwork.foxhttp.cookie.FoxHttpCookieStore;
import ch.viascom.groundwork.foxhttp.exception.FoxHttpException;
import ch.viascom.groundwork.foxhttp.header.HeaderEntry;
import ch.viascom.groundwork.foxhttp.interceptor.FoxHttpInterceptor;
import ch.viascom.groundwork.foxhttp.interceptor.FoxHttpInterceptorType;
import ch.viascom.groundwork.foxhttp.interceptor.request.FoxHttpRequestBodyInterceptor;
import ch.viascom.groundwork.foxhttp.interceptor.request.context.FoxHttpRequestBodyInterceptorContext;
import ch.viascom.groundwork.foxhttp.log.DefaultFoxHttpLogger;
import ch.viascom.groundwork.foxhttp.log.FoxHttpLogger;
import ch.viascom.groundwork.foxhttp.parser.FoxHttpParser;
import ch.viascom.groundwork.foxhttp.parser.GsonParser;
import ch.viascom.groundwork.foxhttp.placeholder.FoxHttpPlaceholderStrategy;
import ch.viascom.groundwork.foxhttp.proxy.FoxHttpProxyStrategy;
import ch.viascom.groundwork.foxhttp.ssl.*;
import ch.viascom.groundwork.foxhttp.timeout.DefaultTimeoutStrategy;
import ch.viascom.groundwork.foxhttp.timeout.FoxHttpTimeoutStrategy;
import ch.viascom.groundwork.foxhttp.type.ContentType;
import ch.viascom.groundwork.foxhttp.type.HeaderTypes;
import ch.viascom.groundwork.foxhttp.type.RequestType;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.EnumMap;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * @author patrick.boesch@viascom.ch
 */
public class FoxHttpBuilderTest {

    @Test
    public void clientBuilderTest() throws Exception {
        FoxHttpClientBuilder foxHttpClientBuilder = new FoxHttpClientBuilder();

        FoxHttpHostTrustStrategy foxHttpHostTrustStrategy = new DefaultHostTrustStrategy();
        FoxHttpAuthorizationStrategy foxHttpAuthorizationStrategy = new DefaultAuthorizationStrategy();
        FoxHttpParser foxHttpParser = new GsonParser();
        FoxHttpLogger foxHttpLogger = new DefaultFoxHttpLogger(false);
        FoxHttpCookieStore foxHttpCookieStore = new DefaultCookieStore();
        FoxHttpProxyStrategy foxHttpProxyStrategy = new FoxHttpProxyStrategy() {
            @Override
            public Proxy getProxy(URL url) {
                return null;
            }

            @Override
            public String getProxyAuthorization(URL url) {
                return null;
            }

            @Override
            public boolean hasProxyAuthorization(URL url) {
                return false;
            }
        };
        FoxHttpSSLTrustStrategy foxHttpSSLTrustStrategy = new DefaultSSLTrustStrategy();
        FoxHttpTimeoutStrategy foxHttpTimeoutStrategy = new DefaultTimeoutStrategy();
        FoxHttpAuthorization authorization = new BasicAuthAuthorization("name", "passwd");
        FoxHttpPlaceholderStrategy placeholderStrategy = new FoxHttpPlaceholderStrategy() {
            @Override
            public String getPlaceholderEscapeCharStart() {
                return "[";
            }

            @Override
            public void setPlaceholderEscapeCharStart(String placeholderEscapeChar) {

            }

            @Override
            public String getPlaceholderEscapeCharEnd() {
                return null;
            }

            @Override
            public void setPlaceholderEscapeCharEnd(String placeholderEscapeChar) {

            }

            @Override
            public String getPlaceholderMatchRegex() {
                return null;
            }

            @Override
            public void addPlaceholder(String placeholder, String value) {

            }

            @Override
            public Map<String, String> getPlaceholderMap() {
                return null;
            }

            @Override
            public String processPlaceholders(String processedURL, FoxHttpClient foxHttpClient) {
                return processedURL;
            }
        };

        foxHttpClientBuilder.setFoxHttpHostTrustStrategy(foxHttpHostTrustStrategy);
        foxHttpClientBuilder.setFoxHttpResponseParser(foxHttpParser);
        foxHttpClientBuilder.setFoxHttpLogger(foxHttpLogger);
        foxHttpClientBuilder.setFoxHttpRequestParser(foxHttpParser);
        foxHttpClientBuilder.setFoxHttpAuthorizationStrategy(foxHttpAuthorizationStrategy);
        foxHttpClientBuilder.setFoxHttpCookieStore(foxHttpCookieStore);
        foxHttpClientBuilder.setFoxHttpInterceptors(new EnumMap<>(FoxHttpInterceptorType.class));
        foxHttpClientBuilder.setFoxHttpProxyStrategy(foxHttpProxyStrategy);
        foxHttpClientBuilder.setFoxHttpSSLTrustStrategy(foxHttpSSLTrustStrategy);
        foxHttpClientBuilder.setFoxHttpTimeouts(0, 0);
        foxHttpClientBuilder.setFoxHttpUserAgent("FoxHttp v1.0");
        foxHttpClientBuilder.addFoxHttpAuthorization(FoxHttpAuthorizationScope.ANY, authorization);
        foxHttpClientBuilder.activateGZipResponseInterceptor(100);
        foxHttpClientBuilder.addFoxHttpInterceptor(FoxHttpInterceptorType.REQUEST_BODY, new FoxHttpRequestBodyInterceptor() {
            @Override
            public void onIntercept(FoxHttpRequestBodyInterceptorContext context) throws FoxHttpException {
                System.out.println(context);
            }

            @Override
            public int getWeight() {
                return 0;
            }
        });
        foxHttpClientBuilder.setFoxHttpPlaceholderStrategy(placeholderStrategy);

        FoxHttpClient foxHttpClient = foxHttpClientBuilder.build();

        assertThat(foxHttpClient.getFoxHttpAuthorizationStrategy()).isEqualTo(foxHttpAuthorizationStrategy);
        assertThat(foxHttpClient.getFoxHttpHostTrustStrategy()).isEqualTo(foxHttpHostTrustStrategy);
        assertThat(foxHttpClient.getFoxHttpRequestParser()).isEqualTo(foxHttpParser);
        assertThat(foxHttpClient.getFoxHttpResponseParser()).isEqualTo(foxHttpParser);
        assertThat(foxHttpClient.getFoxHttpLogger()).isEqualTo(foxHttpLogger);
        assertThat(foxHttpClient.getFoxHttpCookieStore()).isEqualTo(foxHttpCookieStore);
        assertThat(foxHttpClient.getFoxHttpProxyStrategy()).isEqualTo(foxHttpProxyStrategy);
        assertThat(foxHttpClient.getFoxHttpSSLTrustStrategy()).isEqualTo(foxHttpSSLTrustStrategy);
        assertThat(foxHttpClient.getFoxHttpUserAgent()).isEqualTo("FoxHttp v1.0");
        assertThat(foxHttpClient.getFoxHttpTimeoutStrategy().getConnectionTimeout()).isEqualTo(0);
        assertThat(foxHttpClient.getFoxHttpTimeoutStrategy().getReadTimeout()).isEqualTo(0);
        assertThat(foxHttpClient.getFoxHttpAuthorizationStrategy().getAuthorization(null, FoxHttpAuthorizationScope.ANY, foxHttpClient, foxHttpClient.getFoxHttpPlaceholderStrategy()).get(0)).isEqualTo(authorization);
        assertThat(foxHttpClient.getFoxHttpInterceptorStrategy().getAllInterceptorsFromTypeAsArray(FoxHttpInterceptorType.RESPONSE, false)).isNotEmpty();
        assertThat(foxHttpClient.getFoxHttpInterceptorStrategy().getAllInterceptorsFromTypeAsArray(FoxHttpInterceptorType.RESPONSE, false).get(0).getWeight()).isEqualTo(100);
        assertThat(foxHttpClient.getFoxHttpPlaceholderStrategy().getPlaceholderEscapeCharStart()).isEqualTo("[");

        foxHttpClientBuilder.activateFoxHttpLogger(true);
        foxHttpClientBuilder.setFoxHttpTimeoutStrategy(foxHttpTimeoutStrategy);
        foxHttpClientBuilder.setFoxHttpInterceptors(new EnumMap<>(FoxHttpInterceptorType.class));
        foxHttpClientBuilder.activateGzipResponseInterceptor(true, 100);
        foxHttpClientBuilder.activateXStreamParser();

        FoxHttpClient foxHttpClient2 = foxHttpClientBuilder.build();

        assertThat(foxHttpClient2.getFoxHttpTimeoutStrategy().getConnectionTimeout()).isEqualTo(0);
        assertThat(foxHttpClient2.getFoxHttpInterceptorStrategy().getAllInterceptorsFromTypeAsArray(FoxHttpInterceptorType.RESPONSE, false)).isNotEmpty();
        assertThat(foxHttpClient2.getFoxHttpInterceptorStrategy().getAllInterceptorsFromTypeAsArray(FoxHttpInterceptorType.RESPONSE, false).get(0).getWeight()).isEqualTo(100);
        assertThat(foxHttpClient2.getFoxHttpRequestParser()).isNotNull();
        assertThat(foxHttpClient2.getFoxHttpResponseParser()).isNotNull();

        //Reset settings for next test
        foxHttpClientBuilder.setFoxHttpResponseParser(null);
        foxHttpClientBuilder.setFoxHttpRequestParser(null);

        foxHttpClientBuilder.setFoxHttpInterceptors(new EnumMap<>(FoxHttpInterceptorType.class));
        foxHttpClientBuilder.activateDeflateResponseInterceptor(true);
        foxHttpClientBuilder.activateGZipResponseInterceptor();
        foxHttpClientBuilder.activateGsonParser();
        foxHttpClientBuilder.setFoxHttpLogger(foxHttpLogger, true);

        FoxHttpClient foxHttpClient3 = foxHttpClientBuilder.build();
        assertThat(foxHttpClient3.getFoxHttpTimeoutStrategy().getConnectionTimeout()).isEqualTo(0);
        assertThat(foxHttpClient3.getFoxHttpInterceptorStrategy().getAllInterceptorsFromTypeAsArray(FoxHttpInterceptorType.RESPONSE, false)).isNotEmpty();
        assertThat(foxHttpClient3.getFoxHttpInterceptorStrategy().getAllInterceptorsFromTypeAsArray(FoxHttpInterceptorType.RESPONSE, false).size()).isEqualTo(2);
        assertThat(foxHttpClient3.getFoxHttpRequestParser()).isNotNull();
        assertThat(foxHttpClient3.getFoxHttpResponseParser()).isNotNull();
        assertThat(foxHttpClient3.getFoxHttpLogger().isLoggingEnabled()).isTrue();
    }

    @Test
    public void clientInterceptorBuilderTest() throws Exception {
        FoxHttpClientBuilder foxHttpClientBuilder = new FoxHttpClientBuilder(new GsonParser(), new GsonParser());

        FoxHttpInterceptor foxHttpInterceptor = new FoxHttpRequestBodyInterceptor() {
            @Override
            public void onIntercept(FoxHttpRequestBodyInterceptorContext context) throws FoxHttpException {
                try {
                    context.getRequest().setUrl(new URL("TEST"));
                } catch (MalformedURLException e) {
                    throw new FoxHttpException(e);
                }
            }

            @Override
            public int getWeight() {
                return 0;
            }
        };

        foxHttpClientBuilder.addFoxHttpInterceptor(FoxHttpInterceptorType.REQUEST_BODY, foxHttpInterceptor);

        FoxHttpClient foxHttpClient = foxHttpClientBuilder.build();

        assertThat(foxHttpClient.getFoxHttpInterceptorStrategy().getAllInterceptorsFromTypeAsArray(FoxHttpInterceptorType.REQUEST_BODY, false).get(0)).isEqualTo(foxHttpInterceptor);

    }

    @Test
    public void requestBuilderTest() throws Exception {

        FoxHttpClient foxHttpClient = new FoxHttpClient();

        FoxHttpInterceptor foxHttpInterceptor = new FoxHttpRequestBodyInterceptor() {
            @Override
            public void onIntercept(FoxHttpRequestBodyInterceptorContext context) throws FoxHttpException {

            }

            @Override
            public int getWeight() {
                return 0;
            }
        };

        HeaderEntry headerField = new HeaderEntry("Product", "GroundWork");

        FoxHttpRequestBuilder requestBuilder = new FoxHttpRequestBuilder("http://httpbin.org/{method}");
        requestBuilder.setRequestType(RequestType.POST);
        requestBuilder.setFollowRedirect(true);
        requestBuilder.setFoxHttpClient(foxHttpClient);
        requestBuilder.addRequestHeader("Fox-Header", "true");
        requestBuilder.addRequestHeader(HeaderTypes.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
        requestBuilder.addRequestHeader(headerField);
        requestBuilder.addRequestQueryEntry("name", "FoxHttp");
        requestBuilder.setSkipResponseBody(false);
        requestBuilder.addFoxHttpInterceptor(FoxHttpInterceptorType.REQUEST_BODY, foxHttpInterceptor);
        requestBuilder.setRequestBody(new RequestStringBody("Hi!"));
        requestBuilder.addFoxHttpPlaceholderEntry("method", "post");

        FoxHttpRequest foxHttpRequest = requestBuilder.build();

        assertThat(foxHttpRequest.getRequestType()).isEqualTo(RequestType.POST);
        assertThat(foxHttpRequest.getUrl().toString()).isEqualTo("http://httpbin.org/post");
        assertThat(foxHttpRequest.getFoxHttpPlaceholderStrategy().getPlaceholderMap().get("method")).isEqualTo("post");
        assertThat(foxHttpRequest.getFoxHttpClient()).isEqualTo(foxHttpClient);
    }

    @Test
    public void requestConstructorBuilderTest() throws Exception {

        FoxHttpRequestBuilder requestBuilder = new FoxHttpRequestBuilder("http://httpbin.org");
        FoxHttpRequest foxHttpRequest = requestBuilder.build();
        assertThat(foxHttpRequest.getFoxHttpClient()).isNotNull();

        FoxHttpRequestBuilder requestBuilder2 = new FoxHttpRequestBuilder("http://httpbin.org/post", RequestType.DELETE);
        FoxHttpRequest foxHttpRequest2 = requestBuilder2.build();
        assertThat(foxHttpRequest2.getFoxHttpClient()).isNotNull();
        assertThat(foxHttpRequest2.getRequestType()).isEqualTo(RequestType.DELETE);
        assertThat(foxHttpRequest2.getUrl().toString()).isEqualTo("http://httpbin.org/post");

        FoxHttpClient foxHttpClient = new FoxHttpClient();
        FoxHttpRequestBuilder requestBuilder3 = new FoxHttpRequestBuilder("http://httpbin.org/put", RequestType.PUT, foxHttpClient);
        FoxHttpRequest foxHttpRequest3 = requestBuilder3.build();
        assertThat(foxHttpRequest3.getFoxHttpClient()).isNotNull();
        assertThat(foxHttpRequest3.getRequestType()).isEqualTo(RequestType.PUT);
        assertThat(foxHttpRequest3.getUrl().toString()).isEqualTo("http://httpbin.org/put");
        assertThat(foxHttpRequest3.getFoxHttpClient()).isEqualTo(foxHttpClient);

        FoxHttpRequestBuilder requestBuilder4 = new FoxHttpRequestBuilder(new URL("http://httpbin.org/post"));
        FoxHttpRequest foxHttpRequest4 = requestBuilder4.build();
        assertThat(foxHttpRequest4.getFoxHttpClient()).isNotNull();
        assertThat(foxHttpRequest4.getRequestType()).isEqualTo(RequestType.GET);
        assertThat(foxHttpRequest4.getUrl().toString()).isEqualTo("http://httpbin.org/post");

        FoxHttpRequestBuilder requestBuilder5 = new FoxHttpRequestBuilder(new URL("http://httpbin.org/post"), RequestType.DELETE);
        FoxHttpRequest foxHttpRequest5 = requestBuilder5.build();
        assertThat(foxHttpRequest5.getFoxHttpClient()).isNotNull();
        assertThat(foxHttpRequest5.getRequestType()).isEqualTo(RequestType.DELETE);
        assertThat(foxHttpRequest5.getUrl().toString()).isEqualTo("http://httpbin.org/post");


        FoxHttpAuthorizationStrategy authorizationStrategy = new DefaultAuthorizationStrategy();
        FoxHttpClientBuilder foxHttpClientBuilder = new FoxHttpClientBuilder(authorizationStrategy);
        FoxHttpClient foxHttpClient2 = foxHttpClientBuilder.build();
        assertThat(foxHttpClient2.getFoxHttpAuthorizationStrategy()).isEqualTo(authorizationStrategy);

        FoxHttpSSLTrustStrategy sslTrustStrategy = new AllowAllSSLCertificateTrustStrategy();
        FoxHttpHostTrustStrategy hostTrustStrategy = new AllHostTrustStrategy();
        FoxHttpClientBuilder foxHttpClientBuilder3 = new FoxHttpClientBuilder(hostTrustStrategy, sslTrustStrategy);
        FoxHttpClient foxHttpClient3 = foxHttpClientBuilder3.build();
        assertThat(foxHttpClient3.getFoxHttpHostTrustStrategy()).isEqualTo(hostTrustStrategy);
        assertThat(foxHttpClient3.getFoxHttpSSLTrustStrategy()).isEqualTo(sslTrustStrategy);
    }

}
