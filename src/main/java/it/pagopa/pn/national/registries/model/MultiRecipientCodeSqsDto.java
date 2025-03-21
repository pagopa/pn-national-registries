package it.pagopa.pn.national.registries.model;

import com.fasterxml.jackson.annotation.JsonFilter;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

import static it.pagopa.pn.national.registries.utils.JacksonCustomSpELSerializer.FILTER_NAME;

@Builder
@Getter
@Setter
@JsonFilter(FILTER_NAME)
public class MultiRecipientCodeSqsDto {

    private String correlationId;
    private List<InternalRecipientAddress> internalRecipientAdresses;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date referenceRequestDate;
    private String pnNationalRegistriesCxId;

    @Builder
    @Getter
    @Setter
    @JsonFilter(FILTER_NAME)
    public static class InternalRecipientAddress{

        @lombok.ToString.Exclude
        private String taxId;
        private String domicileType;
        private Integer recIndex;
        private String recipientType;

    }

}
