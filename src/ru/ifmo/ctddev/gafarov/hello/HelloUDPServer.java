package ru.ifmo.ctddev.gafarov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Server that receives requests, processes them and sends the answer.
 * </p>
 * Can be run from command line
 */
public class HelloUDPServer implements HelloServer {
    private class Close{
        private ExecutorService service;

        private DatagramSocket socket;
        public Close(ExecutorService service, DatagramSocket socket) {
            this.service = service;
            this.socket = socket;
        }
        public void close(){
            service.shutdown();
            socket.close();
        }

    }

    private List<Close> list = new ArrayList<>();
    public static final int TIMEOUT = 1000;

    /**
     * Main method to start from console
     * @param args command line arguments
     */
    public static void main(String[] args) {
        HelloUDPServer server = new HelloUDPServer();
        server.start(6080, 3);
    }

    /**
     * Creates an instance
     */
    public HelloUDPServer() {}

    /**
     * Start listening
     * @param port
     * @param threads
     */
    @Override
    public void start(int port, int threads) {
        ExecutorService service = Executors.newFixedThreadPool(threads);
        try {
            DatagramSocket socket = new DatagramSocket(port);
            int bufferSize = socket.getReceiveBufferSize();
            list.add(new Close(service, socket));
            socket.setSoTimeout(TIMEOUT);

            for (int i = 0; i < threads; i++) {
                service.submit(() -> {
                    while (!Thread.interrupted()) {
                        DatagramPacket received = new DatagramPacket(new byte[bufferSize], bufferSize);
                        try {
                            socket.receive(received);
                            String str = new String(received.getData(), 0, received.getLength(), Charset.forName("UTF8"));
                            str = "Hello, " + str;
                            DatagramPacket sending = new DatagramPacket(str.getBytes(), str.getBytes().length, received.getAddress(), received.getPort());
                            try {
                                socket.send(sending);
                            } catch (IOException e){
                                System.out.println("Unable to send package: " + e.getMessage());
                            }
                        } catch (IOException e) {
                           // System.out.println("Unable to receive packet: " + e.getMessage());
                        }
                    }
                });
            }
        } catch (SocketException e) {
            System.out.println("Unable to create socket: " + e.getMessage());
        } finally {
            service.shutdown();
        }
    }

    /**
     *
     */
    @Override
    public void close() {
        list.forEach(Close::close);
    }
}
