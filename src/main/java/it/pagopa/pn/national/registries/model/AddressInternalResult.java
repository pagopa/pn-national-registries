package it.pagopa.pn.national.registries.model;

public sealed interface AddressInternalResult {

    record Found(CodeSqsDto codeSqsDto) implements AddressInternalResult {
    }

    record NotFound(CodeSqsDto codeSqsDto) implements AddressInternalResult {
        public NotFound() {
            this(null);
        }
    }
}