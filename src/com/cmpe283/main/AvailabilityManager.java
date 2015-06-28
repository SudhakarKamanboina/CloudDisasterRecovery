package com.cmpe283.main;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.vmware.vim25.mo.Alarm;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.ServiceInstance;

public class AvailabilityManager
{

    public static ServiceInstance si, adminsi ;
    public static ManagedEntity[] mes = null;
    static ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    public static URL adminVCurl, vcenter_url;
    public static String URLname, username, password, adminURLname,username_19;
    public static Alarm alarm; //powerOffAlarm, powerOnAlarm;
    public static int repingCounter;
    
    /**
     * @param args
     */
    public static void main(String[] args)
    {
        Properties prop = getConfig();
        username = prop.getProperty("username");
        password = prop.getProperty("password");
        username_19 =prop.getProperty("username_19");
        int snapInterval = Integer.parseInt(prop.getProperty("snapInterval"));
        int pingInterval = Integer.parseInt(prop.getProperty("pingInterval"));
        
        try
        {
            vcenter_url = new URL(prop.getProperty("vcenter_url"));
            adminVCurl = new URL(prop.getProperty("adminVc_url"));
            si = VMHandlerFunctions.CreateServiceInstance(vcenter_url, username, password);
            mes = VMHandlerFunctions.getAllVM(si);
            if (mes.length > 0  || mes != null)
            {
                //Creating Alarm
                alarm = AlarmHandler.createAlarm(vcenter_url, username, password);
                
                //Start creating snapshots
                Runnable snapshot = new SnapshotFactory(vcenter_url, adminVCurl, username, username_19, password);
                executor.scheduleAtFixedRate(snapshot, 0, snapInterval, TimeUnit.MINUTES);
              
                Runnable pingVM = new PingVM(vcenter_url, username, password, alarm, repingCounter);
                executor.scheduleAtFixedRate(pingVM, 0, pingInterval, TimeUnit.SECONDS);
            }
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    private static Properties getConfig()
    {
        Properties prop = new Properties();
        InputStream input = null;

        try
        {
            input = new FileInputStream("config.properties");
            prop.load(input);
        }
        catch (FileNotFoundException e)
        {
            System.out.println("File Not Found");
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (input != null)
            {
                try
                {
                    input.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }

        }
        return prop;
    }

}
