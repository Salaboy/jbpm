/*
 * Copyright 2013 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jbpm.executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.EntityManagerFactory;

import org.jbpm.executor.impl.jpa.ExecutorJPAAuditService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ErrorInfo;
import org.kie.api.executor.ExecutionResults;
import org.kie.api.executor.ExecutorService;
import org.kie.api.executor.RequestInfo;
import org.kie.api.executor.STATUS;
import org.kie.api.runtime.query.QueryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BasicExecutorBaseTest {
    
    private static final Logger logger = LoggerFactory.getLogger(BasicExecutorBaseTest.class);

    
    protected ExecutorService executorService;
    public static final Map<String, Object> cachedEntities = new HashMap<String, Object>();
    
    protected EntityManagerFactory emf = null;
    
    @Before
    public void setUp() {
        executorService.setThreadPoolSize(1);
        executorService.setInterval(3);
    }

    @After
    public void tearDown() {
        executorService.clearAllRequests();
        executorService.clearAllErrors();
        
        System.clearProperty("org.kie.executor.msg.length");
    	System.clearProperty("org.kie.executor.stacktrace.length");
    }

    @Test
    public void simpleExecutionTest() throws InterruptedException {
        CommandContext ctxCMD = new CommandContext();
        ctxCMD.setData("businessKey", UUID.randomUUID().toString());

        executorService.scheduleRequest("org.jbpm.executor.commands.PrintOutCommand", ctxCMD);

        Thread.sleep(10000);

        List<RequestInfo> inErrorRequests = executorService.getInErrorRequests(new QueryContext());
        assertEquals(0, inErrorRequests.size());
        List<RequestInfo> queuedRequests = executorService.getQueuedRequests(new QueryContext());
        assertEquals(0, queuedRequests.size());
        List<RequestInfo> executedRequests = executorService.getCompletedRequests(new QueryContext());
        assertEquals(1, executedRequests.size());


    }

    @Test
    public void callbackTest() throws InterruptedException {

        CommandContext commandContext = new CommandContext();
        commandContext.setData("businessKey", UUID.randomUUID().toString());
        cachedEntities.put((String) commandContext.getData("businessKey"), new AtomicLong(1));

        commandContext.setData("callbacks", "org.jbpm.executor.SimpleIncrementCallback");
        executorService.scheduleRequest("org.jbpm.executor.commands.PrintOutCommand", commandContext);

        Thread.sleep(10000);

        List<RequestInfo> inErrorRequests = executorService.getInErrorRequests(new QueryContext());
        assertEquals(0, inErrorRequests.size());
        List<RequestInfo> queuedRequests = executorService.getQueuedRequests(new QueryContext());
        assertEquals(0, queuedRequests.size());
        List<RequestInfo> executedRequests = executorService.getCompletedRequests(new QueryContext());
        assertEquals(1, executedRequests.size());

        assertEquals(2, ((AtomicLong) cachedEntities.get((String) commandContext.getData("businessKey"))).longValue());

    }
    
    @Test
    public void addAnotherCallbackTest() throws InterruptedException {

        CommandContext commandContext = new CommandContext();
        commandContext.setData("businessKey", UUID.randomUUID().toString());
        cachedEntities.put((String) commandContext.getData("businessKey"), new AtomicLong(1));

        commandContext.setData("callbacks", "org.jbpm.executor.SimpleIncrementCallback");
        executorService.scheduleRequest("org.jbpm.executor.test.AddAnotherCallbackCommand", commandContext);

        Thread.sleep(10000);

        List<RequestInfo> inErrorRequests = executorService.getInErrorRequests(new QueryContext());
        assertEquals(0, inErrorRequests.size());
        List<RequestInfo> queuedRequests = executorService.getQueuedRequests(new QueryContext());
        assertEquals(0, queuedRequests.size());
        List<RequestInfo> executedRequests = executorService.getCompletedRequests(new QueryContext());
        assertEquals(1, executedRequests.size());

        assertEquals(2, ((AtomicLong) cachedEntities.get((String) commandContext.getData("businessKey"))).longValue());

        ExecutionResults results = null;
        byte[] responseData = executedRequests.get(0).getResponseData();
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new ByteArrayInputStream(responseData));
            results = (ExecutionResults) in.readObject();
        } catch (Exception e) {                        
            logger.warn("Exception while serializing context data", e);
            return;
        } finally {
            if (in != null) {
                try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
            }
        }
        
        String result = (String)results.getData("custom");
        assertNotNull(result);
        assertEquals("custom callback invoked", result);
    }
    
    @Test
    public void multipleCallbackTest() throws InterruptedException {

        CommandContext commandContext = new CommandContext();
        commandContext.setData("businessKey", UUID.randomUUID().toString());
        cachedEntities.put((String) commandContext.getData("businessKey"), new AtomicLong(1));

        commandContext.setData("callbacks", "org.jbpm.executor.SimpleIncrementCallback, org.jbpm.executor.test.CustomCallback");
        executorService.scheduleRequest("org.jbpm.executor.commands.PrintOutCommand", commandContext);

        Thread.sleep(10000);

        List<RequestInfo> inErrorRequests = executorService.getInErrorRequests(new QueryContext());
        assertEquals(0, inErrorRequests.size());
        List<RequestInfo> queuedRequests = executorService.getQueuedRequests(new QueryContext());
        assertEquals(0, queuedRequests.size());
        List<RequestInfo> executedRequests = executorService.getCompletedRequests(new QueryContext());
        assertEquals(1, executedRequests.size());

        assertEquals(2, ((AtomicLong) cachedEntities.get((String) commandContext.getData("businessKey"))).longValue());

        ExecutionResults results = null;
        byte[] responseData = executedRequests.get(0).getResponseData();
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new ByteArrayInputStream(responseData));
            results = (ExecutionResults) in.readObject();
        } catch (Exception e) {                        
            logger.warn("Exception while serializing context data", e);
            return;
        } finally {
            if (in != null) {
                try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
            }
        }
        
        String result = (String)results.getData("custom");
        assertNotNull(result);
        assertEquals("custom callback invoked", result);
    }

    @Test
    public void executorExceptionTest() throws InterruptedException {

        CommandContext commandContext = new CommandContext();
        commandContext.setData("businessKey", UUID.randomUUID().toString());
        cachedEntities.put((String) commandContext.getData("businessKey"), new AtomicLong(1));

        commandContext.setData("callbacks", "org.jbpm.executor.SimpleIncrementCallback");
        commandContext.setData("retries", 0);
        executorService.scheduleRequest("org.jbpm.executor.ThrowExceptionCommand", commandContext);
        logger.info("{} Sleeping for 10 secs", System.currentTimeMillis());
        Thread.sleep(10000);

        List<RequestInfo> inErrorRequests = executorService.getInErrorRequests(new QueryContext());
        assertEquals(1, inErrorRequests.size());
        logger.info("Error: {}", inErrorRequests.get(0));

        List<ErrorInfo> errors = executorService.getAllErrors(new QueryContext());
        logger.info("Errors: {}", errors);
        assertEquals(1, errors.size());


    }

    @Test
    public void defaultRequestRetryTest() throws InterruptedException {
        CommandContext ctxCMD = new CommandContext();
        ctxCMD.setData("businessKey", UUID.randomUUID().toString());

        executorService.scheduleRequest("org.jbpm.executor.ThrowExceptionCommand", ctxCMD);

        Thread.sleep(12000);



        List<RequestInfo> inErrorRequests = executorService.getInErrorRequests(new QueryContext());
        assertEquals(1, inErrorRequests.size());

        List<ErrorInfo> errors = executorService.getAllErrors(new QueryContext());
        logger.info("Errors: {}", errors);
        // Three retries means 4 executions in total 1(regular) + 3(retries)
        assertEquals(4, errors.size());

    }

    @Test
    public void cancelRequestTest() throws InterruptedException {

        //  The executor is on purpose not started to not fight against race condition 
        // with the request cancelations.
        CommandContext ctxCMD = new CommandContext();
        String businessKey = UUID.randomUUID().toString();
        ctxCMD.setData("businessKey", businessKey);

        Long requestId = executorService.scheduleRequest("org.jbpm.executor.commands.PrintOutCommand", ctxCMD);
        
        List<RequestInfo> requests = executorService.getRequestsByBusinessKey(businessKey, new QueryContext());
        assertNotNull(requests);
        assertEquals(1, requests.size());
        assertEquals(requestId, requests.get(0).getId());

        // cancel the task immediately
        executorService.cancelRequest(requestId);

        List<RequestInfo> cancelledRequests = executorService.getCancelledRequests(new QueryContext());
        assertEquals(1, cancelledRequests.size());

    }
    
    @Test
    public void executorExceptionTrimmingTest() throws InterruptedException {
    	System.setProperty("org.kie.executor.msg.length", "10");
    	System.setProperty("org.kie.executor.stacktrace.length", "20");
        CommandContext commandContext = new CommandContext();
        commandContext.setData("businessKey", UUID.randomUUID().toString());
        cachedEntities.put((String) commandContext.getData("businessKey"), new AtomicLong(1));

        commandContext.setData("callbacks", "org.jbpm.executor.SimpleIncrementCallback");
        commandContext.setData("retries", 0);
        executorService.scheduleRequest("org.jbpm.executor.ThrowExceptionCommand", commandContext);
        logger.info("{} Sleeping for 10 secs", System.currentTimeMillis());
        Thread.sleep(10000);

        List<RequestInfo> inErrorRequests = executorService.getInErrorRequests(new QueryContext());
        assertEquals(1, inErrorRequests.size());
        logger.info("Error: {}", inErrorRequests.get(0));

        List<ErrorInfo> errors = executorService.getAllErrors(new QueryContext());
        logger.info("Errors: {}", errors);
        assertEquals(1, errors.size());
        
        ErrorInfo error = errors.get(0);
        
        assertEquals(10, error.getMessage().length());
        assertEquals(20, error.getStacktrace().length());


    }
    
    @Test
    public void reoccurringExecutionTest() throws InterruptedException {
        CommandContext ctxCMD = new CommandContext();
        ctxCMD.setData("businessKey", UUID.randomUUID().toString());

        executorService.scheduleRequest("org.jbpm.executor.commands.ReoccurringPrintOutCommand", ctxCMD);

        Thread.sleep(9000);

        List<RequestInfo> inErrorRequests = executorService.getInErrorRequests(new QueryContext());
        assertEquals(0, inErrorRequests.size());
        List<RequestInfo> queuedRequests = executorService.getQueuedRequests(new QueryContext());
        assertEquals(1, queuedRequests.size());
        List<RequestInfo> executedRequests = executorService.getCompletedRequests(new QueryContext());
        assertEquals(3, executedRequests.size());


    }
    
    @Test
    public void cleanupLogExecutionTest() throws InterruptedException {
        CommandContext ctxCMD = new CommandContext();
        ctxCMD.setData("businessKey", UUID.randomUUID().toString());
        
        Long requestId = executorService.scheduleRequest("org.jbpm.executor.commands.ReoccurringPrintOutCommand", ctxCMD);

        Thread.sleep(9000);

        List<RequestInfo> inErrorRequests = executorService.getInErrorRequests(new QueryContext());
        assertEquals(0, inErrorRequests.size());
        List<RequestInfo> queuedRequests = executorService.getQueuedRequests(new QueryContext());
        assertEquals(1, queuedRequests.size());
        List<RequestInfo> executedRequests = executorService.getCompletedRequests(new QueryContext());
        assertEquals(3, executedRequests.size());
        
        executorService.cancelRequest(requestId+3);
        
        List<RequestInfo> canceled = executorService.getCancelledRequests(new QueryContext());
        
        ExecutorJPAAuditService auditService = new ExecutorJPAAuditService(emf);
        int resultCount = auditService.requestInfoLogDeleteBuilder()
                .date(canceled.get(0).getTime())
                .status(STATUS.ERROR)
                .build()
                .execute();
        
        assertEquals(0, resultCount);
        
        resultCount = auditService.errorInfoLogDeleteBuilder()
                .date(canceled.get(0).getTime())
                .build()
                .execute();
        
        assertEquals(0, resultCount);

        ctxCMD = new CommandContext();
        ctxCMD.setData("businessKey", UUID.randomUUID().toString());
        ctxCMD.setData("SingleRun", "true");
        ctxCMD.setData("EmfName", "org.jbpm.executor");
        ctxCMD.setData("SkipProcessLog", "true");
        ctxCMD.setData("SkipTaskLog", "true");
        executorService.scheduleRequest("org.jbpm.executor.commands.LogCleanupCommand", ctxCMD);
        
        Thread.sleep(5000);
        
        inErrorRequests = executorService.getInErrorRequests(new QueryContext());
        assertEquals(0, inErrorRequests.size());
        queuedRequests = executorService.getQueuedRequests(new QueryContext());
        assertEquals(0, queuedRequests.size());
        executedRequests = executorService.getCompletedRequests(new QueryContext());
        assertEquals(1, executedRequests.size());
    }

    @Test
    public void testCustomConstantRequestRetry() throws InterruptedException {
        CommandContext ctxCMD = new CommandContext();
        ctxCMD.setData("businessKey", UUID.randomUUID().toString());
        ctxCMD.setData("retryDelay", "5s");
        ctxCMD.setData("retries", 2);

        executorService.scheduleRequest("org.jbpm.executor.ThrowExceptionCommand", ctxCMD);

        Thread.sleep(16000);

        List<RequestInfo> inErrorRequests = executorService.getInErrorRequests(new QueryContext());
        assertEquals(1, inErrorRequests.size());

        List<ErrorInfo> errors = executorService.getAllErrors(new QueryContext());
        // Three retries means 4 executions in total 1(regular) + 2(retries)
        assertEquals(3, errors.size());
        
        long firstError = errors.get(0).getTime().getTime();
        long secondError = errors.get(1).getTime().getTime();
        long thirdError = errors.get(2).getTime().getTime();

        // time difference between first and second should be at least 3 seconds
        long diff = secondError - firstError;
        assertTrue(diff > 5000);
        // time difference between second and third should be at least 6 seconds
        diff = thirdError - secondError;
        assertTrue(diff > 5000);

    }
    
    @Test
    public void testCustomIncrementingRequestRetry() throws InterruptedException {
        CommandContext ctxCMD = new CommandContext();
        ctxCMD.setData("businessKey", UUID.randomUUID().toString());
        ctxCMD.setData("retryDelay", "3s, 6s");
        ctxCMD.setData("retries", 2);

        executorService.scheduleRequest("org.jbpm.executor.ThrowExceptionCommand", ctxCMD);

        Thread.sleep(20000);

        List<RequestInfo> inErrorRequests = executorService.getInErrorRequests(new QueryContext());
        assertEquals(1, inErrorRequests.size());

        List<ErrorInfo> errors = executorService.getAllErrors(new QueryContext());
        // Three retries means 4 executions in total 1(regular) + 3(retries)
        assertEquals(3, errors.size());
        
        long firstError = errors.get(0).getTime().getTime();
        long secondError = errors.get(1).getTime().getTime();
        long thirdError = errors.get(2).getTime().getTime();

        // time difference between first and second should be at least 3 seconds
        long diff = secondError - firstError;
        assertTrue(diff > 3000);
        // time difference between second and third should be at least 6 seconds
        diff = thirdError - secondError;
        assertTrue(diff > 6000);
    }

    @Test
    public void testCustomIncrementingRequestRetrySpecialValues() throws InterruptedException {
        CommandContext ctxCMD = new CommandContext();
        ctxCMD.setData("businessKey", UUID.randomUUID().toString());
        ctxCMD.setData("retryDelay", "-1ms, 1m 80s");
        ctxCMD.setData("retries", 2);

        executorService.scheduleRequest("org.jbpm.executor.ThrowExceptionCommand", ctxCMD);

        Thread.sleep(10000);

        List<ErrorInfo> errors = executorService.getAllErrors(new QueryContext());
        // 2 executions in total 1(regular) + 1(retry)
        assertEquals(2, errors.size());

        long firstError = errors.get(0).getTime().getTime();
        long secondError = errors.get(1).getTime().getTime();

        // Time difference between first and second shouldn't be bigger than 4 seconds as executor has 3 second interval and
        // should start executing second command immediately.
        long diff = secondError - firstError;
        assertTrue(diff < 4000);

        List<RequestInfo> allRequests = executorService.getAllRequests(new QueryContext());
        assertEquals(1, allRequests.size());

        // Future execution is planned to be started 2 minutes and 20 seconds after last fail.
        // Time difference vary because of test thread sleeping for 10 seconds.
        diff = allRequests.get(0).getTime().getTime() - Calendar.getInstance().getTimeInMillis();
        assertTrue(diff < 140000);
        assertTrue(diff > 130000);

        executorService.clearAllRequests();
    }
    
    @Test
    public void cancelRequestWithSearchByCommandTest() throws InterruptedException {

        CommandContext ctxCMD = new CommandContext();
        String businessKey = UUID.randomUUID().toString();
        ctxCMD.setData("businessKey", businessKey);

        Long requestId = executorService.scheduleRequest("org.jbpm.executor.test.CustomCommand", ctxCMD);
        
        List<RequestInfo> requests = executorService.getRequestsByCommand("org.jbpm.executor.test.CustomCommand", new QueryContext());
        assertNotNull(requests);
        assertEquals(1, requests.size());
        assertEquals(requestId, requests.get(0).getId());

        // cancel the task immediately
        executorService.cancelRequest(requestId);

        List<RequestInfo> cancelledRequests = executorService.getCancelledRequests(new QueryContext());
        assertEquals(1, cancelledRequests.size());

    }

    @Test
    public void executorPagingTest() throws InterruptedException {
        CommandContext ctxCMD = new CommandContext();
        String businessKey = UUID.randomUUID().toString();
        ctxCMD.setData("businessKey", businessKey);

        Long requestId1 = executorService.scheduleRequest("org.jbpm.executor.test.CustomCommand", ctxCMD);
        Long requestId2 = executorService.scheduleRequest("org.jbpm.executor.test.CustomCommand", ctxCMD);

        QueryContext queryContextFirstPage = new QueryContext(0, 1);
        QueryContext queryContextSecondPage = new QueryContext(1, 1);

        List<RequestInfo> firstRequests = executorService.getRequestsByCommand("org.jbpm.executor.test.CustomCommand", queryContextFirstPage);
        List<RequestInfo> secondRequests = executorService.getRequestsByCommand("org.jbpm.executor.test.CustomCommand", queryContextSecondPage);
        compareRequestsAreNotSame(firstRequests.get(0), secondRequests.get(0));

        firstRequests = executorService.getRequestsByBusinessKey(businessKey, queryContextFirstPage);
        secondRequests = executorService.getRequestsByBusinessKey(businessKey, queryContextSecondPage);
        compareRequestsAreNotSame(firstRequests.get(0), secondRequests.get(0));

        firstRequests = executorService.getQueuedRequests(queryContextFirstPage);
        secondRequests = executorService.getQueuedRequests(queryContextSecondPage);
        compareRequestsAreNotSame(firstRequests.get(0), secondRequests.get(0));

        // cancel the task immediately
        executorService.cancelRequest(requestId1);
        executorService.cancelRequest(requestId2);

        firstRequests = executorService.getCancelledRequests(queryContextFirstPage);
        secondRequests = executorService.getCancelledRequests(queryContextSecondPage);
        compareRequestsAreNotSame(firstRequests.get(0), secondRequests.get(0));

        firstRequests = executorService.getAllRequests(queryContextFirstPage);
        secondRequests = executorService.getAllRequests(queryContextSecondPage);
        compareRequestsAreNotSame(firstRequests.get(0), secondRequests.get(0));

        // Setting too far page
        QueryContext queryContextBigOffset = new QueryContext(10, 1);
        List<RequestInfo> offsetRequests = executorService.getCancelledRequests(queryContextBigOffset);
        assertNotNull(offsetRequests);
        assertEquals(0, offsetRequests.size());
    }

    @Test
    public void clearAllRequestsTest() throws InterruptedException {
        CommandContext ctxCMD = new CommandContext();
        String businessKey = UUID.randomUUID().toString();
        ctxCMD.setData("businessKey", businessKey);

        // Testing clearing of active request.
        Long requestId = executorService.scheduleRequest("org.jbpm.executor.test.CustomCommand", ctxCMD);

        List<RequestInfo> allRequests = executorService.getAllRequests(new QueryContext());
        assertEquals(1, allRequests.size());

        executorService.clearAllRequests();

        allRequests = executorService.getAllRequests(new QueryContext());
        assertEquals(0, allRequests.size());

        // Testing clearing of cancelled request.
        requestId = executorService.scheduleRequest("org.jbpm.executor.test.CustomCommand", ctxCMD);

        allRequests = executorService.getAllRequests(new QueryContext());
        assertEquals(1, allRequests.size());

        executorService.cancelRequest(requestId);
        executorService.clearAllRequests();

        allRequests = executorService.getAllRequests(new QueryContext());
        assertEquals(0, allRequests.size());
    }

    private void compareRequestsAreNotSame(RequestInfo firstRequest, RequestInfo secondRequest) {
        assertNotNull(firstRequest);
        assertNotNull(secondRequest);
        assertNotEquals("Requests are same!", firstRequest.getId(), secondRequest.getId());
    }
    
    public void FIXMEfutureRequestTest() throws InterruptedException {
        CommandContext ctxCMD = new CommandContext();
        ctxCMD.setData("businessKey", UUID.randomUUID().toString());

        Long requestId = executorService.scheduleRequest("org.jbpm.executor.commands.PrintOutCommand", new Date(new Date().getTime() + 10000), ctxCMD);
        assertNotNull(requestId);
        Thread.sleep(5000);
        
        List<RequestInfo> runningRequests = executorService.getRunningRequests(new QueryContext());
        assertEquals(0, runningRequests.size());
        
        List<RequestInfo> futureQueuedRequests = executorService.getFutureQueuedRequests(new QueryContext());
        assertEquals(1, futureQueuedRequests.size());
        
        Thread.sleep(10000);
        
        List<RequestInfo> completedRequests = executorService.getCompletedRequests(new QueryContext());
        assertEquals(1, completedRequests.size());
    }
    
}
