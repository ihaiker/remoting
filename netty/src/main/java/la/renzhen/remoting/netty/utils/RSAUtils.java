package la.renzhen.remoting.netty.utils;

import io.netty.channel.SimpleChannelInboundHandler;
import la.renzhen.remoting.LoggerSupport;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

/**
 * @author <a href="mailto:wo@renzhen.la">haiker</a>
 * @version 2019-01-29 22:37
 */

public class RSAUtils implements LoggerSupport {

    public static File folder;

    public static byte[] test(Key rsaPrivateKey, String name, String password) throws Exception {

        // extract the encoded private key, this is an unencrypted PKCS#8 private key
        byte[] encodedprivkey = rsaPrivateKey.getEncoded();

        // We must use a PasswordBasedEncryption algorithm in order to encrypt the private key, you may use any common algorithm supported by openssl,
        //  you can check them in the openssl documentation http://www.openssl.org/docs/apps/pkcs8.html
        String MYPBEALG = "PBEWithSHA1AndDESede";

        int count = 20;// hash iteration count
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[8];
        random.nextBytes(salt);

        // Create PBE parameter set
        PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, count);
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray());
        SecretKeyFactory keyFac = SecretKeyFactory.getInstance(MYPBEALG);
        SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);

        Cipher pbeCipher = Cipher.getInstance(MYPBEALG);

        // Initialize PBE Cipher with key and parameters
        pbeCipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);

        // Encrypt the encoded Private Key with the PBE key
        byte[] ciphertext = pbeCipher.doFinal(encodedprivkey);

        // Now construct  PKCS #8 EncryptedPrivateKeyInfo object
        AlgorithmParameters algparms = AlgorithmParameters.getInstance(MYPBEALG);
        algparms.init(pbeParamSpec);
        EncryptedPrivateKeyInfo encinfo = new EncryptedPrivateKeyInfo(algparms, ciphertext);

        // and here we have it! a DER encoded PKCS#8 encrypted key!
        byte[] encryptedPkcs8 = encinfo.getEncoded();
        out(name, null, encodedprivkey);
        return encodedprivkey;
    }

    public static void generate(String outFolder) throws Exception {
        folder = new File(outFolder);

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(1024);

        KeyPair kp = kpg.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) kp.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) kp.getPrivate();

        out("rsa.prk", null, privateKey.getEncoded());//PKCS#8
        out("rsa.puk", null, publicKey.getEncoded()); //X.509

        Base64.Encoder encoder = Base64.getEncoder();
        out("rsa.prk.pem", "RSA PRIVATE", encoder.encode(privateKey.getEncoded()));
        out("rsa.puk.pem", "RSA PUBLIC", encoder.encode(privateKey.getEncoded()));


        Signature sign = Signature.getInstance("SHA256withRSA");
        sign.initSign(privateKey);
        sign.update("test".getBytes());

        byte[] signBytes = sign.sign();
        log.info("test sign rsa: {}", Base64.getEncoder().encodeToString(signBytes));

        sign.initVerify(publicKey);
        sign.update("test".getBytes());
        log.info("verity: {}", sign.verify(signBytes));


        java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");

        PrivateKey privateKeyPKC8 = kf.generatePrivate(new PKCS8EncodedKeySpec(privateKey.getEncoded()));
        out("rsa.prk.pkcs8", null, privateKeyPKC8.getEncoded());

        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(publicKey.getModulus(), publicKey.getPublicExponent());
        PublicKey publicKeyPKC8 = kf.generatePublic(publicKeySpec);
        out("rsa.puk.pkcs8", null, publicKeyPKC8.getEncoded());
    }


    public static void out(String name, String type, byte[] encoded) throws Exception {
        if (!folder.exists() && !folder.mkdirs()) {
            throw new FileNotFoundException("the file " + folder.getAbsolutePath() + " not fount and can't create !");
        }
        Base64.Encoder encoder = Base64.getEncoder();
        try (FileOutputStream out = new FileOutputStream(new File(folder, name))) {
            if (type != null) {
                out.write(("-----BEGIN " + type + " KEY-----\n").getBytes());
            }
            out.write(encoded);
            if (type != null) {
                out.write(("\n-----END " + type + " KEY-----\n").getBytes());
            }
        }
    }


    public static void loadPrivateKey() throws Exception {
        /* Read all bytes from the private key file */
        Path path = Paths.get("");
        byte[] bytes = Files.readAllBytes(path);

        /* Generate private key. */
        PKCS8EncodedKeySpec ks = new PKCS8EncodedKeySpec(bytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey pvt = kf.generatePrivate(ks);
    }

    public static void loadPublicKey() throws Exception {
        /* Read all bytes from the private key file */
        Path path = Paths.get("");
        byte[] bytes = Files.readAllBytes(path);

        /* Generate private key. */
        PKCS8EncodedKeySpec ks = new PKCS8EncodedKeySpec(bytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey pvt = kf.generatePrivate(ks);
    }


}
