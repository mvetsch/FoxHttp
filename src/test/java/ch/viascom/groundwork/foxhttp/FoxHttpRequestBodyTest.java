package ch.viascom.groundwork.foxhttp;

import ch.viascom.groundwork.foxhttp.body.request.*;
import ch.viascom.groundwork.foxhttp.builder.FoxHttpClientBuilder;
import ch.viascom.groundwork.foxhttp.builder.FoxHttpRequestBuilder;
import ch.viascom.groundwork.foxhttp.exception.FoxHttpRequestException;
import ch.viascom.groundwork.foxhttp.models.PostResponse;
import ch.viascom.groundwork.foxhttp.models.User;
import ch.viascom.groundwork.foxhttp.parser.GenericParser;
import ch.viascom.groundwork.foxhttp.parser.GsonParser;
import ch.viascom.groundwork.foxhttp.type.ContentType;
import ch.viascom.groundwork.foxhttp.type.RequestType;
import ch.viascom.groundwork.serviceresult.ServiceResult;
import ch.viascom.groundwork.serviceresult.ServiceResultStatus;
import ch.viascom.groundwork.serviceresult.util.Metadata;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * @author patrick.boesch@viascom.ch
 */
public class FoxHttpRequestBodyTest {

    private String endpoint = "http://httpbin.org/";


    @Test
    public void postObjectRequest() throws Exception {

        FoxHttpClientBuilder clientBuilder = new FoxHttpClientBuilder();

        FoxHttpRequestBody requestBody = new RequestObjectBody(new User());

        FoxHttpRequestBuilder requestBuilder = new FoxHttpRequestBuilder(endpoint + "post", RequestType.POST, clientBuilder.build());
        requestBuilder.setRequestBody(requestBody);
        FoxHttpResponse foxHttpResponse = requestBuilder.build().execute();

        assertThat(foxHttpResponse.getResponseCode()).isEqualTo(200);
        assertThat(foxHttpResponse.getByteArrayOutputStreamBody().size()).isGreaterThan(0);

        PostResponse postResponse = foxHttpResponse.getParsedBody(PostResponse.class);

        assertThat(postResponse.getData()).isEqualTo(new GenericParser().objectToSerialized(new User(), ContentType.APPLICATION_JSON));
    }

    @Test
    public void postURLEncodedFormRequest() throws Exception {

        FoxHttpClientBuilder clientBuilder = new FoxHttpClientBuilder(new GsonParser(), new GsonParser());

        RequestUrlEncodedFormBody requestBody = new RequestUrlEncodedFormBody();
        requestBody.addFormEntry("username", "Fox");
        requestBody.addFormEntry("password", "GroundWork1234");

        FoxHttpRequestBuilder requestBuilder = new FoxHttpRequestBuilder(endpoint + "post", RequestType.POST, clientBuilder.build());
        requestBuilder.setRequestBody(requestBody);
        FoxHttpResponse foxHttpResponse = requestBuilder.build().execute();


        assertThat(foxHttpResponse.getResponseCode()).isEqualTo(200);
        assertThat(foxHttpResponse.getByteArrayOutputStreamBody().size()).isGreaterThan(0);

        PostResponse postResponse = foxHttpResponse.getParsedBody(PostResponse.class);

        assertThat(postResponse.getForm().get("username")).isEqualTo("Fox");
        assertThat(postResponse.getForm().get("password")).isEqualTo("GroundWork1234");
    }

    @Test
    public void postMultiPartRequest() throws Exception {

        FoxHttpClientBuilder clientBuilder = new FoxHttpClientBuilder(new GsonParser(), new GsonParser());

        RequestMultipartBody requestBody = new RequestMultipartBody(Charset.forName("UTF-8"));
        requestBody.addFormField("filename", "test.data");
        requestBody.addInputStreamPart("file", "file.json", new ByteArrayInputStream("{\"name\":\"FoxHttp\"}".getBytes()), "QUOTED-PRINTABLE", ContentType.DEFAULT_TEXT.getMimeType());

        FoxHttpRequestBuilder requestBuilder = new FoxHttpRequestBuilder(endpoint + "post", RequestType.POST, clientBuilder.build());
        requestBuilder.setRequestBody(requestBody);
        FoxHttpResponse foxHttpResponse = requestBuilder.build().execute();


        assertThat(foxHttpResponse.getResponseCode()).isEqualTo(200);
        assertThat(foxHttpResponse.getByteArrayOutputStreamBody().size()).isGreaterThan(0);

        PostResponse postResponse = foxHttpResponse.getParsedBody(PostResponse.class);

        assertThat(postResponse.getForm().get("filename")).isEqualTo("test.data");
    }

    @Test
    public void postServiceResultRequest() throws Exception {
        FoxHttpClientBuilder clientBuilder = new FoxHttpClientBuilder(new GsonParser());

        RequestServiceResultBody requestBody = new RequestServiceResultBody(new User());
        requestBody.setHash("FAKE-HASH");
        requestBody.addMetadata("isActive", new Metadata<Serializable>("java.lang.Boolean", true));
        requestBody.setType("ch.viascom.app.models.AppUser");
        requestBody.setStatus(ServiceResultStatus.failed);

        FoxHttpRequestBuilder requestBuilder = new FoxHttpRequestBuilder(endpoint + "post", RequestType.POST, clientBuilder.build());
        requestBuilder.setRequestBody(requestBody);
        FoxHttpResponse foxHttpResponse = requestBuilder.build().execute();

        assertThat(foxHttpResponse.getResponseCode()).isEqualTo(200);
        assertThat(foxHttpResponse.getByteArrayOutputStreamBody().size()).isGreaterThan(0);

        PostResponse postResponse = foxHttpResponse.getParsedBody(PostResponse.class);

        assertThat(postResponse.getData()).isEqualTo("{\"status\":\"failed\",\"type\":\"ch.viascom.app.models.AppUser\",\"content\":{\"username\":\"foxhttp@viascom.ch\",\"firstname\":\"Fox\",\"lastname\":\"Http\"},\"hash\":\"FAKE-HASH\",\"destination\":\"\",\"metadata\":{\"isActive\":{\"type\":\"java.lang.Boolean\",\"content\":true}}}");
    }

    @Test
    public void postByteArrayRequest() throws Exception {

        FoxHttpClientBuilder clientBuilder = new FoxHttpClientBuilder(new GsonParser(), new GsonParser());

        InputStream inputStream = new ByteArrayInputStream("{\"name\":\"FoxHttp\"}".getBytes());

        RequestByteArrayBody requestBody = new RequestByteArrayBody(inputStream, ContentType.APPLICATION_JSON);

        FoxHttpRequestBuilder requestBuilder = new FoxHttpRequestBuilder(endpoint + "post", RequestType.POST, clientBuilder.build());
        requestBuilder.setRequestBody(requestBody);
        FoxHttpResponse foxHttpResponse = requestBuilder.build().execute();

        assertThat(foxHttpResponse.getResponseCode()).isEqualTo(200);
        assertThat(foxHttpResponse.getByteArrayOutputStreamBody().size()).isGreaterThan(0);

        String response = foxHttpResponse.getStringBody();
        JsonElement jsonElement = new Gson().fromJson(response, JsonElement.class);

        assertThat(jsonElement.getAsJsonObject().get("data").getAsString()).isEqualTo("{\"name\":\"FoxHttp\"}");
    }

    @Test
    public void postByteArrayOutputRequest() throws Exception {

        FoxHttpClientBuilder clientBuilder = new FoxHttpClientBuilder(new GsonParser(), new GsonParser());

        InputStream inputStream = new ByteArrayInputStream("{\"name\":\"FoxHttp\"}".getBytes());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            outputStream.write(data, 0, nRead);
        }

        outputStream.flush();

        RequestByteArrayBody requestBody = new RequestByteArrayBody(outputStream, ContentType.APPLICATION_JSON);

        FoxHttpRequestBuilder requestBuilder = new FoxHttpRequestBuilder(endpoint + "post", RequestType.POST, clientBuilder.build());
        requestBuilder.setRequestBody(requestBody);
        FoxHttpResponse foxHttpResponse = requestBuilder.build().execute();

        assertThat(foxHttpResponse.getResponseCode()).isEqualTo(200);
        assertThat(foxHttpResponse.getByteArrayOutputStreamBody().size()).isGreaterThan(0);

        String response = foxHttpResponse.getStringBody();
        JsonElement jsonElement = new Gson().fromJson(response, JsonElement.class);

        assertThat(jsonElement.getAsJsonObject().get("data").getAsString()).isEqualTo("{\"name\":\"FoxHttp\"}");
    }

}
