package cn.com.super_key.skconnect.modal;

/**
 * Created by wwg on 2017/3/24.
 */

public class FightStatus extends Request {

    public FightStatus(){
        super();
        this.setRequestType("FightStatus");
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    private  int status;




}
