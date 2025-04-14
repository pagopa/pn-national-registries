package it.pagopa.pn.national.registries.middleware.queue.consumer.event;

import lombok.*;

import javax.validation.constraints.NotEmpty;

@Builder
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class InternalRecipientAddress {

    @NotEmpty
    @lombok.ToString.Exclude
    private String taxId;
    @NotEmpty
    private String domicileType;
    @NotEmpty
    private String recipientType;
    @NotEmpty
    private Integer recIndex;

}
