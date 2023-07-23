package com.nowcoder.community;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class CommunityApplication {

	/*解决redis和elasticsearch都复用netty导致的报错*/
	@PostConstruct
	public void init(){
		/*Netty4Utils*/
		System.setProperty("es.set.netty.runtime.available.processprs","false");
	}


	public static void main(String[] args) {
		SpringApplication.run(CommunityApplication.class, args);
	}

}
