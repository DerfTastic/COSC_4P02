package server.infrastructure.param.auth;

import framework.util.Tuple;

import java.util.HashMap;

public class SessionCache {
    private final HashMap<Long, Tuple<String, UserSession>> sessionMap = new HashMap<>();

    public synchronized UserSession validate(String token){
        if(token.length()<8)return null;
        var id = Long.parseLong(token.substring(token.length()-8), 16);
        var result = sessionMap.get(id);
        if(result==null) return null;
        if(result.t1().equals(token))return result.t2();
        return null;
    }

    public synchronized void add(String token, UserSession session){
        sessionMap.put(session.session_id, new Tuple<>(token, session));
    }

    public synchronized void invalidate_session(long session) {
        sessionMap.remove(session);
    }

    public synchronized void invalidate() {
        sessionMap.clear();
    }
}
