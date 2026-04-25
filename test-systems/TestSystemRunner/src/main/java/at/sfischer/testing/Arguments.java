package at.sfischer.testing;

import java.io.File;

public abstract class Arguments {

    public static final String OTEL_DEBUG_TRUE = "-Dotel.javaagent.debug=true";

    public static final String OTEL_EXPORTER_LOGGING = "-Dotel.traces.exporter=logging";

    public static final String OTEL_EXPORTER_OTLP = "-Dotel.traces.exporter=otlp";
    public static final String OTEL_EXPORTER_OTLP_PROTOCOL = "-Dotel.exporter.otlp.protocol=grpc";


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

    public static String getOtelJavaAgentArgument(){
        String relativeFromRoot = "test-systems/resources/opentelemetry-javaagent.jar";
        File rootDir = findProjectRoot();
        return "-javaagent:" + new File(rootDir, relativeFromRoot).getAbsolutePath();
    }

    public static String getOtlpExportEndpointArgument(String endpoint){
        return "-Dotel.exporter.otlp.endpoint=" + endpoint;
    }

    public static String getServiceNameArgument(String serviceName){
        return "-Dotel.service.name=" + serviceName;
    }

    public static String getServiceNameAttributeArgument(String serviceName){
        return "-Dotel.resource.attributes=service.name=" + serviceName;
    }
}
