package com.itsolut.mantis.core.exception;

/**
 * The <tt>MantisLocalException</tt> signals an error which has occured locally, rather than in the
 * task repository.
 * 
 * @author Robert Munteanu
 * 
 */
public class MantisLocalException extends MantisException {

    private static final long serialVersionUID = 1L;

    public MantisLocalException() {

    }

    public MantisLocalException(String message) {

        super(message);
    }

    public MantisLocalException(Throwable cause) {

        super(cause);
    }

    public MantisLocalException(String message, Throwable cause) {

        super(message, cause);
    }

}
