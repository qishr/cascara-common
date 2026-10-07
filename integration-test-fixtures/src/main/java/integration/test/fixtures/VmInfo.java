package integration.test.fixtures;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;

public class VmInfo {

    public long rtUptime;
    public String rtVendor;
    public String rtName;
    public String rtVersion;
    public long rtPid;

    public int processors;
    public double loadAverage;
    public String osName;
    public String osVArch;
    public String osVersion;

    public String[] domains;

    public long totalLoadedClasses;
    public long loadedClasses;
    public long unloadedClasses;


    public VmInfo() {
    }

    public void init() {
        OperatingSystemMXBean osmxb = ManagementFactory.getOperatingSystemMXBean();
        ClassLoadingMXBean clmxb = ManagementFactory.getClassLoadingMXBean();
        RuntimeMXBean rtmxb = ManagementFactory.getRuntimeMXBean();

        // ManagementFactory.getMemoryMXBean().setVerbose(true);

        rtUptime = rtmxb.getUptime();
        rtVendor = rtmxb.getVmVendor();
        rtName = rtmxb.getVmName();
        rtVersion = rtmxb.getVmVersion();
        rtPid = rtmxb.getPid();

        processors = osmxb.getAvailableProcessors();
        loadAverage = osmxb.getSystemLoadAverage();
        osName = osmxb.getName();
        osVArch = osmxb.getArch();
        osVersion = osmxb.getVersion();

        domains = ManagementFactory.getPlatformMBeanServer().getDomains();

        // clmxb.setVerbose(true);
        totalLoadedClasses = clmxb.getTotalLoadedClassCount();
        loadedClasses = clmxb.getLoadedClassCount();
        unloadedClasses = clmxb.getUnloadedClassCount();
    }
}
