package it.pagopa.pn.national.registries.model;

import com.fasterxml.jackson.annotation.JsonFilter;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.Date;

import static it.pagopa.pn.national.registries.utils.JacksonCustomSpELSerializer.FILTER_NAME;

@Builder
@Getter
@Setter
@JsonFilter(FILTER_NAME)
public class InternalCodeSqsDto {

    private String correlationId;

    @lombok.ToString.Exclude
    private String taxId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date referenceRequestDate;

    private String domicileType;

    private String recipientType;

    private String pnNationalRegistriesCxId;
}
