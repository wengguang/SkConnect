package cn.com.super_key.skconnect.jsonparse;



import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class JsonParse {
	
	private Gson gson_ = null;		
	public JsonParse(){
		 GsonBuilder gsonBuilder = new GsonBuilder();  
		 gson_ = gsonBuilder.create();
	}
	public Object parse(String str,String type){

		//String a = type;
		Class<?> kk=null;
		try {
			kk =Class.forName(type);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		if( kk == null)
			return null;
		return gson_.fromJson(str,kk);
	}
    public String toJson(Object t){
        return gson_.toJson(t); 
    }

}
