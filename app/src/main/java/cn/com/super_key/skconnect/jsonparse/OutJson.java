package cn.com.super_key.skconnect.jsonparse;

import cn.com.super_key.skconnect.codec.ChannelOutboundHandler;
import cn.com.super_key.skconnect.common.JsonHunter;
import cn.com.super_key.skconnect.jsonparse.JsonParse;

/**
 * Created by wwg on 2017/3/27.
 */

public class OutJson implements ChannelOutboundHandler {
    @Override
    public void channelRead(JsonHunter jsonHunter, Object o) throws Exception {
        if(o == null )
            return;
        JsonParse parse = new JsonParse();
        String className = jsonHunter.getClassName();
        className = "cn.com.super_key.skconnect.modal."+className;
        String str =(String)o;
        Object obj  = parse.parse(str,className);
        jsonHunter.setResult(obj);

    }
}
