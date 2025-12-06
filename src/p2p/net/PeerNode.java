//**Authored by Shivam Kumar on 2025-12-06 with open source community ideas and services
// and with the help of my project team Tushar, Saurabh, and Shreya */

package p2p.net;

import p2p.security.SecurityUtils;
import java.io.*;
import java.net.*;
// import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import javax.crypto.SecretKey;

public class PeerNode {
    private final String username;
    private final int port;
    private final KeyPair keyPair;
    private Socket discoverySocket;
    private PrintWriter discoveryOut;
    private BufferedReader discoveryIn;
    private ServerSocket serverSocket;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final ConcurrentHashMap<String, PeerConnection> activeConnections = new ConcurrentHashMap<>();
    private final Consumer<String> onMessageReceived;
    private final java.util.function.BiConsumer<String, Consumer<Boolean>> onChatRequest;
    private final java.util.function.BiConsumer<String, Boolean> onChatFeedback;
    // Callback: sender, filename, size, confirmCallback -> user decides to accept
    private final java.util.function.BiConsumer<String, Consumer<Boolean>> onFileRequest;
    // Callback: sender, progress(0-100) or -1 for done
    private final java.util.function.BiConsumer<String, Integer> onFileProgress;
    private final List<Consumer<List<String>>> peerCallbacks = new CopyOnWriteArrayList<>();

    public PeerNode(String username, int port, Consumer<String> onMessageReceived,
            java.util.function.BiConsumer<String, Consumer<Boolean>> onChatRequest,
            java.util.function.BiConsumer<String, Boolean> onChatFeedback,
            java.util.function.BiConsumer<String, Consumer<Boolean>> onFileRequest,
            java.util.function.BiConsumer<String, Integer> onFileProgress) throws Exception {
        this.username = username;
        this.port = port;
        this.onMessageReceived = onMessageReceived;
        this.onChatRequest = onChatRequest;
        this.onChatFeedback = onChatFeedback;
        this.onFileRequest = onFileRequest;
        this.onFileProgress = onFileProgress;
        this.keyPair = SecurityUtils.generateRSAKeyPair();
    }

    public void start() throws IOException {
        // Start Server Socket for collecting P2P connections
        serverSocket = new ServerSocket(port);
        pool.execute(this::listenForPeers);

        // Connect to Discovery Server
        connectToDiscoveryServer();
    }

    private void connectToDiscoveryServer() throws IOException {
        discoverySocket = new Socket("localhost", 8888);
        discoveryOut = new PrintWriter(discoverySocket.getOutputStream(), true);
        discoveryIn = new BufferedReader(new InputStreamReader(discoverySocket.getInputStream()));

        // Start listening to Discovery Server
        pool.execute(() -> {
            try {
                String line;
                while ((line = discoveryIn.readLine()) != null) {
                    if (line.startsWith("PEERS ")) {
                        // Handled by fetchPeers technically, but we need to handle async updates if we
                        // want to change architecture.
                        // For now, fetchPeers does a request-response, so it might consume this line.
                        // PROBLEM: If we listen here, fetchPeers might hang waiting for line.
                        // SOLUTION: Global Listener should handle ALL inputs from discovery server.
                        // Refactor: fetchPeers callback should range-register or we just store peers
                        // locally.
                        // QUICK FIX for this iteration: Sync properly or separate channels.
                        // BETTER: Let the listener handle everything and update state.

                        // BUT: fetchPeers is designed as a blocking-ish request in current code.
                        // Since `discoveryIn` is shared, we must be careful.
                        // Current `fetchPeers` calls `readLine`. If we have a loop here, they will
                        // fight.
                        // DECISION: We will NOT loop `discoveryIn` here unless we refactor
                        // `fetchPeers`.
                        // Refactoring `fetchPeers` to be async event-driven is best.
                        handleDiscoveryMessage(line);
                    } else {
                        handleDiscoveryMessage(line);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // Register
        discoveryOut.println("REGISTER " + username + " " + port);
    }

    // We need to refactor fetchPeers to NOT readLine directly, but wait for update.
    // Simplifying: We will make fetchPeers just ASK, and the Listener will notify
    // callbacks.

    private void handleDiscoveryMessage(String line) {
        if (line.startsWith("PEERS ")) {
            String listStr = line.substring(6);
            List<String> peers = listStr.isEmpty() ? Collections.emptyList() : Arrays.asList(listStr.split(","));
            List<Consumer<List<String>>> callbacks = new ArrayList<>(peerCallbacks);
            peerCallbacks.clear();
            for (Consumer<List<String>> cb : callbacks)
                cb.accept(peers);
        } else if (line.startsWith("GLOBAL_MSG ")) {
            String[] parts = line.split(" ", 3);
            if (parts.length == 3) {
                onMessageReceived.accept("GLOBAL [" + parts[1] + "]: " + parts[2]);
            }
        }
    }

    public void fetchPeers(Consumer<List<String>> callback) {
        // Register callback and send request
        peerCallbacks.add(callback);
        if (discoveryOut != null) {
            synchronized (discoveryOut) {
                discoveryOut.println("GET_PEERS");
            }
        }
    }

    public void sendGlobalMessage(String content) {
        if (discoveryOut != null) {
            synchronized (discoveryOut) {
                discoveryOut.println("GLOBAL_MSG " + username + " " + content);
            }
        }
    }

    private void listenForPeers() {
        try {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                PeerConnection handler = new PeerConnection(socket, this);
                pool.execute(handler);
            }
        } catch (IOException e) {
            if (!serverSocket.isClosed())
                e.printStackTrace();
        }
    }

    public void connectToPeer(String host, int port, String remoteUsername) {
        pool.execute(() -> {
            try {
                Socket socket = new Socket(host, port);
                PeerConnection connection = new PeerConnection(socket, this);
                connection.initiateHandshake(remoteUsername);
                activeConnections.put(remoteUsername, connection);
                pool.execute(connection);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void sendMessage(String recipient, String message) {
        PeerConnection conn = activeConnections.get(recipient);
        if (conn != null) {
            conn.send(message);
        } else {
            System.err.println("No connection to " + recipient);
        }
    }

    public void sendFile(String targetUser, java.io.File file) {
        PeerConnection conn = activeConnections.get(targetUser);
        if (conn != null) {
            conn.sendFile(file);
        } else {
            onMessageReceived.accept("System: Not connected to " + targetUser);
        }
    }

    // Inner class for handling P2P connection
    private static class PeerConnection implements Runnable {
        private final Socket socket;
        private final PeerNode node;
        private BufferedReader in;
        private PrintWriter out;
        private SecretKey sessionKey;
        private String remoteUser;

        // File Transfer State
        private String currentlyReceivingFile;
        private java.io.FileOutputStream fileOut;
        private long fileSize;
        private long receivedBytes;
        private java.io.File pendingFile; // The file waiting to be sent

        public PeerConnection(Socket socket, PeerNode node) {
            this.socket = socket;
            this.node = node;
        }

        public void initiateHandshake(String remoteUser) {
            try {
                this.remoteUser = remoteUser;
                out = new PrintWriter(socket.getOutputStream(), true);

                // 1. Send Chat Request
                out.println("CHAT_REQUEST " + node.username);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendHandshakeInit() {
            // 2. Send our Public Key (After Accepted)
            try {
                String pubKeyStr = SecurityUtils.publicKeyToString(node.keyPair.getPublic());
                out.println("HANDSHAKE_INIT " + node.username + " " + pubKeyStr);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void send(String msg) {
            sendEncrypted("MESSAGE " + msg);
        }

        private void sendEncrypted(String message) {
            if (out != null) {
                try {
                    if (sessionKey != null) {
                        String encrypted = SecurityUtils.encryptAES(message, sessionKey);
                        out.println("ENC " + encrypted);
                    } else {
                        System.err.println("Cannot send encrypted message: Handshake not complete");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void sendFile(java.io.File file) {
            if (file.length() > 200 * 1024 * 1024) {
                node.onMessageReceived.accept("System: File too large (>200MB).");
                return;
            }
            pendingFile = file;
            // Send size first to handle spaces in filename correctly
            // Protocol: FILE_REQ size filename
            sendEncrypted("FILE_REQ " + file.length() + " " + file.getName());
        }

        private void startFileTransfer(java.io.File file) {
            node.pool.execute(() -> {
                try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                    byte[] buffer = new byte[4096]; // 4KB chunks
                    int bytesRead;
                    long totalSent = 0;
                    long totalSize = file.length();

                    while ((bytesRead = fis.read(buffer)) != -1) {
                        byte[] chunk = bytesRead == buffer.length ? buffer : java.util.Arrays.copyOf(buffer, bytesRead);
                        // Encrypt and Encode
                        String base64Chunk = Base64.getEncoder().encodeToString(chunk);
                        sendEncrypted("FILE_CHUNK " + base64Chunk);

                        totalSent += bytesRead;
                        int pct = (int) ((totalSent * 100) / totalSize);
                        // Optional: Feedback to sender about progress?
                        // Maybe local UI update? For now, we just send.
                        // node.onFileProgress.accept("Me->"+remoteUser, pct);

                        Thread.sleep(5); // Throttle slightly to not flood socket
                    }
                    sendEncrypted("FILE_END " + file.getName());
                    node.onMessageReceived.accept("System: File sent successfully.");
                } catch (Exception e) {
                    e.printStackTrace();
                    node.onMessageReceived.accept("System: File send failed.");
                }
            });
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                if (out == null)
                    out = new PrintWriter(socket.getOutputStream(), true);

                String line;
                while ((line = in.readLine()) != null) {
                    handleMessage(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void handleMessage(String line) {
            try {
                String cmdLine = line;
                if (line.startsWith("ENC ")) {
                    if (sessionKey == null) {
                        System.err.println("Received encrypted message but no session key!");
                        return;
                    }
                    String cipherText = line.substring(4);
                    cmdLine = SecurityUtils.decryptAES(cipherText, sessionKey);
                }

                String[] parts = cmdLine.split(" ", 3); // Split into type, arg1, arg2...
                String type = parts[0];

                if ("CHAT_REQUEST".equals(type)) {
                    String requestor = parts[1];
                    this.remoteUser = requestor;
                    node.activeConnections.put(requestor, this);

                    Consumer<Boolean> decision = accepted -> {
                        if (accepted) {
                            out.println("CHAT_ACCEPT");
                        } else {
                            out.println("CHAT_DENY");
                            try {
                                socket.close();
                            } catch (IOException e) {
                            }
                            node.activeConnections.remove(requestor);
                        }
                    };
                    node.onChatRequest.accept(requestor, decision);

                } else if ("CHAT_ACCEPT".equals(type)) {
                    sendHandshakeInit();
                    // We assume it's true, handshake will follow
                    node.onChatFeedback.accept(remoteUser, true);

                } else if ("CHAT_DENY".equals(type)) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                    }
                    node.activeConnections.remove(remoteUser);
                    node.onChatFeedback.accept(remoteUser, false);

                } else if ("HANDSHAKE_INIT".equals(type)) {
                    // HANDSHAKE_INIT [user] [pubKey]
                    String otherUser = parts[1];
                    String otherPubKeyStr = parts[2];
                    this.remoteUser = otherUser;
                    node.activeConnections.put(otherUser, this);

                    this.sessionKey = SecurityUtils.generateAESKey();
                    PublicKey otherPubKey = java.security.KeyFactory.getInstance("RSA")
                            .generatePublic(new java.security.spec.X509EncodedKeySpec(
                                    Base64.getDecoder().decode(otherPubKeyStr)));

                    byte[] encryptedSessionKey = SecurityUtils.encryptRSA(sessionKey.getEncoded(), otherPubKey);
                    String encryptedSessionKeyStr = Base64.getEncoder().encodeToString(encryptedSessionKey);

                    out.println("HANDSHAKE_RESPONSE " + node.username + " " + encryptedSessionKeyStr);

                } else if ("HANDSHAKE_RESPONSE".equals(type)) {
                    String otherUser = parts[1];
                    String encryptedSessionKeyStr = parts[2];
                    byte[] encryptedSessionKey = Base64.getDecoder().decode(encryptedSessionKeyStr);
                    byte[] sessionKeyBytes = SecurityUtils.decryptRSA(encryptedSessionKey, node.keyPair.getPrivate());
                    this.sessionKey = new javax.crypto.spec.SecretKeySpec(sessionKeyBytes, "AES");
                } else if ("MESSAGE".equals(type)) {
                    // Logic fixed: parts[1] only captured the first word if the message had spaces.
                    // We need the whole content after "MESSAGE ".
                    if (parts.length >= 2) {
                        String msgContent = cmdLine.substring(cmdLine.indexOf(' ') + 1);
                        node.onMessageReceived.accept(remoteUser + ": " + msgContent);
                    }
                } else if ("FILE_REQ".equals(type)) {
                    // Protocol Update: FILE_REQ size filename
                    // This avoids issues with spaces in filenames.
                    if (parts.length >= 3) {
                        try {
                            System.out.println("DEBUG: Received FILE_REQ " + cmdLine);
                            long size = Long.parseLong(parts[1]);
                            // Filename is everything after the size
                            // cmdLine is "FILE_REQ size filename"
                            // We can find the second space.
                            int firstSpace = cmdLine.indexOf(' ');
                            int secondSpace = cmdLine.indexOf(' ', firstSpace + 1);
                            String filename = cmdLine.substring(secondSpace + 1);

                            node.onFileRequest.accept(remoteUser + ":" + filename + ":" + size, (accepted) -> {
                                if (accepted) {
                                    sendEncrypted("FILE_ACK " + filename);
                                    try {
                                        currentlyReceivingFile = filename;
                                        fileSize = size;
                                        receivedBytes = 0;
                                        fileOut = new java.io.FileOutputStream("download_" + filename);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    sendEncrypted("FILE_DENY " + filename);
                                }
                            });
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid file size in FILE_REQ");
                        }
                    }

                } else if ("FILE_ACK".equals(type)) {
                    // FILE_ACK filename
                    // extract full filename
                    String filename = cmdLine.substring(9); // "FILE_ACK ".length()
                    if (pendingFile != null && pendingFile.getName().equals(filename)) {
                        startFileTransfer(pendingFile);
                        pendingFile = null;
                        // node.onMessageReceived.accept("System: User accepted file.");
                    }
                } else if ("FILE_CHUNK".equals(type)) {
                    if (fileOut != null) {
                        byte[] data = Base64.getDecoder().decode(parts[1]);
                        fileOut.write(data);
                        receivedBytes += data.length;
                        int pct = (int) ((receivedBytes * 100) / fileSize);
                        node.onFileProgress.accept(remoteUser, pct);
                    }
                } else if ("FILE_END".equals(type)) {
                    if (fileOut != null) {
                        fileOut.close();
                        fileOut = null;
                        String filename = cmdLine.substring(9); // "FILE_END ".length()
                        node.onFileProgress.accept(remoteUser, -1);
                        node.onMessageReceived.accept("System: File " + filename + " received.");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
