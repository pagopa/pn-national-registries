package it.pagopa.pn.national.registries.log;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AwsClientLoggerInterceptorTest {

    @Test
    void testBeforeExecution() {
        AwsClientLoggerInterceptor awsClientLoggerInterceptor = new AwsClientLoggerInterceptor();
        Context.BeforeExecution context = () -> mock(SdkRequest.class);

        ExecutionAttributes executionAttributes = new ExecutionAttributes();

        assertDoesNotThrow(() -> awsClientLoggerInterceptor.beforeExecution(context, executionAttributes));
    }

    @Test
    void testAfterExecution() {
        AwsClientLoggerInterceptor awsClientLoggerInterceptor = new AwsClientLoggerInterceptor();
        ExecutionAttributes executionAttributes = new ExecutionAttributes();

        Context.AfterExecution contextScan = contextAfterExecution(ScanResponse.builder().build());
        assertDoesNotThrow(() -> awsClientLoggerInterceptor.afterExecution(contextScan, executionAttributes));
        Context.AfterExecution contextQuery = contextAfterExecution(QueryResponse.builder().build());
        assertDoesNotThrow(() -> awsClientLoggerInterceptor.afterExecution(contextQuery, executionAttributes));
        Context.AfterExecution contextGetItem = contextAfterExecution(GetItemResponse.builder().build());
        assertDoesNotThrow(() -> awsClientLoggerInterceptor.afterExecution(contextGetItem, executionAttributes));
        Context.AfterExecution contextPutItem = contextAfterExecution(PutItemResponse.builder().build());
        assertDoesNotThrow(() -> awsClientLoggerInterceptor.afterExecution(contextPutItem, executionAttributes));
        Context.AfterExecution contextDeleteItem = contextAfterExecution(DeleteItemResponse.builder().build());
        assertDoesNotThrow(() -> awsClientLoggerInterceptor.afterExecution(contextDeleteItem, executionAttributes));
        Context.AfterExecution contextBatchGet = contextAfterExecution(BatchGetItemResponse.builder().build());
        assertDoesNotThrow(() -> awsClientLoggerInterceptor.afterExecution(contextBatchGet, executionAttributes));
        Context.AfterExecution contextBatchWrite = contextAfterExecution(BatchWriteItemResponse.builder().build());
        assertDoesNotThrow(() -> awsClientLoggerInterceptor.afterExecution(contextBatchWrite, executionAttributes));
    }

    @Test
    void testOnExecutionFailure() {
        AwsClientLoggerInterceptor awsClientLoggerInterceptor = new AwsClientLoggerInterceptor();
        Context.FailedExecution context = new Context.FailedExecution() {
            @Override
            public Throwable exception() {
                return null;
            }

            @Override
            public SdkRequest request() {
                return null;
            }

            @Override
            public Optional<SdkHttpRequest> httpRequest() {
                return Optional.empty();
            }

            @Override
            public Optional<SdkHttpResponse> httpResponse() {
                return Optional.empty();
            }

            @Override
            public Optional<SdkResponse> response() {
                return Optional.empty();
            }
        };
        ExecutionAttributes executionAttributes = new ExecutionAttributes();

        assertDoesNotThrow(() -> awsClientLoggerInterceptor.onExecutionFailure(context, executionAttributes));
    }

    private Context.AfterExecution contextAfterExecution(SdkResponse mockedResponse) {
        return new Context.AfterExecution() {
            @Override
            public SdkResponse response() {
                return mockedResponse;
            }

            @Override
            public SdkHttpResponse httpResponse() {
                return null;
            }

            @Override
            public Optional<Publisher<ByteBuffer>> responsePublisher() {
                return Optional.empty();
            }

            @Override
            public Optional<InputStream> responseBody() {
                return Optional.empty();
            }

            @Override
            public SdkHttpRequest httpRequest() {
                return null;
            }

            @Override
            public Optional<RequestBody> requestBody() {
                return Optional.empty();
            }

            @Override
            public Optional<AsyncRequestBody> asyncRequestBody() {
                return Optional.empty();
            }

            @Override
            public SdkRequest request() {
                SdkRequest sdkRequest = mock(SdkRequest.class);
                when(sdkRequest.toString()).thenReturn("");
                return sdkRequest;
            }
        };
    }
}