package org.greenblitz.motion.profiling.exception;

public class ProfilingException extends Exception {

    public ProfilingException() {
        super();
    }

    public ProfilingException(String message) {
        super(message);
    }

    public ProfilingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProfilingException(Throwable cause) {
        super(cause);
    }

    protected ProfilingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}