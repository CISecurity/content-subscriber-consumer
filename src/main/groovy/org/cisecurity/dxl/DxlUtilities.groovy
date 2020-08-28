package org.cisecurity.dxl

import com.opendxl.client.DxlClient
import com.opendxl.client.DxlClientConfig
import com.opendxl.client.ServiceRegistrationInfo
import com.opendxl.client.callback.EventCallback
import com.opendxl.client.callback.RequestCallback
import com.opendxl.client.callback.ResponseCallback
import com.opendxl.client.exception.DxlException
import com.opendxl.client.message.Event
import com.opendxl.client.message.Message
import com.opendxl.client.message.Request
import com.opendxl.client.message.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Singleton
class DxlUtilities {
	final Logger log = LoggerFactory.getLogger(DxlUtilities.class)

	/**
	 * Keep an easily accessible reference to the CIS fabric
	 */
	private DxlClient dxlClient

	private synchronized boolean initialized = false
	private String dxlclientConfig = [
		System.properties["user.dir"], "dxl", "config", "dxlclient.config"].join(File.separator)
	private final long TIMEOUT = 5 * 1000 // 5sec


	/**
	 * Getter
	 */
	String getDxlClientConfig() {
		return dxlclientConfig
	}

	/**
	 * Configure the default connection to the CIS fabric
	 */
	def initialize(String customDxlClientConfig = null) {
		if (!initialized) {
			synchronized (DxlUtilities.class) {
				if (!initialized) {
					log.info "[START] - Initializing CIS DXL Client..."

					if (customDxlClientConfig) {
						dxlclientConfig = customDxlClientConfig
					}

					// Obtain the DXL Client to the CIS fabric.
					initialized = connectDxlClient(dxlclientConfig)
					if (initialized) {
						System.console().println "Connected to Communication Fabric."
					}

					log.info "[ END ] - CIS DXL Client [initialized = ${initialized}]"
				}
			}
		}
	}

	/**
	 * Given the configuration to connect to a DXL fabric, establish the connection
	 * and add it to the map of clients
	 * @param fabricClientConfig
	 * @return whether or not the dxl client is connected
	 */
	boolean connectDxlClient(def fabricClientConfig) {
		try {
			if (!fabricClientConfig) {
				throw new DxlException("Missing DXL Client Configuration File")
			}

			// Parse the dxlclient.config file
			DxlClientConfig clientConfig = DxlClientConfig.createDxlConfigFromFile(fabricClientConfig)

			// Max out the connection retries at 2
			clientConfig.setConnectRetries(2)

			// Max out the number of seconds to wait for connection to 10
			clientConfig.setConnectTimeout(10)

			// TEST: Set the client's keep-alive to 4 minutes (default is 30 min)
			//clientConfig.setKeepAliveInterval(4 * 60)

			// Create the client
			dxlClient = new DxlClient(clientConfig)

			// Connect to the fabric
			dxlClient.connect()

			// Connection successful?
			return dxlClient.isConnected()

		} catch (DxlException de) {
			log.error "DXL Exception", de
			return false
		}
	}

	/**
	 * Disconnect the DXL client
	 */
	def disconnect() {
		dxlClient.close()
	}

	/**
	 * Get the named connection to a DXL fabric.
	 * @param name
	 * @return the connected DXL session or null if the named session doesnt exist
	 */
	DxlClient getClient() { return dxlClient }

	/**
	 * whether or not the DXL fabric is connected
	 * @return T/F
	 */
	boolean isFabricConnected() {
		return (dxlClient ? dxlClient.isConnected() : false)
	}

	/**
	 * Determine if the (a) the fabric is still connected, and (b) if the topic
	 * is receiving requests
	 * @param topic
	 * @return T/F
	 */
	boolean isFabricAndTopicConnected(String topic) {
		boolean topicConnected = isFabricConnected() ? ping(topic) : false

		return (isFabricConnected() && topicConnected)
	}

	/**
	 * Determine if a service is (a) connected to a particular fabric, and (b)
	 * listening for requests
	 * @param fabric
	 * @param topic
	 * @return
	 */
	boolean ping(String topic) {
		log.info "[START] Pinging Service Topic --> ${topic}"
		boolean result = {
			if (isFabricConnected()) {
				try {
					final Request request = new Request(topic)
					request.setPayload("ping".getBytes(Message.CHARSET_UTF8))
					final Response response = dxlClient.syncRequest(request, TIMEOUT) // Only wait for a ping for 5 sec.

					boolean pingResult = (response.messageType != Message.MESSAGE_TYPE_ERROR)

					log.info " Fabric is connected"
					log.info " Topic is ${topic}"
					log.info " Topic is ${pingResult ? "" : "NOT "}listening."
					return pingResult
				} catch (DxlException dxlException) {
					log.error "Exception thrown PINGing topic ${topic}; Timeout?", dxlException
					return false
				}
			} else {
				log.info "Fabric is NOT connected."
				return false
			}
		}.call()

		log.info "[ END ] Pinging Service Topic --> ${topic}; Success --> ${result}"
		return result
	}

	/**
	 * Initiate a request to the fabric, invoking the service attached
	 * to the topic, but essentially dont care about any sort of response.
	 * This is the "fire-and-forget" scenario.
	 *
	 * @param topic
	 * @param payload
	 * @return
	 */
	def makeRequest(String topic, def payload) {
		return makeRequest(topic, payload, null)
	}

	/**
	 * Invoke the requested service
	 * @param topic
	 * @param payload
	 * @param responseListener (may be null)
	 * @return
	 */
//	def makeRequest(String topic, def payload, IResponseHandler responseListener, boolean bypassConnectionTest = false) {
//		log.info "[START] Request to Service Topic --> ${topic}"
//
//		boolean fabricAndTopicConnected = (bypassConnectionTest ?: isFabricAndTopicConnected(topic))
//		if (fabricAndTopicConnected) {
//			// Create the request message
//			final Request req = new Request(topic)
//
//			// Populate the request payload
//			req.setPayload(payload.getBytes(Message.CHARSET_UTF8))
//
//			log.info "Invoking synchronous request"
//
//			// Send the request
//			Response response = dxlClient.syncRequest(req)
//
//			// Callback to the response listener
//			if (responseListener) {
//				responseListener.onResponse(response)
//			}
//
//			def responseTypeError = (response.messageType == Response.MESSAGE_TYPE_ERROR)
//			log.info "Synchronous Request returned Error? ${responseTypeError}"
//
//			return responseTypeError
//		} else {
//			if (isFabricConnected()) {
//				log.error "[ERROR] The service listening on topic: '${topic}' is unavailable; Unable to make request."
//			} else {
//				// TODO: Fabric is not connected.  Fashion an ErrorResponse
//				log.error "[ERROR] The fabric is not connected; Unable to make request on topic: '${topic}'"
//			}
//			return true
//		}
//	}

	/**
	 * Make an asynchronous request, given a response callback to invoke once the
	 * response does come back.
	 * @param topic
	 * @param payload
	 * @param responseCallback
	 * @return
	 */
	def makeRequest(String topic, def payload, ResponseCallback responseCallback, boolean bypassConnectionTest = false) {
		log.info "[START] Async Request to Service Topic --> ${topic}"

		boolean fabricAndTopicConnected = (bypassConnectionTest ?: isFabricAndTopicConnected(topic))
		if (fabricAndTopicConnected) {
			// Create the request message
			final Request req = new Request(topic)

			// Populate the request payload
			req.setPayload(payload.getBytes(Message.CHARSET_UTF8))

			// Invoke the request
			if (responseCallback) {
				log.info "Invoking asynchronous request WITH response callback"
				dxlClient.asyncRequest(req, responseCallback)
			} else {
				log.info "Invoking asynchronous request WITHOUT response callback"
				dxlClient.asyncRequest(req)
			}

		} else {
			if (isFabricConnected()) {
				log.error "[ERROR] The service listening on topic: '${topic}' is unavailable; Unable to make request."
			} else {
				// TODO: Fabric is not connected.  Fashion an ErrorResponse
				log.error "[ERROR] The fabric is not connected; Unable to make request on topic: '${topic}'"
			}
		}
	}

	/**
	 * Publish an event payload to a named fabric on the specified topic
	 * @param fabric
	 * @param topic
	 * @param payload
	 * @return
	 */
	def publish(String topic, def payload) {
		log.info "[START] Publishing to Topic --> ${topic}"

		if (isFabricConnected()) {
			final Event publishEvent = new Event(topic)
			publishEvent.setPayload(payload.getBytes(Message.CHARSET_UTF8))
			dxlClient.sendEvent(publishEvent)
			log.info "[ END ] Event Published to Topic --> ${topic}"
		} else {
			// TODO: Fabric is not connected.
			log.error "[ERROR] FABRIC IS NOT CONNECTED; UNABLE TO PUBLISH PAYLOAD"
		}
	}

	/**
	 * Add the event callback to the topic but do not subscribe automatically
	 * @param topic
	 * @param eventCallback
	 * @return
	 */
	def registerSubscriptionCallback(String topic, EventCallback eventCallback, boolean subscribe = true) {
		if (isFabricConnected()) {
			log.info "[START] Adding subscription callback to topic: ${topic}"
			dxlClient.addEventCallback(topic, eventCallback, subscribe)
			log.info "[ END ] Adding subscription callback to topic: ${topic}"
		} else {
			log.error "Cannot add event callback to topic ${topic}; Fabric NOT CONNECTED"
		}
	}

	/**
	 * Subscribe to the named topic
	 * @param topic
	 * @return
	 */
	def subscribe(String topic) {
		if (isFabricConnected()) {
			log.info "[START] Adding subscription to topic: ${topic}"
			dxlClient.subscribe(topic)
			log.info "[ END ] Adding subscription to topic: ${topic}"
		} else {
			log.error "The application IS NOT connected to the fabric; "
			log.error "Subscription to topic ${topic} is unavailable."
		}
	}

	/**
	 * Unsubscribe from the named topic
	 * @param topic
	 * @return
	 */
	def unsubscribe(String topic) {
		if (isFabricConnected()) {
			log.info "[START] Removing subscription to topic: ${topic}"
			dxlClient.unsubscribe(topic)
			log.info "[ END ] Removing subscription to topic: ${topic}"
		} else {
			log.error "The application IS NOT connected to the fabric; "
			log.error "Unsubscription from topic ${topic} is unavailable."
		}
	}

	/**
	 * Send the response back to a requesting topic.
	 * @param response
	 * @return
	 */
	def respond(Response response) {
		if (isFabricConnected()) {
			log.info "[START] Sending response to topic: ${response.request.destinationTopic}"
			dxlClient.sendResponse(response)
			log.info "[ END ] Sending response to topic: ${response.request.destinationTopic}"
		} else {
			log.error "The application IS NOT connected to the fabric; "
			log.error "Sending response to topic ${response.request.destinationTopic} is unavailable."
		}
	}


	def serviceRegistrations = [:]

	/**
	 * Attach a request handler to a given topic, thereby configuring a "listener" for
	 * requests to a service.
	 * @param topic
	 * @param serviceName
	 * @param requestCallback
	 * @return
	 */
	def registerService(String topic, String serviceName, RequestCallback requestCallback) {
		if (isFabricConnected()) {
			log.info "[START] Registering service ${serviceName} on topic ${topic}"

			// Create service registration object
			final ServiceRegistrationInfo info = new ServiceRegistrationInfo(dxlClient, serviceName)

			// TEMP: Set TTL to 10 min to watch Service Re-Registration
			info.setTtl(10)

			// Add a topic for the service to respond to
			info.addTopic(topic, requestCallback)

			// Register the service with the fabric (wait up to 5 seconds for registration to complete)
			dxlClient.registerServiceSync(info, TIMEOUT)
			log.info "[ END ] Service ${serviceName} on topic ${topic} has been registered."

			serviceRegistrations[topic] = info
		} else {
			log.error "The application IS NOT connected to the fabric; "
			log.error "Service registration is unavailable."
		}
	}
}
