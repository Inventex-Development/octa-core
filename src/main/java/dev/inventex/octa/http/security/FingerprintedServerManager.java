package dev.inventex.octa.http.security;

import dev.inventex.octa.util.Hash;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.security.AlgorithmConstraints;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;

/**
 * Represents an extensions to the X509TrustManager interface to support SSL/TLS connection sensitive trust management.
 * To prevent man-in-the-middle attacks, hostname checks can be done to verify that the hostname in an end-entity certificate matches the targeted hostname. TLS does not require such checks, but some protocols over TLS (such as HTTPS) do. In earlier versions of the JDK, the certificate chain checks were done at the SSL/TLS layer, and the hostname verification checks were done at the layer over TLS. This class allows for the checking to be done during a single call to this class.
 * <br>
 * RFC 2830 defines the server identification specification for the "LDAPS" algorithm. RFC 2818 defines both the server identification and the client identification specification for the "HTTPS" algorithm.
 * <br>
 * @see javax.net.ssl.X509TrustManager
 * @see javax.net.ssl.HostnameVerifier
 */
public class FingerprintedServerManager extends X509ExtendedTrustManager {
    /**
     * The fingerprint of the HTTP server's SSL certificate.
     */
    private final String fingerprint;

    /**
     * Initialize the fingerprinted server manager.
     * @param fingerprint the fingerprint of the server
     */
    public FingerprintedServerManager(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    /**
     * Given the partial or complete certificate chain provided by the peer, build and validate the certificate path based on the authentication type and ssl parameters.
     * The authentication type is determined by the actual certificate used. For instance, if RSAPublicKey is used, the authType should be "RSA". Checking is case-sensitive.
     * <br>
     * If the socket parameter is an instance of SSLSocket, and the endpoint identification algorithm of the SSLParameters is non-empty, to prevent man-in-the-middle attacks, the address that the socket connected to should be checked against the peer's identity presented in the end-entity X509 certificate, as specified in the endpoint identification algorithm.
     * <br>
     * If the socket parameter is an instance of SSLSocket, and the algorithm constraints of the SSLParameters is non-null, for every certificate in the certification path, fields such as subject public key, the signature algorithm, key usage, extended key usage, etc. need to conform to the algorithm constraints in place on this socket.
     *
     * @param chain - the peer certificate chain
     * @param authType - the key exchange algorithm used
     * @param socket - the socket used for this connection. This parameter can be null, which indicates that implementations need not check the ssl parameters
     *
     * @throws IllegalArgumentException - if null or zero-length array is passed in for the chain parameter or if null or zero-length string is passed in for the authType parameter
     *
     * @see SSLParameters#getEndpointIdentificationAlgorithm()
     * @see SSLParameters#setEndpointIdentificationAlgorithm(String),
     * @see SSLParameters#getAlgorithmConstraints(),
     * @see SSLParameters#setAlgorithmConstraints(AlgorithmConstraints)
     */
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
    }

    /**
     * Given the partial or complete certificate chain provided by the peer, build and validate the certificate path based on the authentication type and ssl parameters.
     * The authentication type is the key exchange algorithm portion of the cipher suites represented as a String, such as "RSA", "DHE_DSS". Note: for some exportable cipher suites, the key exchange algorithm is determined at run time during the handshake. For instance, for TLS_RSA_EXPORT_WITH_RC4_40_MD5, the authType should be RSA_EXPORT when an ephemeral RSA key is used for the key exchange, and RSA when the key from the server certificate is used. Checking is case-sensitive.
     * <br>
     * If the socket parameter is an instance of SSLSocket, and the endpoint identification algorithm of the SSLParameters is non-empty, to prevent man-in-the-middle attacks, the address that the socket connected to should be checked against the peer's identity presented in the end-entity X509 certificate, as specified in the endpoint identification algorithm.
     * <br>
     * If the socket parameter is an instance of SSLSocket, and the algorithm constraints of the SSLParameters is non-null, for every certificate in the certification path, fields such as subject public key, the signature algorithm, key usage, extended key usage, etc. need to conform to the algorithm constraints in place on this socket.
     *
     * @param chain - the peer certificate chain
     * @param authType - the key exchange algorithm used
     * @param socket - the socket used for this connection. This parameter can be null, which indicates that implementations need not check the ssl parameters
     *
     * @throws IllegalArgumentException - if null or zero-length array is passed in for the chain parameter or if null or zero-length string is passed in for the authType parameter
     *
     * @see SSLParameters#getEndpointIdentificationAlgorithm()
     * @see SSLParameters#setEndpointIdentificationAlgorithm(String)
     * @see SSLParameters#getAlgorithmConstraints()
     * @see SSLParameters#setAlgorithmConstraints(AlgorithmConstraints)
     */
    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
        // validate the certificate chain count
        if (chain.length == 0)
            return;
        // validate the server certificate
        try {
            // get the server SSL fingerprint
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(chain[0].getEncoded());
            String fingerprint = Hash.toHex(bytes);
            // close the http connection if the fingerprint is invalid
            if (!fingerprint.equals(this.fingerprint)) {
                socket.close();
                // TODO handle invalid fingerprint
            }
        }
        // handle exception whilst validating
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Given the partial or complete certificate chain provided by the peer, build and validate the certificate path based on the authentication type and ssl parameters.
     * The authentication type is determined by the actual certificate used. For instance, if RSAPublicKey is used, the authType should be "RSA". Checking is case-sensitive.
     * <br>
     * If the engine parameter is available, and the endpoint identification algorithm of the SSLParameters is non-empty, to prevent man-in-the-middle attacks, the address that the engine connected to should be checked against the peer's identity presented in the end-entity X509 certificate, as specified in the endpoint identification algorithm.
     * <br>
     * If the engine parameter is available, and the algorithm constraints of the SSLParameters is non-null, for every certificate in the certification path, fields such as subject public key, the signature algorithm, key usage, extended key usage, etc. need to conform to the algorithm constraints in place on this engine.
     *
     * @param chain - the peer certificate chain
     * @param authType - the key exchange algorithm used
     * @param engine - the engine used for this connection. This parameter can be null, which indicates that implementations need not check the ssl parameters
     *
     * @throws IllegalArgumentException - if null or zero-length array is passed in for the chain parameter or if null or zero-length string is passed in for the authType parameter
     *
     * @see SSLParameters#getEndpointIdentificationAlgorithm()
     * @see SSLParameters#setEndpointIdentificationAlgorithm(String)
     * @see SSLParameters#getAlgorithmConstraints()
     * @see SSLParameters#setAlgorithmConstraints(AlgorithmConstraints)
     */
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
    }

    /**
     * Given the partial or complete certificate chain provided by the peer, build and validate the certificate path based on the authentication type and ssl parameters.
     * The authentication type is the key exchange algorithm portion of the cipher suites represented as a String, such as "RSA", "DHE_DSS". Note: for some exportable cipher suites, the key exchange algorithm is determined at run time during the handshake. For instance, for TLS_RSA_EXPORT_WITH_RC4_40_MD5, the authType should be RSA_EXPORT when an ephemeral RSA key is used for the key exchange, and RSA when the key from the server certificate is used. Checking is case-sensitive.
     * <br>
     * If the engine parameter is available, and the endpoint identification algorithm of the SSLParameters is non-empty, to prevent man-in-the-middle attacks, the address that the engine connected to should be checked against the peer's identity presented in the end-entity X509 certificate, as specified in the endpoint identification algorithm.
     * <br>
     * If the engine parameter is available, and the algorithm constraints of the SSLParameters is non-null, for every certificate in the certification path, fields such as subject public key, the signature algorithm, key usage, extended key usage, etc. need to conform to the algorithm constraints in place on this engine.
     *
     * @param chain - the peer certificate chain
     * @param authType - the key exchange algorithm used
     * @param engine - the engine used for this connection. This parameter can be null, which indicates that implementations need not check the ssl parameters
     *
     * @throws IllegalArgumentException - if null or zero-length array is passed in for the chain parameter or if null or zero-length string is passed in for the authType parameter
     *
     * @see SSLParameters#getEndpointIdentificationAlgorithm()
     * @see SSLParameters#setEndpointIdentificationAlgorithm(String)
     * @see SSLParameters#getAlgorithmConstraints()
     * @see SSLParameters#setAlgorithmConstraints(AlgorithmConstraints)
     */
    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
    }

    /**
     * Given the partial or complete certificate chain provided by the peer, build a certificate path to a trusted root and return if it can be validated and is trusted for client SSL authentication based on the authentication type.
     * The authentication type is determined by the actual certificate used. For instance, if RSAPublicKey is used, the authType should be "RSA". Checking is case-sensitive.
     *
     * @param chain - the peer certificate chain
     * @param authType - the authentication type based on the client certificate
     *
     * @throws IllegalArgumentException - if null or zero-length chain is passed in for the chain parameter or if null or zero-length string is passed in for the authType parameter
     */
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) {
    }

    /**
     * Given the partial or complete certificate chain provided by the peer, build a certificate path to a trusted root and return if it can be validated and is trusted for server SSL authentication based on the authentication type.
     * The authentication type is the key exchange algorithm portion of the cipher suites represented as a String, such as "RSA", "DHE_DSS". Note: for some exportable cipher suites, the key exchange algorithm is determined at run time during the handshake. For instance, for TLS_RSA_EXPORT_WITH_RC4_40_MD5, the authType should be RSA_EXPORT when an ephemeral RSA key is used for the key exchange, and RSA when the key from the server certificate is used. Checking is case-sensitive.
     *
     * @param chain - the peer certificate chain
     * @param authType - the key exchange algorithm used
     *
     * @throws IllegalArgumentException - if null or zero-length chain is passed in for the chain parameter or if null or zero-length string is passed in for the authType parameter
     */
    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) {
    }

    /**
     * Return an array of certificate authority certificates which are trusted for authenticating peers.
     * @return a non-null (possibly empty) array of acceptable CA issuer certificates.
     */
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}
