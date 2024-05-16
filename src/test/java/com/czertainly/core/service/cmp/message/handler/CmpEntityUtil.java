package com.czertainly.core.service.cmp.message.handler;

import com.czertainly.api.model.common.enums.cryptography.KeyAlgorithm;
import com.czertainly.api.model.common.enums.cryptography.KeyType;
import com.czertainly.api.model.core.certificate.CertificateState;
import com.czertainly.api.model.core.cmp.CmpProfileVariant;
import com.czertainly.api.model.core.cmp.CmpTransactionState;
import com.czertainly.api.model.core.cmp.ProtectionMethod;
import com.czertainly.api.model.core.connector.ConnectorStatus;
import com.czertainly.api.model.core.cryptography.key.KeyState;
import com.czertainly.core.dao.entity.*;
import com.czertainly.core.dao.entity.cmp.CmpProfile;
import com.czertainly.core.dao.entity.cmp.CmpTransaction;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.util.Arrays;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CmpEntityUtil {

    public static RaProfile createRaProfile(){
        RaProfile raProfile = new RaProfile();
        raProfile.setUuid(UUID.randomUUID());
        raProfile.setEnabled(true);
        raProfile.setName("testRaProfile1_"+System.currentTimeMillis());
        return raProfile;
    }

    public static RaProfile createRaProfile(AuthorityInstanceReference authorityInstanceReference){
        RaProfile raProfile = createRaProfile();
        raProfile.setAuthorityInstanceReference(authorityInstanceReference);
        raProfile.setAuthorityInstanceReferenceUuid(authorityInstanceReference.getUuid());
        return raProfile;
    }

    public static CmpProfile createCmpProfile(RaProfile raProfile, String sharedSecret){
        CmpProfile cmpProfile = new CmpProfile();
        cmpProfile.setUuid(UUID.randomUUID());
        cmpProfile.setName("testCmpProfile1_"+System.currentTimeMillis());
        cmpProfile.setCreated(LocalDateTime.now());
        cmpProfile.setUpdated(LocalDateTime.now());
        cmpProfile.setEnabled(true);
        cmpProfile.setVariant(CmpProfileVariant.V2_3GPP);
        cmpProfile.setRaProfile(raProfile);
        cmpProfile.setRaProfileUuid(raProfile.getUuid());
        cmpProfile.setResponseProtectionMethod(ProtectionMethod.SHARED_SECRET);
        cmpProfile.setSharedSecret(sharedSecret);
        return cmpProfile;
    }

    public static CmpProfile createCmpProfile(RaProfile raProfile, Certificate signingCertificate){
        CmpProfile cmpProfile = new CmpProfile();
        cmpProfile.setUuid(UUID.randomUUID());
        cmpProfile.setName("testCmpProfile0");
        cmpProfile.setCreated(LocalDateTime.now());
        cmpProfile.setUpdated(LocalDateTime.now());
        cmpProfile.setEnabled(true);
        cmpProfile.setVariant(CmpProfileVariant.V2_3GPP);
        cmpProfile.setRaProfile(raProfile);
        cmpProfile.setRaProfileUuid(raProfile.getUuid());
        cmpProfile.setResponseProtectionMethod(ProtectionMethod.SIGNATURE);
        cmpProfile.setSigningCertificate(signingCertificate);
        return cmpProfile;
    }

    public static CertificateContent createEmptyCertContent(){
        return new CertificateContent();
    }

    public static CertificateContent createCertContent(String fingerPrint, String content) {
        CertificateContent certificateContent = new CertificateContent();
        certificateContent.setFingerprint(fingerPrint);
        certificateContent.setContent(content);
        return certificateContent;
    }

    public static Certificate createCertificate(BigInteger serialNumber,
                                                CertificateState state,
                                                CertificateContent certificateContent) {
        Certificate certificate = new Certificate();
        certificate.setCertificateContent(certificateContent);
        certificate.setSerialNumber(serialNumber.toString(16));
        certificate.setUuid(UUID.randomUUID());
        certificate.setState(state);
        return certificate;
    }

    public static Certificate createCertificate(DigestCalculator digester,
                                                      BigInteger serialNumber,
                                                      CertificateState state,
                                                      CertificateContent certificateContent) {
        Certificate certificate = createCertificate(serialNumber, state, certificateContent);
        certificate.setFingerprint(
                new DEROctetString(
                        digester.getDigest()).toString().substring(1)/*remove ''#'*/);
        return certificate;
    }

    public static Certificate createCertificate(BigInteger serialNumber,
                                                CertificateState state,
                                                CertificateContent certificateContent,
                                                CryptographicKey key) {
        Certificate certificate = createCertificate(serialNumber, state, certificateContent);
        certificate.setKey(key);
        return certificate;
    }

    public static CmpTransaction createTransaction(String transactionId,
                                         Certificate issuedCertificate,
                                         CmpProfile cmpProfile,
                                         CmpTransactionState state) {
        CmpTransaction cmpTransaction = new CmpTransaction();
        cmpTransaction.setTransactionId(new DEROctetString(Arrays.clone(transactionId.getBytes())).toString());
        cmpTransaction.setCmpProfile(cmpProfile);
        cmpTransaction.setCertificateUuid(issuedCertificate.getUuid());
        cmpTransaction.setState(state);//CmpTransactionState.CERT_ISSUED
        return cmpTransaction;
    }

    public static CryptographicKey createCryptographicKey() {
        CryptographicKey key = new CryptographicKey();
        key.setUuid(UUID.randomUUID());
        key.setName("testKey1_"+System.currentTimeMillis());
        key.setDescription("description1_"+System.currentTimeMillis());
        return key;
    }

    public static CryptographicKeyItem createCryptographicKeyItem(CryptographicKey key,
                                                                  UUID keyReferenceUuid,
                                                                  KeyType keyType,
                                                                  KeyAlgorithm keyAlgorithm,
                                                                  String fingerprint) {
        CryptographicKeyItem item = new CryptographicKeyItem();
        item.setKeyReferenceUuid(keyReferenceUuid);
        item.setState(KeyState.ACTIVE);
        item.setType(keyType);
        item.setFingerprint(fingerprint);
        item.setEnabled(true);
        item.setCryptographicKey(key);
        item.setKeyAlgorithm(keyAlgorithm);
        return item;
    }
}
