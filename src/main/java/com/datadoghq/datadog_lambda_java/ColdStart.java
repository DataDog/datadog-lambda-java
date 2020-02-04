package com.datadoghq.datadog_lambda_java;

import com.amazonaws.services.lambda.runtime.Context;

class ColdStart {
    private static String coldRequestID;

    /**
     * Gets the cold_start status. The very first request to be made against this Lambda should be considered cold.
     * All others are warm.
     * @return true on the very first invocation, false otherwise
     */
    public synchronized static boolean getColdStart(Context cxt){
        if (cxt == null){
            DDLogger.getLoggerImpl().debug("Unable to determine cold_start: context was null");
            return false;
        }
        String reqId = cxt.getAwsRequestId();
        if (coldRequestID == null) {
            coldRequestID = reqId;
            return true;
        }
        if (coldRequestID.equals(reqId)){
            return true;
        }
        return false;
    }

    public synchronized static void resetColdStart(){
        coldRequestID = null;
    }

}
