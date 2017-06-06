package ch.viascom.groundwork.foxhttp.authorization;

import ch.viascom.groundwork.foxhttp.FoxHttpClient;
import ch.viascom.groundwork.foxhttp.placeholder.FoxHttpPlaceholderStrategy;
import ch.viascom.groundwork.foxhttp.util.RegexUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.net.URLConnection;
import java.util.*;

/**
 * Default AuthorizationStrategy for FoxHttp
 * <p>
 * Stores FoxHttpAuthorization with a FoxHttpAuthorizationScope as key.
 *
 * @author patrick.boesch@viascom.ch
 */
@ToString
public class DefaultAuthorizationStrategy implements FoxHttpAuthorizationStrategy {

    /**
     * AuthorizationStrategy store
     */
    @Getter
    @Setter
    private HashMap<String, HashMap<String, FoxHttpAuthorization>> foxHttpAuthorizations = new HashMap<>();

    /**
     * Add a new FoxHttpAuthorization to the AuthorizationStrategy
     *
     * @param scope         scope in which the authorization is used
     * @param authorization authorization itself
     */
    @Override
    public void addAuthorization(FoxHttpAuthorizationScope scope, FoxHttpAuthorization authorization) {
        addAuthorization(scope, authorization, String.valueOf(UUID.randomUUID()));
    }


    public void addAuthorization(FoxHttpAuthorizationScope scope, FoxHttpAuthorization authorization, String key) {
        if (foxHttpAuthorizations.containsKey(scope.toString())) {
            foxHttpAuthorizations.get(scope.toString()).put(key, authorization);
        } else {
            HashMap<String, FoxHttpAuthorization> foxHttpAuthorizationsMap = new HashMap<>();
            foxHttpAuthorizationsMap.put(key, authorization);
            foxHttpAuthorizations.put(scope.toString(), foxHttpAuthorizationsMap);
        }
    }

    /**
     * Add a new FoxHttpAuthorization to the AuthorizationStrategy
     *
     * @param scopes        scopes in which the authorization is used
     * @param authorization authorization itself
     */
    @Override
    public void addAuthorization(List<FoxHttpAuthorizationScope> scopes, FoxHttpAuthorization authorization) {
        addAuthorization(scopes, authorization, String.valueOf(UUID.randomUUID()));
    }

    public void addAuthorization(List<FoxHttpAuthorizationScope> scopes, FoxHttpAuthorization authorization, String key) {
        scopes.forEach(scope -> addAuthorization(scope, authorization, key));
    }


    /**
     * Returns a list of matching FoxHttpAuthorizations based on the given FoxHttpAuthorizationScope
     *
     * @param connection  connection of the request
     * @param searchScope looking for scope
     *
     * @return
     */
    @Override
    public List<FoxHttpAuthorization> getAuthorization(URLConnection connection, FoxHttpAuthorizationScope searchScope, FoxHttpClient foxHttpClient, FoxHttpPlaceholderStrategy foxHttpPlaceholderStrategy) {
        ArrayList<FoxHttpAuthorization> foxHttpAuthorizationList = new ArrayList<>();

        foxHttpAuthorizations.entrySet().stream()
                .filter(entry -> RegexUtil.doesURLMatch(searchScope.toString(), foxHttpPlaceholderStrategy.processPlaceholders(entry.getKey(), foxHttpClient)))
                .forEach(entry -> foxHttpAuthorizationList.addAll(entry.getValue().values()));

        if (foxHttpAuthorizations.containsKey(FoxHttpAuthorizationScope.ANY.toString()) && (foxHttpAuthorizationList.isEmpty())) {
            foxHttpAuthorizationList.addAll(foxHttpAuthorizations.get(FoxHttpAuthorizationScope.ANY.toString()).values());
        }

        return foxHttpAuthorizationList;
    }


    /**
     * Remove a defined FoxHttpAuthorization from the AuthorizationStrategy
     *
     * @param scope scope in which the authorization is used
     * @param key   key of the authorization
     */
    @Override
    public void removeAuthorizationByKey(FoxHttpAuthorizationScope scope, String key) {
        HashMap<String, FoxHttpAuthorization> clearedMap = new HashMap<>();
        foxHttpAuthorizations.get(scope.toString()).entrySet()
                .stream()
                .filter(entry -> !entry.getKey().equals(key))
                .forEach(entry -> clearedMap.put(entry.getKey(), entry.getValue()));

        foxHttpAuthorizations.get(scope.toString()).clear();
        foxHttpAuthorizations.get(scope.toString()).putAll(clearedMap);
    }

    /**
     * Remove a defined FoxHttpAuthorization from the AuthorizationStrategy
     *
     * @param scope scope in which the authorization is used
     * @param clazz class of the authorization
     */
    @Override
    public void removeAuthorizationByClass(FoxHttpAuthorizationScope scope, Class<? extends FoxHttpAuthorization> clazz) {
        HashMap<String, FoxHttpAuthorization> clearedMap = new HashMap<>();
        foxHttpAuthorizations.get(scope.toString()).entrySet()
                .stream()
                .filter(entry -> !entry.getValue().getClass().isAssignableFrom(clazz))
                .forEach(entry -> clearedMap.put(entry.getKey(), entry.getValue()));

        foxHttpAuthorizations.get(scope.toString()).clear();
        foxHttpAuthorizations.get(scope.toString()).putAll(clearedMap);
    }

    @Override
    public void replaceAuthorization(FoxHttpAuthorizationScope scope, FoxHttpAuthorization newAuthorization, String key) {
        removeAuthorizationByKey(scope, key);
        foxHttpAuthorizations.get(scope.toString()).put(key, newAuthorization);
    }

    @Override
    public FoxHttpAuthorization getAuthorizationByKey(FoxHttpAuthorizationScope scope, String key) {
        return foxHttpAuthorizations.get(scope.toString()).get(key);
    }

    @Override
    public ArrayList<FoxHttpAuthorization> getAuthorizationByClass(FoxHttpAuthorizationScope scope, Class<? extends FoxHttpAuthorization> clazz) {
        ArrayList<FoxHttpAuthorization> authorizationList = new ArrayList<>();
        foxHttpAuthorizations.get(scope.toString()).entrySet()
                .stream()
                .filter((Map.Entry<String, FoxHttpAuthorization> authorization) -> authorization.getValue().getClass().isAssignableFrom(clazz))
                .forEach(entry -> authorizationList.add(entry.getValue()));
        return authorizationList;
    }

    @Override
    public HashMap<String, FoxHttpAuthorization> getAllAuthorizationsFromScope(FoxHttpAuthorizationScope scope) {
        return foxHttpAuthorizations.get(scope.toString());
    }

    @Override
    public ArrayList<FoxHttpAuthorization> getAllAuthorizationsFromScopeAsArray(FoxHttpAuthorizationScope scope) {
        ArrayList<FoxHttpAuthorization> innerAuthorizationList = new ArrayList<>();

        foxHttpAuthorizations.get(scope.toString()).forEach((key, value) -> innerAuthorizationList.add(value));
        return innerAuthorizationList;
    }

    @Override
    public boolean doesTypeExist(FoxHttpAuthorizationScope scope) {
        return foxHttpAuthorizations.containsKey(scope.toString());
    }
}