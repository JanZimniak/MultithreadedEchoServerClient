package projekt;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
    private Socket socket;
    private ExecutorService executor;
    private InetSocketAddress endpoint;

    private ObjectOutputStream write;
    private ObjectInputStream read;

    private volatile boolean isRunning;

    public Client(String address, int port) throws IOException{
        this.socket = new Socket();
        this.socket.setReuseAddress(true);
        this.socket.bind(new InetSocketAddress(address, port));
        this.isRunning = false;
        this.executor = Executors.newCachedThreadPool(); 
    }

    public void connect(String endAddress, int endPort) throws IOException{
        this.endpoint = new InetSocketAddress(endAddress, endPort); 
        this.socket.connect(this.endpoint);
        this.isRunning = true;
        this.write = new ObjectOutputStream(this.socket.getOutputStream());
        this.read = new ObjectInputStream(this.socket.getInputStream());
        this.executor.submit(() -> handleSend());
        this.executor.submit(() -> handleReceive());
    }

    public void closeClient(){
        System.out.println("Closing client");
        this.isRunning = false;
        try{
            this.socket.close();
        }catch(IOException e){
            System.out.println(e.getMessage());
        }
        this.executor.shutdown();
    }

    private void handleSend(){
        try(
            Scanner scanner = new Scanner(System.in);
           ){

            while(this.isRunning){
                String input = scanner.nextLine();
                if(input.equals("end")){
                    closeClient();
                    break;
                }
                DataObject sendData = DataObject.createMessage("client", input);
                this.write.writeObject(sendData);
                this.write.flush();
            } 
        }catch(IOException e){
            if(this.isRunning){
                System.out.println(e.getMessage());
            }
        }
    }

    private void handleReceive(){
        try{
            while(this.isRunning){
                DataObject serverData = (DataObject)this.read.readObject();
                System.out.println(serverData.getMessage());
            }
        }catch(IOException e){
            System.out.println(e.getMessage());
        }catch(ClassNotFoundException e){
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client("localhost", 5555);
        client.connect("127.0.0.1", 6666);
    }
}
