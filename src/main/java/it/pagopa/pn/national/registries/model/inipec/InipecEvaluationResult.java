package it.pagopa.pn.national.registries.model.inipec;

import it.pagopa.pn.national.registries.model.CodeSqsDto;

public sealed interface InipecEvaluationResult {

    record Found(CodeSqsDto codeSqsDto) implements InipecEvaluationResult {}

    record NotFound() implements InipecEvaluationResult {}

    record Retry() implements InipecEvaluationResult {}
}
