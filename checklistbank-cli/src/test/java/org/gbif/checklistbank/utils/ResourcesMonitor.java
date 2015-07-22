package org.gbif.checklistbank.utils;

import java.util.TimerTask;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.google.common.base.Throwables;

import static java.lang.management.ManagementFactory.getPlatformMBeanServer;

/**
 * Monitors and reports regulary the number of open files and threads.
 */
public class ResourcesMonitor extends TimerTask {
    private MBeanServer jmx;
    private ObjectName osMBean;

    public ResourcesMonitor() {
        try {
            osMBean = ObjectName.getInstance("java.lang:type=OperatingSystem");
        } catch (MalformedObjectNameException e) {
            Throwables.propagate(e);
        }
        jmx = getPlatformMBeanServer();
    }

    @Override
    public void run() {
        System.out.println("OPEN FILES: " + getOpenFileDescriptorCount());
        System.out.println("NUMBER OF THREADS: " + Thread.getAllStackTraces().size());
    }

    public long getOpenFileDescriptorCount() {
        try {
            return (long) jmx.getAttribute(osMBean, "OpenFileDescriptorCount");
        } catch (Exception e) {
            Throwables.propagate(e);
        }
        return -1;
    }
}

