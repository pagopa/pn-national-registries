package it.pagopa.pn.national.registries.model;

import lombok.Getter;

@Getter
public enum EService {
    INIPEC(1, "IPA"),
    IPA(2, "INAD"),
    INAD(3, null);

    private final int stepNumber;
    private final String nextStep;

    EService(int stepNumber, String nextStep) {
        this.stepNumber = stepNumber;
        this.nextStep = nextStep;
    }
}
