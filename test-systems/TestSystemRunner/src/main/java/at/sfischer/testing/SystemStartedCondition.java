package at.sfischer.testing;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

public abstract class SystemStartedCondition {

    private static class StreamObserver extends OutputStream {

        private ByteBuffer buffer = ByteBuffer.allocate(1024);

        private final StreamObserverListener listener;

        public StreamObserver(StreamObserverListener listener) {
            this.listener = listener;
        }

        @Override
        public void write(int b) throws IOException {
            try {
                buffer.put((byte)b);
            } catch(BufferOverflowException overflowException){
                ByteBuffer newBB = ByteBuffer.allocate(buffer.capacity() * 2);
                newBB.put(buffer.array(), buffer.arrayOffset(), buffer.position());
                buffer = newBB;
                write(b);
            }

            String s = new String(buffer.array(), buffer.arrayOffset(), buffer.position());
            if(s.endsWith("\n")){
                listener.lineReceived(s);
                buffer.clear();
            }
        }
    }

    private interface StreamObserverListener{
        void lineReceived(String line);
    }

    private final StreamObserver stdOut;
    private final StreamObserver stdErr;

    private boolean systemIsStarted = false;

    private boolean systemStartFailed = false;

    public SystemStartedCondition() {
        this.stdOut = new StreamObserver(this::receivedStdOutLine);
        this.stdErr = new StreamObserver(this::receivedStdErrLine);
    }

    public boolean isSystemStarted(){
        return systemIsStarted;
    }

    protected void setSystemIsStarted() {
        this.systemIsStarted = true;
    }

    public abstract void receivedStdOutLine(String line);

    public abstract void receivedStdErrLine(String line);

    public OutputStream getStdOut() {
        return stdOut;
    }

    public OutputStream getStdErr() {
        return stdErr;
    }

    public void systemStartFailed(){
        this.systemStartFailed = true;
    }

    public void waitForSystemStart(){
        while(!systemIsStarted && !systemStartFailed){
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
