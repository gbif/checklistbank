/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
