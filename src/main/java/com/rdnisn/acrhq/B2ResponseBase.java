package com.rdnisn.acrhq;

import com.fasterxml.jackson.annotation.JsonProperty;

public class B2ResponseBase {
	
	@JsonProperty
	private String code;
	
	@JsonProperty
	private Integer status;
	
	@JsonProperty
	private String message;
}
