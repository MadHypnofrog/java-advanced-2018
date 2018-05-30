package ru.ifmo.rain.kurilenko.helloudp;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class HelloUDPServer implements HelloServer {
    private static final String SERVER_HEADER = "Hello, ";
    private ExecutorService threadpool;
    private Semaphore sem;
    private DatagramSocket serverSocket;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: HelloUDPServer port thread_count");
            return;
        }
        int port, threadCount;
        try {
            port = Integer.valueOf(args[0]);
            threadCount = Integer.valueOf(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Can't parse number: " + e.getMessage());
            return;
        }
        HelloUDPServer udp = new HelloUDPServer();
        udp.start(port, threadCount);
    }

    public HelloUDPServer() {}

    @Override
    public void start(int port, int threadCount) {
        threadpool = Executors.newFixedThreadPool(threadCount + 1);
        sem = new Semaphore(threadCount);
        int packetSize;
        try {
            serverSocket = new DatagramSocket(port);
            packetSize = serverSocket.getReceiveBufferSize();
        } catch (SocketException e) {
            System.err.println("An error occured while creating the socket: " + e.getMessage());
            return;
        }
        threadpool.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                byte[] request = new byte[packetSize];
                DatagramPacket receivePacket = new DatagramPacket(request, request.length);
                try {
                    serverSocket.receive(receivePacket);
                } catch (IOException e) {
                    System.err.println("An error occured: " + e.getMessage());
                    continue;
                }
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                String requestString;
                try {
                    requestString = new String(request, 0, receivePacket.getLength(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    requestString = "";
                }
                final String requestStringFinal = requestString;
                try {
                    sem.acquire();
                } catch (InterruptedException e) {
                    //
                }
                threadpool.submit(() -> {
                    String responseString = SERVER_HEADER + requestStringFinal;
                    byte[] response = responseString.getBytes(Charset.forName("UTF-8"));
                    DatagramPacket sendPacket = new DatagramPacket(response, response.length, clientAddress, clientPort);
                    try {
                        serverSocket.send(sendPacket);
                    } catch (IOException e) {
                        //
                    }
                    sem.release();
                });
            }
        });
    }

    @Override
    public void close() {
        if(serverSocket != null) {
            serverSocket.close();
        }
        threadpool.shutdownNow();
        try {
            threadpool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //
        }
    }
}
