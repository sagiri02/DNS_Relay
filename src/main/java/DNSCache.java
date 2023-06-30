
import java.io.*;
import java.net.InetAddress;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DNSCache {

    private final File cache_File;
    private final String File_Path = "cache.txt";
    private final Map<String, Set<InetAddress>> cache;
    private final Object cache_Lock = new Object();

    public DNSCache() {
        cache_File = new File(File_Path);
        cache = new HashMap<>();
        if (!cache_File.exists()) {
            try {
                new FileOutputStream(cache_File);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public ArrayList<InetAddress> getIPFromCache(String domain) {
        synchronized (cache_Lock) {
            if (cache.containsKey(domain)) {
                ArrayList<InetAddress> res = new ArrayList<>();
                Set<InetAddress> ipArray = cache.get(domain);
                res.addAll(ipArray);
                int index = new Random().nextInt(ipArray.size());

                return res;
            }
        }
        return null;
    }
    public InetAddress getIPFromCache1(String domain) {
        synchronized (cache_Lock) {
            if (cache.containsKey(domain)) {
                Set<InetAddress> ipArray = cache.get(domain);
                int index = new Random().nextInt(ipArray.size());
                return (InetAddress) ipArray.toArray()[index];
            }
        }
        return null;
    }

    public void readCacheFromFile() {
        synchronized (cache_Lock) {
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(cache_File));
                String single_line;
                while ((single_line = bufferedReader.readLine()) != null) {
                    String[] contents = single_line.split(" ");
                    String[] ipArray= Arrays.copyOfRange(contents, 2, contents.length);
                    Set<InetAddress> ipSet = new HashSet<>();
                    for (String ip : ipArray) {
                        ipSet.add(InetAddress.getByName(ip));
                    }
                    cache.put(contents[1], ipSet);
                }
                bufferedReader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void addCacheToFile(String domain, ArrayList<InetAddress> ipArray) {
        synchronized (cache_Lock) {
            BufferedWriter bufferedWriter;
            try {
                bufferedWriter = new BufferedWriter(new FileWriter(cache_File, true));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Date date = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
            String single_line = formatter.format(date) + " " + domain;
            for (InetAddress ip : ipArray) {
                single_line += (" " + ip.toString().substring(1));
            }
            try {
                bufferedWriter.write(single_line);
                bufferedWriter.newLine();
                bufferedWriter.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        readCacheFromFile();
    }
    //flush the cashe,we can set the time that The cache lasts for 3 days
    public void flushCacheFile(int limit_day) {
        SimpleDateFormat formatOfDate = new SimpleDateFormat("yyyy/MM/dd");
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, -limit_day * 24);
        Date expireDate = calendar.getTime();
        String newCache = "";
        synchronized (cache_Lock) {
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(cache_File));
                String single_line;
                while ((single_line = bufferedReader.readLine()) != null) {
                    String[] contents = single_line.split(" ");
                    //if the contents equal the "Black_List",save it in cache
                    if (contents[0].equals("BlackList")) {
                        newCache = newCache + single_line + "\n";
                    } else
                    //if the contents equal the "Black_List",save it in cache
                    {
                        Date cacheDate = formatOfDate.parse(contents[0]);
                        if (cacheDate.after(expireDate)) {
                            newCache = newCache + single_line + "\n";
                        }
                    }
                }
                bufferedReader.close();
            } catch (IOException | ParseException e) {
                throw new RuntimeException(e);
            }
            try {
                //write new cache.
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(cache_File, false));
                bufferedWriter.write(newCache);
                bufferedWriter.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        readCacheFromFile();
    }

    public Object getCacheLock() {
        return cache_Lock;
    }
}


