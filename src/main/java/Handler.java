import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Handler implements Runnable{
    DatagramPacket request;
    DatagramSocket socket;
    DNSCache cache;
    Log log;
    String remoteDNSServer;
    boolean useCache;

    public Handler(DatagramPacket request, DatagramSocket socket,
                   DNSCache cache, Log log, String remoteDNSServer, boolean useCache) {
        this.request = request;
        this.socket = socket;
        this.log = log;
        this.remoteDNSServer = remoteDNSServer;
        this.useCache = useCache;
        if (useCache)
            this.cache = cache;
    }

    public void run(){
        InetAddress sourceIP = request.getAddress();
        int sourcePort = request.getPort();
        Message messageReceived;
        try {
            messageReceived = new Message(request.getData());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Record question = messageReceived.getQuestion();
        String domain = question.getName().toString();
        boolean valid = true, useV6 = false, nop = false;

        int type = question.getType();
        switch (type) {
            // IPv4 question
            case 1 -> {
                log.addLog("****->using thread: [" + Thread.currentThread().getName() + "]<-**** " + "question for: " + domain + "(ipv4)");
            }
            // inverse dns
            case 12 -> {
                nop = true;
                log.addLog("****->using thread: [" + Thread.currentThread().getName() + "]<-**** " + "using inverse DNS");
            }
            // IPv6 question
            case 28 -> {
                useV6 = true;
                log.addLog("****->using thread: [" + Thread.currentThread().getName() + "]<-**** " + "question for domain: " + domain + "(ipv6)");
            }
            default -> {
                nop = true;
                log.addLog("****->using thread: [" + Thread.currentThread().getName() + "]<-**** " + "other protocol(not defined as an IP)");
            }
        }
        InetAddress answerIP= null;
        DatagramPacket response = null;

        if (useCache)
            answerIP = cache.getIPFromCache(domain + (useV6 ? ":IPv6" : ""));
        if (!nop && (answerIP != null)) {
            log.addLog("****->using thread: [" + Thread.currentThread().getName() + "]<-**** " + "extracted in cache");
        } else {
            if (!nop && useCache)
                log.addLog("****->using thread: [" + Thread.currentThread().getName() + "]<-**** " + "not in the cache");
            DatagramSocket relayPacket;
            try {
                relayPacket = new DatagramSocket();
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }
            byte[] relaybufferfer = messageReceived.toWire();
            InetAddress DNSSeverIP;
            try {
                DNSSeverIP = InetAddress.getByName(remoteDNSServer);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            DatagramPacket relayRequest = new DatagramPacket(relaybufferfer, relaybufferfer.length, DNSSeverIP, 53);
            byte[] bufferfer = new byte[1024];
            DatagramPacket relayResponse = new DatagramPacket(bufferfer, bufferfer.length);

            try {
                relayPacket.send(relayRequest);
                relayPacket.receive(relayResponse);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }relayPacket.close();

            if (nop) {
                response = relayResponse;
            } else {
                Message messageResponse;
                try {
                    messageResponse = new Message(relayResponse.getData());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                List<Record> records = messageResponse.getSection(Section.ANSWER);
                ArrayList<InetAddress> IPs = new ArrayList<>();
                for (Record record : records) {
                    if (!useV6 && record instanceof ARecord) {
                        // IPv4 records
                        ARecord aRecord = (ARecord) record;
                        try {
                            InetAddress IP = InetAddress.getByAddress(aRecord.getAddress().getAddress());
                            IPs.add(IP);
                        } catch (UnknownHostException e) {
                            throw new RuntimeException(e);
                        }
                    } else if (useV6 && record instanceof AAAARecord) {
                        // IPv6 records
                        AAAARecord aaaaRecord = (AAAARecord) record;
                        try {
                            InetAddress IP = InetAddress.getByAddress(aaaaRecord.getAddress().getAddress());
                            IPs.add(IP);
                        } catch (UnknownHostException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                if (IPs.size() == 0) {
                    log.addLog("[" + Thread.currentThread().getName() + "] " + "no IPv" + (useV6 ? 6 : 4) + " result from remote DNS");
                    valid = false;
                } else {
                    log.addLog("****->using thread: [" + Thread.currentThread().getName() + "]<-**** " + "in total " + IPs.size() + " result(s)");
                    answerIP = IPs.get(new Random().nextInt(IPs.size()));
                    if (useCache) {
                        boolean finalUseV6 = useV6;
                        String parentName = Thread.currentThread().getName();
                        Thread update = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                synchronized (cache.getCacheLock()) {
                                    if (cache.getIPFromCache(domain + (finalUseV6 ? ":IPv6" : "")) == null) {
                                        cache.addCacheToFile(domain + (finalUseV6 ? ":IPv6" : ""), IPs);
                                        log.addLog("****->[" + parentName + "-child]<-****" + "added & reloaded cache");
                                    }
                                }

                            }
                        });
                        update.start();
                    }
                }
            }
        }
        if (!nop) {
            Message messageTransmitted = messageReceived.clone();
            if (!valid || answerIP.toString().substring(1).equals("0.0.0.0")
                    || answerIP.toString().substring(1).equals("::")
                    || answerIP.toString().substring(1).equals("0:0:0:0:0:0:0:0")) {
                messageTransmitted.getHeader().setRcode(3);
                log.addLog("****->[" + Thread.currentThread().getName() + "]<-****" + "answer: doesn't exist domain");
            } else {
                Record answer;
                // IPv4 answer
                if (!useV6)
                    answer = new ARecord(question.getName(), question.getDClass(), 64, answerIP);
                    // IPv6 answer
                else
                    answer = new AAAARecord(question.getName(), question.getDClass(), 64, answerIP);
                messageTransmitted.addRecord(answer, Section.ANSWER);
                log.addLog("****->using thread: [" + Thread.currentThread().getName() + "]<-****" + "answer IP: " + answerIP.toString().substring(1));
            }
            byte[] buffer = messageTransmitted.toWire();
            response = new DatagramPacket(buffer, buffer.length);
        }
        response.setAddress(sourceIP);
        response.setPort(sourcePort);
        try {
            socket.send(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.writeLog();
    }

}
