package ru.ifmo.rain.kurilenko.helloudp;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPClient implements HelloClient {
    private static final int UDP_TIMEOUT = 1000;

    public static void main(String[] args) {
        if (args.length < 5 || args[0] == null || args[2] == null) {
            System.out.println("Usage: HelloUDPClient address port request_prefix threads_count number_of_requests");
            return;
        }
        String address, requestPrefix;
        int port, threadsCount, numberOfRequests;
        try {
            address = args[0];
            port = Integer.valueOf(args[1]);
            requestPrefix = args[2];
            threadsCount = Integer.valueOf(args[3]);
            numberOfRequests = Integer.valueOf(args[4]);
        } catch (NumberFormatException e) {
            System.err.println("Can't parse number: " + e.getMessage());
            return;
        }
        HelloUDPClient udp = new HelloUDPClient();
        udp.run(address, port, requestPrefix, threadsCount, numberOfRequests);
    }


    @Override
    public void run(String addressString, int port, final String requestPrefix, int threadsCount, int numberOfRequests) {
        ExecutorService[] threads = new ExecutorService[threadsCount];
        for (int i = 0; i < threadsCount; i++) {
            threads[i] = Executors.newSingleThreadExecutor();
        }
        InetAddress address;
        try {
            address = InetAddress.getByName(addressString);
        } catch (UnknownHostException e) {
            System.err.println("Unable to find the host :" + addressString);
            return;
        }
        for (int j = 0; j < threadsCount; j++) {
            ExecutorService threadpool = threads[j];
            final int current = j;
            threadpool.submit(() -> {
                for (int i = 0; i < numberOfRequests; i++) {
                    DatagramSocket clientSocket;
                    int packetSize;
                    try {
                        clientSocket = new DatagramSocket();
                        clientSocket.setSoTimeout(UDP_TIMEOUT);
                        packetSize = clientSocket.getReceiveBufferSize();
                    } catch (SocketException e) {
                        System.err.println("An error occured while creating the socket: " + e.getMessage());
                        return;
                    }
                    String requestString = requestPrefix + current + "_" + i;
                    byte[] request = requestString.getBytes(Charset.forName("UTF-8"));
                    byte[] response = new byte[packetSize];
                    for(int attempts = 0; attempts < 10; attempts++) {
                        try {
                            DatagramPacket sendPacket = new DatagramPacket(request, request.length, address, port);
                            DatagramPacket receivePacket = new DatagramPacket(response, response.length);
                            clientSocket.send(sendPacket);
                            System.out.println("Message sent: " + requestString);
                            clientSocket.receive(receivePacket);
                            String input = new String(response, 0, receivePacket.getLength(), "UTF-8");
                            if(!input.equals("Hello, " + requestString)) {
                                //System.err.println("Attempt " + (attempts + 1) + " failed, trying again...");
                                continue;
                            }
                            System.out.println("Message received: " + input);
                        } catch (IOException e) {
                            System.err.println("Attempt " + (attempts + 1) + " failed, trying again...");
                            continue;
                        }
                        break;
                    }
                }
            });
        }
        for (ExecutorService thread: threads) {
            thread.shutdownNow();
        }
        for (ExecutorService thread: threads) {
            try {
                thread.awaitTermination(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                //
            }
        }
    }
}
