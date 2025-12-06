//**Authored by Shivam Kumar on 2025-12-06 with open source community ideas and services
// and with the help of my project team Tushar, Saurabh, and Shreya */

package p2p;

import p2p.net.PeerNode;
import p2p.net.DiscoveryServer;
import java.util.concurrent.*;
import java.util.List;

public class TestRunner {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting Test Runner...");

        // 1. Start Discovery Server
        ExecutorService pool = Executors.newCachedThreadPool();
        pool.execute(() -> DiscoveryServer.main(new String[] {}));
        Thread.sleep(1000); // Wait for server start

        // 2. Start Peer A
        System.out.println("Starting Peer A...");
        CompletableFuture<String> msgA = new CompletableFuture<>();
        PeerNode alice = new PeerNode("Alice", 30001,
                msg -> {
                    System.out.println("Alice received: " + msg);
                    msgA.complete(msg);
                },
                (req, cb) -> cb.accept(true), // Auto-accept requests
                (user, accepted) -> System.out.println("Alice feedback: " + user + " " + accepted),
                (req, cb) -> cb.accept(true), // Auto-accept files
                (sender, prog) -> {
                });
        alice.start();

        // 3. Start Peer B
        System.out.println("Starting Peer B...");
        CompletableFuture<String> msgB = new CompletableFuture<>();
        PeerNode bob = new PeerNode("Bob", 30002,
                msg -> {
                    System.out.println("Bob received: " + msg);
                    msgB.complete(msg);
                },
                (req, cb) -> cb.accept(true),
                (user, accepted) -> System.out.println("Bob feedback: " + user + " " + accepted),
                (req, cb) -> cb.accept(true),
                (sender, prog) -> {
                });
        bob.start();

        Thread.sleep(1000); // Wait for registration

        // 4. Discover Peers (Alice finds Bob)
        alice.fetchPeers(peers -> {
            System.out.println("Alice found peers: " + peers);
            if (peers.contains("Bob:127.0.0.1:30002")) {
                System.out.println("SUCCESS: Alice found Bob");
            } else {
                System.err.println("FAILURE: Alice did not find Bob");
                System.exit(1);
            }
        });

        Thread.sleep(1000);

        // 5. Connect and Chat
        System.out.println("Connecting Alice to Bob...");
        alice.connectToPeer("localhost", 30002, "Bob");

        Thread.sleep(1000); // Wait for handshake

        alice.sendMessage("Bob", "Hello Bob, this is Alice!");

        try {
            String received = msgB.get(5, TimeUnit.SECONDS);
            if ("Hello Bob, this is Alice!".equals(received)) {
                System.out.println("SUCCESS: Bob received encrypted message");
            } else {
                System.err.println("FAILURE: Message mismatch: " + received);
                System.exit(1);
            }
        } catch (TimeoutException e) {
            System.out.println("TEST FAILED: Message timeout.");
            System.exit(1);
        } catch (Exception e) { // Catch other potential exceptions from get()
            e.printStackTrace();
            System.exit(1);
        }
    }
}
