package it.pagopa.pn.national.registries.constant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Stato dell'api key
 */

public enum BatchStatus {

  NOT_WORKED("NOT_WORKED"),
  TO_WORK("TO_WORK"),
  WORKING("WORKING"),
  WORKED("WORKED"),
  ERROR("ERROR");

  private String value;

  BatchStatus(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static BatchStatus fromValue(String value) {
    for (BatchStatus b : BatchStatus.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

