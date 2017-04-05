package cn.com.super_key.skconnect.modal;

/**
 * Created by wwg on 2017/3/24.
 */

public class ServiceStatus extends Request {
    private String id;
    private int  status;

    public ServiceStatus(){
        super();
        this.setRequestType("ServiceStatus");
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }






}
