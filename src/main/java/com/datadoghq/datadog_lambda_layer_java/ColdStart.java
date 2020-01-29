package com.datadoghq.datadog_lambda_layer_java;

class ColdStart {
    private static boolean isColdStart = true;

    /**
     * Gets the cold_start status. The very first time we try to get cold_start, we expect cold_start to be true.
     * Otherwise, we expect it to be false.
     * @return true on the very first invocation, false otherwise
     */
    public synchronized static boolean getColdStart(){
        if (isColdStart){
            isColdStart = false;
            return true;
        }
        return false;
    }

    public synchronized static void resetColdStart(){
        isColdStart = true;
    }

}
