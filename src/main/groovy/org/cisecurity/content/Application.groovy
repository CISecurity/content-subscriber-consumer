package org.cisecurity.content

import groovy.cli.commons.CliBuilder
import org.cisecurity.dxl.ContentSubscriberEventListener
import org.cisecurity.dxl.DxlUtilities

import java.util.logging.Logger

class Application {
	final Logger log = Logger.getLogger(Application.class.getName())

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
			System.console().println "Shutting Down..."
			DxlUtilities.instance.disconnect()
		}

		new Application().start(args)
	}

	/**
	 * Initialize the command line options
	 */
	void initialize() {
		cli.with {
			c  longOpt: "config", "Custom path to the dxlclient.config file"
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
			if (options.c) {
				File dxlclientConfig = new File(options.c)
				if (!dxlclientConfig.exists()) {
					log.warning "DXL Client Config File does not exist at ${options.c}; Exiting."
					System.exit(1)
				} else {
					return dxlclientConfig.canonicalPath
				}
			} else {
				return null
			}
		}.call()

		// Connect the fabric
		DxlUtilities.instance.initialize(cfg)

		final String OPTIMUS_BENCHMARKS_SCAP_SIGNED = "/optimus/benchmarks/scap/signed"
		final String OPTIMUS_BENCHMARKS_JSON_SIGNED = "/optimus/benchmarks/json/signed"
		final String OPTIMUS_BENCHMARKS_YAML_SIGNED = "/optimus/benchmarks/yaml/signed"
		final String OPTIMUS_BENCHMARKS_XCAE_SIGNED = "/optimus/benchmarks/xccdfae/signed"


		// Register the subscriptions
		DxlUtilities.instance
			.registerSubscriptionCallback(
				OPTIMUS_BENCHMARKS_SCAP_SIGNED,             // Topic
				new ContentSubscriberEventListener("SCAP"), // Event Handler/Listener
				true)                                       // Auto-subscribe

		DxlUtilities.instance
			.registerSubscriptionCallback(
				OPTIMUS_BENCHMARKS_YAML_SIGNED,             // Topic
				new ContentSubscriberEventListener("YAML"), // Event Handler/Listener
				true)                                       // Auto-subscribe

		DxlUtilities.instance
			.registerSubscriptionCallback(
				OPTIMUS_BENCHMARKS_JSON_SIGNED,             // Topic
				new ContentSubscriberEventListener("JSON"), // Event Handler/Listener
				true)                                       // Auto-subscribe

		DxlUtilities.instance
			.registerSubscriptionCallback(
				OPTIMUS_BENCHMARKS_XCAE_SIGNED,                 // Topic
				new ContentSubscriberEventListener("XCCDF-AE"), // Event Handler/Listener
				true)                                           // Auto-subscribe

		println "Application is listening on topic '${OPTIMUS_BENCHMARKS_SCAP_SIGNED}'."
		println "Press Ctrl+C to stop this script or wait for events."

		while(true) { /* listen */ }
	}
}
