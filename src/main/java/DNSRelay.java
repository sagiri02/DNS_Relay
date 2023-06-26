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
    static int cacheLimitInDays = 3;
    static int threadPoolSize = 10;

    //aliyun DNS sever
    static String remoteDnsServer = "223.5.5.5";

    public static void main(String[] args){
        // init log
        Log log = new Log();
        log.addLog("initializing DNS_Relay");

        // load config
        Properties config = new Properties();
        try {
            FileInputStream Filein = new FileInputStream("boot.properties");
            config.load(Filein);
            Filein.close();
            useCache = Boolean.parseBoolean(config.getProperty("use-cache", "true"));
            cacheLimitInDays = Integer.parseInt(config.getProperty("cache-limit-in-days", "3"));
            threadPoolSize = Integer.parseInt(config.getProperty("thread-pool-size", "10"));
            remoteDnsServer = config.getProperty("remote-dns-server", "223.5.5.5");
            log.addLog("config loaded");
        } catch (IOException ignored) {
            log.addLog("config load failed, use default settings");
        }

    }



}
