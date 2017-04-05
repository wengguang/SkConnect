package cn.com.super_key.skconnect.modal;

import java.util.List;

/**
 * Created by wwg on 2017/3/24.
 */

public class GetServicesListRet extends Ret {

    public List<Content> getResponseContent() {
        return responseContent;
    }

    public void setResponseContent(List<Content> responseContent) {
        this.responseContent = responseContent;
    }

    private List<Content> responseContent;



}
