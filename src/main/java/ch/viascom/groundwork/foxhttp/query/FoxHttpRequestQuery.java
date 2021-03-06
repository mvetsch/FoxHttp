package ch.viascom.groundwork.foxhttp.query;

import ch.viascom.groundwork.foxhttp.annotation.types.QueryName;
import ch.viascom.groundwork.foxhttp.exception.FoxHttpException;
import ch.viascom.groundwork.foxhttp.exception.FoxHttpRequestException;
import ch.viascom.groundwork.foxhttp.util.QueryBuilder;
import com.google.gson.annotations.SerializedName;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * FoxHttpRequestQuery stores query entries for a request
 *
 * @author patrick.boesch@viascom.ch
 */
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class FoxHttpRequestQuery {

    private HashMap<String, String> queryMap = new HashMap<>();

    /**
     * Add a new query entry
     *
     * @param name name of the query entry
     * @param value value of the query entry
     */
    public void addQueryEntry(String name, String value) {
        if (value != null) {
            queryMap.put(name, value);
        }
    }

    /**
     * Add a map of new query entries
     *
     * @param queryMap map of query entries
     */
    public void addQueryMap(HashMap<String, String> queryMap) {
        this.queryMap.putAll(queryMap);
    }

    /**
     * Remove a query entry
     *
     * @param name name of the query entry
     */
    public void removeQueryEntry(String name) {
        queryMap.remove(name);
    }

    /**
     * Check if there are query entries stored
     *
     * @return true if more than 0 entries stored
     */
    public boolean hasQueryEntries() {
        return queryMap.size() > 0;
    }

    /**
     * Get all stored queries as string
     *
     * @return string with all query entries
     * @throws FoxHttpRequestException can throw an exception if the "UTF-8" encoding is not found
     */
    public String getQueryString() throws FoxHttpRequestException {
        return "?" + QueryBuilder.buildQuery(queryMap);
    }

    /**
     * Parse an object as query map
     *
     * @param params list of attribute names which will be included
     * @param o object with the attributes
     * @throws FoxHttpRequestException can throw an exception if a field does not exist
     */
    public void parseObjectAsQueryMap(List<String> params, Object o, boolean parseSerializedName, boolean allowOptional, boolean recursiveOptional) throws FoxHttpRequestException {

        if (!allowOptional && o == null) {
            throw new FoxHttpRequestException("The query object parameter is not optional and can't be null because of this.");
        }

        if (o != null) {
            Class clazz = o.getClass();

            //Get all fields including all super classes
            HashMap<String, Field> fields = new HashMap<>();
            fields = getAllFields(fields, clazz);

            HashMap<String, String> paramMap = new HashMap<>();
            ArrayList<String> paramNames = new ArrayList<>(params);

            try {
                if (!paramNames.get(0).isEmpty()) {
                    for (String param : paramNames) {
                        Field field = fields.get(param);
                        paramMap = processQueryField(o, parseSerializedName, recursiveOptional, clazz, paramMap, field);
                    }
                } else {
                    for (Entry<String, Field> fieldSet : fields.entrySet()) {
                        paramMap = processQueryField(o, parseSerializedName, recursiveOptional, clazz, paramMap, fieldSet.getValue());
                    }
                }
            } catch (FoxHttpException e) {
                throw e;
            } catch (Exception e) {
                throw new FoxHttpRequestException(e);
            }
            queryMap = paramMap;
        }
    }

    private HashMap<String, Field> getAllFields(HashMap<String, Field> fields, Class<?> type) {

        Arrays.asList(type.getDeclaredFields()).forEach(field -> fields.put(field.getName(), field));

        if (type.getSuperclass() != null) {
            getAllFields(fields, type.getSuperclass());
        }

        return fields;
    }

    private HashMap<String, String> processQueryField(Object o, boolean parseSerializedName, boolean recursiveOptional, Class clazz, HashMap<String, String> paramMap, Field field)
        throws IllegalAccessException, FoxHttpRequestException {
        field.setAccessible(true);

        //Check optional
        boolean isOptional = recursiveOptional;
        if (field.getAnnotationsByType(QueryName.class).length > 0) {
            isOptional = field.getAnnotationsByType(QueryName.class)[0].allowOptional();
        }

        if (field.get(o) == null && !isOptional) {
            throw new FoxHttpRequestException(
                "The query parameter attribute " + field.getName() + " in " + clazz.getSimpleName() + " is not optional and can't be null because of this.");
        }
        if (field.get(o) != null) {

            //Load query parameter name
            String paramName = field.getName();
            if (parseSerializedName && field.getAnnotationsByType(SerializedName.class).length != 0) {
                paramName = field.getAnnotationsByType(SerializedName.class)[0].value();
            }

            if (field.getAnnotationsByType(QueryName.class).length != 0 && !field.getAnnotationsByType(QueryName.class)[0].value().isEmpty()) {
                paramName = field.getAnnotationsByType(QueryName.class)[0].value();
            }

            //Load query value
            String value = String.valueOf(field.get(o));

            paramMap.put(paramName, value);
        }

        return paramMap;
    }

}
