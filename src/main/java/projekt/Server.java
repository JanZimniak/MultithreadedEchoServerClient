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

import projekt.visuals.Draw;
import projekt.enums.ConnectionState;

public class Server {

    private final int MAX_CLIENT_COUNT = 2;

    private ServerSocket socket;
    private volatile ConnectionState state;
    private ExecutorService executor = Executors.newCachedThreadPool(); 
    private InetSocketAddress bindpoint;
    private Draw draw;
    private final Object drawLock = new Object();
    private ConcurrentHashMap<String, Socket> clientsMap = new ConcurrentHashMap<>();
    private Semaphore semaphore = new Semaphore(MAX_CLIENT_COUNT);

    public Server(int port) throws IOException{
        this.socket = new ServerSocket();
        this.bindpoint = new InetSocketAddress(port);
        this.state = ConnectionState.OFFLINE;
        this.draw = new Draw();
        setupDraw();
    }

    public void start() throws IOException{
        this.socket.bind(this.bindpoint);
        this.state = ConnectionState.RUNNING;
        this.executor.submit(()->{
            getUserInput();
        });
        synchronized(this.drawLock){
            this.draw.redraw();
        }
        acceptClients();
    }

    private void setupDraw(){
        String serverHuge ="""
           ____                            
          / __/ ___   ____ _  __ ___   ____
         _\\ \\  / -_) / __/| |/ // -_) / __/
        /___/  \\__/ /_/   |___/ \\__/ /_/   """;


        synchronized(this.drawLock){
            this.draw.addConstMessage(serverHuge);
            this.draw.addConstMessage("Press <ENTER> to stop server.");
            this.draw.addConstMessage("Connected clients:");
        }
    }

    private void getUserInput(){
        Scanner sc = new Scanner(System.in);
        sc.nextLine();
        sc.close();
        closeServer();
    }

    private void closeServer(){
        this.state = ConnectionState.OFFLINE;

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
            while(this.state == ConnectionState.RUNNING){
                Socket clientSocket = this.socket.accept();
                if(!this.semaphore.tryAcquire()){
                    clientSocket.close();
                    continue;
                }
                this.clientsMap.put(getClientIdentifier(clientSocket), clientSocket);
                synchronized(this.drawLock){
                    this.draw.addConstMessage(getClientIdentifier(clientSocket));
                    this.draw.redraw();
                }
                this.executor.submit(() -> handleClient(clientSocket)); 
            }
        }catch(IOException e){
            if(this.state == ConnectionState.RUNNING){
                System.out.println(e.getMessage());
            }
        }
    }

    private void handleClient(Socket client){
        try(
            ObjectOutputStream writeToClient = new ObjectOutputStream(client.getOutputStream());
            ObjectInputStream fromClient = new ObjectInputStream(client.getInputStream());
        ){
            while(this.state == ConnectionState.RUNNING){
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
            synchronized(this.drawLock){
                this.draw.removeConstMessage(getClientIdentifier(client));
                this.draw.redraw();
            }
            try{
                client.close();
            }catch(IOException ignore){}
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
        if(args.length != 1){
            System.out.println("Wrong number of arguments: <port>");
        }else{
            Server server = new Server(Integer.valueOf(args[0]));
            server.start();
        }
    }
}
