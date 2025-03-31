package it.pagopa.pn.national.registries.model;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.pagopa.pn.national.registries.model.inipec.DigitalAddress;
import it.pagopa.pn.national.registries.model.inipec.PhysicalAddress;
import it.pagopa.pn.national.registries.utils.JsonFilterSpEL;
import lombok.Data;
import lombok.ToString;

import java.util.List;

import static it.pagopa.pn.national.registries.utils.JacksonCustomSpELSerializer.FILTER_NAME;

@Data
@JsonFilter(FILTER_NAME)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MultiCodeSqsDto {

    private String correlationId;

    private List<PhysicalAddressSQSMessage> addresses;

    @JsonIgnore
    @ToString.Exclude
    private String addressType;

    @Data
    public static class PhysicalAddressSQSMessage {
        private Integer recIndex;

        @JsonFilterSpEL("#? != null || (#this.error == null && #this.addressType == 'PHYSICAL')")
        @ToString.Exclude
        private PhysicalAddress physicalAddress;

        @JsonFilterSpEL("#? != null || (#this.error == null && #this.addressType == 'DIGITAL')")
        @ToString.Exclude
        private List<DigitalAddress> digitalAddress;

        @JsonFilterSpEL("#? != null")
        private String error;

        private String registry;
    }
}
