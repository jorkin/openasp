package com.zfbots.asp;

import static org.junit.Assert.*;

import java.io.IOException;

import javax.servlet.ServletException;

import org.junit.Test;

import com.zfbots.asp.AspServer;


public class TestAspServer {

	@Test
	public void test() {
		try {
			AspServer.Transfer("/test.asp", null, null);
		} catch (ServletException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		fail("尚未实现");
	}

}
