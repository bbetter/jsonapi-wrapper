package com.teamvoy.jsonapi;

/**
 * Created by mac on 17.02.16
 */


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.Set;

/**
 * Created by mac on 16.02.16.
 * class used to convert JSONAPI style -> to simple JSON
 * compatible with Retrofit and Gson
 */
public class JsonApiTransformer {


    /**
     * interface that contains applicable transformation
     */
    interface Transformable {
        JsonElement transform(JsonElement jsonApiElement) throws JsonApiException;
    }

    interface Validatable {
        boolean validate(JsonElement jsonApiElement);
    }

    /**
     * JSONAPI node
     */
    static class JSONAPIElement {
        private String id;
        private String type;

        public void setId(String id) {
            this.id = id;
        }

        public void setType(String type) {
            this.type = type;
        }

        private JsonElement element;

        public JsonElement getElement() {
            return element;
        }

        public JSONAPIElement(JsonElement element) {
            this.element = element;
            if (element != JsonNull.INSTANCE) {
                this.id = element.getAsJsonObject().get(Constants.ID).getAsString();
                this.type = element.getAsJsonObject().get(Constants.TYPE).getAsString();
            }
        }

        public String getId() {
            if (element == JsonNull.INSTANCE) return null;
            return id;
        }

        public String getType() {
            if (element == JsonNull.INSTANCE) return null;
            return type;
        }

        /**
         * JSONAPI node -> simple JSON
         *
         * @return transformed node
         * @throws JsonApiException on malforemed jsonapi node
         */
        public JsonElement getConverted() throws JsonApiException {
            if (getId() == null || getType() == null)
                throw new JsonApiException(Constants.DATA, "no id and type in data");
            JsonObject object = element.getAsJsonObject();
            if (object.has(Constants.ATTRIBUTES)) {
                JsonObject json = object.get(Constants.ATTRIBUTES).getAsJsonObject();
                json.addProperty(Constants.ID, getId());
                json.addProperty(Constants.TYPE, getType());

                if (object.has(Constants.RELATIONSHIPS)) {
                    JsonObject relationships = object.get(Constants.RELATIONSHIPS).getAsJsonObject();
                    Set<Map.Entry<String, JsonElement>> entries = relationships.entrySet();
                    for (Map.Entry<String, JsonElement> pair : entries) {
                        json.add(pair.getKey(), convertFromData(pair.getValue().getAsJsonObject().get(Constants.DATA)));
                    }
                }
                return json;
            }
            return JsonNull.INSTANCE;
        }

        public boolean equals(JSONAPIElement o) {
            String selfId = getId();
            String selfType = getType();
            String oId = o.getId();
            String oType = o.getType();
            return selfId.equals(oId) && selfType.equals(oType);
        }
    }

    /**
     *
     * @param element json element
     * @return transformed json element
     * @throws JsonApiException on malformed jsonapi element
     */
    private static JsonElement convertFromData(JsonElement element) throws JsonApiException {
        try {
            return transform(element, new Transformable() {
                @Override
                public JsonElement transform(JsonElement jsonElement) throws JsonApiException {
                    return new JSONAPIElement(jsonElement).getConverted();
                }
            });
        } catch (JsonApiException exception) {
            throw exception;
        }
    }

    /**
     * search @item in @json
     *
     * @param item item to search
     * @param json where to search
     * @return found item if found && JsonNull.INSTANCE if not found
     * @throws JsonApiException on malformed jsonapi
     */
    private static JsonElement search(JsonElement item, JsonElement json) {
        JSONAPIElement apiItem = new JSONAPIElement(item);
        if (json.isJsonArray()) {
            JsonArray array = json.getAsJsonArray();
            for (int i = 0; i < array.size(); ++i) {
                JSONAPIElement jsonapiElement = new JSONAPIElement(array.get(i));
                if (jsonapiElement.equals(apiItem)) {
                    return jsonapiElement.getElement();
                }
            }
        } else {
            if (apiItem.equals(new JSONAPIElement(json))) {
                return item;
            }
        }
        return JsonNull.INSTANCE;
    }

    /**
     * link all objects from "relationships" to their instances in "includes"
     *
     * @param jsonElement what to link
     * @param data        to what
     * @return linked object
     * @throws JsonApiException on malformed jsonapi
     */
    private static JsonElement link(JsonElement jsonElement, final JsonElement data) throws JsonApiException {
        JsonObject object = jsonElement.getAsJsonObject();
        if (object.has(Constants.RELATIONSHIPS)) {
            JsonObject relationships = object.get(Constants.RELATIONSHIPS).getAsJsonObject();
            for (Map.Entry<String, JsonElement> pair : relationships.entrySet()) {
                JsonElement transformedData = transform(pair.getValue().getAsJsonObject().get(Constants.DATA), new Transformable() {
                    @Override
                    public JsonElement transform(JsonElement jsonApiElement) {
                        return search(jsonApiElement, data);
                    }
                });

                object.get(Constants.RELATIONSHIPS)
                        .getAsJsonObject()
                        .get(pair.getKey())
                        .getAsJsonObject()
                        .add(Constants.DATA, transformedData);
            }
        }
        return jsonElement;
    }

    /**
     * transforms @element with the given @transformable
     *
     * @param element       json element to transform
     * @param transformable action to apply to it
     * @return transformed json element
     * @throws JsonApiException on malformed jsonapi
     */
    private static JsonElement transform(JsonElement element, Transformable transformable) throws JsonApiException {
        try {
            if (element.isJsonArray()) {
                JsonArray array = element.getAsJsonArray();
                for (int i = 0; i < array.size(); ++i) {
                    JsonElement e = array.get(i);
                    array.set(i, transformable.transform(e));
                }
                return array;
            } else {
                return transformable.transform(element);
            }
        } catch (JsonApiException e) {
            throw e;
        }
    }

    /**
     * JSONAPI style -> Simple JSON
     *
     * @param jsonApiElement JSONAPI to transform as JsonElement
     * @return Simple JSON as JsonElement
     * @throws JsonApiException on malformed jsonapi
     */
    public static JsonElement transform(JsonElement jsonApiElement) throws JsonApiException {
        if (jsonApiElement == null || jsonApiElement == JsonNull.INSTANCE)
            throw new JsonApiException(Constants.TOP_LEVEL, "No json object on top level");

        JsonElement root = new Gson().fromJson("{}",JsonElement.class);
        //must have at least one
        boolean hasData = jsonApiElement.getAsJsonObject().has(Constants.DATA);
        boolean hasError = jsonApiElement.getAsJsonObject().has(Constants.ERROR);
        boolean hasMeta = jsonApiElement.getAsJsonObject().has(Constants.META);

        //may have
        boolean hasIncluded = jsonApiElement.getAsJsonObject().has(Constants.INCLUDED);

        if (hasData && hasError)
            throw new JsonApiException(Constants.TOP_LEVEL, "data && error can't coexist on top level");
        if (!hasData && !hasError && !hasMeta)
            throw new JsonApiException(Constants.TOP_LEVEL, "at least one of data,error,meta has to be present");

        JsonObject jsonApiElementAsJsonObject = jsonApiElement.getAsJsonObject();

        JsonElement data = jsonApiElementAsJsonObject.get(Constants.DATA);

        if (!hasIncluded) {
            root.getAsJsonObject().add("data",convertFromData(data));
            root.getAsJsonObject().add("meta",jsonApiElementAsJsonObject.get("meta"));
            return root;
        }

        JsonElement included = jsonApiElementAsJsonObject.get(Constants.INCLUDED);
        //because of "only final in blocks" restriction
        final JsonElement includedCopyBeforeTransformation = included;
        included = transform(included, new Transformable() {
            @Override
            public JsonElement transform(JsonElement jsonApiElement) throws JsonApiException {
                return link(jsonApiElement, includedCopyBeforeTransformation);
            }
        });

        //because of "only final in blocks" restriction
        final JsonElement includedCopyAfterTransformation = included;
        data = transform(data, new Transformable() {
            @Override
            public JsonElement transform(JsonElement jsonApiElement) throws JsonApiException {
                return link(jsonApiElement, includedCopyAfterTransformation);
            }
        });

        root.getAsJsonObject().add("data",convertFromData(data));
        root.getAsJsonObject().add("meta",jsonApiElementAsJsonObject.get("meta"));
        return root;
    }
}
