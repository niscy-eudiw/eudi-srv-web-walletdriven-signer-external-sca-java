/*
 Copyright 2024 European Commission

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      https://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package eu.europa.ec.eudi.signer.r3.sca.web.dto.signDoc;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class SignaturesSignDocRequest {
    private String credentialID;
    private List<DocumentsSignDocRequest> documents;
    private Boolean returnValidationInfo = false;
    @NotBlank
    private String request_uri;
    private String hashAlgorithmOID;
    private long signature_date;
    private String clientData;

    public SignaturesSignDocRequest() {
    }

    @JsonProperty
    public String getCredentialID() {
        return credentialID;
    }

    @JsonProperty
    public void setCredentialID(String credentialID) {
        this.credentialID = credentialID;
    }

    @JsonProperty
    public List<DocumentsSignDocRequest> getDocuments() {
        return documents;
    }

    @JsonProperty
    public void setDocuments(List<DocumentsSignDocRequest> documents) {
        this.documents = documents;
    }

    @JsonProperty
    public String getClientData() {
        return clientData;
    }

    @JsonProperty
    public void setClientData(String clientData) {
        this.clientData = clientData;
    }

    @JsonProperty
    public Boolean getReturnValidationInfo() {
        return returnValidationInfo;
    }

    @JsonProperty
    public void setReturnValidationInfo(Boolean returnValidationInfo) {
        this.returnValidationInfo = returnValidationInfo;
    }

    @JsonProperty
    public String getRequest_uri() {
        return request_uri;
    }

    @JsonProperty
    public void setRequest_uri(String request_uri) {
        this.request_uri = request_uri;
    }

    public String getHashAlgorithmOID() {
        return hashAlgorithmOID;
    }

    public void setHashAlgorithmOID(String hashAlgorithmOID) {
        this.hashAlgorithmOID = hashAlgorithmOID;
    }

    public long getSignature_date() {
        return signature_date;
    }

    public void setSignature_date(long signature_date) {
        this.signature_date = signature_date;
    }

    @Override
    public String toString() {
        return "SignaturesSignDocRequest{" +
              "credentialID='" + credentialID + '\'' +
              ", documents=" + documents +
              ", returnValidationInfo=" + returnValidationInfo +
              ", request_uri='" + request_uri + '\'' +
              ", hashAlgorithmOID='" + hashAlgorithmOID + '\'' +
              ", signature_date=" + signature_date +
              ", clientData='" + clientData + '\'' +
              '}';
    }
}
