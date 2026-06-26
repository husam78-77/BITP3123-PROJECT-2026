// First Commit - Ahmed Abdulrahman Ahmed Ali Gamel - B032320114
// git commit -m "Add NotificationProducerClient TCP producer for Library Booking - Ahmed B032320114"
package com.smartcampus.librarybooking.socket;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * R6 — DISTRIBUTED MESSAGING: TCP Socket Producer (Library Booking)
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * This class implements the <b>Producer</b> side of the Producer–Consumer
 * architecture for the Library Booking Service.
 *
 * <h3>Architecture Role</h3>
 * <pre>
 *   ┌───────────────────────┐          TCP (port 9999)          ┌─────────────────────────┐
 *   │  Library Booking      │ ──────────────────────────────────>│   Notification Service  │
 *   │  Service (PRODUCER)   │         LIBRARY messages          │   (CONSUMER)            │
 *   └───────────────────────┘                                   └─────────────────────────┘
 * </pre>
 *
 * <h3>Events Produced</h3>
 * <ul>
 *   <li>{@code LIBRARY:timestamp Student X borrowed book Y}</li>
 *   <li>{@code LIBRARY:timestamp Student X booked Room Y}</li>
 * </ul>
 *
 * <h3>Asynchronous Workflow</h3>
 * <p>The {@link #sendAsync(String, String)} method uses
 * {@link CompletableFuture#runAsync(Runnable)} to dispatch the TCP send
 * to a background thread.  This ensures that {@code borrowBook()} and
 * {@code bookRoom()} return immediately — notification delivery does not
 * block the HTTP response.</p>
 *
 * <h3>Connection Management</h3>
 * <ul>
 *   <li>Lazy initialization — connection opens on first send.</li>
 *   <li>Persistent — one connection is reused across sends.</li>
 *   <li>Auto-reconnect — if the connection drops, the next send
 *       re-establishes it.</li>
 *   <li>Graceful shutdown via {@code @PreDestroy}.</li>
 * </ul>
 * ═══════════════════════════════════════════════════════════════════════════
 */
@Component
public class NotificationProducerClient {

    private static final String NOTIFICATION_HOST = "localhost";
    private static final int NOTIFICATION_PORT = 9999;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private Socket socket;
    private PrintWriter writer;

    /**
     * Establishes a TCP connection to the Notification Socket Server.
     *
     * <p>The connection is created lazily on first use and reused for
     * subsequent messages.  If the connection is lost, it will be
     * re-established on the next send attempt.</p>
     */
    private synchronized void ensureConnected() {
        if (socket == null || socket.isClosed()) {
            try {
                socket = new Socket(NOTIFICATION_HOST, NOTIFICATION_PORT);
                writer = new PrintWriter(socket.getOutputStream(), true);
                System.out.println("[NotificationProducer-Library] Connected to Notification Server at "
                        + NOTIFICATION_HOST + ":" + NOTIFICATION_PORT);
            } catch (Exception e) {
                System.err.println("[NotificationProducer-Library] Failed to connect to Notification Server: "
                        + e.getMessage());
                socket = null;
                writer = null;
            }
        }
    }

    /**
     * R6 — ASYNCHRONOUS MESSAGE SEND.
     *
     * <p>Sends a notification message to the consumer asynchronously.
     * The calling method (e.g., {@code borrowBook}, {@code bookRoom})
     * returns immediately — message delivery happens on a background
     * thread via {@link CompletableFuture}.</p>
     *
     * @param type    the message type (e.g., "LIBRARY")
     * @param content the event description
     */
    public void sendAsync(String type, String content) {
        CompletableFuture.runAsync(() -> {
            try {
                ensureConnected();
                if (writer != null) {
                    // Serialize using the custom protocol: TYPE:TIMESTAMP CONTENT
                    String message = type + ":" + LocalDateTime.now().format(FORMATTER) + " " + content;

                    // Send over TCP (println adds newline for line-based framing)
                    writer.println(message);

                    System.out.println("[NotificationProducer-Library] Sent: " + message);
                }
            } catch (Exception e) {
                System.err.println("[NotificationProducer-Library] Failed to send message: " + e.getMessage());
                closeConnection();
            }
        });
    }

    /**
     * Closes the TCP connection and releases resources.
     */
    private synchronized void closeConnection() {
        try {
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception e) {
            // Ignore close errors
        } finally {
            writer = null;
            socket = null;
        }
    }

    /**
     * R6 — Graceful shutdown: closes the TCP connection when the
     * Spring application context is destroyed.
     */
    @PreDestroy
    public void shutdown() {
        closeConnection();
        System.out.println("[NotificationProducer-Library] Connection closed.");
    }
}
