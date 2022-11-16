package it.pagopa.pn.national.registries.log;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.interceptor.*;
import software.amazon.awssdk.services.dynamodb.model.*;

@Slf4j
public class AwsClientLoggerInterceptor implements ExecutionInterceptor {

    private static final ExecutionAttribute<String> SERVICE_NAME = SdkExecutionAttribute.SERVICE_NAME;
    private static final ExecutionAttribute<String> OPERATION_NAME = SdkExecutionAttribute.OPERATION_NAME;
    private static final ExecutionAttribute<Long> START_TIME= new ExecutionAttribute<>("startTime");

    @Override
    public void beforeExecution(Context.BeforeExecution context, ExecutionAttributes executionAttributes) {
        final Object operationName = executionAttributes.getAttributes().get(OPERATION_NAME);
        final Object serviceName = executionAttributes.getAttributes().get(SERVICE_NAME);
        log.info("START - {}.{} {}",
               serviceName, operationName,
                context.request());
        executionAttributes.putAttribute(START_TIME, System.currentTimeMillis());
    }

    @Override
    public void afterExecution(Context.AfterExecution context, ExecutionAttributes executionAttributes) {
        Long startTime = executionAttributes.getAttribute(START_TIME);
        Long elapsed = startTime != null ? System.currentTimeMillis() - startTime : null;

        final Object operationName = executionAttributes.getAttributes().get(OPERATION_NAME);
        final Object serviceName = executionAttributes.getAttributes().get(SERVICE_NAME);

        if (context.response() instanceof ScanResponse) {
            ScanResponse scanResponse = (ScanResponse) context.response();
            log.info("END - {}.{} request: {} count: {} timelapse: {} ms",serviceName, operationName, context.request(),
                    scanResponse.count(), elapsed);
        } else if (context.response() instanceof QueryResponse) {
            QueryResponse queryResponse = (QueryResponse) context.response();
            log.info("END - {}.{} request: {} count: {} timelapse: {} ms",serviceName, operationName, context.request(),
                    queryResponse.count(), elapsed);
        } else if (context.response() instanceof GetItemResponse) {
            GetItemResponse getItemResponse = (GetItemResponse) context.response();
            log.info("END - {}.{} request: {} hasItem: {} response: {} timelapse: {} ms",serviceName, operationName,
                    context.request(), getItemResponse.hasItem(), context.response(), elapsed);
        } else if (context.response() instanceof PutItemResponse
                || context.response() instanceof UpdateItemResponse
                || context.response() instanceof DeleteItemResponse) {
            log.info("END - {}.{} request: {} timelapse: {} ms",serviceName, operationName, context.request(), elapsed);
        } else if (context.response() instanceof BatchGetItemResponse) {
            BatchGetItemResponse batchGetItemResponse = (BatchGetItemResponse) context.response();
            log.info("END - {}.{} request: {} hasUnprocessedKeys: {} timelapse: {} ms",serviceName, operationName,
                    context.request(), batchGetItemResponse.hasUnprocessedKeys(), elapsed);
        } else if (context.response() instanceof BatchWriteItemResponse) {
            BatchWriteItemResponse batchWriteItemResponse = (BatchWriteItemResponse) context.response();
            log.info("END - {}.{} request: {} hasUnprocessedItems: {} timelapse: {} ms",serviceName, operationName,
                    context.request(), batchWriteItemResponse.hasUnprocessedItems(), elapsed);
        }
    }

    @Override
    public void onExecutionFailure(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
        final Object serviceName = executionAttributes.getAttributes().get(SERVICE_NAME);
        final Object operationName = executionAttributes.getAttributes().get(OPERATION_NAME);
        log.warn("{}.{}",
               serviceName, operationName,
                context.exception());
    }

}
