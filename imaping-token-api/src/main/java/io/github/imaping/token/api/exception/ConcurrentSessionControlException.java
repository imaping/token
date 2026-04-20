package io.github.imaping.token.api.exception;

/**
 * 并发会话控制异常。
 */
public class ConcurrentSessionControlException extends RuntimeException {

    private static final long serialVersionUID = -5603153895679416408L;

    public ConcurrentSessionControlException(final String message) {
        super(message);
    }

    public ConcurrentSessionControlException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

