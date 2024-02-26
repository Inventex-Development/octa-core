package dev.inventex.octa.data.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.experimental.UtilityClass;

import java.util.Map;

/**
 * Represents a utility that merges json objects and handles merge conflicts.
 */
@UtilityClass
public class JsonMerger {
    /**
     * Marge two json objects into each other using a conflict strategy.
     *
     * @param source source object to merge into
     * @param update update object to merge from
     * @param strategy conflict strategy to use
     */
    public void merge(JsonObject source, JsonObject update, ConflictStrategy strategy) {
        // loop through the update pairs
        for (Map.Entry<String, JsonElement> entry : update.entrySet()) {
            // get the key and value of the pair
            String key = entry.getKey();
            JsonElement updateValue = entry.getValue();

            // append the value to the source, if the key does not exist
            if (!source.has(key)) {
                source.add(key, updateValue);
                continue;
            }

            // get the value of the key from the source
            JsonElement sourceValue = source.get(key);

            // check if the source and the update are arrays
            if (sourceValue.isJsonArray() && updateValue.isJsonArray()) {
                mergeArray(sourceValue, updateValue);
                continue;
            }

            // check if the source and the update are objects
            if (sourceValue.isJsonObject() && updateValue.isJsonObject()) {
                // marge the json objects recursively
                merge(sourceValue.getAsJsonObject(), updateValue.getAsJsonObject(), strategy);
                continue;
            }

            // types does not match or are recursive, handle merge conflict
            mergeConflict(source, key, updateValue, strategy);
        }
    }

    /**
     * Merge the update array into the source array.
     *
     * @param sourceValue the source array
     * @param updateValue the update array
     */
    private void mergeArray(JsonElement sourceValue, JsonElement updateValue) {
        // get the arrays as values
        JsonArray sourceArray = sourceValue.getAsJsonArray();
        JsonArray updateArray = updateValue.getAsJsonArray();
        // concat the arrays, nothing to override
        for (JsonElement element : updateArray)
            sourceArray.add(element);
    }

    /**
     * Handle merge conflict between the source and the update value.
     *
     * @param source the source object
     * @param key the key of the update value
     * @param updateValue the update value
     * @param strategy the conflict strategy
     */
    private void mergeConflict(JsonObject source, String key, JsonElement updateValue, ConflictStrategy strategy) {
        // types does not match or are recursive, handle merge conflict
        switch (strategy) {
            // don't modify anything, keep the original value
            case KEEP_ORIGINAL:
                break;
            // force override the original value
            case OVERRIDE:
                source.add(key, updateValue);
                break;
            // override the original value if the update value is not null
            case OVERRIDE_NOT_NULL:
                if (!updateValue.isJsonNull())
                    source.add(key, updateValue);
                break;
            // throw an exception if the update key is already exists in the source
            case THROW_EXCEPTION:
                throw new IllegalStateException("Key '" + key + "' already exists in object " + source);
        }
    }
}
