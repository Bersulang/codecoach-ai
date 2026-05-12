package com.codecoach.module.resume.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.util.StringUtils;

public class FlexibleStringListDeserializer extends JsonDeserializer<List<String>> {

    @Override
    public List<String> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.currentToken();
        if (token == JsonToken.START_ARRAY) {
            List<String> values = new ArrayList<>();
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                String value = parser.getValueAsString();
                if (StringUtils.hasText(value)) {
                    values.add(value.trim());
                }
            }
            return values;
        }
        if (token == JsonToken.VALUE_STRING) {
            return split(parser.getValueAsString());
        }
        if (token == JsonToken.VALUE_NULL) {
            return List.of();
        }
        String value = parser.getValueAsString();
        return StringUtils.hasText(value) ? List.of(value.trim()) : List.of();
    }

    private List<String> split(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String item : value.split("\\s*(?:[|,，;；/、]|\\n)+\\s*")) {
            if (StringUtils.hasText(item)) {
                values.add(item.trim());
            }
        }
        return values;
    }
}
