package com.teamvoy.jsonapi;

/**
 * Created by mac on 17.02.16
 */


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

    static final String DATA = "data";
    static final String RELATIONSHIPS = "relationships";
    static final String INCLUDED = "included";
    static final String ID = "id";
    static final String TYPE = "type";
    static final String ATTRIBUTES = "attributes";

    /**
     * interface that contains applicable action
     */
    interface Transformable {
        JsonElement transform(JsonElement jsonApiElement);
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
            if(element != JsonNull.INSTANCE) {
                this.id = element.getAsJsonObject().get(ID).getAsString();
                this.type = element.getAsJsonObject().get(TYPE).getAsString();
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
         * JSONAPI node -> simple JSON (cutting of id and type)
         * @return
         */
        public JsonElement getConverted() {
            if (getElement() == null) return JsonNull.INSTANCE;
            if (getId() == null || getType() == null) return JsonNull.INSTANCE;
            JsonObject object = element.getAsJsonObject();
            if (object.has(ATTRIBUTES)) {
                JsonObject json = object.get(ATTRIBUTES).getAsJsonObject();
                json.addProperty(ID, getId());
                json.addProperty(TYPE, getType());

                if (object.has(RELATIONSHIPS)) {
                    JsonObject relationships = object.get(RELATIONSHIPS).getAsJsonObject();
                    Set<Map.Entry<String, JsonElement>> entries = relationships.entrySet();
                    for (Map.Entry<String, JsonElement> pair : entries) {
                        json.add(pair.getKey(), convertFromData(pair.getValue().getAsJsonObject().get(DATA)));
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

    private static JsonElement convertFromData(JsonElement element) {
        return transform(element, new Transformable() {
            @Override
            public JsonElement transform(JsonElement jsonElement) {
                return new JSONAPIElement(jsonElement).getConverted();
            }
        });
    }

    /**
     * search @item in @json
     * @param item item to search
     * @param json where to search
     * @return found item if found && JsonNull.INSTANCE if not found
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
     * @param jsonElement what to link
     * @param data to what
     * @return linked object
     */
    private static JsonElement link(JsonElement jsonElement, final JsonElement data) {
        JsonObject object = jsonElement.getAsJsonObject();
        if(object.has(RELATIONSHIPS)){
            JsonObject relationships = object.get(RELATIONSHIPS).getAsJsonObject();
            for (Map.Entry<String,JsonElement> pair: relationships.entrySet()) {

               JsonElement transformedData = transform(pair.getValue().getAsJsonObject().get(DATA), new Transformable() {
                            @Override
                            public JsonElement transform(JsonElement jsonApiElement) {
                                return search(jsonApiElement,data);
                            }
                        });

                object
                        .get(RELATIONSHIPS)
                        .getAsJsonObject()
                        .get(pair.getKey())
                        .getAsJsonObject()
                        .add(DATA,transformedData);
            }
        }


        return jsonElement;
    }

    /**
     * transforms @element with the given @transformable
     * @param element json element to transform
     * @param transformable action to apply to it
     * @return transformed json element
     */
    private static JsonElement transform(JsonElement element, Transformable transformable) {
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
    }

    /**
     * JSONAPI style -> Simple JSON
     * @param jsonApiElement JSONAPI to transform as JsonElement
     * @return Simple JSON as JsonElement
     */
    public static JsonElement transform(JsonElement jsonApiElement){
        if(jsonApiElement == null || jsonApiElement == JsonNull.INSTANCE) return JsonNull.INSTANCE;
        JsonObject jsonApiObject = jsonApiElement.getAsJsonObject();
        if (!jsonApiObject.has(DATA)) return jsonApiElement;
        JsonElement data = jsonApiObject.get(DATA);
        if(!jsonApiObject.has(INCLUDED)){
            return convertFromData(data);
        }

        JsonElement included = jsonApiObject.get(INCLUDED);
        //because of "only final in blocks" restriction
        final JsonElement includedCopyBeforeTransformation = included;
        included = transform(included, new Transformable() {
            @Override
            public JsonElement transform(JsonElement jsonApiElement) {
                return link(jsonApiElement, includedCopyBeforeTransformation);
            }
        });

        //because of "only final in blocks" restriction
        final JsonElement includedCopyAfterTransformation = included;
        data = transform(data,new Transformable() {
            @Override
            public JsonElement transform(JsonElement jsonApiElement) {
                return  link(jsonApiElement, includedCopyAfterTransformation);
            }
        });

        return convertFromData(data);
    }

}
