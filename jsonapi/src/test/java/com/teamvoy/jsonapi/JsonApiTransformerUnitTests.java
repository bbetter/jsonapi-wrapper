package com.teamvoy.jsonapi;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class JsonApiTransformerUnitTests {

    private final Map<String,String> simpleTestCases =  new HashMap<String, String>() {{
        put("{\"data\":{\"name\":\"Andrew\",\"id\":\"1\",\"type\":\"person\"},\"meta\":null}",
                "{data:{'type':'person','id':'1',attributes:{'name':'Andrew'}}}");
        put("{\"data\":{\"name\":\"Andrew\",\"surname\":\"Puhach\",\"id\":\"1\",\"type\":\"person\"},\"meta\":null}",
                "{data:{'type':'person','id':'1',attributes:{'name':'Andrew',surname:'Puhach'}}}");
    }};

    private final Map<String,String> notSoSimpleTestCases = new HashMap<String,String>(){{
        put("{\"data\":{\"name\":\"Andrew\",\"id\":\"1\",\"type\":\"person\",\"device\":{\"id\":\"1\",\"type\":\"devices\"}},\"meta\":null}",
                "{'data':{'type':'person','id':'1','attributes':{'name':'Andrew'},'relationships':{'device':{'data':{'id':1,'type':'devices'}}}},'included':[{'id':1,'type':'devices','attributes':{}}]}");
    }};

    @Test(expected = JsonApiException.class)
    public void emptyJsonShouldFailWithException() throws JsonApiException {
        JsonApiTransformer.transform(new Gson().fromJson("{}", JsonElement.class));
    }

    @Test(expected = JsonApiException.class)
    public void nullJsonShouldFailWithException() throws JsonApiException{
        JsonApiTransformer.transform(null);
    }

    @Test(expected = JsonApiException.class)
    public void notJsonAPIStyleShoulFailWithException() throws JsonApiException{
        JsonApiTransformer.transform(new Gson().fromJson("{'name':'Andrew'}",JsonElement.class));
    }

    @Test
    public void simpleJsonShouldSuccess() throws JsonApiException{
        for(Map.Entry<String,String> testCase:simpleTestCases.entrySet()) {
            assertEquals(testCase.getKey(), JsonApiTransformer.transform(new Gson().fromJson(testCase.getValue(), JsonElement.class)).toString());
        }
    }

    @Test
    public void withIncludesShouldSuccess() throws JsonApiException{
        for(Map.Entry<String,String> testCase:notSoSimpleTestCases.entrySet()) {
            assertEquals(testCase.getKey(), JsonApiTransformer.transform(new Gson().fromJson(testCase.getValue(), JsonElement.class)).toString());
        }
    }
}