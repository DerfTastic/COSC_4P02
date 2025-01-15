package server.web.route;

import java.util.List;

public interface StringSingleAdapter<T> extends StringsAdapter<T> {
    T parseSingular(String str) throws Exception;

    default T parseMultiple(List<String> str) throws Exception {
        if (str == null) throw new ClientError.BadRequest("Expected a single value");
        if (str.size() == 1) return parseSingular(str.get(0));
        if (str.isEmpty()) throw new ClientError.BadRequest("Expected a single value");
        throw new ClientError.BadRequest("Expected a single value");
    }
}
