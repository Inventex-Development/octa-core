package dev.inventex.octa.data.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;

public class JsonArrayBuilder {
    private final JsonArray json;

    public JsonArrayBuilder() {
        json = new JsonArray();
    }

    public JsonArrayBuilder push(Number value) {
        json.add(new JsonPrimitive(value));
        return this;
    }

    public JsonArrayBuilder push(String value) {
        json.add(new JsonPrimitive(value));
        return this;
    }

    public JsonArrayBuilder push(Boolean value) {
        json.add(new JsonPrimitive(value));
        return this;
    }

    public JsonArrayBuilder push(Character character) {
        json.add(new JsonPrimitive(character));
        return this;
    }

    public JsonArrayBuilder push(JsonArray value) {
        json.add(value);
        return this;
    }

    public JsonArrayBuilder push(JsonBuilder value) {
        json.add(value.build());
        return this;
    }

    public JsonArrayBuilder push(JsonArrayBuilder value) {
        json.add(value.build());
        return this;
    }

    public JsonArray build() {
        return json;
    }
}
