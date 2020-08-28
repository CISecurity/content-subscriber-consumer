package org.cisecurity.dxl

import com.opendxl.client.callback.EventCallback
import com.opendxl.client.message.Event
import com.opendxl.client.message.Message
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ContentSubscriberEventListener implements EventCallback {
	final Logger log = LoggerFactory.getLogger(ContentSubscriberEventListener.class)

	ContentSubscriber contentSubscriber

	ContentSubscriberEventListener(String subscriberCategory) {
		contentSubscriber = {
			switch (subscriberCategory) {
				case "SCAP":
					log.info "Creating SCAP Content Subscriber"
					return new ScapContentSubscriber()
				case "YAML":
					log.info "Creating YAML Content Subscriber"
					return new YamlContentSubscriber()
				case "JSON":
					log.info "Creating JSON Content Subscriber"
					return new JsonContentSubscriber()
				case "XCCDF-AE":
					log.info "Creating XCCDF+AE Content Subscriber"
					return new XccdfAeContentSubscriber()
				default:
					log.info "Creating (Default) SCAP Content Subscriber"
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
