package ch.viascom.groundwork.foxhttp.interfaces;

import ch.viascom.groundwork.foxhttp.FoxHttpRequest;
import ch.viascom.groundwork.foxhttp.FoxHttpResponse;
import ch.viascom.groundwork.foxhttp.annotation.types.*;
import ch.viascom.groundwork.foxhttp.body.request.RequestStringBody;
import ch.viascom.groundwork.foxhttp.exception.FoxHttpException;
import ch.viascom.groundwork.foxhttp.header.HeaderEntry;
import ch.viascom.groundwork.foxhttp.models.*;
import ch.viascom.groundwork.foxhttp.util.NamedInputStream;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author patrick.boesch@viascom.ch
 */
@Path("{host}")
public interface FoxHttpInterfaceTest {

    @GET("get")
    GetResponse get(@Query(value = "key", allowOptional = true) String key);

    @GET(value = "{host}get", completePath = true)
    @Header(name = "foo", value = "bar")
    GetResponse bigGet(@QueryMap HashMap<String, String> queryMap, @HeaderField("Product") String product);

    @GET("get")
    @Header(name = "foo", value = "bar")
    GetResponse objectGet(@QueryObject QueryObjectModel queryObjectModel) throws FoxHttpException;

    @GET("get")
    @Header(name = "foo", value = "bar")
    GetResponse objectOptionalGet(@QueryObject(allowOptional = true) QueryObjectModel queryObjectModel);

    @GET("get")
    @Header(name = "foo", value = "bar")
    GetResponse objectEmptyOptionalGet(@QueryObject(recursiveOptional = true) QueryEmptyObjectModel queryEmptyObjectModel);

    @GET("{path}")
    @Header(name = "foo", value = "bar")
    FoxHttpRequest getRequest(@Path("path") String path);

    @POST("post")
    PostResponse postBody(@Body RequestStringBody stringBody, @HeaderFieldMap ArrayList<HeaderEntry> headerFields);

    @POST("post")
    PostResponse postString(@Body String body);

    @POST("post")
    PostResponse postObject(@Body User body);

    @POST("post")
    @FormUrlEncodedBody
    PostResponse postForm(@Field("username") String username, @Field("password") String password, @HeaderFieldMap HashMap<String, String> headerFields);

    @POST("post")
    @FormUrlEncodedBody
    PostResponse postFormMap(@FieldMap HashMap<String, String> fromData);

    @POST("post")
    @MultipartBody
    FoxHttpResponse postMulti(@Part("test") String text, @Part("stream") NamedInputStream namedInputStream);

    @POST("post")
    @MultipartBody
    FoxHttpResponse postMultiMap(@PartMap(isStreamMap = true) HashMap<String, NamedInputStream> namedInputStreamMap);
}
