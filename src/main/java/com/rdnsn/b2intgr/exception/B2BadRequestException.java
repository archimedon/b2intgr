package com.rdnsn.b2intgr.exception;

import sun.awt.CausedFocusEvent;

public class B2BadRequestException extends Exception {
    public B2BadRequestException(String msg) {
        super(msg);
    }

    public B2BadRequestException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
