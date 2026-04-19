package projekt;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private ServerSocket socket;
    private volatile boolean isRunning;
    private ExecutorService executor;
    private InetSocketAddress bindpoint;

    private Draw draw;

    private ConcurrentHashMap<String, Socket> clientsMap;

    public Server(int port) throws IOException{
        this.socket = new ServerSocket();
        this.bindpoint = new InetSocketAddress(port);
        this.isRunning = false;
        this.executor = Executors.newCachedThreadPool(); 
        this.clientsMap = new ConcurrentHashMap<>();
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
        this.draw.addConstMessage("Server.");
        this.draw.addConstMessage("Press <ENTER> to stop server.");
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

        this.clientsMap.forEach((clientAddress, clientSocket) -> {
            try{
                clientSocket.close();
            }catch(IOException ignore){}
        });
        this.executor.shutdown();
    }

    private void acceptClients(){
        try{
            while(this.isRunning){
                Socket clientSocket = this.socket.accept();
                this.clientsMap.put(clientSocket.getInetAddress().toString(), clientSocket);
                this.draw.addConstMessage(clientSocket.getInetAddress().toString());
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
            System.out.println("Client disconnected.");
        }catch(ClassNotFoundException e){
            System.out.println(e.getMessage());
        }finally{
            this.clientsMap.remove(client.getInetAddress().toString());
            try{
                client.close();
            }catch(IOException ignore){}
            this.draw.redraw();
        }
    }
    
    public static void main(String[] args) throws IOException {
        Server server = new Server(6666);
        server.start();
    }
}
