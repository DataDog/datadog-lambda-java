package com.serverless;

import com.datadoghq.datadog_lambda_java.DDLambda;
import java.util.Collections;

import org.apache.log4j.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;


public class ApiGatewayHandler implements RequestHandler<APIGatewayProxyRequestEvent, ApiGatewayResponse> {

	private static final Logger LOG = Logger.getLogger(Handler.class);

	@Override
	public ApiGatewayResponse handleRequest(APIGatewayProxyRequestEvent input, Context context) {
		DDLambda  ddl  = new DDLambda(input, context);
		LOG.info("received: " + input);
		Response responseBody = new Response("Go Serverless v1.x! Your function executed successfully!", null);
		ddl.finish();
		return ApiGatewayResponse.builder()
				.setStatusCode(200)
				.setObjectBody(responseBody)
				.setHeaders(Collections.singletonMap("X-Powered-By", "AWS Lambda & serverless"))
				.build();
	}


}
