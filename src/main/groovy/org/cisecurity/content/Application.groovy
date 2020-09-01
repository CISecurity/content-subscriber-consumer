package org.cisecurity.content

import groovy.cli.commons.CliBuilder
import org.cisecurity.dxl.ContentSubscriberEventListener
import org.cisecurity.dxl.DxlUtilities
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Application {
	final Logger log = LoggerFactory.getLogger(Application.class)

	//
	// Command-Line Arguments
	// -c (--config) Path to dxlclient.config, if different from default
	//
	// connect to fabric
	// registerSubscriptionCallback for the EventListener

	def cli = new CliBuilder(usage: 'Application.[bat|sh] -[options] <extras>', width: 100)

	/**
	 * Entry point
	 * @param args
	 */
	static void main(String[] args) {
		// Allow for the user to stop the application via Ctrl+C or a SIGINT
		addShutdownHook {
			System.console().println ""
			System.console().println "Shutting Down the Application:"
			DxlUtilities.instance.disconnect()
			System.console().println "Application Shut Down."
		}

		new Application().start(args)
	}

	/**
	 * Initialize the command line options
	 */
	void initialize() {
		cli.with {
			c  longOpt: "config", type: String, "Custom path to the dxlclient.config file"
			k  longOpt: "key", type: String, "Custom path to SecureSuite member key file"
			s  longOpt: "scap", "Subscribe to SCAP content feed"
			y  longOpt: "yaml", "Subscribe to YAML content feed"
			j  longOpt: "json", "Subscribe to JSON content feed"
			x  longOpt: "xccdf", "Subscribe to XCCDF+AE content feed"
			h  longOpt: 'help', 'Show usage information'
		}

		StringWriter hdr = new StringWriter()
		hdr.println("---------------------------------------------------------------------------------------------------")
		hdr.println(" Options                                           Tip")
		hdr.println("---------------------------------------------------------------------------------------------------")

		cli.header = hdr.toString()
	}

	/**
	 * Start your engines
	 * @param args
	 */
	void start(String[] args) {

		// Initialize the CLI parser
		initialize()

		// Parse the command-line options
		def options = cli.parse(args)

		if (options.h) {
			cli.usage()
			System.exit(0)
		}

		def cfg = {
			if (options.hasOption("c")) {
				File dxlclientConfig = new File(options.c)
				if (!dxlclientConfig.exists()) {
					log.error "DXL Client Config File does not exist at ${options.c}; Exiting."
					System.exit(1)
				} else {
					return dxlclientConfig.canonicalPath
				}
			} else {
				return null
			}
		}.call()

		def key = {
			if (options.hasOption("k")) {
				File memberKey = new File(options.k)
				if (!memberKey.exists()) {
					log.error "CIS member key does not exist at ${options.k}; Exiting."
					System.exit(1)
				} else {
					return memberKey.canonicalPath
				}
			} else {
				return null
			}
		}.call()

		def banner = """
------------------------------------------------------------
   ______            __             __               
  / ____/___  ____  / /____  ____  / /_              
 / /   / __ \\/ __ \\/ __/ _ \\/ __ \\/ __/              
/ /___/ /_/ / / / / /_/  __/ / / / /_                
\\____/\\____/_/ /_/\\__/\\___/_/ /_/\\__/ __             
  / ___/__  __/ /_  _______________(_) /_  ___  _____
  \\__ \\/ / / / __ \\/ ___/ ___/ ___/ / __ \\/ _ \\/ ___/
 ___/ / /_/ / /_/ (__  ) /__/ /  / / /_/ /  __/ /    
/____/\\__,_/_.___/____/\\___/_/  /_/_.___/\\___/_/     
------------------------------------------------------------
		"""
		System.console().println banner

		// Connect the fabric
		DxlUtilities.instance.initialize(cfg, key)

		if (DxlUtilities.instance.isFabricConnected()) {
			boolean verified = DxlUtilities.instance.verifyLicense(true)

			if (verified) {
				final String OPTIMUS_BENCHMARKS_SCAP_SIGNED = "/optimus/benchmarks/scap/signed"
				final String OPTIMUS_BENCHMARKS_JSON_SIGNED = "/optimus/benchmarks/json/signed"
				final String OPTIMUS_BENCHMARKS_YAML_SIGNED = "/optimus/benchmarks/yaml/signed"
				final String OPTIMUS_BENCHMARKS_XCAE_SIGNED = "/optimus/benchmarks/xccdfae/signed"

				if (options.s || options.y || options.j || options.x) {
					System.console().println ""
					System.console().println "Application is subscribing to the following topics:"

					// Register the subscriptions
					if (options.s) {
						System.console().println " - ${OPTIMUS_BENCHMARKS_SCAP_SIGNED}"
						DxlUtilities.instance
							.registerSubscriptionCallback(
								OPTIMUS_BENCHMARKS_SCAP_SIGNED,             // Topic
								new ContentSubscriberEventListener("SCAP"), // Event Handler/Listener
								true)                                       // Auto-subscribe
					}

					if (options.y) {
						System.console().println " - ${OPTIMUS_BENCHMARKS_YAML_SIGNED}"
						DxlUtilities.instance
							.registerSubscriptionCallback(
								OPTIMUS_BENCHMARKS_YAML_SIGNED,             // Topic
								new ContentSubscriberEventListener("YAML"), // Event Handler/Listener
								true)                                       // Auto-subscribe
					}

					if (options.j) {
						System.console().println " - ${OPTIMUS_BENCHMARKS_JSON_SIGNED}"
						DxlUtilities.instance
							.registerSubscriptionCallback(
								OPTIMUS_BENCHMARKS_JSON_SIGNED,             // Topic
								new ContentSubscriberEventListener("JSON"), // Event Handler/Listener
								true)                                       // Auto-subscribe
					}

					if (options.x) {
						System.console().println " - ${OPTIMUS_BENCHMARKS_XCAE_SIGNED}"
						DxlUtilities.instance
							.registerSubscriptionCallback(
								OPTIMUS_BENCHMARKS_XCAE_SIGNED,                 // Topic
								new ContentSubscriberEventListener("XCCDF-AE"), // Event Handler/Listener
								true)                                           // Auto-subscribe
					}

					System.console().println "--------------------------------------------------------------------"
					System.console().println "Application is listening.  Press Ctrl+C to stop the Application."
					System.console().println "--------------------------------------------------------------------"

					while (true) {} /* listen */
				} else {
					System.console().println "Application was not configured to listen for content; Exiting."
				}
			} else {
				System.console().println "Application does NOT possess a valid CIS SecureSuite Member Key; Exiting."
			}
		} else {
			System.console().println "Application is NOT connected to the fabric; Cannot listen for events; Exiting."
		}
	}
}
