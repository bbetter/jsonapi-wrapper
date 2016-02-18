package com.teamvoy.jsonapi;

/**
 * Created by mac on 18.02.16.
 */
public class JsonApiException extends Exception{
    String level;
    String message;

    public JsonApiException(String level, String message) {
        this.level = level;
        this.message = message;
    }
}
