package org.cisecurity.dxl

import com.opendxl.client.callback.EventCallback
import com.opendxl.client.message.Event
import com.opendxl.client.message.Message

class ContentSubscriberEventListener implements EventCallback {
	ContentSubscriber contentSubscriber

	ContentSubscriberEventListener(String subscriberCategory) {
		contentSubscriber = {
			switch (subscriberCategory) {
				case "SCAP":
					return new ScapContentSubscriber()
				case "YAML":
					return new YamlContentSubscriber()
				case "JSON":
					return new JsonContentSubscriber()
				case "XCCDF-AE":
					return new XccdfAeContentSubscriber()
				default:
					return new ScapContentSubscriber()
			}
		}.call()
	}

	/**
	 * Invoked when an {@link Event} has been received.
	 *
	 * @param event The {@link Event} message that was received
	 */
	@Override
	void onEvent(Event event) {
		def payloadString = new String(event.getPayload(), Message.CHARSET_UTF8)
		contentSubscriber.contentReceived(event.messageId, payloadString)
	}
}
