package cn.com.super_key.skconnect.modal;

/**
 * Created by wwg on 2017/3/24.
 */

public class Ret {
    private String responseType;

    public Response getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(Response responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    private Response responseStatus;


}
