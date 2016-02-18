package com.teamvoy.jsonapi;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class JsonApiTransformerUnitTests {

    private final Map<String,String> simpleTestCases =  new HashMap<String, String>() {{
        put("{\"name\":\"Andrew\",\"id\":\"1\",\"type\":\"person\"}",
                "{data:{'type':'person','id':'1',attributes:{'name':'Andrew'}}}");
        put("{\"name\":\"Andrew\",\"surname\":\"Puhach\",\"id\":\"1\",\"type\":\"person\"}",
                "{data:{'type':'person','id':'1',attributes:{'name':'Andrew',surname:'Puhach'}}}");
    }};

    @Test
    public void emptyJsonShouldBeCorrect(){
        assertEquals("{}",JsonApiTransformer.transform(new Gson().fromJson("{}", JsonElement.class)).toString());
    }

    @Test
    public void nullJsonShouldBeCorrect(){
        assertEquals(JsonNull.INSTANCE,JsonApiTransformer.transform(null));
    }


    @Test
    public void notJsonAPIStyleShouldBeCorrect(){
        assertEquals("{\"name\":\"Andrew\"}",JsonApiTransformer.transform(new Gson().fromJson("{'name':'Andrew'}",JsonElement.class)).toString());
    }

    @Test
    public void simpleJsonShouldBeCorrect(){
        for(Map.Entry<String,String> testCase:simpleTestCases.entrySet()) {
            assertEquals(testCase.getKey(), JsonApiTransformer.transform(new Gson().fromJson(testCase.getValue(), JsonElement.class)).toString());
        }
    }

    @Test(expected = JsonParseException.class)
    public void wrongJsonShouldFailWithException(){
        //missing closing parenthesis
        JsonApiTransformer.transform(new Gson().fromJson("{'name':'Andrew'",JsonElement.class));
    }
}