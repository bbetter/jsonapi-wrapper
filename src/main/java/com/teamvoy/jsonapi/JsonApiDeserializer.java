package com.teamvoy.jsonapi;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * Created by mac on 17.02.16.
 * deserialize json api style to object
 * compatible with Retrofit
 */
public class JsonApiDeserializer<T> implements JsonDeserializer<T> {
    @Override
    public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            return new Gson().fromJson(JsonApiTransformer.transform(json),typeOfT);
        } catch (JsonApiException e) {
            e.printStackTrace();
            return null;
        }
    }
}
