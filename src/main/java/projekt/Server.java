package projekt;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class Server {

    private ServerSocket socket;
    private volatile boolean isRunning;
    private ExecutorService executor = Executors.newCachedThreadPool(); 
    private InetSocketAddress bindpoint;
    private Draw draw;
    private ConcurrentHashMap<String, Socket> clientsMap = new ConcurrentHashMap<>();
    private Semaphore semaphore = new Semaphore(2);

    public Server(int port) throws IOException{
        this.socket = new ServerSocket();
        this.bindpoint = new InetSocketAddress(port);
        this.isRunning = false;
        this.draw = new Draw();
        setupDraw();
    }

    public void start() throws IOException{
        this.socket.bind(this.bindpoint);
        this.isRunning = true;
        this.executor.submit(()->{
            getUserInput();
        });
        this.draw.redraw();
        acceptClients();
    }

    private void setupDraw(){
        String serverHuge ="""
           ____                            
          / __/ ___   ____ _  __ ___   ____
         _\\ \\  / -_) / __/| |/ // -_) / __/
        /___/  \\__/ /_/   |___/ \\__/ /_/   """;


        this.draw.addConstMessage(serverHuge);
        this.draw.addConstMessage("Press <ENTER> to stop server.");
        this.draw.addConstMessage("Connected clients:");
    }

    private void getUserInput(){
        Scanner sc = new Scanner(System.in);
        sc.nextLine();
        sc.close();
        closeServer();
    }

    private void closeServer(){
        this.isRunning = false;

        try{
            this.socket.close();
        }catch(IOException e){
            System.out.println(e.getMessage());
        }

        this.clientsMap.forEach((k, clientSocket) -> {
            try{
                clientSocket.close();
            }catch(IOException ignore){}
        });
        
        this.executor.shutdownNow();
    }

    private void acceptClients(){
        try{
            while(this.isRunning){
                Socket clientSocket = this.socket.accept();
                if(!this.semaphore.tryAcquire()){
                    clientSocket.close();
                    continue;
                }
                this.clientsMap.put(getClientIdentifier(clientSocket), clientSocket);
                this.draw.addConstMessage(getClientIdentifier(clientSocket));
                this.draw.redraw();
                this.executor.submit(() -> handleClient(clientSocket)); 
            }
        }catch(IOException e){
            if(this.isRunning){
                System.out.println(e.getMessage());
            }
        }
    }

    private void handleClient(Socket client){
        try(
            ObjectOutputStream writeToClient = new ObjectOutputStream(client.getOutputStream());
            ObjectInputStream fromClient = new ObjectInputStream(client.getInputStream());
        ){
            while(this.isRunning){
                DataObject clientData = (DataObject)fromClient.readObject(); 
                if(DataObject.isValidRequest(clientData)){
                    writeToClient.writeObject(DataObject.instantiateResponsePing());
                    writeToClient.flush();
                }else{
                    writeToClient.writeObject(clientData);
                    writeToClient.flush();
                }
            }
        }catch(IOException e){
            this.clientsMap.remove(getClientIdentifier(client));
            this.draw.removeConstMessage(getClientIdentifier(client));
            try{
                client.close();
            }catch(IOException ignore){}
            this.draw.redraw();
        }catch(ClassNotFoundException e){
            System.out.println(e.getMessage());
        }finally{
            this.semaphore.release();
        }
    }

    private String getClientIdentifier(Socket client){
        return client.getInetAddress().toString() + ":" + String.valueOf(client.getPort());
    }
    
    public static void main(String[] args) throws IOException {
        Server server = new Server(6666);
        server.start();
    }
}
