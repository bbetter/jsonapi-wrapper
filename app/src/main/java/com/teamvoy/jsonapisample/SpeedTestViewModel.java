package com.teamvoy.jsonapisample;

import android.databinding.ObservableField;
import android.util.Log;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.teamvoy.jsonapi.JsonApiException;
import com.teamvoy.jsonapi.JsonApiTransformer;

import java.util.Calendar;

/**
 * Created by mac on 17.02.16.
 */
public class SpeedTestViewModel {

    private String initialJson = "{}";

    public ObservableField<String> ms = new ObservableField<>();
    public ObservableField<String> json = new ObservableField<>();

    public void onSpeedTestClick(View view){
        long ms = Calendar.getInstance().getTimeInMillis();
        JsonElement element = new Gson().fromJson(initialJson,JsonElement.class);
        JsonElement transformedElement = null;
        try {
            transformedElement = JsonApiTransformer.transform(element);
        } catch (JsonApiException e) {
            e.printStackTrace();
            return ;
        }

        this.ms.set("Time spent:"+String.valueOf(Calendar.getInstance().getTimeInMillis()-ms)+" ms");
        this.json.set("Result:"+transformedElement.toString());

        Log.i("jsonapi","result:"+transformedElement.toString());
        Log.i("jsonapi","speed:"+String.valueOf(this.ms.get()));
    }

    public String getInitialJson() {
        return initialJson;
    }

    public void setInitialJson(String initialJson) {
        this.initialJson = initialJson;
    }
}
