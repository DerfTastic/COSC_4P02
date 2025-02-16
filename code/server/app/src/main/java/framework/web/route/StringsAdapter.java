package framework.web.route;

import java.util.List;

public interface StringsAdapter<T> {
    T parseSingular(String str) throws Exception;

    T parseMultiple(List<String> str) throws Exception;
}
