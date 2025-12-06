package p2p.net;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class DiscoveryServer {
    private static final int PORT = 8888;
    private static final ConcurrentHashMap<String, PeerInfo> peers = new ConcurrentHashMap<>();
    private static final List<PrintWriter> connectedClients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        ExecutorService pool = Executors.newCachedThreadPool();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Discovery Server started on port " + PORT);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    pool.execute(new ClientHandler(clientSocket));
                } catch (IOException e) {
                    System.err.println("Accept failed: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void broadcast(String msg) {
        for (PrintWriter writer : connectedClients) {
            try {
                writer.println(msg);
            } catch (Exception e) {
                // Ignore errors, will be cleaned up
            }
        }
    }

    public record PeerInfo(String username, String address, int port, long lastSeen) {
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));) {
                this.out = new PrintWriter(socket.getOutputStream(), true);
                connectedClients.add(out);

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    String[] parts = inputLine.split(" ", 3); // Limit split for message content
                    String command = parts[0];

                    if ("REGISTER".equalsIgnoreCase(command) && parts.length >= 3) {
                        String username = parts[1];
                        int port = Integer.parseInt(parts[2]);
                        String remoteAddress = socket.getInetAddress().getHostAddress();
                        peers.put(username, new PeerInfo(username, remoteAddress, port, System.currentTimeMillis()));
                        System.out.println("Registered: " + username + " at " + remoteAddress + ":" + port);
                        out.println("OK REGISTERED");
                    } else if ("GET_PEERS".equalsIgnoreCase(command)) {
                        StringBuilder peerList = new StringBuilder();
                        for (PeerInfo info : peers.values()) {
                            peerList.append(info.username).append(":").append(info.address).append(":")
                                    .append(info.port).append(",");
                        }
                        if (peerList.length() > 0)
                            peerList.setLength(peerList.length() - 1);
                        out.println("PEERS " + peerList.toString());
                    } else if ("HEARTBEAT".equalsIgnoreCase(command) && parts.length >= 2) {
                        String username = parts[1];
                        if (peers.containsKey(username)) {
                            PeerInfo old = peers.get(username);
                            peers.put(username,
                                    new PeerInfo(old.username, old.address, old.port, System.currentTimeMillis()));
                            out.println("OK HEARTBEAT");
                        }
                    } else if ("GLOBAL_MSG".equalsIgnoreCase(command) && parts.length == 3) {
                        String sender = parts[1];
                        String content = parts[2];
                        broadcast("GLOBAL_MSG " + sender + " " + content);
                    } else {
                        out.println("ERROR UNKNOWN_COMMAND");
                    }
                }
            } catch (Exception e) {
                // System.err.println("Handler error: " + e.getMessage());
            } finally {
                if (out != null)
                    connectedClients.remove(out);
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
