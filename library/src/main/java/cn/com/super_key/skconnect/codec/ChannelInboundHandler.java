package cn.com.super_key.skconnect.codec;

/**
 * Created by wwg on 2017/1/3.
 */
import cn.com.super_key.skconnect.common.JsonHunter;

public interface ChannelInboundHandler {
    void channelRead(JsonHunter ctx, Object msg) throws Exception;
}
