package cn.com.super_key.skconnect.modal;

/**
 * Created by wwg on 2017/3/24.
 */

public class GetServicesList extends Request {
    public GetServicesList(){
        super();
        this.setRequestType("GetServicesList");
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    private int type;



}
