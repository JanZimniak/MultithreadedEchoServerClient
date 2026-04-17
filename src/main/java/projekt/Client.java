package projekt;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Client {
    private Socket socket;
    private ExecutorService executor;
    private ScheduledExecutorService scheduled_executor;
    private InetSocketAddress endpoint;

    private ObjectOutputStream write;
    private ObjectInputStream read;

    private volatile boolean isRunning;
    private volatile boolean isConnected;

    public Client(String address, int port) throws IOException{
        this.socket = new Socket();
        this.socket.setReuseAddress(true);
        this.socket.bind(new InetSocketAddress(address, port));

        this.isRunning = false;
        this.isConnected = false;

        this.executor = Executors.newCachedThreadPool(); 
        this.scheduled_executor = Executors.newSingleThreadScheduledExecutor();
    }

    public void connect(String endAddress, int endPort) throws IOException{
        this.endpoint = new InetSocketAddress(endAddress, endPort); 
        this.socket.connect(this.endpoint);

        this.isRunning = true;
        this.isConnected = true;
        
        this.write = new ObjectOutputStream(this.socket.getOutputStream());
        this.read = new ObjectInputStream(this.socket.getInputStream());

        this.executor.submit(this::handleSend);
        this.executor.submit(this::handleReceive);
        this.scheduled_executor.scheduleAtFixedRate(()->{
            handleConnectionCheck();
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void closeClient(){
        System.out.println("Closing client");
        this.isRunning = false;
        this.isConnected = false;
        try{
            this.socket.close();
        }catch(IOException e){
            System.out.println(e.getMessage());
        }
        this.executor.shutdownNow();
        this.scheduled_executor.shutdownNow();
    }

    private void handleSend(){
        Scanner scanner = new Scanner(System.in);
        while(this.isRunning){
            try{
                String input = scanner.nextLine();
                if(input.equals("end")){
                    closeClient();
                    break;
                }
                DataObject sendData = DataObject.createMessage("client", input);
                this.write.writeObject(sendData);
                this.write.flush();
            }catch(IOException e){
                if(this.isRunning && this.isConnected){
                    System.out.println("HANDLE_SEND: " + e.getMessage());
                }else if(this.isRunning){
                    System.out.println("COULDN'T SEND MESSAGE");
                }
            }
        }
        scanner.close();
    }

    private void handleReceive(){
        try{
            while(this.isRunning){
                DataObject serverData = (DataObject)this.read.readObject();
                if(DataObject.isValidResponse(serverData)){
                    setConnected(true);
                }else{
                    System.out.println(serverData.getMessage());
                }
            }
        }catch(IOException e){
            if(!this.isRunning){
                System.out.println("HANDLE_RECEIVE: " + e.getMessage());
            }
        }catch(ClassNotFoundException e){
            System.out.println("HANDLE_RECEIVE: " + e.getMessage());
        }
    }

    private void handleConnectionCheck(){
        try{
            this.write.writeObject(DataObject.instantiateRequestPing());
            this.write.flush();
        }catch(IOException e){
            if(this.isRunning){
                setConnected(false);
            }else{
                System.out.println("HANDLE_CONNECTION_CHECK: " + e.getMessage());
            }
        }
    }

    private void setConnected(boolean value){
        if(this.isConnected != value){
            this.isConnected = value;
        }
    }

    public boolean isConnected(){
        return this.isConnected;
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client("localhost", 5555);
        client.connect("127.0.0.1", 6666);
    }
}
