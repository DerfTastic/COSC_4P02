package framework.web.route;

import framework.web.error.BadRequest;

import java.util.List;

public interface StringSingleNullableAdapter<T> extends StringsAdapter<T> {
    T parse(String str) throws Exception;

    default T parseSingular(String str) throws Exception {
        if (str == null) return null;
        return this.parse(str);
    }

    default T parseMultiple(List<String> str) throws Exception {
        if (str == null) return null;
        if (str.isEmpty()) return null;
        if (str.size() == 1) return parseSingular(str.get(0));
        throw new BadRequest("Expected a single value");
    }
}
