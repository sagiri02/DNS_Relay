import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class DNSRelay {

    // default settings
    static boolean useCache = true;
    static int cacheDays = 3;
    static int poolSize = 20;

    //aliyun DNS sever
    static String remoteDNSServer = "223.5.5.5";

    public static void main(String[] args){
        DatagramSocket socket = new Init_DNS().init();
        useCache = Init_DNS.useCache;
        cacheDays =  Init_DNS.cacheDays;
        poolSize =  Init_DNS.poolSize;
        remoteDNSServer = Init_DNS.remoteDNSServer;
        // init log
        Log log = new Log();

        byte[] buffer = new byte[1024];
        DatagramPacket request = new DatagramPacket(buffer, buffer.length);

        DNSCache cache = new DNSCache();


        //DNS threads
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);
        //flush cache thread
        ScheduledExecutorService executorService =
                Executors.newScheduledThreadPool(1);
        executorService.scheduleAtFixedRate(() -> {
            cache.flushCacheFile(cacheDays);
            log.addLog("DNS cache is flushed");
            log.writeLog();
        }, 0, 1, TimeUnit.DAYS);

        // wait for cache loaded
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.addLog("server started");
        while (true) {
            try {
                socket.receive(request);
            } catch (IOException e) {
                e.printStackTrace();
            }
            pool.execute(new Handler(request, socket, cache, new Log(), remoteDNSServer, useCache));
        }
    }

}
