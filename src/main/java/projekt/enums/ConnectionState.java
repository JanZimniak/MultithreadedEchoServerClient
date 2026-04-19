package projekt.enums;

public enum ConnectionState{
    OFFLINE("OFFLINE"),
    RUNNING("RUNNING"),
    CONNECTED("CONNECTED"),
    SERVER_BUSY("SERVER_BUSY");

    private final String text;

    private ConnectionState(String value){
        this.text = value;
    }

    public String getState(){
        return this.text;
    }
}
