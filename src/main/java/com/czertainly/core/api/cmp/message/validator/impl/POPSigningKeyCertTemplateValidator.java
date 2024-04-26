package com.czertainly.core.api.cmp.message.validator.impl;

import com.czertainly.core.api.cmp.error.CmpException;
import com.czertainly.core.api.cmp.error.CmpProcessingException;
import com.czertainly.core.api.cmp.error.ImplFailureInfo;
import com.czertainly.core.api.cmp.message.validator.Validator;
import com.czertainly.core.api.cmp.util.CryptoUtil;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.cmp.PKIFailureInfo;
import org.bouncycastle.asn1.cmp.PKIMessage;
import org.bouncycastle.asn1.crmf.*;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

/**
 *
 * <p>POP validator if:
 *        The certificate subject places its name in the Certificate
 *        Template structure along with the public key. In this case the
 *        poposkInput field is omitted from the POPOSigningKey structure.
 *        The signature field is computed over the DER-encoded certificate
 *        template structure. See https://www.rfc-editor.org/rfc/rfc4211#section-4.1, point 3
 * </p>
 *
 * <pre>
 *    POPOSigningKey ::= SEQUENCE {
 *        poposkInput         [0] POPOSigningKeyInput OPTIONAL,
 *        algorithmIdentifier     AlgorithmIdentifier,
 *        signature               BIT STRING }
 * </pre>
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc4211#section-4.1">Signature Key POP</a>
 */
public class POPSigningKeyCertTemplateValidator implements Validator<PKIMessage, Void> {
    @Override
    public Void validate(PKIMessage message) throws CmpException {
        CertReqMsg certReqMsg = ((CertReqMessages) message.getBody().getContent()).toCertReqMsgArray()[0];
        CertRequest certRequest = certReqMsg.getCertReq();
        ProofOfPossession proofOfPossession = certReqMsg.getPop();

        // -- public key (if poposkInput is null)
        // this field must be filled
        // @see https://www.rfc-editor.org/rfc/rfc4211#section-4.1 (point 3)
        SubjectPublicKeyInfo subjectPublicKeyInfo = getSubjectPublicKeyInfo(certRequest);
        PublicKey publicKey = getPublicKey(subjectPublicKeyInfo);

        // -- subject (if poposkInput is null)
        // this field must be filled
        // @see https://www.rfc-editor.org/rfc/rfc4211#section-4.1 (point 3)
        X500Name subject = certRequest.getCertTemplate().getSubject();
        if(subject == null) throw new CmpProcessingException(PKIFailureInfo.badPOP,
                ImplFailureInfo.CMPVALPOP509);

        // -- signature
        Signature signature = buildSignature(certReqMsg, publicKey);
        try {
            POPOSigningKey popoSigningKey = (POPOSigningKey) proofOfPossession.getObject();
            if(!signature.verify(popoSigningKey.getSignature().getBytes())) {//podpisy nesedi
                throw new CmpProcessingException(PKIFailureInfo.badPOP,
                        ImplFailureInfo.CMPVALPOP506);
            }
        } catch (SignatureException e) {
            throw new CmpProcessingException(PKIFailureInfo.badPOP,
                    ImplFailureInfo.CMPVALPOP503, e);
        }

        return null;
    }

    /**
     * Try to get wrapper element {@link SubjectPublicKeyInfo} from {@link CertRequest}
     * and validate its content (public key bytes cannot be empty or zero).
     * <p>
     * if <code>poposkInput</code> is null, <code>public key</code> and subject must be filled (see POP/4.1 - point 3, rfc 4211)
     *
     * @param certRequest field which keeps public key metadata
     * @return public key metadata field
     * @throws CmpProcessingException if validation of field is going to fail
     */
    private SubjectPublicKeyInfo getSubjectPublicKeyInfo(CertRequest certRequest) throws CmpProcessingException {
        SubjectPublicKeyInfo subjectPublicKeyInfo = certRequest.getCertTemplate().getPublicKey();
        if(subjectPublicKeyInfo==null
                || subjectPublicKeyInfo.getPublicKeyData() == null
                || subjectPublicKeyInfo.getPublicKeyData().getBytes() == null
                || subjectPublicKeyInfo.getPublicKeyData().getBytes().length == 0) {
            throw new CmpProcessingException(PKIFailureInfo.badPOP,
                    ImplFailureInfo.CMPVALPOP507);
        }
        return subjectPublicKeyInfo;
    }

    /**
     * Try to extract public key from incoming pki message.
     *
     * @param subjectPublicKeyInfo contains {@link PublicKey} for signature verification (motivation)
     * @return public key incoming from {@link PKIMessage}
     *
     * @throws CmpProcessingException if extraction of public key is going to fail
     */
    private PublicKey getPublicKey(SubjectPublicKeyInfo subjectPublicKeyInfo) throws CmpProcessingException {
        try {
            return KeyFactory.getInstance(
                    subjectPublicKeyInfo.getAlgorithm().getAlgorithm().toString(),
                    CryptoUtil.getBouncyCastleProvider()
            ).generatePublic(new X509EncodedKeySpec(subjectPublicKeyInfo.getEncoded(ASN1Encoding.DER)));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | IOException e) {
            throw new CmpProcessingException(PKIFailureInfo.badPOP,
                    ImplFailureInfo.CMPVALPOP501, e);
        }
    }

    /**
     * Signature object (java.security) to check Proof-Of-Possesion using signature
     * @param certReqMsg field which keeps all needed field to check POP
     * @param publicKey extracted public key from incoming cert template
     * @return built signature object for verification
     */
    private Signature buildSignature(CertReqMsg certReqMsg, PublicKey publicKey) throws CmpProcessingException {
        CertRequest certRequest = certReqMsg.getCertReq();
        ProofOfPossession proofOfPossession = certReqMsg.getPop();
        POPOSigningKey popoSigningKey = (POPOSigningKey) proofOfPossession.getObject();
        try {
            /*
            POPOSigningKey ::= SEQUENCE {
             poposkInput           [0] POPOSigningKeyInput OPTIONAL,
             algorithmIdentifier   AlgorithmIdentifier,
             signature             BIT STRING }
             -- The signature (using "algorithmIdentifier") is on the
             -- DER-encoded value of poposkInput.  NOTE: If the CertReqMsg
             -- certReq CertTemplate contains the subject and publicKey values,
             -- then poposkInput MUST be omitted and the signature MUST be
             -- computed over the DER-encoded value of CertReqMsg certReq.  If
             -- the CertReqMsg certReq CertTemplate does not contain both the
             -- public key and subject values (i.e., if it contains only one
             -- of these, or neither), then poposkInput MUST be present and
             -- MUST be signed.
             @see https://www.rfc-editor.org/rfc/rfc4211#appendix-B

             The certificate subject places its name in the Certificate
             Template structure along with the public key.  In this case the
             poposkInput field is omitted from the POPOSigningKey structure.
             The signature field is computed over the DER-encoded certificate
             template structure.
             @see https://www.rfc-editor.org/rfc/rfc4211#section-4.1 (point 3)
             */
            byte[] subjectOfVerification = certRequest.getEncoded(ASN1Encoding.DER);
            Signature signature = Signature.getInstance(
                    popoSigningKey.getAlgorithmIdentifier().getAlgorithm().getId(), CryptoUtil.getBouncyCastleProvider());
            signature.initVerify(publicKey);
            signature.update(subjectOfVerification);
            return signature;
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException | IOException e) {
            throw new CmpProcessingException(PKIFailureInfo.badPOP,
                    ImplFailureInfo.CMPVALPOP502, e);
        }

    }
}
