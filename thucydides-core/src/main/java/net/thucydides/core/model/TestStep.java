package net.thucydides.core.model;

import static ch.lambdaj.Lambda.extract;
import static ch.lambdaj.Lambda.join;
import static ch.lambdaj.Lambda.on;
import static net.thucydides.core.model.TestResult.FAILURE;
import static net.thucydides.core.model.TestResult.IGNORED;
import static net.thucydides.core.model.TestResult.PENDING;
import static net.thucydides.core.model.TestResult.SKIPPED;
import static net.thucydides.core.model.TestResult.SUCCESS;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.inject.internal.ImmutableList;

/**
 * An acceptance test run is made up of test steps.
 * Test steps can be either concrete steps or groups of steps.
 * Each concrete step should represent an action by the user, and (generally) an expected outcome.
 * A test step is described by a narrative-style phrase (e.g. "the user clicks 
 * on the 'Search' button', "the user fills in the registration form', etc.).
 * A screenshot is stored for each step.
 * 
 * @author johnsmart
 *
 */
public class TestStep {

    private String description;    
    private long duration;
    private long startTime;
    private String screenshotPath;
    private File screenshot;
    private File htmlSource;
    private String errorMessage;
    private Throwable cause;
    private TestResult result;

    private List<TestStep> children = new ArrayList<TestStep>();

    public TestStep() {
        startTime = System.currentTimeMillis();
    }


    @Override
    public String toString() {
        if (!hasChildren()) {
            return description;
        } else {
            String childDescriptions = join(extract(children, on(TestStep.class).toString()));
            return description + " [" + childDescriptions + "]";
        }
    }

    public TestStep(final String description) {
        this();
        this.description = description;
    }

    public void recordDuration() {
        setDuration(System.currentTimeMillis() - startTime);
    }
    
    public void setDescription(final String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public List<TestStep> getChildren() {
        return ImmutableList.copyOf(children);
    }
    /**
     * Each test step can be associated with a screenshot.
     */
    public void setScreenshot(final File screenshot) {
        this.screenshot = screenshot;
    }

    public File getScreenshot() {
        return screenshot;
    }

    public void setScreenshotPath(final String screenshotPath) {
        this.screenshotPath = screenshotPath;
    }

    public String getScreenshotPath() {
        return screenshotPath;
    }

    public String getScreenshotPage() {
        if (screenshot != null) {
            return "screenshot_" + withoutType(screenshot.getName()) + ".html";
        } else {
            return "";
        }
    }

    private String withoutType(final String screenshot) {
        int dot = screenshot.lastIndexOf('.');
        return screenshot.substring(0, dot);
    }

    public File getHtmlSource() {
        return htmlSource;
    }

    public void setHtmlSource(final File htmlSource) {
        this.htmlSource = htmlSource;
    }

    /**
     * Each test step has a result, indicating the outcome of this step.
     */
    public void setResult(final TestResult result) {
        this.result = result;
    }

    public TestResult getResult() {
        if (isAGroup() && !groupResultOverridesChildren()) {
            return getResultFromChildren();
        } else {
            return getResultFromThisStep();
        }
    }

    private TestResult getResultFromThisStep() {
        if (result != null) {
            return result;
        } else {
            return TestResult.PENDING;
        }
    }

    private boolean groupResultOverridesChildren() {
        return ((result == SKIPPED) || (result == IGNORED) || (result == PENDING));
    }

    private TestResult getResultFromChildren() {
        TestResultList resultList = new TestResultList(getChildResults());
        return resultList.getOverallResult();
    }

    private List<TestResult> getChildResults() {
        List<TestResult> results = new ArrayList<TestResult>();
        for (TestStep step : getChildren()) {
            results.add(step.getResult());
        }
        return results;
    }

    public Boolean isSuccessful() {
        return getResult() == SUCCESS;
    }

    public Boolean isFailure() {
        return  getResult() == FAILURE;
    }

    public Boolean isIgnored() {
        return  getResult() == IGNORED;
    }

    public Boolean isSkipped() {
        return  getResult() == SKIPPED;
    }

    public Boolean isPending() {
        return  getResult() == PENDING;
    }

    public void setDuration(final long duration) {
        this.duration = duration;
    }

    public long getDuration() {
        return duration;
    }

    /**
     * Indicate that this step failed with a given error.
     */
    public void failedWith(final String message, final Throwable exception) {
        setResult(TestResult.FAILURE);
        this.errorMessage = message;
        this.cause = exception;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Throwable getException() {
        return cause;
    }

    public List<? extends TestStep> getFlattenedSteps() {
        List<TestStep> flattenedSteps = new ArrayList<TestStep>();
        for(TestStep child : getChildren()) {
            flattenedSteps.add(child);
            if (child.isAGroup()) {
                flattenedSteps.addAll(child.getFlattenedSteps());
            }
        }
        return flattenedSteps;
    }
    
    public boolean isAGroup() {
        return hasChildren();
    }

    public void addChildStep(final TestStep step) {
        children.add(step);
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public Collection<? extends TestStep> getLeafTestSteps() {
        List<TestStep> leafSteps = new ArrayList<TestStep>();
        for(TestStep child : getChildren()) {
            if (child.isAGroup()) {
                leafSteps.addAll(child.getLeafTestSteps());
            } else {
                leafSteps.add(child);
            }
        }
        return leafSteps;
    }
}
