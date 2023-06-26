import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {
    //存储log的路径
    private final String path = "../output/log.txt";
    //log 文件
    private final File log;
    //创建buffer，默认为空
    private String buffer = "";
    //构造器
    public Log() {
        log = new File(path);
        if (!log.exists()) {
            try {
                log.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    //创建log缓存，链接字符串
    public void addLog(String logPiece) {
        System.out.println(logPiece);
        //补全字符串
        buffer = buffer.concat("\t").concat(logPiece).concat("\n");
    }

    //IO写入本地文件log.txt
    synchronized public void writeLog() {
        Date time = new Date();
        SimpleDateFormat form = new SimpleDateFormat("yy/MM/dd HH:mm:ss");
        String item = "****->[" + form.format(time) + "]<-****\n" + buffer + "\n";
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(log, true));
            bufferedWriter.write(item);
            bufferedWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        buffer = "";
    }
}
