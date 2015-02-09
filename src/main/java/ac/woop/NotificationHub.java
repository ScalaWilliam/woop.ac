package ac.woop;

import java.util.Set;
import javax.management.*;

public class NotificationHub
        extends NotificationBroadcasterSupport
        implements NotificationHubMBean, MBeanRegistration,
        NotificationEmitter, NotificationListener {

    private MBeanServer mbs;
    private ObjectName myObjectName;

    private int notificationCount;

    public NotificationHub() {
    }

    public int getNotificationCount() {
        return notificationCount;
    }

    public ObjectName preRegister(MBeanServer mbs, ObjectName name)
            throws Exception {

        this.mbs = mbs;
        this.myObjectName = name;

        // Subscribe to all existing ObjectNames that are broadcasters

        Set<ObjectName> objectNameSet =
                mbs.queryNames(new ObjectName("*:*"), null);

        for (ObjectName on : objectNameSet) {

            // We don't subscribe to any other NotificationHubs
            // as this could lead to an infinite loop!
            try {
                if (!mbs.isInstanceOf(on, this.getClass().getName())) {
                    mbs.addNotificationListener(on, this, null, null);
                }
            } catch (Exception e) {
                // Ignore exception as the current MBean in the list
                // might not be a notification broadcaster.
            }
        }
        return name;
    }

    public void postRegister(Boolean registrationDone) {
    }

    public void preDeregister() throws Exception {
        // Unsubscribe to all existing ObjectNames that are broadcasters

        Set<ObjectName> objectNameSet =
                mbs.queryNames(new ObjectName("*:*"), null);

        for (ObjectName on : objectNameSet) {

            // Try and unsubscribe from all mbeans
            try {
                mbs.removeNotificationListener(on, this);
            } catch (Exception e) {
                // Ignore exception as the current MBean in the list
                // might not be a notification broadcaster.
            }
        }
    }

    public void postDeregister() {
    }

    public void handleNotification(Notification notification,
                                   Object handback) {
        // If new MBeans are created then put listeners on them.

        String type = notification.getType();

        notificationCount++;

        if (type != null &&
                type.equals(MBeanServerNotification.REGISTRATION_NOTIFICATION)) {

            try {
                ObjectName on =
                        ((MBeanServerNotification)notification).getMBeanName();

                // Avoid loops by not subscribing to other Hubs.
                if (!mbs.isInstanceOf(on,
                        this.getClass().getName())) {
                    mbs.addNotificationListener(on,
                            this, null, null);
                }
            } catch (Exception e) {
                // Ignore exception as the new created MBean might
                // not be a notification broadcaster.
            }
        }
        // Deliver all notifications to anyone subscribed to us
        sendNotification(notification);
    }
}