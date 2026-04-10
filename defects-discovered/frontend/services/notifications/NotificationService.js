/**
 * Request notification permission from the user.
 * Best practice: call this method in response to a user gesture (e.g., button click).
 */
export async function requestNotificationPermission() {
  if (!("Notification" in window)) {
    console.warn("This browser does not support desktop notifications.");
    return false;
  }

  try {
    const permission = await Notification.requestPermission();
    if (permission === 'granted') {
      console.log('Notification permission granted.');
      return true;
    } else {
      console.warn(`Notification permission was ${permission}.`);
      return false;
    }
  } catch (error) {
    console.error("Failed to request notification permission:", error);
    return false;
  }
}

/**
 * Send a desktop notification using the browser Notification API.
 * 
 * @param {string} title - The title of the notification.
 * @param {Object} options - Optional configuration options (e.g., body, icon).
 */
export function sendNotification(title, options = {}) {
  if (!("Notification" in window)) {
    console.warn("This browser does not support desktop notifications. Cannot send:", title, options.body);
    return;
  }

  if (Notification.permission === "granted") {
    new Notification(title, options);
  } else {
    console.warn("Notification permission is not granted. Cannot send:", title, options.body);
  }
}
