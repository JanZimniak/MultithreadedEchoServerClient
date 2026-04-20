package projekt;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;

import projekt.enums.ConnectionState;
import projekt.visuals.Draw;

public class Client {
    private Socket socket;
    private ExecutorService executor = Executors.newCachedThreadPool();
    private ScheduledExecutorService scheduled_executor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> scheduledTask;

    private InetSocketAddress endpoint;

    private ObjectOutputStream write;
    private ObjectInputStream read;

    private volatile ConnectionState state;

    private Draw draw = new Draw();
    private final Object drawLock = new Object();

    public Client() throws IOException{
        this.socket = new Socket();
        this.socket.setReuseAddress(true);
        setupDraw();
        setState(ConnectionState.RUNNING);

        synchronized(this.drawLock){
            this.draw.redraw();
        }
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

    private void setupDraw(){
        String serverHuge ="""
          _____   __   _              __ 
         / ___/  / /  (_) ___   ___  / /_
        / /__   / /  / / / -_) / _ \\/ __/
        \\___/  /_/  /_/  \\__/ /_//_/\\__/ """;


        synchronized(this.drawLock){
            this.draw.addConstMessage(serverHuge);
            this.draw.addConstMessage("Type 'end' to stop conversation.");
            this.draw.addConstMessage("Status: ");
            this.draw.addConstMessage(ConnectionState.OFFLINE.getState());
        }
    }

    private synchronized void setState(ConnectionState state){
        this.state = state;
        synchronized(this.drawLock){
            this.draw.popConstMessage();
            this.draw.addConstMessage(this.state.getState());
            this.draw.redraw();
        }
    }

    private synchronized void reconnect(){
        connectionCleanup();
        if(this.scheduledTask != null){
            this.scheduledTask.cancel(false);
            this.scheduledTask = null;
        }
        this.scheduledTask = this.scheduled_executor.scheduleAtFixedRate(()->{
            if(this.state != ConnectionState.OFFLINE){
                try{
                    startConnection();
                    this.scheduledTask.cancel(false);
                }catch(ConnectException e){
                    setState(ConnectionState.RUNNING);
                }catch(IOException e){
                    setState(ConnectionState.SERVER_BUSY);
                }
            }
        }, 2, 2, TimeUnit.SECONDS);
    }
    
    public synchronized void startConnection() throws IOException{
        connectionCleanup();
        this.socket = new Socket();
        this.socket.setReuseAddress(true);  
        this.socket.connect(this.endpoint);
        this.write = new ObjectOutputStream(this.socket.getOutputStream());
        this.read = new ObjectInputStream(this.socket.getInputStream());
        setState(ConnectionState.CONNECTED);
        this.executor.submit(this::handleReceive);
    }

    private synchronized void connectionCleanup(){
        try{
            if(this.socket != null){
                this.socket.close();
            }
        }
        catch(IOException ignore){}
    }

    public synchronized void closeClient(){
        setState(ConnectionState.OFFLINE);
        if(this.scheduledTask != null){
            this.scheduledTask.cancel(false);
            this.scheduledTask = null;
        }
        try{
            this.socket.close();
        }catch(IOException e){
            System.out.println(e.getMessage());
        }
        this.executor.shutdown();
        this.scheduled_executor.shutdown();
    }

    private void handleSend(){
        Scanner scanner = new Scanner(System.in);
        while(this.state != ConnectionState.OFFLINE){
            String input = scanner.nextLine();
            if(input.equals("end")){
                closeClient();
                break;
            }
            synchronized(this.drawLock){
                this.draw.addScrolledData(input);
            }
            boolean isSent = false;
            synchronized(this){
                if(this.state == ConnectionState.CONNECTED && this.write != null){
                    try{
                        DataObject sendData = DataObject.createMessage("client", input);
                        this.write.writeObject(sendData);
                        this.write.flush();
                        isSent = true;
                    }catch(IOException e){
                        if(this.state != ConnectionState.OFFLINE){
                            System.out.println(e.getMessage());
                        }
                    }
                }
            }
            if(!isSent){
                synchronized(this.drawLock){
                    this.draw.addScrolledData("#: MESSAGE NOT SENT, NOT CONNECTED");
                }
            }
        }
        scanner.close();
    }

    private void handleReceive(){
        try{
            while(this.state == ConnectionState.CONNECTED){
                DataObject serverData = (DataObject)this.read.readObject();
                String message = serverData.getMessage();

                int sizeSent = serverData.getMessageSize();
                int sizeRec = message != null ? message.getBytes(StandardCharsets.UTF_8).length : 0;
                String showMessage = message + " [SENT: " + sizeSent + ", GOT: " + sizeRec + "]";
                synchronized(this.drawLock){
                    this.draw.addScrolledData(showMessage);
                }
            }
        }catch(IOException | ClassNotFoundException e){
            if(this.state != ConnectionState.OFFLINE){
                setState(ConnectionState.RUNNING);
                reconnect();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        
        if(args.length != 2){
            System.out.println("Wrong number of arguments: <ip_address> <port>");
        }else{
            client.connect(args[0], Integer.valueOf(args[1]));
        }
    }
}
