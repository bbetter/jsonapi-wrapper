package com.teamvoy.jsonapi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

/**
 * Created by mac on 18.02.16.
 * recursively validate all levels
 * @deprecated now built in {@link JsonApiTransformer})
 */

public class JsonApiValidator {
    interface Validatable {
        boolean validate(JsonElement jsonApiElement);
    }


    public static boolean validateLevel(String level, final JsonElement jsonRoot) {
        switch (level) {
            case Constants.TOP_LEVEL:
                if (jsonRoot == JsonNull.INSTANCE || jsonRoot == null) return false;

                //must have at least one
                boolean hasData = jsonRoot.getAsJsonObject().has(Constants.DATA);
                boolean hasError = jsonRoot.getAsJsonObject().has(Constants.ERROR);
                boolean hasMeta = jsonRoot.getAsJsonObject().has(Constants.META);

                //may have
                boolean hasLinks = jsonRoot.getAsJsonObject().has(Constants.LINKS);
                boolean hasIncluded = jsonRoot.getAsJsonObject().has(Constants.INCLUDED);

                if (hasData && hasError) return false;

                boolean result = true;
                if (hasData)
                    result &= validateLevel(Constants.PRIMARY_DATA, jsonRoot.getAsJsonObject().get(Constants.DATA));
                if (hasMeta)
                    result &= validateLevel(Constants.META, jsonRoot.getAsJsonObject().get(Constants.META));
                if(hasIncluded)
                    result &= validateLevel(Constants.INCLUDED,jsonRoot.getAsJsonObject().get(Constants.INCLUDED));
                if(hasLinks)
                    result &= validateLevel(Constants.LINKS,jsonRoot.getAsJsonObject().get(Constants.LINKS));
                return result;
            case Constants.PRIMARY_DATA:
                break;
            case Constants.DATA:
                break;
            case Constants.RELATIONSHIPS:
                break;
            case Constants.LINKS:
                break;
            case Constants.ERROR:
                break;
            case Constants.META:
                break;
            case Constants.INCLUDED:
                break;
            default:
                return false;
        }
    return false;
    }

    boolean validate(JsonElement jsonRoot, Validatable validatable) {
        if (jsonRoot.isJsonArray()) {
            JsonArray array = jsonRoot.getAsJsonArray();
            for (int i = 0; i < array.size(); ++i) {
                JsonElement e = array.get(i);
                if (!validatable.validate(e)) return false;
            }
            return true;
        } else {
            return validatable.validate(jsonRoot);
        }
    }
}
