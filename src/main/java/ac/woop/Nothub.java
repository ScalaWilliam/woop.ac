package ac.woop;

import java.lang.management.ManagementFactory;
import java.util.Date;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.timer.Timer;

public class Nothub implements NotificationListener {


    public Nothub() {
        try {
            ObjectName on;

            on = new ObjectName("example:type=NotificationHubMBean");

            NotificationHubMBean nh = new NotificationHub();

            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

            mbs.registerMBean(nh, on);

            // Now add ourselves as a listener to the hub
            mbs.addNotificationListener(on, this, null, null);

            // Create a few timer mbeans that send regular notifications
            for (int i = 1 ; i < 4; i++) {
                on = new ObjectName("example:type=TimerMBean,name="+i);
                Timer timer = new Timer();
                timer.addNotification("TICK"+i, "tick from "+i,
                        null,
                        new Date(),
                        1000 * i);
                mbs.registerMBean(timer, on);
                timer.start();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void handleNotification(Notification notification, Object handback) {
        System.out.println("Got notification : "+notification);
    }

    public static void main(String[] args) {

        Nothub me = new Nothub();

        // wait for a keypress and exit
        try {
            int c = System.in.read();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        // Don't bother killing Timer threads, just exit
        System.exit(0);
    }
}
