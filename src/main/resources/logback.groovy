import ch.qos.logback.classic.encoder.PatternLayoutEncoder

//import ch.qos.logback.core.ConsoleAppender
// See http://logback.qos.ch/manual/groovy.html for details on configuration
//appender('STDOUT', ConsoleAppender) {
//    encoder(PatternLayoutEncoder) {
//        pattern = "%level %logger - %msg%n"
//    }
//}

def logDirectory = "."
//if (System.getenv("CCPD_LOG_DIR")) {
//    logDirectory = System.getenv("CCPD_LOG_DIR")
//}

appender("ROLLING", RollingFileAppender) {
    encoder(PatternLayoutEncoder) {
        Pattern = "%d{MM/dd/yyyy HH:mm:ss.SSS} %level %logger - %msg%n"
    }
    file = "${logDirectory}/logs/subscriber.log"
    rollingPolicy(FixedWindowRollingPolicy) {
        fileNamePattern = "${logDirectory}/logs/subscriber.%i.log"
        minIndex = 1
        maxIndex = 21
    }
    triggeringPolicy(SizeBasedTriggeringPolicy) {
        maxFileSize = "25MB"
    }
}

//root(INFO, ["STDOUT"])
root(INFO, ["ROLLING"])
