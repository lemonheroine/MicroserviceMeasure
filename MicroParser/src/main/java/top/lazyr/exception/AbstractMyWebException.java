package top.lazyr.exception;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;


/**
 * Base exception of the project.
 * @author lazyr
 * @created 2021/5/10
 */


public abstract class AbstractMyWebException extends RuntimeException {

    /**
     * Error errorData.
     */
    private Object errorData;

    public AbstractMyWebException(String message) {
        super(message);
    }

    public AbstractMyWebException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Http status code
     *
     * @return {@link HttpStatus}
     */
    @NonNull
    public abstract HttpStatus getStatus();

    @Nullable
    public Object getErrorData() {
        return errorData;
    }

    /**
     * Sets error errorData.
     *
     * @param errorData error data
     * @return current exception.
     */
    @NonNull
    public AbstractMyWebException setErrorData(@Nullable Object errorData) {
        this.errorData = errorData;
        return this;
    }
}
