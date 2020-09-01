package org.cisecurity.dxl

import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.JobExecutionException

class MemberKeyVerifier implements Job {
	/**
	 * Scheduled job to re-verify the member's key
	 */
	@Override
	void execute(JobExecutionContext context) throws JobExecutionException {
		DxlUtilities.instance.verifyLicense()
	}
}
