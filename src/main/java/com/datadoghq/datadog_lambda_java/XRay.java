package com.datadoghq.datadog_lambda_java;

import java.util.Map;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.xray.AWSXRay;
import org.jetbrains.annotations.NotNull;

class XRay {

    private Map<String,String> headers;
    private XRayHeader header;

    public XRay(@NotNull APIGatewayV2ProxyRequestEvent req){
        this.headers = req.getHeaders();
    }

    public XRay(@NotNull APIGatewayProxyRequestEvent req){
        this.headers = req.getHeaders();
    }


}

class XRayHeader {
    private String traceId;
    private String parentId;
    private String samplePriority;

    private String traceIdKey = "X-Amzn-Trace-Id";


    public XRayHeader buildHeader(Map<String, String> headers) throws Exception{
        if (headers = null){
            DD
            throw new Exception("API Gateway Request headers are null!");
        }

    }

}


