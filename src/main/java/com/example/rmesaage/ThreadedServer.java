package com.example.rmesaage;

import com.example.rmesaage.Chat.Message;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadedServer {
    private static int PORT,PORTSERVER;
    private static int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors() / 2;
    private static List<ClientHandler> activeClients = new ArrayList<>();
    private static final Object activeClientsLock = new Object();
    private static String ServName;
    private static String ServKey;
    private static ThreadPoolExecutor Userexecutor,serverExecutor;
    private static Thread serverConnThread,userConnThread;
    private static databaseUtils databaseUtils = new databaseUtils();

    public static void main(String[] args) {
        ServName = args[0];
        ServKey = args[1];
        PORT = Integer.parseInt(args[2]);
        PORTSERVER = Integer.parseInt(args[3]);
        if (args.length!=4){
            THREAD_POOL_SIZE = Integer.parseInt(args[4]);
        }
        databaseUtils.registerServer(ServName,ServKey,getExternalIP()+":"+PORT+":"+PORTSERVER);
        Userexecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(THREAD_POOL_SIZE-1);
        serverExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(THREAD_POOL_SIZE-1);
        try {
            serverConnThreadStart();
            userConnThreadStart();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (!getExternalIP().equals("80.254.123.76") || PORT!=2511 || PORTSERVER!=2510){
                    try {
                        Socket regSocket = new Socket("80.254.123.76",2510);
                        ObjectOutputStream out = new ObjectOutputStream(regSocket.getOutputStream());
                        out.writeObject(new ServerMessage<>("Register",ServName,getExternalIP() + ":"+ PORT + ":" + PORTSERVER,ServKey));
                        out.flush();
                        ObjectInputStream in = new ObjectInputStream(regSocket.getInputStream());
                        System.out.println("Register = "+((ServerMessage<?>)in.readObject()).getData());

                        out.writeObject(new ServerMessage<>("SyncUsersDatabase",ServName,databaseUtils.getUsersDatabase(),ServKey));
                        out.flush();
                        System.out.println("SyncUsersDatabase = " + ((ServerMessage<?>)in.readObject()).getData());

                        out.writeObject(new ServerMessage<>("SyncServersDatabase",ServName,databaseUtils.getServerDatabase(),ServKey));
                        out.flush();
                        System.out.println("SyncServersDatabase = " + ((ServerMessage<?>)in.readObject()).getData());

                        out.writeObject(new ServerMessage<>("SyncSavedMessageDatabase",ServName,databaseUtils.getSavedMessageDatabase(),ServKey));
                        out.flush();
                        System.out.println("SyncSavedMessageDatabase = " + ((ServerMessage<?>)in.readObject()).getData());
                        regSocket.close();
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public static String getExternalIP() {
        try {
            URL url = new URL("http://checkip.amazonaws.com");
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            String externalIP = br.readLine().trim();
            br.close();
            return externalIP;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    private static void serverConnThreadStart() throws IOException {
        ServerSocket serverConnection = new ServerSocket(PORTSERVER);
        serverConnThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    try {
                        Socket socket = serverConnection.accept();
                        serverExecutor.submit(new ServerHandler(socket));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        serverConnThread.start();
    }
    private static void userConnThreadStart() throws IOException {
        ServerSocket userConnection = new ServerSocket(PORT);
        System.out.println("Server started");
        userConnThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    try {
                        Socket socket = userConnection.accept();
                        if (Userexecutor.getActiveCount()==Userexecutor.getMaximumPoolSize()){
                            ArrayList<com.example.rmesaage.databaseUtils.Server> ServersIps = databaseUtils.getServerDatabase();
                            for (int i = 0; i < ServersIps.size(); i++) {
                                String[] servData = ServersIps.get(i).getIp().split(":");
                                if (ServersIps.get(i).getServerName().equals(ServName) || ServersIps.get(i).getServerKey().equals(ServKey)) continue;
                                try {
                                    Socket serverTrySocket = new Socket(servData[0], Integer.parseInt(servData[2]));
                                    ObjectOutputStream out = new ObjectOutputStream(serverTrySocket.getOutputStream());
                                    out.writeObject(new ServerMessage<>("ApplyNewUser",ServName,null,ServKey));
                                    out.flush();
                                    ObjectInputStream in = new ObjectInputStream(serverTrySocket.getInputStream());
                                    ServerMessage<Boolean> response = (ServerMessage<Boolean>) in.readObject();
                                    if (response.getData().equals(true)){
                                        ObjectOutputStream outClient = new ObjectOutputStream(socket.getOutputStream());
                                        outClient.writeObject(new Response<>("Redirect",null,null,null,servData[0]+":"+servData[1],null));
                                        outClient.flush();
                                        outClient.close();
                                        serverTrySocket.close();
                                        break;
                                    }
                                    serverTrySocket.close();
                                }catch (Exception ignored){}
                            }
                        }else {
                            ObjectOutputStream outClient = new ObjectOutputStream(socket.getOutputStream());
                            outClient.writeObject(new Response<>("NoRedirect", null, null, null, null, null));
                            outClient.flush();
                            ClientHandler clientHandler = new ClientHandler(new ObjectInputStream(socket.getInputStream()), outClient);
                            addActiveClient(clientHandler);
                            Userexecutor.submit(clientHandler);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        userConnThread.start();
    }
    private static void addActiveClient(ClientHandler clientHandler) {
        synchronized (activeClientsLock) {
            activeClients.add(clientHandler);
        }
    }
    private static void removeActiveClient(ClientHandler clientHandler) {
        synchronized (activeClientsLock) {
            activeClients.remove(clientHandler);
        }
    }
    static class ClientHandler implements Runnable {
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private String username;

        public ClientHandler(ObjectInputStream in, ObjectOutputStream out) {
            this.in = in;
            this.out = out;
        }

        public String getUsername() {
            return username;
        }

        private void sendNewMessage(String sender, String recipient, String message) {
            synchronized (activeClientsLock) {
                for (ClientHandler activeClient : activeClients) {
                    if (activeClient.getUsername().equals(recipient)) {
                        activeClient.sendMessage(sender, message);
                        return;
                    }
                }
            }
                ArrayList<com.example.rmesaage.databaseUtils.Server> servers = databaseUtils.getServerDatabase();
                for (int i = 0; i < servers.size(); i++) {
                    String[] serverAdr = servers.get(i).getIp().split(":");
                    if (serverAdr.length==1 || servers.get(i).getServerName().equals(ServName) || servers.get(i).getServerKey().equals(ServKey)) continue;
                    try {
                        Socket socket = new Socket(serverAdr[0], Integer.parseInt(serverAdr[2]));
                        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                        out.writeObject(new ServerMessage<>("FindUser", ServName, recipient, ServKey));
                        out.flush();
                        if (((ServerMessage) in.readObject()).getData().equals(true)) {
                            out.writeObject(new ServerMessage<>("SendMessage", ServName, new Message(0, sender, message, null, recipient, null), ServKey));
                            out.flush();
                            out.close();
                            socket.close();
                            return;
                        }
                        socket.close();
                    } catch (IOException | ClassNotFoundException e) {
                        System.out.println("No connect");
                    }
                }
        }

        private void sendNewMessageImage(String sender, String recipient, ArrayList<byte[]> message) {
            synchronized (activeClientsLock) {
                for (ClientHandler activeClient : activeClients) {
                    if (activeClient.getUsername().equals(recipient)) {
                        activeClient.sendMessage(sender, message);
                        break;
                    }
                }
            }
        }

        private void sendMessage(String sender, String message) {
            try {
                out.writeObject(new Response<>("NewMessage", sender, null, username, message, "user"));
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void sendMessage(String sender, ArrayList<byte[]> message) {
            try {
                out.writeObject(new Response<>("NewMessage", sender, null, username, message, "user"));
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        private Response<?> parse(Response<?> request) {
            databaseUtils utils = new databaseUtils();
            switch (request.getComma()) {
                case "Register" -> {
                    boolean bool = utils.register(request.getUsername(), request.getPassword(), request.getType());
                    return new Response<>(bool);
                }
                case "Auth" -> {
                    boolean bool = utils.auth(request.getUsername(), request.getPassword());
                    return new Response<>(bool);
                }
                case "SendMessage" -> {
                    String sender = request.getUsername();
                    String recipient = request.getSendTo();
                    if (request.getData() instanceof String){
                        sendNewMessage(sender, recipient, (String)request.getData());
                    }else if (request.getData() instanceof ArrayList){
                        sendNewMessageImage(sender,recipient,(ArrayList<byte[]>)request.getData());
                    }
                    return new Response<>(true);
                }
                case "Search" -> {
                    boolean bool = utils.search(request.getUsername());
                    return new Response<>("Search",bool);
                }
                case "Sync" -> {
                    ArrayList<Message> res = utils.getChats(request.getUsername());
                    return new Response<>("Sync",res);
                }
            }
            return new Response<>(false);
        }

        @Override
        public void run() {
            Thread incomingThread = new Thread(() -> {
                try {
                    while (true) {
                        Response<?> request = (Response<?>) in.readObject();
                        if (request.getComma().equals("Exit")) break;
                        Response<?> response = parse(request);
                        out.writeObject(response);
                        out.flush();
                        username = request.getUsername();
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        in.close();
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    removeActiveClient(this);
                }
            });

            incomingThread.start();

            try {
                incomingThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    static class ServerHandler implements Runnable {
        private ObjectInputStream in;
        private ObjectOutputStream out;
    public ServerHandler(Socket socket) {
        try {
            this.in = new ObjectInputStream(socket.getInputStream());
            this.out = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

        private static ServerMessage<?> handleServerMessage(ServerMessage message) {
            databaseUtils databaseUtils = new databaseUtils();
            //Server Commands:
            //Register,SyncDatabases,ApplyNewUser,FindUser
            String command = message.getComma();
            String serverName = message.getServerName();
            switch (command) {
                case "Register":
                    return new ServerMessage<>("RegisterResult", ServName, databaseUtils.registerServer(serverName, message.getKey(), (String)message.getData()), ServKey);
                case "SyncUsersDatabase":
                    return new ServerMessage<>("SyncUsersDatabaseResult", ServName, databaseUtils.syncDatabases((ArrayList) message.getData()), ServKey);
                case "ApplyNewUser":
                    if (Userexecutor.getActiveCount()<Userexecutor.getMaximumPoolSize()){
                        return new ServerMessage<>("ApplyNewUserResult", ServName, true, ServKey);
                    }else{
                        return new ServerMessage<>("ApplyNewUserResult", ServName, false, ServKey);
                    }
                case "SyncServersDatabase":
                    return new ServerMessage<>("SyncServersDatabaseResult",ServName,databaseUtils.syncDatabases((ArrayList) message.getData()),ServKey);
                case "SyncSavedMessageDatabase":
                    return new ServerMessage<>("SyncSavedMessageDatabaseResult",ServName,databaseUtils.syncDatabases((ArrayList) message.getData()),ServKey);
                case "FindUser":
                            if (tryFindConnectedUser((String) message.getData())){
                                return new ServerMessage<>("FindUserResult",ServName,true,ServKey);
                            }else return new ServerMessage<>("FindUserResult",ServName,false,ServKey);
                case "SendMessage":
                    Message UserMessage = (Message)message.getData();
                    synchronized (activeClientsLock) {
                        for (ClientHandler clientHandler : activeClients) {
                            if (clientHandler.getUsername().equals(UserMessage.getSendTo())) {
                                clientHandler.sendNewMessage(UserMessage.getMessageUser(), UserMessage.getSendTo(), UserMessage.getText());
                                return new ServerMessage<>("SendMessageResult", ServName, true, ServKey);
                            }
                        }
                    }
                        return new ServerMessage<>("SendMessageResult",ServName,false,ServKey);

            }

            return null;
        }

        @Override
        public void run() {
        try {
            while (true){
                ServerMessage message = (ServerMessage) in.readObject();
                out.writeObject(handleServerMessage(message));
                out.flush();
            }

        }catch (Exception e) {
        }
    }
    }
    private static boolean tryFindConnectedUser(String recipient) {
        synchronized (activeClientsLock) {
            for (ClientHandler activeClient : activeClients) {
                if (activeClient.getUsername().equals(recipient)) {
                    return true;
                }
            }
        }
        return false;
    }

}