package server.handlebars;

import com.alibaba.fastjson2.JSON;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.context.MethodValueResolver;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import server.infrastructure.root.api.PaymentAPI;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;

public class Hbs {
    private static HashMap<String, Template> compiled = new HashMap<>();

    static{
        TemplateLoader loader = new ClassPathTemplateLoader();
        loader.setPrefix("/handlebars");
        loader.setSuffix(".hbs");
        Handlebars handlebars = new Handlebars(loader);

        for(var cnd : ConditionalHelpers.values()){
            handlebars.registerHelper(cnd.name(), cnd);
        }

        handlebars.registerHelper("formatPrice", (o, options) -> switch(o){
            case Integer l -> PaymentAPI.formatPrice(l);
            case Long l -> PaymentAPI.formatPrice(l);
            case null, default -> "ERROR";
        });

        handlebars.registerHelper("isTicket", (o, options) -> o instanceof PaymentAPI.TicketReceipt);

        handlebars.registerHelper("formatDate", (o, options) -> switch (o) {
            case Integer l -> new Date(l).toString();
            case Long l -> new Date(l).toString();
            case null, default -> "ERROR";
        });

        handlebars.registerHelper("encodeURI", (o, options) -> URLEncoder.encode((o==null?"":o).toString(), StandardCharsets.UTF_8));

        handlebars.registerHelper("json", (o, options) -> JSON.toJSON(o));

        try {
            for(var name: new String[]{"email_receipt"}){
                compiled.put(name, handlebars.compile(name));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> String run(String name, T obj) throws IOException {
        return compiled.get(name).apply(Context
                .newBuilder(obj)
                .resolver(JavaBeanValueResolver.INSTANCE,
                        FieldValueResolver.INSTANCE,
                        MapValueResolver.INSTANCE,
                        MethodValueResolver.INSTANCE
                )
                .build()
        );
    }
}
