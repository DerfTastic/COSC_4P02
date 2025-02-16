package server.framework.web.route;

import java.util.List;

public interface StringListAdapter<T> extends StringsAdapter<T> {
    @Override
    T parseMultiple(List<String> str) throws Exception;

    @Override
    default T parseSingular(String str) throws Exception {
        return parseMultiple(List.of(str));
    }
}
