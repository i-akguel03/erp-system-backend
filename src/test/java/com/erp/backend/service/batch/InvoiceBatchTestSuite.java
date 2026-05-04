//TestSuiteRunner.java - Führt alle Tests in definierter Reihenfolge aus

package com.erp.backend.service.batch;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Invoice Batch System Test Suite")
@SelectClasses({
        // Unit Tests zuerst (schnell)
        InvoiceBatchAnalyzerTest.class,
        InvoiceBatchProcessorTest.class,
        InvoiceBatchOrchestratorTest.class,

        // Integration Tests danach (langsam)
        InvoiceBatchAnalyzerIntegrationTest.class,
        InvoiceBatchIntegrationTest.class
})
public class InvoiceBatchTestSuite {
    // Leere Klasse - nur Annotation wichtig
}