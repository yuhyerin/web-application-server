package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import model.User;
import util.HttpRequestUtils;
import util.IOUtils;

public class RequestHandler extends Thread {
	private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

	private Socket connection;

	public RequestHandler(Socket connectionSocket) {
		this.connection = connectionSocket;
	}

	public void run() {
		log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
				connection.getPort());

		try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
			// TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
			BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			String line = br.readLine();
			log.debug("reqeust line : {}", line);
			if (line == null) {
				return;
			}

			String[] tokens = line.split(" ");
			int contentLength = 0;
			
			while (!line.equals("")) {
				line = br.readLine();
				log.debug("header : {}", line);
				if(line.contains("Content-Length")) {
					contentLength = getContentLength(line);
				}
				
			}

			// 요구사항2. GET방식으로 회원가입 하기 -> 요구사항3. POST방식으로 회원가입 하기
			String url = tokens[1];
			
			if ("/user/create".equals(url)) {
				// GET방식 
//				int index = url.indexOf("?");
//				String queryString = url.substring(index + 1);
//				Map<String, String> params = HttpRequestUtils.parseQueryString(queryString);
				
				// POST방식
				String body = IOUtils.readData(br, contentLength);
				Map<String, String> params = HttpRequestUtils.parseQueryString(body);
				User user = new User(params.get("userId"), params.get("password"), params.get("name"),
						params.get("email"));
				log.debug("User : {}", user);
				DataOutputStream dos = new DataOutputStream(out);
				response302Header(dos, "/index.html");
//				url = "/index.html"; // 회원가입 요청 완료 후 index.html 파일을 읽어 응답으로 보낸다.
				
			} else {
				DataOutputStream dos = new DataOutputStream(out);
//              byte[] body = "Hello World".getBytes();
				byte[] body = Files.readAllBytes(new File("./webapp" + tokens[1]).toPath());
				response200Header(dos, body.length);
				responseBody(dos, body);
			}
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

	private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
		try {
			dos.writeBytes("HTTP/1.1 200 OK \r\n");
			dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
			dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
			dos.writeBytes("\r\n");
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
	
	private void response302Header(DataOutputStream dos, String url) {
		try {
			dos.writeBytes("HTTP/1.1 302 Redirect \r\n");
			dos.writeBytes("Location: " + url + " \r\n");
			dos.writeBytes("\r\n");
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

	private void responseBody(DataOutputStream dos, byte[] body) {
		try {
			dos.write(body, 0, body.length);
			dos.flush();
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}
	
	private int getContentLength(String line) {
		String[] headerTokens = line.split(":");
		return Integer.parseInt(headerTokens[1].trim());
	}
}
