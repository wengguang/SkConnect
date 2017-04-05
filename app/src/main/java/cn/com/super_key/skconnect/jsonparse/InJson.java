package cn.com.super_key.skconnect.jsonparse;


import cn.com.super_key.skconnect.codec.ChannelInboundHandler;
import cn.com.super_key.skconnect.common.JsonHunter;

/**
 * Created by wwg on 2017/3/27.
 */

public class InJson implements ChannelInboundHandler {
    @Override
    public void channelRead(JsonHunter jsonHunter, Object o) throws Exception {
        if(o == null)
            return;

        JsonParse parse = new JsonParse();
        String str  = parse.toJson(o);
        jsonHunter.setRequestObj(str);

    }
}
