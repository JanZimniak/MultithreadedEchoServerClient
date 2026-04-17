package projekt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {
    private Socket socket;
    private ExecutorService executor;
    private InetSocketAddress bindpoint;
    private InetSocketAddress endpoint;

    private volatile boolean isRunning;

    public Client(String address, int port) throws IOException{
        this.socket = new Socket();
        this.bindpoint = new InetSocketAddress(address, port);
        this.socket.bind(this.bindpoint);
        this.isRunning = false;
        this.executor = Executors.newCachedThreadPool(); 
    }

    public void connect(String endAddress, int endPort) throws IOException{
        this.endpoint = new InetSocketAddress(endAddress, endPort); 
        this.socket.connect(this.endpoint);
        this.isRunning = true;
        this.executor.submit(() -> handleSend());
        this.executor.submit(() -> handleReceive());
    }

    public void close(){
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
            ObjectOutputStream write = new ObjectOutputStream(this.socket.getOutputStream());
           ){

            while(this.isRunning){
                String input = scanner.nextLine();
                DataObject sendData = DataObject.createMessage("client", input);
                write.writeObject(sendData);
            } 
        }catch(IOException e){
            System.out.println(e.getMessage());
        }
    }

    private void handleReceive(){
        try(
            ObjectInputStream read = new ObjectInputStream(this.socket.getInputStream());
           ){
            DataObject serverData;
            while( (serverData = (DataObject)read.readObject()) != null ){
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
