package com.szn.merger.Utils.Signing;

import android.util.Log;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public final class KeystoreGenerator {

    public final String keystoreName;
    public final String alias;
    public final String password;
    public String keystoreType = "PKCS12";
    public String keyAlgorithm = "RSA";
    public int keySize = 2048;
    public String signatureAlgorithm = "SHA256withRSA";
    public Date validityNotBefore;
    public Date validityNotAfter;
    public String commonName;
    public String organizationUnit;
    public String organization;
    public String locality;
    public String state;
    public String country;

    public KeystoreGenerator(String keystoreName, String alias, String password, String commonName, String organization, String organizationUnit, String locality, String state, String country, Date validityNotBefore, Date validityNotAfter) {
        this.keystoreName = keystoreName;
        this.alias = alias;
        this.password = password;

        this.commonName = valueOrDefault(commonName, "Android");
        this.organization = valueOrDefault(organization, "Google Inc");
        this.organizationUnit = valueOrDefault(organizationUnit, "Android");
        this.locality = valueOrDefault(locality, "Mountain View");
        this.state = valueOrDefault(state, "California");
        this.country = valueOrDefault(country, "US");

        if (validityNotBefore == null || validityNotAfter == null) {
            Calendar calendar = Calendar.getInstance();
            this.validityNotBefore = calendar.getTime();
            calendar.add(Calendar.YEAR, 30);
            this.validityNotAfter = calendar.getTime();
        } else {
            if (!validityNotAfter.after(validityNotBefore)) {
                throw new IllegalArgumentException("Expiry must be after start");
            }

            this.validityNotBefore = validityNotBefore;
            this.validityNotAfter = validityNotAfter;
        }
    }

    private static String valueOrDefault(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    public KeystoreManager.Item generate(File directory) throws Exception {
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("Cannot create directory");
        }

        String id = UUID.randomUUID().toString();

        String extension = keystoreType.equalsIgnoreCase("PKCS12") ? ".p12" : ".jks";
        String fileName = keystoreName + extension;
        File output = new File(directory, fileName);

        Log.d("KEYSTORE", "Output: " + output.getAbsolutePath());

        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        SecureRandom random = new SecureRandom();

        KeyPairGenerator generator = KeyPairGenerator.getInstance(keyAlgorithm);
        generator.initialize(keySize, random);

        KeyPair keyPair = generator.generateKeyPair();

        Date notBefore = validityNotBefore;
        Date notAfter = validityNotAfter;

        BigInteger serial = new BigInteger(64, random);

        X500Name subject = new X500Name(
                "CN=" + commonName +
                        ", OU=" + organizationUnit +
                        ", O=" + organization +
                        ", L=" + locality +
                        ", ST=" + state +
                        ", C=" + country
        );

        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                subject,
                serial,
                notBefore,
                notAfter,
                subject,
                keyPair.getPublic()
        );

        ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithm).build(keyPair.getPrivate());

        X509CertificateHolder holder = builder.build(signer);

        X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(holder);

        KeyStore keyStore = KeyStore.getInstance(keystoreType);

        keyStore.load(null, null);

        keyStore.setKeyEntry(
                alias,
                keyPair.getPrivate(),
                password.toCharArray(),
                new Certificate[]{certificate}
        );

        try (FileOutputStream fos = new FileOutputStream(output)) {
            keyStore.store(fos, password.toCharArray());
            fos.flush();
        }

        Log.d("KEYSTORE", "Created: " + output.exists());

        KeystoreManager.Item item = new KeystoreManager.Item();
        item.id = id;
        item.name = keystoreName;
        item.fileName = fileName;
        item.alias = alias;
        item.password = password;
        item.type = keystoreType;

        return item;
    }
}