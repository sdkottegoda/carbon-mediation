package org.wso2.carbon.transports.sap.bapi;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.transport.OutTransportInfo;

/**
 * This class is used to handle the synchronous BAPI listener's response payload.
 */
public class SAPOutTransportInfo implements OutTransportInfo {

    private OMElement payload;
    private String protocol;

    public void setPayload(OMElement payload) {

        this.payload = payload;
    }

    public OMElement getPayload() {

        return this.payload;
    }

    public void setProtocol(String protocol) {

        this.protocol = protocol;
    }

    public String getProtocol() {

        return this.protocol;
    }

    @Override
    public void setContentType(String s) {

    }
}
