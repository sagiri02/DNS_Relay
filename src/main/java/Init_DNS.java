import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Properties;

public class Init_DNS {
    // default settings
    static boolean useCache = true;
    static int cacheDays = 3;
    static int poolSize = 10;
    static boolean useSimpleLog = true;

    //aliyun DNS sever
    static String remoteDNSServer = "223.5.5.5";

    public DatagramSocket init(){
        // init log
        Log log = new Log();
        log.addLog("initializing DNS_Relay");

        // load config
        Properties config = new Properties();
        try {
            FileInputStream Filein = new FileInputStream("boot.properties");
            config.load(Filein);
            Filein.close();
            useCache = Boolean.parseBoolean(config.getProperty("useCache", "true"));
            cacheDays = Integer.parseInt(config.getProperty("cacheDays", "3"));
            poolSize = Integer.parseInt(config.getProperty("poolSize", "20"));
            remoteDNSServer = config.getProperty("DNSServer", "223.5.5.5");
            useSimpleLog = Boolean.parseBoolean(config.getProperty("useSimpleLog","true"));
            log.addLog("finish initial configuration");
        } catch (IOException ignored) {
            log.addLog("failed to read file, use default settings");
        }


        //add the configuration to the log file
        byte[] buffer = new byte[1024];
        DatagramPacket request = new DatagramPacket(buffer, buffer.length);
        log.addLog("\tuseCache: " + useCache);
        log.addLog("\tcacheDay: " + cacheDays);
        log.addLog("\tpoolSize: " + poolSize);
        log.addLog("\tDNSServer: " + remoteDNSServer);
        log.writeLog();

        //create socket
        DatagramSocket socket;
        try {
            socket = new DatagramSocket(53);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        log.addLog("socket connected successfully");

        return socket;
    }
}