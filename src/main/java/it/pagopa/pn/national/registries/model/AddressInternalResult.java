package it.pagopa.pn.national.registries.model;

public sealed interface AddressInternalResult {

    CodeSqsDto codeSqsDto();

    record Found(CodeSqsDto codeSqsDto) implements AddressInternalResult {
    }

    record NotFound(CodeSqsDto codeSqsDto) implements AddressInternalResult {
        public NotFound() {
            this(null);
        }
    }
}