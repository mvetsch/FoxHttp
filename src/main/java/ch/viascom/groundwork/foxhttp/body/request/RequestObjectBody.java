package ch.viascom.groundwork.foxhttp.body.request;

import ch.viascom.groundwork.foxhttp.annotation.types.SerializeContentType;
import ch.viascom.groundwork.foxhttp.exception.FoxHttpException;
import ch.viascom.groundwork.foxhttp.exception.FoxHttpRequestException;
import ch.viascom.groundwork.foxhttp.type.ContentType;
import java.io.Serializable;
import java.nio.charset.Charset;
import lombok.ToString;

/**
 * RequestObjectBody for FoxHttp <p> Stores an object for a request body. To use this you have to set a RequestParser.
 *
 * @author patrick.boesch@viascom.ch
 */
@ToString
public class RequestObjectBody extends FoxHttpRequestBody {

    private Serializable content;

    /**
     * Create a new RequestObjectBody
     *
     * @param content serializable object
     */
    public RequestObjectBody(Serializable content) {
        this(content, ContentType.APPLICATION_JSON);
    }

    /**
     * Create a new RequestObjectBody
     *
     * @param content serializable object
     * @param contentType type of the content
     */
    public RequestObjectBody(Serializable content, ContentType contentType) {
        this.content = content;
        this.outputContentType = contentType;
    }

    /**
     * Set the body of the request
     *
     * @param context context of the request
     * @throws FoxHttpRequestException can throw different exception based on input streams and interceptors
     */
    @Override
    public void setBody(FoxHttpRequestBodyContext context) throws FoxHttpException {
        if (context.getClient().getFoxHttpRequestParser() == null) {
            throw new FoxHttpRequestException("RequestObjectBody needs a FoxHttpRequestParser to serialize the body");
        }

        //Check Model for SerializeContentType
        if (content.getClass().isAnnotationPresent(SerializeContentType.class)) {
            Charset charset = Charset.forName(content.getClass().getAnnotation(SerializeContentType.class).charset());
            String mimeType = content.getClass().getAnnotation(SerializeContentType.class).mimeType();
            this.outputContentType = ContentType.create(mimeType, charset);
        }

        String json = context.getClient().getFoxHttpRequestParser().objectToSerialized(content, this.outputContentType);

        writeBody(context, json);
    }

    /**
     * Checks if the body contains data
     *
     * @return true if data is stored in the body
     */
    @Override
    public boolean hasBody() {
        return content != null;
    }

    /**
     * Get the ContentType of this body
     *
     * @return ContentType of this body
     */
    @Override
    public ContentType getOutputContentType() {
        return outputContentType;
    }
}
