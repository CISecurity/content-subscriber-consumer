package org.cisecurity.dxl

import groovy.json.JsonSlurper
import groovyx.net.http.FromServer
import groovyx.net.http.HttpBuilder
import groovyx.net.http.optional.Download
import org.slf4j.LoggerFactory
import org.slf4j.Logger

import static groovyx.net.http.util.SslUtils.ignoreSslIssues

class ContentSubscriber {
	final Logger log = LoggerFactory.getLogger(ContentSubscriber.class)

	protected String downloadCategory

	/**
	 * (for now) This method makes the request to the URL and downloads the response
	 * to a byte array which is stored to the database.
	 * {
	 *     "benchmark": {
	 *         "benchmark_filename": "CIS_Microsoft_Windows_10_Blah_v1.0.0.zip",
	 *         "benchmark_url": "signed s3 url"
	 *     }
	 * }
	 * @param messageId
	 * @param payloadUrl
	 * @return
	 */
	def contentReceived(String messageId, String payloadString) {
		def receivedDate      = new Date()
		def receivedTimestamp = receivedDate.time

		final String DOWNLOAD_BASEPATH = [
			System.properties["user.dir"],
			"downloads",
			downloadCategory].join(File.separator)

		log.info "Receiving content for messageId: ${messageId}; date/time: ${receivedDate.format("MM/dd/YYYY HH:MM:SS")}"

		try {
			def json = new JsonSlurper().parseText(payloadString)
			def payloadFilename = json.benchmark."benchmark_filename"
			def payloadUrl      = json.benchmark."benchmark_url"

			if (DxlUtilities.instance.isMemberKeyVerified()) {
				def downloadDirname = [DOWNLOAD_BASEPATH, String.valueOf(receivedTimestamp)].join(File.separator)
				File downloadDir = new File(downloadDirname)
				if (!downloadDir.exists()) {
					downloadDir.mkdirs()
				}

				def downloadFilepath = [downloadDirname, payloadFilename].join(File.separator)
				File downloadFile = new File(downloadFilepath)

				log.info "Downloading file to ${downloadFilepath}"

				def result = HttpBuilder.configure {
					request.raw = payloadUrl
					ignoreSslIssues execution
				}.get {
					request.headers.'User-Agent' = 'Mozilla/5.0'

					Download.toFile(delegate, downloadFile)

					response.success { FromServer fs, Object body ->
						System.console().println "Received ${downloadCategory} content: ${payloadFilename} "
						" - Content successfully downloaded to ${downloadFilepath}"
					}
					response.failure { FromServer fs, Object body ->
						" - Content FAILED to download: ${fs.statusCode}/${fs.message}"
					}
				}
				log.info result
				log.info "Download complete."
			} else {
				System.console().println "Application does NOT possess a valid CIS SecureSuite Member Key; Download Failed."
			}
		} catch (Exception e) {
			log.error "Exception thrown", e
		}
	}
}

class ScapContentSubscriber extends ContentSubscriber {
	ScapContentSubscriber() {
		downloadCategory = "SCAP"
	}
}

class YamlContentSubscriber extends ContentSubscriber {
	YamlContentSubscriber() {
		downloadCategory = "YAML"
	}
}

class JsonContentSubscriber extends ContentSubscriber {
	JsonContentSubscriber() {
		downloadCategory = "JSON"
	}
}

class XccdfAeContentSubscriber extends ContentSubscriber {
	XccdfAeContentSubscriber() {
		downloadCategory = "XCCDF-AE"
	}
}