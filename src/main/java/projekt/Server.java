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

    private Set<Socket> clientsSet;

    public Server(int port) throws IOException{
        this.socket = new ServerSocket();
        this.bindpoint = new InetSocketAddress(port);
        this.isRunning = false;
        this.executor = Executors.newCachedThreadPool(); 
        this.clientsSet = ConcurrentHashMap.newKeySet();
    }

    public void start() throws IOException{
        this.socket.bind(this.bindpoint);
        this.isRunning = true;
        this.executor.submit(()->{
            getUserInput();
        });
        acceptClients();
    }

    private void getUserInput(){
        System.out.println("Press <ENTER> to stop server");
        Scanner sc = new Scanner(System.in);
        sc.nextLine();
        sc.close();
        closeServer();
    }

    private void closeServer(){
        System.out.println("Closing server");
        this.isRunning = false;

        try{
            this.socket.close();
        }catch(IOException e){
            System.out.println(e.getMessage());
        }

        for(Socket client : this.clientsSet){
            try{
                client.close();
            }catch(IOException ignore){}
        }
        this.executor.shutdown();
    }

    private void acceptClients(){
        try{
            while(this.isRunning){
                Socket clientSocket = this.socket.accept();
                this.clientsSet.add(clientSocket);
                System.out.println("NEW CLIENT " + clientSocket.getRemoteSocketAddress());
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
                    String clientMessage = clientData.getMessage();
                    DataObject echoData = DataObject.createMessage("server_echo", clientMessage); 
                    writeToClient.writeObject(echoData);
                    writeToClient.flush();
                }
            }
        }catch(IOException e){
            System.out.println("Client disconnected.");
        }catch(ClassNotFoundException e){
            System.out.println(e.getMessage());
        }finally{
            this.clientsSet.remove(client);
            try{
                client.close();
            }catch(IOException ignore){}
        }
    }
    
    public static void main(String[] args) throws IOException {
        Server server = new Server(6666);
        server.start();
    }
}
