package com.geeekr.db;


public class DBException extends RuntimeException {
    private static final long serialVersionUID = 6300888107599855908L;

    public DBException(Exception e) {
        e.printStackTrace();
    }

}
