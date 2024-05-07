package com.czertainly.core.service.cmp.message;

import com.czertainly.api.model.client.attribute.RequestAttributeDto;
import com.czertainly.api.model.core.cmp.ProtectionMethod;
import com.czertainly.core.api.cmp.error.CmpBaseException;
import com.czertainly.core.api.cmp.error.CmpConfigurationException;
import com.czertainly.core.api.cmp.error.CmpProcessingException;
import com.czertainly.core.dao.entity.cmp.CmpProfile;
import com.czertainly.core.service.cmp.message.protection.ProtectionStrategy;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.cmp.PKIMessage;
import org.bouncycastle.asn1.x509.GeneralName;

import java.util.List;

public interface ConfigurationContext {

    CmpProfile getProfile();

    GeneralName getRecipient();

    ASN1OctetString getSenderKID();

    byte[] getSharedSecret();

    /**
     * It allows to client define specified validation of CRMF request messages
     * @param request of crmf based message
     */
    void validateRequest(PKIMessage request)
            throws CmpProcessingException;

    /**
     * It allows to client define specified validation of response messages
     * @param response of message
     */
    void validateResponse(PKIMessage response)
            throws CmpProcessingException;

    /**
     * @return get protection method (how REQUEST message has to be protected)
     */
    ProtectionMethod getProtectionMethod() throws CmpConfigurationException;

    /**
     * @return get protection strategy (how RESPONSE message has to be protected)
     */
    ProtectionStrategy getProtectionStrategy() throws CmpBaseException;

    List<RequestAttributeDto> getClientOperationAttributes(boolean isRevoke);
}
