package projekt;

import java.io.Serializable;

public class DataObject implements Serializable{

    private static final String RESPONSE = "PONG";
    private static final String REQUEST = "PING";

    private String title;
    private String message;

    private DataObject(){
        throw new AssertionError("Cannot instantiate DataObject class");
    }
    private DataObject(String title){
        this.title = title;
    }
    private DataObject(String title, String message){
        this.title = title;
        this.message = message;
    }

    public static DataObject instantiateRequestPing(){
        return new DataObject(REQUEST);
    }
    
    public static DataObject instantiateResponsePing(){
        return new DataObject(RESPONSE);
    }

    public static DataObject createMessage(String title, String message){
        return new DataObject(title, message);
    }

    public boolean isValidResponse(DataObject obj){
        return (obj.title == RESPONSE && obj.message == null);
    }

    public String getMessage(){
        return this.message;
    }
}
