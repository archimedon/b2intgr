package com.rdnsn.b2intgr;

import java.io.IOException;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

public class UploadAgent implements AggregationStrategy {
	
	private final MultipartAgent ma;
	private final ObjectMapper objectMapper;
	
	public UploadAgent(String url, Map<String, String> headers, ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		ma = new MultipartAgent( url, headers );
	}	
//	
//	 public UploadAuthResponse getUploadUrl() {
//			final MultipartAgent getupl = new MultipartAgent(
//					remoteAuthenticationUrl,
//					ImmutableMap.of("Authorization", "Basic " + basicAuthHeader)
//				);
//
//		if (auth == null) {
//			try {
//				auth = objectMapper.readValue(ma.doGet(), AuthResponse.class);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		return auth;
//	}
	
	public Exchange aggregate(Exchange original, Exchange resource) {
		if (original == null) {
            // the first time we only have the new exchange
            return resource;
        }
		auth = resource.getIn().getBody(AuthResponse.class);
		original.getIn().setHeader("remoteAuth", auth);
	    if (original.getPattern().isOutCapable()) {
	        original.getOut().setHeader("remoteAuth", auth);
	    }
	    return original;
	}

	//	
//	{
//	    "bucketId" : "4a48fe8875c6214145260818",
//	    "uploadUrl" : "https://pod-000-1005-03.backblaze.com/b2api/v1/b2_upload_file?cvt=c001_v0001005_t0027&bucket=4a48fe8875c6214145260818",
//	    "authorizationToken" : "2_20151009170037_f504a0f39a0f4e657337e624_9754dde94359bd7b8f1445c8f4cc1a231a33f714_upld"
//	}
//
//	public void process(Exchange exchange) throws Exception {
//		
//		exchange.getIn().setHeader("remoteAuth", getRemoteAuth());
//	    if (exchange.getPattern().isOutCapable()) {
//	    		exchange.getOut().setHeader("remoteAuth", auth);
//	    }
//	}
}
