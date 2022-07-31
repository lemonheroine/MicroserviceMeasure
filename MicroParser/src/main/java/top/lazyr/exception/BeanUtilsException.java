package top.lazyr.exception;

import org.springframework.http.HttpStatus;

/**
 * @author lazyr
 * @created 2021/5/10
 */
public class BeanUtilsException extends AbstractMyWebException{
    public BeanUtilsException(String message) {
        super(message);
    }

    public BeanUtilsException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
