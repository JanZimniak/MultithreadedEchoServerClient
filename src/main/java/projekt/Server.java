package projekt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private ServerSocket socket;
    private volatile boolean isRunning;
    private ExecutorService executor;

    private InetSocketAddress bindpoint;

    public Server(int port) throws IOException{
        this.socket = new ServerSocket();
        this.bindpoint = new InetSocketAddress(port);
        this.isRunning = false;
        this.executor = Executors.newCachedThreadPool(); 
    }

    public void start() throws IOException{
        this.socket.bind(this.bindpoint);
        this.isRunning = true;
        acceptClients();
    }

    private void acceptClients() throws IOException{
        while(this.isRunning){
            Socket clientSocket = this.socket.accept();
            System.out.println("NEW CLIENT");
            this.executor.submit(() -> handleClient(clientSocket)); 
        }
    }

    private void handleClient(Socket client){
        try(
            ObjectInputStream fromClient = new ObjectInputStream(client.getInputStream());
            ObjectOutputStream writeToClient = new ObjectOutputStream(client.getOutputStream());
        ){
            DataObject clientData;

            while( (clientData = (DataObject)fromClient.readObject()) != null ){
                String clientMessage = clientData.getMessage();
                //System.out.println("FROM CLIENT: " + clientMessage);
                DataObject echoData = DataObject.createMessage("server_echo", "ECHO "+clientMessage); 
                writeToClient.writeObject(echoData);
            }
        }catch(IOException e){
            System.out.println("Client disconnected.");
        }catch(ClassNotFoundException e){
            System.out.println(e.getMessage());
        }
    }
    
    public static void main(String[] args) throws IOException {
        Server server = new Server(6666);
        server.start();
    }
}
