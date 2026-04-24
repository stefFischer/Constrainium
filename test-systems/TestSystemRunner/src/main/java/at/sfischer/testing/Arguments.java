package at.sfischer.testing;

import java.io.File;

public abstract class Arguments {

    public static final String OTEL_DEBUG_LOGS = "-Dotel.traces.exporter=logging;-Dotel.javaagent.debug=true";

    static File findProjectRoot() {
        File dir = new File(System.getProperty("user.dir"));
        while (dir != null) {
            if (new File(dir, "settings.gradle").exists() ||
                    new File(dir, "settings.gradle.kts").exists()) {
                return dir;
            }
            dir = dir.getParentFile();
        }
        throw new IllegalStateException("Could not find project root");
    }

    public static String getOtelJavaAgentPath(){
        String relativeFromRoot = "test-systems/resources/opentelemetry-javaagent.jar";
        File rootDir = findProjectRoot();
        return new File(rootDir, relativeFromRoot).getAbsolutePath();
    }
}
