package projekt;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
    private Socket socket;
<<<<<<< HEAD
=======
    private ExecutorService executor = Executors.newCachedThreadPool();
    private ScheduledExecutorService scheduled_executor = Executors.newSingleThreadScheduledExecutor();
>>>>>>> 0ce6508 (Removed ping, added reconnect when server is not found)
    private InetSocketAddress endpoint;

    private ObjectOutputStream write;
    private ObjectInputStream read;

    private volatile boolean isRunning;

    public Client() throws IOException{
        this.socket = new Socket();
        this.socket.setReuseAddress(true);
<<<<<<< HEAD
=======

        this.isRunning = true;
        this.isConnected = false;

        this.executor.submit(this::handleSend);
    }

    public void connect(String endAddress, int endPort){
        try{
            this.endpoint = new InetSocketAddress(endAddress, endPort); 
            startConnection();
        }catch(IOException e){
            reconnect();
        }
    }

    private void reconnect(){
        connectionCleanup();
        this.scheduled_executor.scheduleAtFixedRate(()->{
            if(!this.isConnected && this.isRunning){
                try{
                    System.out.println("RECONNECTING");
                    startConnection();
                }catch(IOException e){
                    System.out.println("FAILED TO RECONNECT");
                }
            }
        }, 2, 2, TimeUnit.SECONDS);
    }
    
    public void startConnection() throws IOException{
        connectionCleanup();
        this.socket = new Socket();
        this.socket.setReuseAddress(true);  
        this.socket.connect(this.endpoint);
        this.write = new ObjectOutputStream(this.socket.getOutputStream());
        this.read = new ObjectInputStream(this.socket.getInputStream());
        this.isConnected = true;
        this.executor.submit(this::handleReceive);
        System.out.println("CONNECTION SUCCESSFUL");
    }

    private void connectionCleanup(){
        try{
            if(this.socket != null){
                this.socket.close();
            }
        }
        catch(IOException ignore){}
>>>>>>> 0ce6508 (Removed ping, added reconnect when server is not found)
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
<<<<<<< HEAD
=======
        Scanner scanner = new Scanner(System.in);
        while(this.isRunning){
            String input = scanner.nextLine();
            if(input.equals("end")){
                closeClient();
                break;
            }
            if(!this.isConnected || this.write == null){
                System.out.println("MESSAGE NOT SENT, NOT CONNECTED");
                continue;
            }
            try{
>>>>>>> 0ce6508 (Removed ping, added reconnect when server is not found)
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
            while(this.isRunning && this.isConnected){
                DataObject serverData = (DataObject)this.read.readObject();
                String message = serverData.getMessage();

                int sizeSent = serverData.getMessageSize();
                int sizeRec = message != null ? message.getBytes(StandardCharsets.UTF_8).length : 0;
                System.out.println(message + " [SENT: " + sizeSent + ", GOT: " + sizeRec + "]");
            }
<<<<<<< HEAD
=======
        }catch(IOException | ClassNotFoundException e){
            if(!this.isRunning){
                System.out.println("HANDLE_RECEIVE: " + e.getMessage());
            }else{
                this.isConnected = false;
                reconnect();
            }
>>>>>>> 0ce6508 (Removed ping, added reconnect when server is not found)
        }
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        client.connect("127.0.0.1", 6666);
    }
}
