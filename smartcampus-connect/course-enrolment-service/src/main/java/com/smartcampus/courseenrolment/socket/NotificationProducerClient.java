// First Commit - Ahmed Abdulrahman Ahmed Ali Gamel - B032320114
// git commit -m "Add NotificationProducerClient TCP producer for Course Enrolment - Ahmed B032320114"
package com.smartcampus.courseenrolment.socket;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * R6 — DISTRIBUTED MESSAGING: TCP Socket Producer (Course Enrolment)
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * This class implements the <b>Producer</b> side of the Producer–Consumer
 * architecture for the Course Enrolment Service.
 *
 * <h3>Architecture Role</h3>
 * <pre>
 *   ┌───────────────────────┐          TCP (port 9999)          ┌─────────────────────────┐
 *   │  Course Enrolment     │ ──────────────────────────────────>│   Notification Service  │
 *   │  Service (PRODUCER)   │         ENROLMENT messages        │   (CONSUMER)            │
 *   └───────────────────────┘                                   └─────────────────────────┘
 * </pre>
 *
 * <h3>How It Works</h3>
 * <ol>
 *   <li>On first use, the client opens a persistent TCP socket connection
 *       to the Notification Service running on {@code localhost:9999}.</li>
 *   <li>Messages are sent asynchronously using {@link CompletableFuture}
 *       so the calling method (e.g., {@code enrolStudent()}) returns
 *       immediately without waiting for notification delivery.</li>
 *   <li>Each message is serialized into the custom protocol format:
 *       {@code TYPE:TIMESTAMP CONTENT} and written as a single line
 *       terminated by {@code \n}.</li>
 *   <li>The connection is maintained across multiple sends (persistent
 *       connection) and closed gracefully on shutdown via
 *       {@code @PreDestroy}.</li>
 * </ol>
 *
 * <h3>Asynchronous Workflow</h3>
 * <p>The {@link #sendAsync(String, String)} method uses
 * {@link CompletableFuture#runAsync(Runnable)} to dispatch the TCP send
 * operation to a background thread.  This ensures that the enrolment
 * response is returned to the client immediately — the notification
 * is delivered in the background without blocking the HTTP response.</p>
 *
 * <h3>Message Format</h3>
 * <pre>
 *   ENROLMENT:2026-06-10T10:30:00 Student 1 enrolled in DAD3123
 * </pre>
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
                System.out.println("[NotificationProducer-Enrolment] Connected to Notification Server at "
                        + NOTIFICATION_HOST + ":" + NOTIFICATION_PORT);
            } catch (Exception e) {
                System.err.println("[NotificationProducer-Enrolment] Failed to connect to Notification Server: "
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
     * The calling method returns immediately — message delivery happens
     * on a background thread via {@link CompletableFuture}.</p>
     *
     * <p>This is the core of the asynchronous workflow: the enrolment
     * HTTP response is not delayed by notification processing.</p>
     *
     * @param type    the message type (e.g., "ENROLMENT")
     * @param content the event description (e.g., "Student 1 enrolled in DAD3123")
     */
    public void sendAsync(String type, String content) {
        CompletableFuture.runAsync(() -> {
            try {
                ensureConnected();
                if (writer != null) {
                    // Serialize the message using the custom protocol format
                    String message = type + ":" + LocalDateTime.now().format(FORMATTER) + " " + content;

                    // Send over TCP (println adds the newline for line-based framing)
                    writer.println(message);

                    System.out.println("[NotificationProducer-Enrolment] Sent: " + message);
                }
            } catch (Exception e) {
                System.err.println("[NotificationProducer-Enrolment] Failed to send message: " + e.getMessage());
                // Reset connection so next attempt reconnects
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
        System.out.println("[NotificationProducer-Enrolment] Connection closed.");
    }
}
