package cn.com.super_key.skconnect.modal;

import java.util.List;

/**
 * Created by wwg on 2017/3/24.
 */

public class CabinService extends Request {

    private List<Service> sercices;

    public CabinService(){
        super();
        setRequestType("CabinService");
    }
    public List<Service> getSercices() {
        return sercices;
    }

    public void setSercices(List<Service> sercices) {
        this.sercices = sercices;
    }






}
