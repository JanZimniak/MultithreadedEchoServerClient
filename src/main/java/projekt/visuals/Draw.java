package projekt.visuals;

import java.util.ArrayList;

public class Draw {

    private ArrayList<String> constData = new ArrayList<>();
    private ArrayList<String> scrolledData = new ArrayList<>();

    private final int MAX_SCROLL = 10;

    public void redraw(){
        clearScreen();

        for(String msg : this.constData){
            System.out.println(msg);
        }

        int start = (this.scrolledData.size() - 10 > 0) ? this.scrolledData.size()-10 : 0;
        for(String msg : this.scrolledData.subList(start, this.scrolledData.size())){
            System.out.println(msg);
        }

    }


    private void clearScreen(){
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public void addConstMessage(String message){
        this.constData.add(message);
    }
    public void addScrolledData(String message){
        this.scrolledData.add(message);
        this.redraw();
    }
    public void removeConstMessage(String message){
        this.constData.remove(message);
    }
    public void popConstMessage(){
        this.constData.remove(this.constData.size() - 1);
    }
}
