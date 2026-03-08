package at.sfischer.testing.systems;

import at.sfischer.testing.SystemStartedCondition;
import at.sfischer.testing.TestSystemRunner;
import at.sfischer.testing.gradle.GradleProject;

import java.io.File;

public class PayrollApplication implements TestSystemRunner {

    private final GradleProject project;

    public PayrollApplication() {
        File testSystemProject = TestSystemRunner.findProjectDir("test-systems/Rest/SpringPayrollTestSystem");
        project = new GradleProject("8.5", testSystemProject);
    }

    @Override
    public boolean start() {
        SystemStartedCondition systemStartedCondition = new SystemStartedCondition() {
            @Override
            public void receivedStdOutLine(String line) {
                System.out.print(line);
                if(line.contains(": Started PayrollApplication in ")){
                    setSystemIsStarted();
                }
            }

            @Override
            public void receivedStdErrLine(String line) {
                // Nothing to do.
                System.err.print(line);
            }
        };

        project.runProject(systemStartedCondition);
        systemStartedCondition.waitForSystemStart();
        return systemStartedCondition.isSystemStarted();
    }

    @Override
    public boolean stop() {
        project.stopProject();
        return true;
    }
}
