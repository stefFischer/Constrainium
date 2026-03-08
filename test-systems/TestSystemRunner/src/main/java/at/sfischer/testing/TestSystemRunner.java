package at.sfischer.testing;

import java.io.File;

public interface TestSystemRunner {

    boolean start();

    boolean stop();

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

    static File findProjectDir(String projectPath){
        File rootDir = findProjectRoot();
        return new File(rootDir, projectPath);
    }
}
