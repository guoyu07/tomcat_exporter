package nl.nlighten.prometheus.tomcat;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.*;

/**
 * Exports Tomcat <a href="http://tomcat.apache.org/tomcat-8.5-doc/jdbc-pool.html">jdbc-pool</a> metrics.
 * <p>
 * Example usage:
 * <pre>
 * {@code
 *   new TomcatJdbcPoolExports().register();
 * }
 * </pre>
 * Example metrics being exported:
 * <pre>
 *    tomcat_jdbc_connections_max{pool="jdbc/mypool"} 20.0
 *    tomcat_jdbc_connections_active_total{pool="jdbc/mypool"} 2.0
 *    tomcat_jdbc_connections_idle_total{pool="jdbc/mypool"} 6.0
 *    tomcat_jdbc_connections_total{pool="jdbc/mypool"} 8.0
 *    tomcat_jdbc_connections_threadswaiting_total{pool="jdbc/mypool"} 0.0
 * </pre>
 */

public class TomcatJdbcPoolExports extends Collector {

    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> mfs = new ArrayList<MetricFamilySamples>();
        try {
            final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName filterName = new ObjectName("tomcat.jdbc:class=org.apache.tomcat.jdbc.pool.DataSource,type=ConnectionPool,*");
            Set<ObjectInstance> mBeans = server.queryMBeans(filterName, null);

            if (mBeans.size() > 0) {
                List<String> labelList = Collections.singletonList("pool");

                GaugeMetricFamily maxActiveConnectionsGauge = new GaugeMetricFamily(
                        "tomcat_jdbc_connections_max",
                        "Maximum number of active connections that can be allocated from this pool at the same time",
                        labelList);

                GaugeMetricFamily activeConnectionsGauge = new GaugeMetricFamily(
                        "tomcat_jdbc_connections_active_total",
                        "Number of active connections allocated from this pool",
                        labelList);

                GaugeMetricFamily idleConnectionsGauge = new GaugeMetricFamily(
                        "tomcat_jdbc_connections_idle_total",
                        "Number of idle connections in this pool",
                        labelList);

                GaugeMetricFamily totalConnectionsGauge = new GaugeMetricFamily(
                        "tomcat_jdbc_connections_total",
                        "Total number of connections in this pool",
                        labelList);

                GaugeMetricFamily waitingThreadsCountGauge = new GaugeMetricFamily(
                        "tomcat_jdbc_waitingthreads_total",
                        "Number of threads waiting for connections from this pool",
                        labelList);

                for (final ObjectInstance mBean : mBeans) {
                    List<String> labelValueList = Collections.singletonList(mBean.getObjectName().getKeyProperty("name").replaceAll("[\"\\\\]", ""));

                    maxActiveConnectionsGauge.addMetric(
                            labelValueList,
                            ((Integer) server.getAttribute(mBean.getObjectName(), "MaxActive")).doubleValue());

                    activeConnectionsGauge.addMetric(
                            labelValueList,
                            ((Integer) server.getAttribute(mBean.getObjectName(), "Active")).doubleValue());

                    idleConnectionsGauge.addMetric(
                            labelValueList,
                            ((Integer) server.getAttribute(mBean.getObjectName(), "Idle")).doubleValue());

                    totalConnectionsGauge.addMetric(
                            labelValueList,
                            ((Integer) server.getAttribute(mBean.getObjectName(), "Size")).doubleValue());

                    waitingThreadsCountGauge.addMetric(
                            labelValueList,
                            ((Integer) server.getAttribute(mBean.getObjectName(), "WaitCount")).doubleValue());
                }

                mfs.add(maxActiveConnectionsGauge);
                mfs.add(activeConnectionsGauge);
                mfs.add(idleConnectionsGauge);
                mfs.add(totalConnectionsGauge);
                mfs.add(waitingThreadsCountGauge);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mfs;
    }

    public static boolean isTomcatJdbcUsed() {
        try {
            final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName filterName = new ObjectName("tomcat.jdbc:class=org.apache.tomcat.jdbc.pool.DataSource,type=ConnectionPool,*");
            Set<ObjectInstance> mBeans = server.queryMBeans(filterName, null);
            return !mBeans.isEmpty();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
