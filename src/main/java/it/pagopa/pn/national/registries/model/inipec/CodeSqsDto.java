package it.pagopa.pn.national.registries.model.inipec;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import it.pagopa.pn.national.registries.utils.JsonFilterSpEL;
import lombok.Data;
import lombok.ToString;

import java.util.List;

import static it.pagopa.pn.national.registries.utils.JacksonCustomSpELSerializer.FILTER_NAME;

@Data
@JsonFilter(FILTER_NAME)
public class CodeSqsDto {

    private String correlationId;

    @JsonFilterSpEL("#? != null || (#this.error == null && #this.addressType == 'PHYSICAL')")
    @ToString.Exclude
    private PhysicalAddress physicalAddress;

    @JsonFilterSpEL("#? != null || (#this.error == null && #this.addressType == 'DIGITAL')")
    @ToString.Exclude
    private List<DigitalAddress> digitalAddress;

    @JsonFilterSpEL("#? != null")
    private String error;

    @JsonIgnore
    @ToString.Exclude
    private String addressType;
}
