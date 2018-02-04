package com.rdnsn.b2intgr;

import com.google.common.net.HttpHeaders;
import java.nio.charset.StandardCharsets;

public interface Constants {
	public String UTF_8 = StandardCharsets.UTF_8.name();
	public String AUTHORIZATION = HttpHeaders.AUTHORIZATION;

	public String USER_FILE = "userFile";
	public String AUTH_RESPONSE = "authResponse";
	
	public String X_BZ_FILE_NAME = "X-Bz-File-Name";
	public String X_BZ_INFO_AUTHOR = "X-Bz-Info-Author";
	public String X_BZ_CONTENT_SHA1 = "X-Bz-Content-Sha1";

	public String DOWNLOAD_URL = "downloadUrl";
	public String TRNSNT_FILE_DESTDIR = "destDir";

	public long B2_TOKEN_TTL = (24 * 60 * 60) - 10;
	public String DIR_PATH = "path";
}
