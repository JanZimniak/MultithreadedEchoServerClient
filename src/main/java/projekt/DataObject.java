package projekt;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

public class DataObject implements Serializable{

    private static final String RESPONSE = "PONG";
    private static final String REQUEST = "PING";

    private String title;
    private String message;
    private int messageSize;

    private DataObject(){
        throw new AssertionError("Cannot instantiate DataObject class");
    }
    private DataObject(String title){
        this.title = title;
    }
    private DataObject(String title, String message){
        this.title = title;
        this.message = message;
        this.messageSize = this.message != null ? this.message.getBytes(StandardCharsets.UTF_8).length : 0;
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

    public static boolean isValidResponse(DataObject obj){
        return (obj.title.equals(RESPONSE) && obj.message == null);
    }

    public static boolean isValidRequest(DataObject obj){
        return (obj.title.equals(REQUEST) && obj.message == null);
    }

    public String getMessage(){
        return this.message;
    }

    public int getMessageSize(){
        return this.messageSize;
    }
}
