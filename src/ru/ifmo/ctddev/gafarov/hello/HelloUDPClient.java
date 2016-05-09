package ru.ifmo.ctddev.gafarov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Client that sending requests until it receives the correct response.
 * </p>
 * Client can be run from the command line.
 */
public class HelloUDPClient implements HelloClient {
    private static final int TIMEOUT = 100;
    private static final String USAGE =
            "HelloUDPClient <hostname> <port> <prefix> <threads amount> <requests amount on each thread>";
    /**
     * Creates instance of class.
     */
    public HelloUDPClient() {
    }

    /**
     * Main methdo to start from console
     * @param args command line arguments
     */
    public static void main(String[] args) {
        if (args.length != 5){
            System.out.println(USAGE);
            return;
        }
        try {
            int port = Integer.valueOf(args[1]);
            if (port < 0){
                System.out.println("port < 0");
                return;
            }
            int requests = Integer.valueOf(args[3]);
            if (port < 0){
                System.out.println("requests < 0");
                return;
            }
            int nThreads = Integer.valueOf(args[4]);
            if (port < 0){
                System.out.println("nThreads < 0");
                return;
            }
            HelloUDPClient client = new HelloUDPClient();
            client.start(args[0], port, args[2], requests, nThreads);
        } catch (NumberFormatException e){
            System.out.println("Port, number of requests or nThreads not a number");
        }
    }


    /**
     * Starts sending requests.
     *
     * @param host address to send requests to
     * @param port port to send requests to
     * @param  prefix prefix of each request
     * @param requests amount of the requests in each thread
     * @param nThreads number of the threads to perform sending on
     */
    @Override
    public void start(String host, int port, String prefix, int requests, int nThreads) {
        ExecutorService service = Executors.newFixedThreadPool(nThreads);
        try {
            InetAddress address = InetAddress.getByName(host);
            List<Callable<Object>> list = new ArrayList<>();
            for (int i = 0; i < nThreads; i++) {
                final int threadNumber = i;
                list.add(() -> {
                    DatagramSocket socket = new DatagramSocket();
                    socket.setSoTimeout(TIMEOUT);

                    byte[] buffer = new byte[socket.getReceiveBufferSize()];
                    for (int queryNumber = 0; queryNumber < requests; queryNumber++) {
                        String s = prefix + threadNumber + "_" + queryNumber;

                        byte[] sending_data = s.getBytes("UTF8");
                        DatagramPacket sending = new DatagramPacket(sending_data, sending_data.length, address, port);
                        DatagramPacket received = new DatagramPacket(buffer, buffer.length);

                        String req = "Hello, " + s, res = "";
                        while (!req.equals(res)) {
                            try {
                                System.out.println("Sent: " + s);
                                socket.send(sending);
                                try {
                                    socket.receive(received);
                                    res = new String(received.getData(), received.getOffset(), received.getLength());
                                    System.out.println("Received: " + res);
                                } catch (IOException e) {
                                    System.out.println("Error to receive packet: " + e.getMessage());
                                }
                            } catch (IOException e) {
                                System.out.println("Error to send packet: " + e.getMessage());
                            }
                        }
                    }
                    socket.close();
                    return null;
                });
            }
            service.invokeAll(list);
        } catch (InterruptedException e) {
            System.out.println("Worker is interrupted: " + e.getMessage());
        } catch (UnknownHostException e) {
            System.out.println("Unknown host: " + e.getMessage());
        } finally {
            service.shutdown();
        }
    }
}