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
			c  longOpt: "config", type: String, "Custom path to the dxlclient.config file"
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

		def banner = """
----------------------------------------------------------------------------------------------------------------------------------------------------
    ,o888888o.     8 888888888o 8888888 8888888888  8 8888          ,8.       ,8.       8 8888      88    d888888o.                                 
 . 8888     `88.   8 8888    `88.     8 8888        8 8888         ,888.     ,888.      8 8888      88  .`8888:' `88.                               
,8 8888       `8b  8 8888     `88     8 8888        8 8888        .`8888.   .`8888.     8 8888      88  8.`8888.   Y8                               
88 8888        `8b 8 8888     ,88     8 8888        8 8888       ,8.`8888. ,8.`8888.    8 8888      88  `8.`8888.                                   
88 8888         88 8 8888.   ,88'     8 8888        8 8888      ,8'8.`8888,8^8.`8888.   8 8888      88   `8.`8888.                                  
88 8888         88 8 888888888P'      8 8888        8 8888     ,8' `8.`8888' `8.`8888.  8 8888      88    `8.`8888.                                 
88 8888        ,8P 8 8888             8 8888        8 8888    ,8'   `8.`88'   `8.`8888. 8 8888      88     `8.`8888.                                
`8 8888       ,8P  8 8888             8 8888        8 8888   ,8'     `8.`'     `8.`8888.` 8888     ,8P 8b   `8.`8888.                               
 ` 8888     ,88'   8 8888             8 8888        8 8888  ,8'       `8        `8.`8888. 8888   ,d8P  `8b.  ;8.`8888                               
    `8888888P'     8 8888             8 8888        8 8888 ,8'         `         `8.`8888. `Y88888P'    `Y8888P ,88P'                               
                                                                                                                                                    
   d888888o.   8 8888      88 8 888888888o      d888888o.       ,o888888o.    8 888888888o.    8 8888 8 888888888o   8 8888888888   8 888888888o.   
 .`8888:' `88. 8 8888      88 8 8888    `88.  .`8888:' `88.    8888     `88.  8 8888    `88.   8 8888 8 8888    `88. 8 8888         8 8888    `88.  
 8.`8888.   Y8 8 8888      88 8 8888     `88  8.`8888.   Y8 ,8 8888       `8. 8 8888     `88   8 8888 8 8888     `88 8 8888         8 8888     `88  
 `8.`8888.     8 8888      88 8 8888     ,88  `8.`8888.     88 8888           8 8888     ,88   8 8888 8 8888     ,88 8 8888         8 8888     ,88  
  `8.`8888.    8 8888      88 8 8888.   ,88'   `8.`8888.    88 8888           8 8888.   ,88'   8 8888 8 8888.   ,88' 8 888888888888 8 8888.   ,88'  
   `8.`8888.   8 8888      88 8 8888888888      `8.`8888.   88 8888           8 888888888P'    8 8888 8 8888888888   8 8888         8 888888888P'   
    `8.`8888.  8 8888      88 8 8888    `88.     `8.`8888.  88 8888           8 8888`8b        8 8888 8 8888    `88. 8 8888         8 8888`8b       
8b   `8.`8888. ` 8888     ,8P 8 8888      88 8b   `8.`8888. `8 8888       .8' 8 8888 `8b.      8 8888 8 8888      88 8 8888         8 8888 `8b.     
`8b.  ;8.`8888   8888   ,d8P  8 8888    ,88' `8b.  ;8.`8888    8888     ,88'  8 8888   `8b.    8 8888 8 8888    ,88' 8 8888         8 8888   `8b.   
 `Y8888P ,88P'    `Y88888P'   8 888888888P    `Y8888P ,88P'     `8888888P'    8 8888     `88.  8 8888 8 888888888P   8 888888888888 8 8888     `88. 
----------------------------------------------------------------------------------------------------------------------------------------------------
		"""
		System.console().println banner

		// Connect the fabric
		DxlUtilities.instance.initialize(cfg)

		if (DxlUtilities.instance.isFabricConnected()) {

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


			System.console().println "Application is subscribed to the following topics:"
			System.console().println " - ${OPTIMUS_BENCHMARKS_SCAP_SIGNED}"
			System.console().println " - ${OPTIMUS_BENCHMARKS_JSON_SIGNED}"
			System.console().println " - ${OPTIMUS_BENCHMARKS_YAML_SIGNED}"
			System.console().println " - ${OPTIMUS_BENCHMARKS_XCAE_SIGNED}"
			System.console().println "----------------------------------------------------------------------------------------------------------------------------------------------------"
			System.console().println "Application is listening.  Press Ctrl+C to stop the Application."
			System.console().println "----------------------------------------------------------------------------------------------------------------------------------------------------"

			while (true) { /* listen */
			}
		} else {
			System.console().println "Application is NOT connected to the fabric; Cannot listen for events; Exiting."
		}
	}
}
