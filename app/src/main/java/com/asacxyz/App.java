package com.asacxyz;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

public class App implements RequestHandler<Map<String, Object>, Map<String, Object>> {
  private static final String PATH_PARAMETER_KEY = "rawPath";
  private static final String ERROR_VALUE = "error";
  private static final String DEFAULT_URL = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
  private static final String BUCKET_NAME = "urlshortener-s3-asacxyz";

  @Override
  public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
    String shortenedUrlCode = ((String) input.get(App.PATH_PARAMETER_KEY)).replace("/", "");
    if (shortenedUrlCode == App.ERROR_VALUE) return this.buildRedirectResponse(App.DEFAULT_URL);

    S3Client s3 = S3Client.builder().build();
    try (InputStream originalLinkObjectStream = s3.getObject(GetObjectRequest.builder().bucket(App.BUCKET_NAME).key(shortenedUrlCode + ".json").build())) {
      UrlData urlData = new ObjectMapper().readValue(originalLinkObjectStream, UrlData.class);

      if (System.currentTimeMillis() / 1000 < urlData.getExpirationTime()) return this.buildRedirectResponse(urlData.getOriginalUrl());
    } catch (Exception e) {
      this.log(e);
    }

    return this.buildRedirectResponse(App.DEFAULT_URL);
  }

  private Map<String, Object> buildRedirectResponse(String url) {
    Map<String, Object> response = new HashMap<>();
    response.put("statusCode", 302);

    Map<String, String> headers = new HashMap<>();
    headers.put("Location", url);
    response.put("headers", headers);
    return response;
  }

  private void log(Exception e) {
    e.printStackTrace();
  }
}