package ch.viascom.groundwork.foxhttp.ssl;

import ch.viascom.groundwork.foxhttp.log.FoxHttpLogger;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * @author patrick.boesch@viascom.ch
 */
public class OnwSSLCertificateTrustStrategy implements FoxHttpSSLTrustStrategy {

    private InputStream keyStoreInputStream;
    private String jksPassword;
    private String keyPassword;

    public OnwSSLCertificateTrustStrategy(InputStream keyStoreInputStream, String jksPassword, String keyPassword) {
        this.keyStoreInputStream = keyStoreInputStream;
        this.jksPassword = jksPassword;
        this.keyPassword = keyPassword;
    }

    @Override
    public SSLSocketFactory getSSLSocketFactory(HttpsURLConnection httpsURLConnection, FoxHttpLogger logger) {
        final char[] jksPasswordCharArray = jksPassword.toCharArray();
        final char[] keyPasswordCharArray = keyPassword.toCharArray();
        try {
            /* Get the JKS contents */
            final KeyStore keyStore = KeyStore.getInstance("JKS");
            try (final InputStream is = keyStoreInputStream) {
                keyStore.load(is, jksPasswordCharArray);
            }
            final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, keyPasswordCharArray);
            final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            /*
             * Creates a socket factory for HttpsURLConnection using JKS
             * contents
             */
            logger.log("createSocketFactoryUsingJKS(" + kmf.getProvider().getName() + ")");
            final SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new java.security.SecureRandom());
            return sc.getSocketFactory();

        } catch (final GeneralSecurityException | IOException exc) {
            throw new RuntimeException(exc);
        }
    }
}
