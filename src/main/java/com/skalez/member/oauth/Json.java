package com.skalez.member.oauth;

import java.util.Map;

/** 카카오·네이버 응답(Map)에서 값 꺼내기. 없으면 null · false · 빈 Map */
final class Json {
    private Json() {}

    static String text(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    static boolean bool(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> object(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value instanceof Map<?, ?> nested ? (Map<String, Object>) nested : Map.of();
    }
}
