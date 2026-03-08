package at.sfischer.testing.gradle;

import at.sfischer.testing.SystemStartedCondition;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GradleProject {

    private static final Logger LOGGER = Logger.getLogger(GradleProject.class.getCanonicalName());

    private GradleConnector gradleConnector;

    private final String gradleVersion;
    private final File projectDir;

    private final String bootTask;

    public GradleProject(String gradleVersion, File projectDir) {
        this(gradleVersion, projectDir, "run");
    }

    public GradleProject(String gradleVersion, File projectDir, String bootTask) {
        this.gradleVersion = gradleVersion;
        this.projectDir = projectDir;
        this.bootTask = bootTask;
    }

    public void runProject(SystemStartedCondition systemStartedCondition){
        Thread t = new Thread(() -> {
            gradleConnector = GradleConnector.newConnector().useGradleVersion(gradleVersion);
            try (ProjectConnection project = gradleConnector.forProjectDirectory(projectDir).connect()) {
                project.newBuild().forTasks(bootTask)
                        .setStandardOutput(systemStartedCondition.getStdOut())
                        .setStandardError(systemStartedCondition.getStdErr())
                        .run();

            } catch (GradleConnectionException | IllegalStateException e) {
                systemStartedCondition.systemStartFailed();
                LOGGER.log(Level.SEVERE, "Project did not start correctly.", e);
            }
        });
        t.start();

        systemStartedCondition.waitForSystemStart();
    }

    public void stopProject(){
        gradleConnector.disconnect();
    }
}
