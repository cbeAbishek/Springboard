package org.automation.listeners;

import org.automation.ui.BaseTest;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.automation.utils.DbMigrationUtil; // added

public class SuiteExecutionListener extends BaseTest implements ISuiteListener {

    @Override
    public void onStart(ISuite suite) {
        System.out.println("Suite Execution Started: " + suite.getName());
        // Ensure DB schema is present before any tests log results
        DbMigrationUtil.migrate();
    }

    @Override
    public void onFinish(ISuite suite) {
        System.out.println("Suite Execution Finished: " + suite.getName());

        // Take a screenshot of the last browser state at the end of suite
        if (getDriver() != null) {
            takeScreenshot("Suite_" + suite.getName() + "_END");
        }
    }
}
