package cn.com.super_key.skconnect.codec;

import cn.com.super_key.skconnect.common.JsonHunter;

/**
 * Created by wwg on 2017/1/3.
 */

public interface ChannelOutboundHandler {
    void channelRead(JsonHunter ctx, Object msg) throws Exception;
}
