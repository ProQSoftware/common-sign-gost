package ru.i_novus.common.sign.exception;

public class CommonSignFailureException extends Exception {
    public CommonSignFailureException(Throwable cause) {
        super(cause);
    }
    public CommonSignFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}