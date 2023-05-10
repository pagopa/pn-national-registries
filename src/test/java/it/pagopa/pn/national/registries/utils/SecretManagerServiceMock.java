package it.pagopa.pn.national.registries.utils;

import it.pagopa.pn.national.registries.service.SecretManagerService;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.util.Optional;

public class SecretManagerServiceMock extends SecretManagerService {

    public SecretManagerServiceMock() {
        super(null);
    }

    @Override
    public Optional<GetSecretValueResponse> getSecretValue(String secretId) {
        GetSecretValueResponse.Builder builder = GetSecretValueResponse.builder();
        builder.secretString("{ \"cert\": \"LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUQ2ekNDQXRPZ0F3SUJBZ0lVQ1o4NUJxa1RwTi9tZ0NsVzNZT0hjcDBJcmpFd0RRWUpLb1pJaHZjTkFRRUwKQlFBd2dZUXhDekFKQmdOVkJBWVRBa1ZWTVE0d0RBWURWUVFJREFWSlZFRk1XVEVOTUFzR0ExVUVCd3dFVUVsVApRVEVSTUE4R0ExVUVDZ3dJVGxSVUlFUkJWRUV4RHpBTkJnTlZCQXNNQmxCQlIwOVFRVEVUTUJFR0ExVUVBd3dLClkyOXRiVzl1VG1GdFpURWRNQnNHQ1NxR1NJYjNEUUVKQVJZT1pXMWhhV3hBWlcxaGFXd3VhWFF3SGhjTk1qTXcKTlRBME1URXdNREUyV2hjTk16TXdOVEF4TVRFd01ERTJXakNCaERFTE1Ba0dBMVVFQmhNQ1JWVXhEakFNQmdOVgpCQWdNQlVsVVFVeFpNUTB3Q3dZRFZRUUhEQVJRU1ZOQk1SRXdEd1lEVlFRS0RBaE9WRlFnUkVGVVFURVBNQTBHCkExVUVDd3dHVUVGSFQxQkJNUk13RVFZRFZRUUREQXBqYjIxdGIyNU9ZVzFsTVIwd0d3WUpLb1pJaHZjTkFRa0IKRmc1bGJXRnBiRUJsYldGcGJDNXBkRENDQVNJd0RRWUpLb1pJaHZjTkFRRUJCUUFEZ2dFUEFEQ0NBUW9DZ2dFQgpBTWgwQ1AyNVk3MUVGRklUSndMS1J0TFNMUTgxVy9lbGs0eFp1QnREa0JWb01zYVgwYU5qTmRVeVUrSWpEenYrCk5wYzZJdWhZdENBN0gzTEhYMmlJNE9pZkQ4Ymt2cC8ya3ZRV3BIdXcxQUdvL0Y0Rkk2MWpibis3WFVHdHhtNWMKaDl1Tm00bGpBM245YVlXbEJsUXR2SHNUYzNxVjl1WXQxcHgxTFlCT3ltVHRDT0ZCd003UlI3UXZkR0poalVLZgoreXh5WDljY2pSYnFNd3FKdlNLN2NJMTZsblVsRDUwMS9FcVVpcUQ3VC9NVzJCckpHTndkdkhuTm9mZ2JXTE1hCk5NaXF0TnVlcERTMFYranAzdEllZlhvTFc4eUVFemQ3d21FZ1ZzZW5xOWlpTEZCMGdsUjg2WnlhN2Mwd2ZPamQKUnQ1Z1BCZysrdi8xK2xINFJKZE5qQXNDQXdFQUFhTlRNRkV3SFFZRFZSME9CQllFRk1lVGI1dnEzSHVUNmRVVwoxd2hlb1BhRFpQVmpNQjhHQTFVZEl3UVlNQmFBRk1lVGI1dnEzSHVUNmRVVzF3aGVvUGFEWlBWak1BOEdBMVVkCkV3RUIvd1FGTUFNQkFmOHdEUVlKS29aSWh2Y05BUUVMQlFBRGdnRUJBRkRBR096MDJVcENLMWFBbithOCtJWVcKbTl2eGdlL1R0bjI1VFk5WEdETVpMYlFON3M1RjVhZkN3ZFRuVFdRY1hPbkNOUTVGNWsycXBUNDg2QnN1TUYvTAo2MEh5NUlReEJSbFl5cVVqb3dNS1FIVjZVWjBKUlp2N1Ewa0FqdGt2OVR6YTNNajhhZFFYNEMyN2pHSmRlWWhuCmZ6MEl0TTREWnN2ZjVrc3hDOVpoMjJWTDNCQmEvTjVEb09yNkQ3UVc3K1RlMlIyWkExZWJOQk55ZUNXNmc5QmgKaW5ub0pacDdKeG9Ra3kxZnVqcmZtNE1HM21ZL3Fya2Z0ckd5MnU4QnNFemJKQTRhbFBqTE9mTXdnVzV1T1I3UgpKT21JNVBRUncxWkJVTUJITE9Sd3VWVkx3VGZXUElXb3RXTUViNXZ4VnozNWViSjE0bzBzWWFqb3BPV3VVbXM9Ci0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0=\", \"key\": \"LS0tLS1CRUdJTiBQUklWQVRFIEtFWS0tLS0tTUlJRXZRSUJBREFOQmdrcWhraUc5dzBCQVFFRkFBU0NCS2N3Z2dTakFnRUFBb0lCQVFESWRBajl1V085UkJSU0V5Y0N5a2JTMGkwUE5WdjNwWk9NV2JnYlE1QVZhRExHbDlHall6WFZNbFBpSXc4Ny9qYVhPaUxvV0xRZ094OXl4MTlvaU9Eb253L0c1TDZmOXBMMEZxUjdzTlFCcVB4ZUJTT3RZMjUvdTExQnJjWnVYSWZialp1Sll3TjUvV21GcFFaVUxieDdFM042bGZibUxkYWNkUzJBVHNwazdRamhRY0RPMFVlMEwzUmlZWTFDbi9zc2NsL1hISTBXNmpNS2liMGl1M0NOZXBaMUpRK2ROZnhLbElxZyswL3pGdGdheVJqY0hieDV6YUg0RzFpekdqVElxclRibnFRMHRGZm82ZDdTSG4xNkMxdk1oQk0zZThKaElGYkhwNnZZb2l4UWRJSlVmT21jbXUzTk1Iem8zVWJlWUR3WVB2ci85ZnBSK0VTWFRZd0xBZ01CQUFFQ2dnRUFBZndQT21zUEg5YmpOc3R2TVhuSFV2OHRNOXBWWVVRb3Z5anR6QktZT2lTYjFjYW1qbXREc2JPMG4vTHQwdDlzU1pZd2R4d0VIamZ1Q0tnZDhEbjdKalE2aldTSFA0K3VsNXQ0OEJIYlRtMDJZaXRBbk9BQUlleS93WkFPck5ucHAzUURMN3RWUFFYbWtzOXFxOVhkZHhGZkJZZ2Q0TXRKbGwxQ0RrSUIrajdtVytCdjl3QmtCQTFUb0c5a1hHNmVSbWZma2VOV3h5aW9FZWRyQ2tLaDlzTDYybzZHdUFlWkxLeHdIYUxGT0Y1bEx4eFJCL0w3WmxVRGV2dmViT2NOV3g1ZnZjMktPbmRnMDlpMjdiaXdaTk13Tm9MeEFsU3ptTVBsK040Z3kydzlzaXJIcU5jT0pSdkp3d2E4U3VxQVc1c242bC9ZZGpxcjFjVHBsZUNnalFLQmdRRFdJOThSVEw2bXh1NGxUZTZsaEVHTmVxekxnRmcxcXhjZDB5L20wbzdWM1U1cDdycVRVSjR1UEdPMmpQU0FiUlEwbjJlWkRqaHdrR2ZPeVNGTk56ck8yeE1NdSs1Vjd5endQcWk5UG0wejJDT0RaTTlJZVRxODJCYXJ2RWNMUzR2bVhKZlEwWEZ5S0dFUy8yQ3NqQm9INi9VQlBCcXVyTklaLzMvdDhTWGZQd0tCZ1FEdm96ellpdU41WVoxZlYxMUYwK1NFVnlHT2VKeFYxdXBselNtc01xa0JydUFOV3VOeXZ5eUxqa0lXVTkxSWFKTnJMSjM4NGdRNDd1QnBvSmJsd1ZENDN2bE1wdXczSStnMHJSMFRWM3FJVUtqQWgvUXRneFlSTVJaNTJZOWNLNVlYdWd1SmJFemRqL0w4SlUzdnp4QkI1ZzlYYzhIYjY5bDJZcGpKb1lpc05RS0JnQXcrUXN1M2FwTVlmcGN0Ykg2VUlEZFpreldUWWZlM2ozdktEa3dSTDg5MGRyNUJ3dk1YUWVQODJlcWZCbit0UE9HQlY2ZjlreWFFYXhyOGp1WGVTWU40TFErbVV3c2d3SjMveHlBQ3lMV1ZIdnF0TWRpLzdhYk1hQWRyblZnOCtvT3lPWS9iMmJPZ01XU25xenhTU2s2M28rdHRKMDFqWGlQVVl0a1UxNGg3QW9HQVhZWGVsRUE0SU1JWThnR2RsRWZxOGo4ekNIQkh5elJ5REVMeWxaSzc1TDFFRWRJNzNDZUNFZ0QyQjlLNUZLNURnbHRLV1V3NEM2UW1qdkt1a1lKTndLTDh0bzZhdDdaYXhGd2ZjZEg5TDBBVGEvaGpNblByWTlBOWpsWjdIYVNoRWlkNG95eFNIMUl6cVhaQm9XcTFWSktkd0NxY0tPRTE0SDhBVkFpbjExVUNnWUVBbVBvYUtCYTJVSlVIRFVXYXo5QXpldXUrazJySXpRNW5tanRVWmxoSmRKZk1Fd1dDSzRSS2VENC9rZzd5d0NFVlpRR3BIai9Ld01sK256M0FjRGZtNlh6S1diZFNYenpDZmFISkVYSlgvdGtrazVwem9pZ1liRldmTCswMVR4dTFkSDRmdXlxNHdSYXRtUTRid2V3c1VVVFo4Ukw5bmN0RHpKcE9OcSs4bmxjPS0tLS0tRU5EIFBSSVZBVEUgS0VZLS0tLS0=\", \"pub\": \"LS0tLS1CRUdJTiBQVUJMSUMgS0VZLS0tLS0KTUlJQklqQU5CZ2txaGtpRzl3MEJBUUVGQUFPQ0FROEFNSUlCQ2dLQ0FRRUF5SFFJL2JsanZVUVVVaE1uQXNwRwowdEl0RHpWYjk2V1RqRm00RzBPUUZXZ3l4cGZSbzJNMTFUSlQ0aU1QTy80Mmx6b2k2RmkwSURzZmNzZGZhSWpnCjZKOFB4dVMrbi9hUzlCYWtlN0RVQWFqOFhnVWpyV051Zjd0ZFFhM0dibHlIMjQyYmlXTURlZjFwaGFVR1ZDMjgKZXhOemVwWDI1aTNXbkhVdGdFN0taTzBJNFVIQXp0Rkh0QzkwWW1HTlFwLzdMSEpmMXh5TkZ1b3pDb205SXJ0dwpqWHFXZFNVUG5UWDhTcFNLb1B0UDh4YllHc2tZM0IyOGVjMmgrQnRZc3hvMHlLcTAyNTZrTkxSWDZPbmUwaDU5CmVndGJ6SVFUTjN2Q1lTQld4NmVyMktJc1VIU0NWSHpwbkpydHpUQjg2TjFHM21BOEdENzYvL1g2VWZoRWwwMk0KQ3dJREFRQUIKLS0tLS1FTkQgUFVCTElDIEtFWS0tLS0t\"}");
        return Optional.of(builder.build());
    }
}