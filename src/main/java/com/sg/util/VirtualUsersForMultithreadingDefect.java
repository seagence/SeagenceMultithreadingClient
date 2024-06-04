package com.sg.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.apache.hc.core5.http.message.BasicNameValuePair;

/**
 * Usage: java VirtualUsersForMultithreadingDefect numberOfVirtualUsers
 * totalNumberOfRequestsToSend
 * 
 * @author Srinivas
 *
 */
public class VirtualUsersForMultithreadingDefect {
	
	public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
		int numberOfVirtualUsers = 8;// default
		if (args.length == 1) {
			numberOfVirtualUsers = Integer.parseInt(args[0]);
		}
		int totalNumberOfRequestsToSend = 500;// default
		if (args.length == 2) {
			totalNumberOfRequestsToSend = Integer.parseInt(args[1]);
		}
//		System.out.println("Establish session");
//		establishSession();
//		System.out.println("Session established");
		System.out.println("Starting with " + numberOfVirtualUsers + " threads and " + totalNumberOfRequestsToSend
				+ " total requests");
		createVirtualUsersAdnSubmit(numberOfVirtualUsers, totalNumberOfRequestsToSend);
		System.out.println("Done...");
	}

	public static String establishSession() {

		try (final CloseableHttpClient httpclient = HttpClients.createDefault()) {

			HttpClientContext context = HttpClientContext.create();
			ClassicHttpRequest httpPost = ClassicRequestBuilder.post("http://localhost:8083")
					.setEntity(new UrlEncodedFormEntity(Arrays.asList())).build();
			httpPost.setHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			httpPost.setHeader("Accept-Language", "en-US,en;q=0.9");
			httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
			httpPost.setHeader("Origin", "http://localhost:8083");
			httpPost.setHeader("User-Agent",
					"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");

			String resultTemp = httpclient.execute(httpPost, context, response -> {
				String sessionId = null;
				org.apache.hc.core5.http.Header setCookieHeader = response.getHeader("Set-Cookie");
				if (setCookieHeader != null) {
					sessionId = setCookieHeader.getValue();
					sessionId = sessionId.substring(sessionId.indexOf("=") + 1/* , sessionId.indexOf(";") */);
					return sessionId;
				} else {
					return null;
				}
			});
			return resultTemp;
		} catch (IOException exc) {
			exc.printStackTrace();
		}
		return null;
	}

	public static void createVirtualUsersAdnSubmit(int numberOfVirtualUsers, int totalNumberOfRequestsToSend)
			throws IOException, InterruptedException, ExecutionException {
		ExecutorService executorService = Executors.newFixedThreadPool(numberOfVirtualUsers);
		List<Callable<String>> callables = create500AddItemToCartRequests(totalNumberOfRequestsToSend);
		executorService.invokeAll(callables);
		executorService.shutdown();
	}

	public static List<Callable<String>> create500AddItemToCartRequests(int totalNumberOfRequestsToSend) {
		int productIds[] = { 4, 5, 6, 7, 8, 9, 10, 11, 12, 13 };
		List<Callable<String>> callables = new ArrayList<Callable<String>>();
		String sessionId = null;
		for (int i = 0; i < totalNumberOfRequestsToSend; i++) {
			int productIdIndex = i % productIds.length;
			if(productIdIndex == 0) {
				sessionId = establishSession();
			}
			
			callables.add(new LocalCallable(productIds[productIdIndex], sessionId));
		}
		return callables;
	}
	
	static class LocalCallable implements Callable<String> {
		int productId;
		String sessionId;
		public LocalCallable(int productId, String sessionId) {
			this.productId = productId;
			this.sessionId = sessionId;
		}

		@Override
		public String call() {
			System.out.println("productIdIndex="+productId);
			System.out.println("sessionId="+sessionId);
		try (final CloseableHttpClient httpclient = HttpClients.createDefault()) {
				final BasicCookieStore cookieStore = new BasicCookieStore();

				BasicClientCookie cookie = new BasicClientCookie("JSESSIONID", sessionId);
				cookie.setDomain("localhost");
//				cookie.setAttribute("domain", "true");
				cookie.setPath("/");
				cookieStore.addCookie(cookie);

				HttpClientContext context = HttpClientContext.create();
				context.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);
				ClassicHttpRequest httpPost = ClassicRequestBuilder.post("http://localhost:8083/cart/add")
						.setEntity(new UrlEncodedFormEntity(
								Arrays.asList(new BasicNameValuePair("productId", "" + productId),
										new BasicNameValuePair("quantity", "1"),
										new BasicNameValuePair("hasProductOptions", "false"))))
						.build();
				httpPost.setHeader("Accept", "application/json, text/javascript, */*; q=0.01");
				httpPost.setHeader("Accept-Language", "en-US,en;q=0.9");
				httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
				httpPost.setHeader("Origin", "http://localhost:8083");
				httpPost.setHeader("User-Agent",
						"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");

				String resultTemp = httpclient.execute(httpPost, context, response -> {
					org.apache.hc.core5.http.Header header = response.getHeader("result");
					if (header != null && "success".equalsIgnoreCase(header.getValue())) {
						return "success";
					} else {
						return "fail";
					}
				});
			} catch (IOException exc) {
				exc.printStackTrace();
			}
			return null;
		};
		
	}
}
