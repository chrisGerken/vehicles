package com.gerkenip.vehicles.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * JSON utility wrapper around Gson.
 */
public class JsonUtil {
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Gson compactGson = new Gson();

    /**
     * Converts object to JSON string (pretty-printed).
     */
    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }

    /**
     * Converts object to compact JSON string (no formatting).
     */
    public static String toJsonCompact(Object obj) {
        return compactGson.toJson(obj);
    }

    /**
     * Parses JSON string to object.
     */
    public static <T> T fromJson(String json, Class<T> classOfT) throws JsonSyntaxException {
        return gson.fromJson(json, classOfT);
    }

    /**
     * Pretty-prints a JSON string.
     */
    public static String prettyPrint(String json) {
        Object obj = gson.fromJson(json, Object.class);
        return gson.toJson(obj);
    }
}
